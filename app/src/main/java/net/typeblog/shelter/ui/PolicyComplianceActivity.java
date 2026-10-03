package net.typeblog.shelter.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Presents the required managed-profile disclosure during Android Enterprise provisioning. */
public final class PolicyComplianceActivity extends Activity {
    private static final String ACTION_ADMIN_POLICY_COMPLIANCE =
            "android.app.action.ADMIN_POLICY_COMPLIANCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ACTION_ADMIN_POLICY_COMPLIANCE.equals(getIntent().getAction())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundColor(0xFFFFFFFF);

        TextView disclosure = new TextView(this);
        disclosure.setTextColor(0xFF000000);
        disclosure.setTextSize(20);
        disclosure.setText("Shelter+ creates a separate Work Profile for apps you choose to add. "
                + "Android manages the profile's security challenge. You can pause the profile "
                + "from Shelter+ using LOCK, or resume it using UNLOCK.");
        content.addView(disclosure, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button continueButton = new Button(this);
        continueButton.setText("Continue");
        continueButton.setTextSize(18);
        continueButton.setOnClickListener(view -> {
            setResult(RESULT_OK);
            finish();
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = padding;
        content.addView(continueButton, buttonParams);
        setContentView(content);
    }
}
