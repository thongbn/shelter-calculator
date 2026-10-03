# Build and device checks

Build and run available checks with JDK 17 and Android SDK 35:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On a test device, verify:

1. Install and open the Personal app. It appears as Calculator and performs `+`, `−`, `×`, `÷`, parentheses, decimal and remainder calculations offline.
2. Set and confirm a six-digit PIN. Provision the Work Profile from Shelter. Confirm the Work copy's launcher icon disappears while the Personal Calculator icon remains after restart.
3. Enter wrong and correct PINs followed by `=`. Only the exact PIN opens Shelter; repeated failures incur a bounded delay. Confirm the PIN is absent from logs, keyboard suggestions, saved state and task previews.
4. Use Shelter to unfreeze one app. Toggle Light/Dark in Calculator. All eligible unfrozen Work apps are frozen; already-hidden apps stay hidden. Toggling again never unfreezes.
5. Confirm public/old unfreeze shortcuts cannot unlock apps; only Shelter's app list can unfreeze after a new correct PIN. Check normal app notifications before and after freezing; there is no persistent unlock notification.
6. Lock the screen, kill the app process, and reboot. A fresh PIN is required, Calculator remains the public UI, Work alias remains hidden, and hidden/unhidden app state is consistent.
7. Repeat provisioning, profile enable/disable, and theme checks on Android 14 and Bigme B7 Pro. Check low-redraw behavior and vendor notification changes.

Build and unit tests cannot verify OEM launcher, managed-profile provisioning, notification or E-Ink behavior. Record those as device results before claiming compatibility.
