package net.typeblog.shelter.plus.launcher;

/** Process-local gate: reboot and process death always require a fresh PIN entry. */
public final class PrivateSession {
    private static volatile boolean authorized;
    private static long generation;
    private PrivateSession() {}
    public static synchronized long beginAttempt() { return generation; }
    public static synchronized boolean authorize(long token) {
        if (token != generation) return false;
        authorized = true;
        return true;
    }
    public static boolean isAuthorized() { return authorized; }
    public static synchronized void clear() { authorized = false; generation++; }
}
