# Lite Launcher

A small, customisable home screen for Android TV / Google TV, written in Kotlin with Compose for TV.
Built for the Google TV Streamer (4K); should run on any Android TV with Android 8.0 or newer.

## What it does

- **Home row** of favourite apps over a wallpaper, with clock, date, optional weather and a Wi-Fi / Ethernet indicator.
- **All apps** grid (last tile on the home row).
- **Hold OK on any app** for: add to / remove from home row, move left / right, hide, app info, uninstall.
- **Settings** (gear, top right): accent colour, wallpaper (6 presets, an image, a slideshow of a folder, a looping muted video, or an image link), slideshow speed, tile size,
  app names on/off, 12/24-hour clock, weather city, °C/°F, hidden apps, shortcut to Android settings.
- **Built-in file browser** for picking wallpapers with the remote (internal storage and USB drives).

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

## Automated releases

`.github/workflows/release.yml` builds a signed APK and publishes it as a GitHub Release.

### One-time setup: signing key

Android only updates an app when the new APK is signed with the same key, so releases need one
permanent key. Create it once and keep a backup somewhere safe; if it is lost, updates need an uninstall first.

1. Create the key (keytool ships with Android Studio's JDK). Use the same password when asked
   for the keystore and the key:

```
keytool -genkeypair -v -keystore lite-launcher.jks -alias lite-launcher -keyalg RSA -keysize 2048 -validity 10000
```

2. Turn the file into text so it can be stored as a secret.

Windows PowerShell (copies it to the clipboard):

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("lite-launcher.jks")) | Set-Clipboard
```

macOS / Linux:

```
base64 -w0 lite-launcher.jks      # macOS: base64 -i lite-launcher.jks
```

3. On GitHub: repository Settings > Secrets and variables > Actions > New repository secret. Add four:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the text from step 2 |
| `KEYSTORE_PASSWORD` | the password you chose |
| `KEY_ALIAS` | `lite-launcher` |
| `KEY_PASSWORD` | the same password |

Never commit the `.jks` file; `.gitignore` already excludes it.

### Publishing a version

Either:

- Actions tab > Release > Run workflow, type a version such as `1.2`, run; or
- push a tag: `git tag v1.2 && git push origin v1.2`

A few minutes later the release appears under Releases with `LiteLauncher-1.2.apk` attached.

The first APK signed with your own key cannot install over an earlier debug-signed build:
run `adb uninstall com.litelauncher.tv` once, then install. After that, updates install in place.

## Install on the Streamer

```
adb connect <streamer-ip>:5555
adb install -r app-release.apk
```

Open "Lite Launcher" from the apps list and check it works before going further.

## Make it the home screen

Google TV has no "default launcher" setting, so the app includes a helper (an accessibility
service) that does the job without a PC or root.

On the TV: Launcher settings > Home button helper, turn on "Lite Launcher Home button".

With the helper on, Lite Launcher opens:

- when you press Home on the remote;
- when the TV starts;
- whenever the Google TV home screen would appear (after Back out of an app, on wake).

The last one is the "Replace Google TV home" switch in Launcher settings. Turn it off if you
need to reach the Google TV home screen.

The helper only looks at the Home key and at which app comes to the front. Trade-off: a long
press of Home no longer opens the Google TV side panel while it is on.

Optional, with ADB from a PC, to also register it as the system home activity:

```
adb shell cmd package set-home-activity --user 0 com.litelauncher.tv/.MainActivity
```

## Undo

Turn the helper off in the TV's accessibility settings, or uninstall Lite Launcher. Nothing on
the TV is disabled or removed by this app, so the Google TV home comes straight back.

Do not disable the stock launcher with ADB (`pm disable-user ...launcherx`): if a replacement
ever fails, the Home button would go nowhere.

## Wallpapers from the device

Settings > Wallpaper offers "Image from this device", "Slideshow from a folder" and
"Video from this device". The first time, Android asks for permission to read photos and videos.
If a file is missing from the browser, choose "Can't see your files?" to grant all files access.

Video plays muted and looped, and only while the launcher is on screen. MP4 (H.264) is the
safest format; very large 4K files may stutter.

## Where to customise the code

| What | File |
| --- | --- |
| Accent colours, wallpaper presets, tile sizes | `ui/Theme.kt` |
| Tile look, focus border, dialogs | `ui/Components.kt` |
| Home layout, clock, weather text | `ui/HomeScreen.kt` |
| Wallpaper drawing, slideshow, video | `ui/Wallpaper.kt` |
| File browser | `ui/FileBrowser.kt`, `data/MediaFiles.kt` |
| Wi-Fi indicator | `ui/NetworkIndicator.kt`, `data/NetworkMonitor.kt` |
| Settings list | `ui/SettingsScreen.kt` |
| Which apps are listed, default favourites | `data/AppRepository.kt` |
| Saved settings | `data/Prefs.kt` |
| Home button helper | `HomeKeyService.kt` |

All paths are under `app/src/main/java/com/litelauncher/tv/`.

## Licence

MIT. See `LICENSE`.
