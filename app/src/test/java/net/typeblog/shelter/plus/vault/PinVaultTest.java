package net.typeblog.shelter.plus.vault;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PinVaultTest {
    @Test
    public void enrollmentPersistsOnlyVerifierAndCanReload() {
        MemoryStore store = new MemoryStore();
        FakeClock clock = new FakeClock();
        PinVault first = newVault(store, clock);
        assertFalse(first.isEnrolled());

        char[] badLength = "1234".toCharArray();
        try {
            first.enroll(badLength);
            fail("Expected six-digit policy");
        } catch (IllegalArgumentException expected) {
            assertCleared(badLength);
        }
        char[] badCharacter = "12a456".toCharArray();
        try {
            first.enroll(badCharacter);
            fail("Expected numeric PIN policy");
        } catch (IllegalArgumentException expected) {
            assertCleared(badCharacter);
        }

        char[] enrollment = "012345".toCharArray();
        first.enroll(enrollment);
        assertCleared(enrollment);
        assertTrue(first.isEnrolled());
        assertTrue(store.saved.record.startsWith("shelter-pin-v1:$argon2id$v=19$"));
        assertFalse(store.saved.record.contains("012345"));

        PinVault reloaded = newVault(store, clock);
        assertTrue(reloaded.isEnrolled());
        char[] correct = "012345".toCharArray();
        assertTrue(reloaded.verify(correct));
        assertCleared(correct);
        assertEquals(0, store.saved.failures);
    }

    @Test
    public void retryDelaySurvivesRecreation() {
        MemoryStore store = new MemoryStore();
        FakeClock clock = new FakeClock();
        PinVault first = newVault(store, clock);
        first.enroll("123456".toCharArray());
        assertFalse(first.verify("123457".toCharArray()));
        assertEquals(1, store.saved.failures);
        assertTrue(store.saved.retryAtEpochMillis > clock.now);

        PinVault reloaded = newVault(store, clock);
        assertTrue(reloaded.remainingDelayMillis() > 0L);
        char[] blocked = "123456".toCharArray();
        assertFalse(reloaded.verify(blocked));
        assertCleared(blocked);
        clock.now += PinThrottle.BASE_DELAY_MILLIS;
        assertEquals(0L, reloaded.remainingDelayMillis());
        assertTrue(reloaded.verify("123456".toCharArray()));
    }

    private static PinVault newVault(final MemoryStore store, final FakeClock clock) {
        PinThrottle throttle = new PinThrottle(new PinThrottle.Clock() {
            @Override public long elapsedRealtimeMillis() { return clock.now; }
        });
        return new PinVault(store, new PinVerifier(), throttle, clock);
    }

    private static void assertCleared(char[] value) {
        for (char c : value) assertEquals('\0', c);
    }

    private static final class MemoryStore implements PinVault.Store {
        PinVault.Snapshot saved;
        @Override public PinVault.Snapshot load() { return saved; }
        @Override public boolean save(String record, int failures, long retryAtEpochMillis) {
            saved = new PinVault.Snapshot(record, failures, retryAtEpochMillis);
            return true;
        }
    }

    private static final class FakeClock implements PinVault.WallClock {
        long now = 1_000L;
        @Override public long currentTimeMillis() { return now; }
    }
}
