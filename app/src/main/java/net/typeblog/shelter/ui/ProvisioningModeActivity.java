package net.typeblog.shelter.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;

/** Answers modern Android Enterprise provisioning-mode discovery for Shelter's work-profile DPC. */
public final class ProvisioningModeActivity extends Activity {
    private static final String ACTION_GET_PROVISIONING_MODE =
            "android.app.action.GET_PROVISIONING_MODE";
    private static final String EXTRA_ALLOWED_MODES =
            "android.app.extra.PROVISIONING_ALLOWED_PROVISIONING_MODES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || !ACTION_GET_PROVISIONING_MODE.equals(getIntent().getAction())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        int managedProfile = DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE;
        ArrayList<Integer> allowedModes = getIntent().getIntegerArrayListExtra(EXTRA_ALLOWED_MODES);
        if (allowedModes != null && !allowedModes.contains(managedProfile)) {
            setResult(RESULT_CANCELED);
        } else {
            Intent result = new Intent();
            result.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, managedProfile);
            setResult(RESULT_OK, result);
        }
        finish();
    }
}
