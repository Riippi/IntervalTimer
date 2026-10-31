# Interval Timer X

Android interval/workout timer (`org.leeko.intervaltimer`), originally built in 2015 and modernized in 2026. Published on Google Play. Single Gradle module (`app`), Java, classic View system (no Compose, no Kotlin sources). `minSdk 21`, `compileSdk`/`targetSdk 36`.

This repo is **public**. Never put secrets, passwords or account details in any tracked file.

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home   # AGP 8.x needs JDK 17, not the newer openjdk on this machine
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/
./gradlew bundleRelease      # signed AAB -> app/build/outputs/bundle/release/app-release.aab
```

The Android SDK is the Homebrew cask install (`/opt/homebrew/Caskroom/android-commandlinetools/<version>`), not `~/Library/Android/sdk`. `local.properties` (gitignored) points at it.

## Testing on an emulator

- AVD `IntervalTimerTest` (API 34). Boot headless: `<sdk>/emulator/emulator -avd IntervalTimerTest -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot`
- `avdmanager` is broken on this SDK install, so AVDs are created by hand-writing `~/.android/avd/<name>.ini` and `config.ini`. Do not try to fix `avdmanager`.
- Edge-to-edge bugs only reproduce on API 35+ (the API 34 AVD cannot show them). Use an API 36 AVD for any insets/layout work.
- `uiautomator dump` fails with "could not get idle state" on the live timer screen because the UI never idles. Dump from a static screen (main screen, paused/over state) and tap using bounds from that, or use screenshots.
- Switching between debug-signed and release-signed builds on the same device fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. `adb uninstall org.leeko.intervaltimer` first.
- The app logs almost nothing on purpose (all `Log.d` calls were removed). Verify behavior from screenshots and `logcat | grep FATAL`, not from app log lines.
- Phone-call pausing can be tested with `adb emu gsm call <number>`.

## Release rules

- Every upload to Play Console needs a **new `versionCode`** in `app/build.gradle`. Play rejects a re-upload of a versionCode that was already used, even on a different track. Bump `versionName` alongside it.
- Signing config reads `keystore.properties` (gitignored, along with `*.jks` / `*.keystore`). If the file is absent the project still builds, unsigned. **Never read, print, echo or commit its contents or the keystore.** Passwords and passphrases are the user's: they run `keytool` / `ssh-keygen` themselves in their own terminal.
- Play Console and account-level settings are done by the user in the browser. Claude cannot reach them.
- Release notes and store listing copy: **no em dashes**, keep them short.

## Conventions

- Commit messages end with the `Co-Authored-By` trailer given in the session attribution instructions.
- Check `git status` before any destructive git operation.
- Verify changes empirically (emulator screenshots, logcat) rather than assuming "it compiles" is enough. Every change in this project's history was tested that way.

## Architecture

- `MainActivity`: workout editor. A `ViewPager` + `SlidingTabLayout` with one tab per saved workout (max 10), hosted in `SlidingTabsBasicFragment`. Dialogs in `dialog/` (`TimeDialog`, `RoundsDialog`, `RenameDialog`, `ActionsDialog`) use `NumberPicker` wheels. Rounds value `0` means **unlimited**.
- `TimerActivity`: the running-workout screen. Launched from `MainActivity` through an `ActivityResultLauncher`.
- `SettingsActivity`: legacy `PreferenceActivity` + `PreferenceFragment`, screen defined in `res/xml/pref_general.xml`.
- `AppController` (singleton): owns the active counter, alert sounds (`SoundPool`, preloaded by `ensureSoundPoolLoaded()` at `startTimer()`), vibration (`vibrateCompat()`, `VibrationEffect` on API 26+), and audio focus. Losing audio focus pauses the workout. This replaced `READ_PHONE_STATE`, which Play restricts, so do not reintroduce that permission.
- `counter/`: `CounterFactory` picks a `BaseTimer` subclass from (manual rest?) x (unlimited rounds?): `DefaultTimer`, `DefaultManual`, `UnlimitedNormal`, `UnlimitedManual`. `BaseTimer` ticks once per second on the main-thread `Handler`, scheduled against a fixed `SystemClock.elapsedRealtime()` anchor so it cannot drift.
- `WorkoutModel` + `contentprovider/MyContentProvider` + `database/`: SQLite persistence behind a `ContentProvider`, with an in-memory `HashMap` cache keyed by tab id. Default workouts are seeded when the database is first created.
- `EdgeToEdgeHelper`: applies system-bar insets as padding on each activity's content root. Required because targeting API 35+ forces edge-to-edge.
- All UI strings live in `res/values/strings*.xml`. A few preference labels in `pref_general.xml` are still inline literals.

## Decisions already made (do not redo unprompted)

- **Static `MainActivity.singleton` / `TimerActivity.instance` pattern is left as is.** It is a real leak risk in theory, but refactoring it (for example with `WeakReference`) was judged not worth the risk for this app. Only touch it if the user asks.
- **Theme is still Holo** (`ThemeX` in `res/values/themes_themex.xml`). `AppTheme`/`AppBaseTheme` in `styles.xml` are dead leftovers. A Material 3 migration was discussed but not requested.
- `NumberPicker` wheels for time entry are fine to keep. It is the same pattern as Google's own Clock timer.
- The manifest requires `android.hardware.touchscreen` so Play hides the app from non-touch devices. There is intentionally no Wear OS / TV build, so do not add `android.hardware.type.watch`.
- The Data Safety form, content rating and ads declaration already exist in Play Console (the app has been published since ~2015). Do not treat them as pending work.

## Gotchas

- `android.R.string.yes` resolves to **"OK"**, not "Yes". Use `R.string.yes`. (`android.R.string.ok` is fine.)
- `SoundPool.load()` is asynchronous. Keep loading kicked off early (at `startTimer()`), well before the first alert plays.
- `DialogFragment` subclasses have no `Context` at construction time. Store string resource IDs and resolve them in `onCreateDialog()`.
- `build.gradle` forces `kotlin-stdlib` versions in `resolutionStrategy` to avoid a duplicate-class build error. Leave that block alone.
- Play Store privacy policy lives at https://moriest.fi/IntervalTimer/ (served from `docs/` via GitHub Pages, which is why the repo must stay public). It is also linked from the in-app Settings screen, and Play policy requires both.
- Icon sources are SVGs in `design/icon/`. The Play Store 512x512 listing icon is a separate file the user keeps outside the repo.
