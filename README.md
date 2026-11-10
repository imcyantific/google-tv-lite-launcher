# Lite Launcher

A small, customisable home screen for Android TV / Google TV, written in Kotlin with Compose for TV.
Built for the Google TV Streamer (4K); should run on any Android TV with Android 8.0 or newer.

## What it does

- **Home row** of favourite apps over a wallpaper, with clock, date, optional weather and a Wi-Fi / Ethernet indicator.
- **All apps** grid (last tile on the home row).
- **Hold OK on any app** for: add to / remove from home row, move left / right, hide, app info, uninstall.
- **Settings** (gear, top right): accent colour, wallpaper (6 presets, an image picked through a file manager app, or an image link), tile size,
  app names on/off, 12/24-hour clock, weather city, °C/°F, hidden apps, shortcut to Android settings.

Weather comes from Open-Meteo (no account or key). Nothing else leaves the device.

## Build

Open the folder in Android Studio and run the `app` configuration, or from a terminal:

```
./gradlew assembleRelease
```

The APK lands in `app/build/outputs/apk/release/app-release.apk` (about 1 MB).
The release build is signed with your debug key so it can be sideloaded.

Toolchain: Android Gradle Plugin 8.13.2, Gradle 8.14.3, Kotlin 2.3.21, compileSdk 36.
If Android Studio offers to upgrade these, that is fine but not required.

## Install on the Streamer

```
adb connect <streamer-ip>:5555
adb install -r app-release.apk
```

Open "Lite Launcher" from the apps list and check it works before going further.

## Make it the home screen

Google TV has no "default launcher" setting, and the remote's Home button stays wired to the
stock launcher. Two steps get around that without root and without disabling anything:

1. Set Lite Launcher as the home activity, then check it took:

```
adb shell cmd package set-home-activity --user 0 com.litelauncher.tv/.MainActivity
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

2. Turn on the Home button helper (an accessibility service inside the app that only watches
   for the Home key). First see whether another service is already enabled:

```
adb shell settings get secure enabled_accessibility_services
```

If that prints `null` or nothing:

```
adb shell settings put secure enabled_accessibility_services com.litelauncher.tv/com.litelauncher.tv.HomeKeyService
adb shell settings put secure accessibility_enabled 1
```

If it prints something, keep it and add ours after a colon:
`<existing>:com.litelauncher.tv/com.litelauncher.tv.HomeKeyService`.

You can also switch the helper on from the TV: Launcher settings > Home button helper.

Trade-off: while the helper is on, a long press of Home no longer opens the Google TV side panel.

## Undo

```
adb shell settings put secure enabled_accessibility_services ''
adb shell settings put secure accessibility_enabled 0
adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.MainActivity
adb uninstall com.litelauncher.tv
```

(If you appended to an existing accessibility list, restore that list instead of clearing it.)

Do not disable the stock launcher (`pm disable-user ...launcherx`) unless Lite Launcher already
answers the Home button reliably. If the stock one is disabled and the replacement fails, Home goes nowhere.

## Where to customise the code

| What | File |
| --- | --- |
| Accent colours, wallpaper presets, tile sizes | `ui/Theme.kt` |
| Tile look, focus border, dialogs | `ui/Components.kt` |
| Home layout, clock, weather text | `ui/HomeScreen.kt` |
| Wi-Fi indicator | `ui/NetworkIndicator.kt`, `data/NetworkMonitor.kt` |
| Settings list | `ui/SettingsScreen.kt` |
| Which apps are listed, default favourites | `data/AppRepository.kt` |
| Saved settings | `data/Prefs.kt` |
| Home button helper | `HomeKeyService.kt` |

All paths are under `app/src/main/java/com/litelauncher/tv/`.

## Licence

MIT. See `LICENSE`.
