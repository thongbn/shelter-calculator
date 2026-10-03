package net.typeblog.shelter.plus.vault;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;

/**
 * Context-backed PIN enrollment, verification, and retry throttling.
 * Invoke {@link #verify(char[])} from a worker thread: Argon2 is deliberately
 * synchronous here so callers can choose their executor and lifecycle handling.
 */
public final class PinVault {
    private static final String PREFERENCES_NAME = "shelter_plus_pin_vault";
    private static final String KEY_RECORD = "verifier_record";
    private static final String KEY_FAILURES = "failure_count";
    private static final String KEY_RETRY_AT = "retry_at_epoch_millis";

    interface Store {
        Snapshot load();
        boolean save(String record, int failures, long retryAtEpochMillis);
    }

    interface WallClock {
        long currentTimeMillis();
    }

    static final class Snapshot {
        final String record;
        final int failures;
        final long retryAtEpochMillis;

        Snapshot(String record, int failures, long retryAtEpochMillis) {
            this.record = record;
            this.failures = failures;
            this.retryAtEpochMillis = retryAtEpochMillis;
        }
    }

    private final PinVerifier verifier;
    private final PinThrottle throttle;
    private final Store store;
    private final WallClock wallClock;
    private volatile String record;

    public PinVault(Context context) {
        this(new SharedPreferencesStore(requireApplicationContext(context)),
                new PinVerifier(), new PinThrottle(), new WallClock() {
                    @Override public long currentTimeMillis() { return System.currentTimeMillis(); }
                });
    }

    PinVault(Store store, PinVerifier verifier, PinThrottle throttle, WallClock wallClock) {
        if (store == null || verifier == null || throttle == null || wallClock == null) {
            throw new IllegalArgumentException("dependencies must not be null");
        }
        this.store = store;
        this.verifier = verifier;
        this.throttle = throttle;
        this.wallClock = wallClock;
        Snapshot snapshot = store.load();
        if (snapshot != null && PinVerifier.isRecordSupported(snapshot.record)) {
            record = snapshot.record;
            long remaining = snapshot.retryAtEpochMillis - wallClock.currentTimeMillis();
            if (remaining < 0L) remaining = 0L;
            throttle.restoreState(snapshot.failures, remaining);
        } else {
            throttle.restoreState(0, 0L);
        }
    }

    public boolean isEnrolled() {
        return record != null;
    }

    /** Enrolls a six-digit PIN and synchronously commits only its salted verifier. */
    public synchronized void enroll(char[] pin) {
        if (pin == null) throw new IllegalArgumentException("PIN must not be null");
        try {
            if (!isSixDigits(pin)) throw new IllegalArgumentException("PIN must contain exactly six digits");
            String newRecord = verifier.createRecord(pin);
            if (!store.save(newRecord, 0, 0L)) {
                throw new IllegalStateException("Unable to commit PIN verifier");
            }
            record = newRecord;
            throttle.recordSuccess();
        } finally {
            Arrays.fill(pin, '\0');
        }
    }

    /**
     * Verifies a candidate and synchronously commits bounded retry state.
     * Call from a worker thread; this method runs the memory-hard KDF synchronously.
     */
    public synchronized boolean verify(char[] pin) {
        try {
            String currentRecord = record;
            if (currentRecord == null || throttle.remainingDelayMillis() > 0L) return false;
            if (!isSixDigits(pin)) {
                persistFailure(throttle.recordFailure());
                return false;
            }
            boolean valid = verifier.verify(pin, currentRecord);
            if (valid) {
                throttle.recordSuccess();
                if (!store.save(currentRecord, 0, 0L)) {
                    throw new IllegalStateException("Unable to commit PIN retry state");
                }
            } else {
                persistFailure(throttle.recordFailure());
            }
            return valid;
        } finally {
            if (pin != null) Arrays.fill(pin, '\0');
        }
    }

    public long remainingDelayMillis() {
        return throttle.remainingDelayMillis();
    }

    private void persistFailure(long delayMillis) {
        int failures = throttle.getConsecutiveFailures();
        long now = wallClock.currentTimeMillis();
        long retryAt = now > Long.MAX_VALUE - delayMillis ? Long.MAX_VALUE : now + delayMillis;
        if (!store.save(record, failures, retryAt)) {
            throw new IllegalStateException("Unable to commit PIN retry state");
        }
    }

    private static boolean isSixDigits(char[] pin) {
        if (pin == null || pin.length != 6) return false;
        for (char digit : pin) if (digit < '0' || digit > '9') return false;
        return true;
    }

    private static Context requireApplicationContext(Context context) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        Context app = context.getApplicationContext();
        return app == null ? context : app;
    }

    private static final class SharedPreferencesStore implements Store {
        private final SharedPreferences preferences;

        SharedPreferencesStore(Context context) {
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        @Override public Snapshot load() {
            if (!preferences.contains(KEY_RECORD)) return null;
            return new Snapshot(preferences.getString(KEY_RECORD, null),
                    preferences.getInt(KEY_FAILURES, 0), preferences.getLong(KEY_RETRY_AT, 0L));
        }

        @Override public boolean save(String record, int failures, long retryAtEpochMillis) {
            return preferences.edit()
                    .putString(KEY_RECORD, record)
                    .putInt(KEY_FAILURES, failures)
                    .putLong(KEY_RETRY_AT, retryAtEpochMillis)
                    .commit();
        }
    }
}
