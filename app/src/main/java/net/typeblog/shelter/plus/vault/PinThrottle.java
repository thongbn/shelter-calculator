package net.typeblog.shelter.plus.vault;

/** A bounded, process-local retry delay. It intentionally stores no PIN data. */
public final class PinThrottle {
    public static final long BASE_DELAY_MILLIS = 500L;
    public static final long MAX_DELAY_MILLIS = 30_000L;

    /** Injectable monotonic clock, useful for deterministic tests. */
    public interface Clock {
        long elapsedRealtimeMillis();
    }

    private final Clock clock;
    private int consecutiveFailures;
    private long blockedUntil;

    public PinThrottle() {
        this(new Clock() {
            @Override public long elapsedRealtimeMillis() {
                return System.nanoTime() / 1_000_000L;
            }
        });
    }

    public PinThrottle(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock must not be null");
        this.clock = clock;
    }

    /** Remaining delay before another attempt is allowed, in milliseconds. */
    public synchronized long remainingDelayMillis() {
        return Math.max(0L, blockedUntil - clock.elapsedRealtimeMillis());
    }

    /** Records a failed attempt and returns the delay that now applies. */
    public synchronized long recordFailure() {
        if (consecutiveFailures < 31) consecutiveFailures++;
        int shift = Math.min(consecutiveFailures - 1, 16);
        long delay = Math.min(MAX_DELAY_MILLIS, BASE_DELAY_MILLIS << shift);
        long now = clock.elapsedRealtimeMillis();
        blockedUntil = now > Long.MAX_VALUE - delay ? Long.MAX_VALUE : now + delay;
        return delay;
    }

    /** Clears the failure streak after a successful authentication. */
    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        blockedUntil = 0L;
    }

    /** Restores persisted retry state, clamping all inputs to the supported bounds. */
    public synchronized void restoreState(int failures, long remainingMillis) {
        consecutiveFailures = Math.max(0, Math.min(31, failures));
        long delay = Math.max(0L, Math.min(MAX_DELAY_MILLIS, remainingMillis));
        long now = clock.elapsedRealtimeMillis();
        blockedUntil = now > Long.MAX_VALUE - delay ? Long.MAX_VALUE : now + delay;
    }

    public synchronized int getConsecutiveFailures() {
        return consecutiveFailures;
    }
}
