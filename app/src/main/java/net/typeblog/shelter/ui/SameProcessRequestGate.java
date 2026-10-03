package net.typeblog.shelter.ui;

import java.util.UUID;

/** One-shot capability for same-process DummyActivity requests without a signature. */
final class SameProcessRequestGate {
    private static final long TIMEOUT_MS = 5_000;
    private String pendingAction;
    private String pendingToken;
    private long deadlineMs;

    synchronized String issue(String action, long nowMs) {
        pendingAction = action;
        pendingToken = UUID.randomUUID().toString();
        deadlineMs = nowMs + TIMEOUT_MS;
        return pendingToken;
    }

    synchronized boolean consume(String action, String token, long nowMs) {
        if (pendingToken == null || nowMs > deadlineMs) {
            clear();
            return false;
        }
        if (token == null || !pendingToken.equals(token)
                || action == null || !pendingAction.equals(action)) return false;
        clear();
        return true;
    }

    private void clear() {
        pendingAction = null;
        pendingToken = null;
        deadlineMs = 0;
    }
}
