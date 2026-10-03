package net.typeblog.shelter.plus.launcher;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import net.typeblog.shelter.R;
import net.typeblog.shelter.plus.vault.PinVault;
import net.typeblog.shelter.ui.DummyActivity;
import net.typeblog.shelter.ui.MainActivity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Public, offline calculator. Six digits followed by equals is the private entry gesture. */
public class LauncherActivity extends AppCompatActivity {
    private static final String THEME_PREFS = "calculator_theme";
    private static final String KEY_DARK = "dark";
    private final ExecutorService pinWorker = Executors.newSingleThreadExecutor();
    private TextView display;
    private String expression = "";
    private boolean resultShown;
    private boolean checkingPin;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setCalculatorTheme(false);
        buildCalculator();
        if (isWorkProfileOwner()) hideWorkLauncherAlias();
        if (!new PinVault(this).isEnrolled()) promptEnrollment();
    }

    private void buildCalculator() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(14));
        android.util.TypedValue background = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, background, true);
        root.setBackgroundColor(background.data);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Calculator");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button theme = new Button(this);
        theme.setText(isDark() ? "☼" : "◐");
        theme.setContentDescription(isDark() ? "Switch to light theme and freeze work apps" : "Switch to dark theme and freeze work apps");
        theme.setMinWidth(dp(56));
        theme.setOnClickListener(v -> toggleThemeAndFreeze());
        top.addView(theme, new LinearLayout.LayoutParams(dp(64), dp(52)));
        root.addView(top);

        display = new TextView(this);
        display.setTextSize(34);
        display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        display.setSingleLine(true);
        display.setPadding(dp(8), 0, dp(8), 0);
        display.setContentDescription("Calculator display");
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 1));

        String[][] keys = {{"AC", "⌫", "%", "÷"}, {"7", "8", "9", "×"},
                {"4", "5", "6", "−"}, {"1", "2", "3", "+"}, {"±", "0", ".", "="}};
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(keys.length);
        for (String[] row : keys) for (String key : row) {
            Button button = new Button(this);
            button.setText(key);
            button.setTextSize(22);
            button.setPadding(0, 0, 0, 0);
            button.setMinHeight(0);
            button.setMinWidth(0);
            button.setOnClickListener(v -> press(key));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            grid.addView(button, params);
        }
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 3));
        setContentView(root);
        refreshDisplay();
    }

    private void press(String key) {
        if ("AC".equals(key)) { expression = ""; resultShown = false; }
        else if ("⌫".equals(key)) {
            if (!expression.isEmpty()) expression = expression.substring(0, expression.length() - 1);
            resultShown = false;
        } else if ("=".equals(key)) calculateOrUnlock();
        else if ("±".equals(key)) {
            if (expression.startsWith("-")) expression = expression.substring(1);
            else if (!expression.isEmpty()) expression = "-" + expression;
            resultShown = false;
        } else {
            if (resultShown && isDigitOrDot(key)) expression = "";
            resultShown = false;
            switch (key) {
                case "÷": expression += "/"; break;
                case "×": expression += "*"; break;
                case "−": expression += "-"; break;
                default: expression += key;
            }
        }
        refreshDisplay();
    }

    private void calculateOrUnlock() {
        if (checkingPin || expression.isEmpty()) return;
        if (expression.matches("[0-9]{6}")) {
            char[] pin = expression.toCharArray();
            long attempt = PrivateSession.beginAttempt();
            checkingPin = true;
            expression = "";
            refreshDisplay();
            pinWorker.execute(() -> {
                boolean ok = new PinVault(getApplicationContext()).verify(pin);
                runOnUiThread(() -> {
                    checkingPin = false;
                    if (ok && PrivateSession.authorize(attempt)) {
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        Toast.makeText(this, "Invalid entry", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            return;
        }
        try {
            BigDecimal value = new CalculatorExpression(expression).evaluate();
            expression = value.stripTrailingZeros().toPlainString();
            resultShown = true;
        } catch (IllegalArgumentException failure) {
            expression = "";
            resultShown = false;
            Toast.makeText(this, "Invalid expression", Toast.LENGTH_SHORT).show();
        }
        refreshDisplay();
    }

    private void promptEnrollment() {
        EditText pin = pinField("Choose a 6-digit PIN");
        EditText confirm = pinField("Confirm PIN");
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(24), dp(8), dp(24), dp(4));
        fields.addView(pin);
        fields.addView(confirm);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("First setup")
                .setMessage("Set a numeric PIN to protect Shelter. The PIN is stored only as a salted Argon2id verifier.")
                .setView(fields)
                .setCancelable(false)
                .setNegativeButton("Exit", (d, w) -> finish())
                .setPositiveButton("Save PIN", null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String a = pin.getText().toString();
            String b = confirm.getText().toString();
            if (!a.matches("[0-9]{6}") || !a.equals(b)) {
                pin.setText(""); confirm.setText("");
                Toast.makeText(this, "Enter matching six-digit PINs", Toast.LENGTH_SHORT).show();
                return;
            }
            char[] secret = a.toCharArray();
            pin.setText(""); confirm.setText("");
            long attempt = PrivateSession.beginAttempt();
            pinWorker.execute(() -> {
                boolean saved;
                try { new PinVault(getApplicationContext()).enroll(secret); saved = true; }
                catch (RuntimeException error) { Arrays.fill(secret, '\0'); saved = false; }
                boolean result = saved;
                runOnUiThread(() -> {
                    if (result && PrivateSession.authorize(attempt)) {
                        dialog.dismiss();
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        dialog.dismiss();
                        Toast.makeText(this, "PIN setup failed. Try again.", Toast.LENGTH_SHORT).show();
                        promptEnrollment();
                    }
                });
            });
        }));
        dialog.show();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private EditText pinField(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setSingleLine(true);
        input.setSaveEnabled(false);
        return input;
    }

    private void toggleThemeAndFreeze() {
        boolean dark = !isDark();
        getSharedPreferences(THEME_PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply();
        setCalculatorTheme(true);
        if (!net.typeblog.shelter.util.Utility.isWorkProfileAvailable(this)) return;
        Intent freeze = new Intent(DummyActivity.PUBLIC_FREEZE_ALL);
        freeze.setComponent(new ComponentName(this, DummyActivity.class));
        DummyActivity.registerSameProcessRequest(freeze);
        try { startActivity(freeze); }
        catch (RuntimeException ignored) { /* theme still changes when no active work profile exists */ }
    }

    private void setCalculatorTheme(boolean recreate) {
        AppCompatDelegate.setDefaultNightMode(isDark() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        if (recreate && !isFinishing()) recreate();
    }

    private boolean isDark() {
        return getSharedPreferences(THEME_PREFS, MODE_PRIVATE).getBoolean(KEY_DARK, false);
    }

    private void refreshDisplay() { if (display != null) display.setText(expression.isEmpty() ? "0" : expression); }
    private static boolean isDigitOrDot(String key) { return key.length() == 1 && (Character.isDigit(key.charAt(0)) || ".".equals(key)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private boolean isWorkProfileOwner() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        return dpm != null && dpm.isProfileOwnerApp(getPackageName());
    }

    private void hideWorkLauncherAlias() {
        ComponentName alias = LauncherAlias.component(this);
        getPackageManager().setComponentEnabledSetting(alias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP);
    }
}
