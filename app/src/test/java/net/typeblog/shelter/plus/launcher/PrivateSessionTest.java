package net.typeblog.shelter.plus.launcher;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrivateSessionTest {
    @After public void clear() { PrivateSession.clear(); }

    @Test public void lockRevokesSessionAndInvalidatesPendingAttempt() {
        assertFalse(PrivateSession.isAuthorized(0));
        long attempt = PrivateSession.beginAttempt();
        assertTrue(PrivateSession.authorize(attempt, 0));
        assertTrue(PrivateSession.isAuthorized(0));
        long staleAttempt = PrivateSession.beginAttempt();
        PrivateSession.lock();
        assertFalse(PrivateSession.isAuthorized(0));
        assertFalse(PrivateSession.authorize(staleAttempt, 0));
    }

    @Test public void backgroundTimeoutExpiresExactlyAtFifteenMinutes() {
        PrivateSession.onActivityStarted(0);
        long attempt = PrivateSession.beginAttempt();
        assertTrue(PrivateSession.authorize(attempt, 10));
        PrivateSession.onActivityStopped(100, false);

        assertTrue(PrivateSession.isAuthorized(100 + PrivateSession.BACKGROUND_TIMEOUT_MS - 1));
        assertFalse(PrivateSession.isAuthorized(100 + PrivateSession.BACKGROUND_TIMEOUT_MS));
        assertFalse(PrivateSession.authorize(attempt, 100 + PrivateSession.BACKGROUND_TIMEOUT_MS + 1));
    }

    @Test public void activityTransitionRestartsDeadlineAndOverlappingActivityStaysForeground() {
        PrivateSession.onActivityStarted(0);
        PrivateSession.onActivityStarted(1);
        long attempt = PrivateSession.beginAttempt();
        assertTrue(PrivateSession.authorize(attempt, 2));
        PrivateSession.onActivityStopped(3, false);
        assertTrue(PrivateSession.isAuthorized(PrivateSession.BACKGROUND_TIMEOUT_MS + 10));
        PrivateSession.onActivityStopped(PrivateSession.BACKGROUND_TIMEOUT_MS + 11, false);

        PrivateSession.onActivityStarted(PrivateSession.BACKGROUND_TIMEOUT_MS + 12);
        assertTrue(PrivateSession.isAuthorized(PrivateSession.BACKGROUND_TIMEOUT_MS + 12));
        PrivateSession.onActivityStopped(PrivateSession.BACKGROUND_TIMEOUT_MS + 20, false);
        assertFalse(PrivateSession.isAuthorized(2 * PrivateSession.BACKGROUND_TIMEOUT_MS + 20));
    }

    @Test public void configurationRecreationDoesNotStartBackgroundTimeout() {
        PrivateSession.onActivityStarted(0);
        long attempt = PrivateSession.beginAttempt();
        assertTrue(PrivateSession.authorize(attempt, 1));
        PrivateSession.onActivityStopped(2, true);
        PrivateSession.onActivityStarted(PrivateSession.BACKGROUND_TIMEOUT_MS + 100);
        assertTrue(PrivateSession.isAuthorized(PrivateSession.BACKGROUND_TIMEOUT_MS + 100));
        PrivateSession.onActivityStopped(PrivateSession.BACKGROUND_TIMEOUT_MS + 101, false);
    }
}
