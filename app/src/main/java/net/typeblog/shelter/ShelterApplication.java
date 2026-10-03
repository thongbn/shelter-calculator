package net.typeblog.shelter;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.IntentFilter;
import android.app.admin.DevicePolicyManager;
import android.content.pm.PackageManager;

import net.typeblog.shelter.services.FileShuttleService;
import net.typeblog.shelter.plus.launcher.PrivateSession;
import net.typeblog.shelter.plus.launcher.LauncherAlias;
import net.typeblog.shelter.services.ShelterService;
import net.typeblog.shelter.util.LocalStorageManager;
import net.typeblog.shelter.util.SettingsManager;

public class ShelterApplication extends Application {
    private ServiceConnection mShelterServiceConnection = null;
    private ServiceConnection mFileShuttleServiceConnection = null;

    @Override
    public void onCreate() {
        super.onCreate();
        LocalStorageManager.initialize(this);
        SettingsManager.initialize(this);
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        if (dpm != null && dpm.isProfileOwnerApp(getPackageName())) {
            // Component state is scoped to this Android user, so the personal launcher stays visible.
            getPackageManager().setComponentEnabledSetting(
                    LauncherAlias.component(this),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        android.content.BroadcastReceiver screenLockReceiver = new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) { PrivateSession.clear(); }
        };
        IntentFilter screenOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(screenLockReceiver, screenOff, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenLockReceiver, screenOff);
    }


    public void bindShelterService(ServiceConnection conn, boolean foreground) {
        unbindShelterService();
        Intent intent = new Intent(getApplicationContext(), ShelterService.class);
        intent.putExtra("foreground", foreground);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        mShelterServiceConnection = conn;
    }

    public void bindFileShuttleService(ServiceConnection conn) {
        unbindFileShuttleService();;
        Intent intent = new Intent(getApplicationContext(), FileShuttleService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        mFileShuttleServiceConnection = conn;
    }

    public void unbindShelterService() {
        if (mShelterServiceConnection != null) {
            try {
                unbindService(mShelterServiceConnection);
            } catch (Exception e) {
                // This method call might fail if the service is already unbound
                // just ignore anything that might happen.
                // We will be stopping already if this would ever happen.
            }
        }

        mShelterServiceConnection = null;
    }

    public void unbindFileShuttleService() {
        if (mFileShuttleServiceConnection != null) {
            try {
                unbindService(mFileShuttleServiceConnection);
            } catch (Exception e) {
                // ...
            }
        }

        mFileShuttleServiceConnection = null;
    }
}
