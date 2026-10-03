package net.typeblog.shelter.plus.launcher;

import android.content.ComponentName;
import android.content.Context;

/** The manifest namespace differs from applicationId, so keep this alias name canonical. */
public final class LauncherAlias {
    public static final String CLASS_NAME = "net.typeblog.shelter.plus.CalculatorLauncher";

    private LauncherAlias() {}

    public static ComponentName component(Context context) {
        return new ComponentName(context.getPackageName(), CLASS_NAME);
    }
}
