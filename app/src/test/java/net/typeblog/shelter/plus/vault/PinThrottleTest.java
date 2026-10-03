package net.typeblog.shelter.plus.vault;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PinThrottleTest {
    @Test
    public void delayGrowsAndIsBoundedThenResetsOnSuccess() {
        FakeClock clock = new FakeClock();
        PinThrottle throttle = new PinThrottle(clock);

        assertEquals(500L, throttle.recordFailure());
        assertEquals(500L, throttle.remainingDelayMillis());
        clock.now += 500L;
        assertEquals(0L, throttle.remainingDelayMillis());
        assertEquals(1_000L, throttle.recordFailure());
        for (int i = 0; i < 12; i++) throttle.recordFailure();
        assertEquals(PinThrottle.MAX_DELAY_MILLIS, throttle.remainingDelayMillis());

        throttle.recordSuccess();
        assertEquals(0L, throttle.remainingDelayMillis());
        assertEquals(500L, throttle.recordFailure());
    }

    private static final class FakeClock implements PinThrottle.Clock {
        long now;
        @Override public long elapsedRealtimeMillis() { return now; }
    }
}
