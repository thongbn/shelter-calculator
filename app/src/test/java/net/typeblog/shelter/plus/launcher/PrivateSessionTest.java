package net.typeblog.shelter.plus.launcher;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrivateSessionTest {
    @After public void clear() { PrivateSession.clear(); }

    @Test public void requiresCurrentVerifiedAttemptAndClearsOnLock() {
        assertFalse(PrivateSession.isAuthorized());
        long attempt = PrivateSession.beginAttempt();
        assertTrue(PrivateSession.authorize(attempt));
        assertTrue(PrivateSession.isAuthorized());
        long staleAttempt = PrivateSession.beginAttempt();
        PrivateSession.clear();
        assertFalse(PrivateSession.isAuthorized());
        assertFalse(PrivateSession.authorize(staleAttempt));
    }
}
