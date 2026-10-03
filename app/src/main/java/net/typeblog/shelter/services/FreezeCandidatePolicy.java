package net.typeblog.shelter.services;

/** Defines which Work Profile apps Shelter's bulk-freeze action considers eligible. */
public final class FreezeCandidatePolicy {
    private FreezeCandidatePolicy() {}

    public static boolean shouldFreeze(boolean installed, boolean systemApp, boolean hasLauncher, boolean shelterDpc) {
        if (!installed || shelterDpc) return false;
        return !systemApp || hasLauncher;
    }
}
