# Shelter Calculator

An offline calculator front end for Shelter's Android Work Profile manager. The launcher entry is named and drawn as **Calculator**. Enter the configured six-digit numeric PIN in the calculator and press `=` to open Shelter. First launch asks the user to set the PIN, then starts Shelter's existing Work Profile setup.

The PIN is verified with Argon2id and a random salt; only the verifier and bounded retry state are stored. A verified session exists in process memory only and clears on screen lock or process death. The calculator has no analytics or network client. Its Light/Dark button also asks Shelter's existing Device Policy Controller path to freeze visible, installed Work Profile apps. Already-frozen apps remain frozen. This includes installed user apps and enabled system apps with a launcher activity; headless system services and Shelter itself are excluded. Platform rejections are reported without interrupting the remaining batch. Individual unfreeze remains in Shelter's authenticated app list.

The calculator and PIN UI use plain Android widgets, decimal arithmetic, and no calculator dependency. Shelter's provisioning and per-app freeze/unfreeze core are retained with minimal flow changes. The Work Profile copy disables only its own calculator launcher alias; package/component state is per Android user, so the Personal icon remains available.

## Build

Use JDK 17 and Android SDK platform/build tools 35:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

At this revision, 15 JVM unit tests pass. Lint reports 89 warnings and no fatal findings; these include legacy Shelter source, translations, and resources. Android 14 provisioning, reboot behavior, and E-Ink rendering need device validation. See [architecture](docs/ARCHITECTURE.md) and [build/device checks](docs/BUILDING.md).

## Source and licenses

This project is based on [PeterCxy/Shelter](https://github.com/PeterCxy/Shelter), whose GPL-3.0 license and notices are retained in [LICENSE](LICENSE). Shelter Calculator changes are also GPL-3.0. The included Setup Wizard Library retains its Apache-2.0 license. Bouncy Castle's Argon2 implementation is consumed through the existing `bcprov-jdk18on` dependency. No calculator source was imported.
