package net.typeblog.shelter.services;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FreezeCandidatePolicyTest {
    @Test public void includesInstalledUserAppsWithOrWithoutLauncher() {
        assertTrue(FreezeCandidatePolicy.shouldFreeze(true, false, true, false));
        assertTrue(FreezeCandidatePolicy.shouldFreeze(true, false, false, false));
    }

    @Test public void includesOnlyVisibleSystemAppsAndNeverShelterOrMissingPackages() {
        assertTrue(FreezeCandidatePolicy.shouldFreeze(true, true, true, false));
        assertFalse(FreezeCandidatePolicy.shouldFreeze(true, true, false, false));
        assertFalse(FreezeCandidatePolicy.shouldFreeze(true, false, true, true));
        assertFalse(FreezeCandidatePolicy.shouldFreeze(false, false, true, false));
    }
}
