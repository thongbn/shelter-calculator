package net.typeblog.shelter.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class SameProcessRequestGateTest {
    @Test public void requestIsBoundToActionAndConsumedOnce() {
        SameProcessRequestGate gate = new SameProcessRequestGate();
        String token = gate.issue("freeze", 100);
        assertFalse(gate.consume("unfreeze", token, 101));
        assertTrue(gate.consume("freeze", token, 101));
        assertFalse(gate.consume("freeze", token, 102));
    }

    @Test public void wrongTokenDoesNotConsumePendingRequest() {
        SameProcessRequestGate gate = new SameProcessRequestGate();
        String token = gate.issue("freeze", 100);
        assertFalse(gate.consume("freeze", "forged", 101));
        assertTrue(gate.consume("freeze", token, 102));
    }

    @Test public void expiredRequestIsRejected() {
        SameProcessRequestGate gate = new SameProcessRequestGate();
        String token = gate.issue("freeze", 100);
        assertFalse(gate.consume("freeze", token, 5_101));
    }
}
