package net.typeblog.shelter.plus.launcher;

/** Process-local gate: reboot and process death always require a fresh PIN entry. */
public final class PrivateSession {
    public static final long BACKGROUND_TIMEOUT_MS = 15 * 60 * 1000L;
    private static final long NO_BACKGROUND_TIME = -1L;
    private static volatile boolean authorized;
    private static long generation;
    private static int startedActivityCount;
    private static long backgroundedAtMs = NO_BACKGROUND_TIME;

    private PrivateSession() {}

    public static synchronized long beginAttempt() { return generation; }

    public static synchronized boolean authorize(long token, long nowMs) {
        expireIfNeeded(nowMs);
        if (token != generation) return false;
        authorized = true;
        return true;
    }

    public static synchronized boolean isAuthorized(long nowMs) {
        expireIfNeeded(nowMs);
        return authorized;
    }

    /** Overlapping activities (including configuration changes) keep the process foreground. */
    public static synchronized void onActivityStarted(long nowMs) {
        if (startedActivityCount == 0) {
            expireIfNeeded(nowMs);
            backgroundedAtMs = NO_BACKGROUND_TIME;
        }
        startedActivityCount++;
    }

    public static synchronized void onActivityStopped(long nowMs, boolean changingConfigurations) {
        if (startedActivityCount > 0) {
            startedActivityCount--;
            if (startedActivityCount == 0 && !changingConfigurations) backgroundedAtMs = nowMs;
        }
    }

    public static synchronized void clear() {
        authorized = false;
        backgroundedAtMs = NO_BACKGROUND_TIME;
        generation++;
    }

    /** Revoke the current session and invalidate any in-flight PIN authorization attempt. */
    public static synchronized void lock() {
        clear();
    }

    private static void expireIfNeeded(long nowMs) {
        if (authorized && backgroundedAtMs != NO_BACKGROUND_TIME
                && nowMs - backgroundedAtMs >= BACKGROUND_TIMEOUT_MS) {
            authorized = false;
            backgroundedAtMs = NO_BACKGROUND_TIME;
            generation++;
        }
    }
}
