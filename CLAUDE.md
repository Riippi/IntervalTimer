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
- Rotation: `adb shell settings put system accelerometer_rotation 0` then `user_rotation 1` (landscape) / `0` (portrait). The system may reset `user_rotation` to 0 after an app forces an orientation, so re-apply it before checking other screens. Put `accelerometer_rotation` back to 1 afterward.
- Large-screen behavior without a tablet AVD: `adb shell wm density 240` makes the phone AVD report sw720dp. Undo with `adb shell wm density reset`.
- Foreground `sleep` is blocked in the harness, so wait with `adb shell 'sleep N'` instead.
- Fast test workouts: the debug build allows `adb shell "run-as org.leeko.intervaltimer sqlite3 databases/workouts.db \"update workout set warm_up_sec=5,rounds=2,work_sec=6,rest_sec=4 where _id=1;\""` (table `workout`, columns in `database/WorkoutTable.java`; `force-stop` the app first). A 20 second workout reaches every timer state quickly. The app reopens on the last viewed tab, so rewriting that tab's row between runs is more reliable than tapping tabs.
- Unit tests (pure Java, no emulator): `./gradlew testDebugUnitTest`. Only `PhasePlan` has tests so far.

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
- `TimerActivity`: the running-workout screen, already in the new design (see Redesign below). The whole window takes the phase color (warm-up yellow, work green, rest red, done grey; fades in 350 ms), digits auto-fit the screen, one full-width Pause/Resume/Start/Restart pill. `AppController` still calls `switchState()` / `updateView()`; both just call `render()`, which redraws everything from the counter's current state. Portrait in `layout/`, landscape in `layout-land/`; rotation swaps layouts in `onConfigurationChanged` (the manifest declares `configChanges`). Launched from `MainActivity` through an `ActivityResultLauncher`; uses its own `ThemeTimer`.
- `PhasePlan`: the ordered warm-up/work/rest phases of a workout (plain Java, unit tested). It only describes the workout for the progress bar and the "Next: ..." line; it never drives the timer. Rest is left out when rest is 0 or manual rest is on; unlimited rounds (0) is one repeating cycle. Do not make the counters depend on it.
- `view/FitTextView` (largest text size that fits its box, sized from a digits-as-zeros template so it stays still while ticking) and `view/SegmentedProgressView` (one rounded segment per phase). `Fonts` loads the bundled fonts.
- `SettingsActivity`: legacy `PreferenceActivity` + `PreferenceFragment`, screen defined in `res/xml/pref_general.xml`. The "Lock timer to portrait" checkbox (`Utils.PREF_LOCK_PORTRAIT`, default off) only affects `TimerActivity`. It is hidden on screens >= 600dp (`Utils.isLargeScreen()`), because Android 16 ignores orientation locks there for apps targeting API 36.
- `AppController` (singleton): owns the active counter, alert sounds (`SoundPool`, preloaded by `ensureSoundPoolLoaded()` at `startTimer()`), vibration (`vibrateCompat()`, `VibrationEffect` on API 26+), and audio focus. Losing audio focus pauses the workout. This replaced `READ_PHONE_STATE`, which Play restricts, so do not reintroduce that permission.
- `counter/`: `CounterFactory` picks a `BaseTimer` subclass from (manual rest?) x (unlimited rounds?): `DefaultTimer`, `DefaultManual`, `UnlimitedNormal`, `UnlimitedManual`. `BaseTimer` ticks once per second on the main-thread `Handler`, scheduled against a fixed `SystemClock.elapsedRealtime()` anchor so it cannot drift.
- `WorkoutModel` + `contentprovider/MyContentProvider` + `database/`: SQLite persistence behind a `ContentProvider`, with an in-memory `HashMap` cache keyed by tab id. Default workouts are seeded when the database is first created.
- `EdgeToEdgeHelper`: applies system-bar insets as padding on each activity's content root. Required because targeting API 35+ forces edge-to-edge.
- All UI strings live in `res/values/strings*.xml`. A few preference labels in `pref_general.xml` are still inline literals.

## Redesign (in progress)

Source of truth: `design/redesign/DESIGN_HANDOFF.md` (mockups next to it are not shipped; rebuild natively, Material 3 dark). Work happens on the `redesign` branch so `main` stays releasable. Order: (1) running timer, done; (2) Material 3 foundation (AppCompat + Material libraries, dark theme, move the activities); (3) main screen with workout chips + swipe, hero card, steppers; (4) the bottom sheet from the menu button; (5) manage screen; (6) polish.
- Previous/Next phase buttons were removed from the design on purpose. Do not add them, and do not give the counters a "jump to phase" method.
- Rounds `0` stays "Unlimited" even though the handoff says rounds 1-99.
- **Time settings stay tap-to-pick, no − / + steppers** (user decision). On the main screen each setting row (warm-up, rounds, work, rest) is one tappable row: phase dot, label, value on the right in Space Grotesk on a small button-grey pill with a chevron so it reads as tappable. Tapping opens the existing roller dialogs (`TimeDialog`, `RoundsDialog`; minutes and seconds wheels, rounds wheel with "Unlimited" at 0), restyled for the dark theme (surfaceHigh, 28dp corners, new fonts). Keep the callbacks (`onDialogPositiveClick`, `onRoundsDialogOk`) and the 1-second precision. With manual rest on, the Rest row is dimmed, not tappable, and shows "Manual". Show the user a screenshot of the restyled dialog before calling it done (the handoff leaves the picker dialog design open).
- Manual rest while running: REST color, big "READY?", START pill (the design did not cover this state).
- Two deliberate deviations from the mockup text: "Last one, finish strong" has a comma instead of an em dash, and the exit confirmation dialog is kept when ending a workout.
- Fonts: Inter (text) and Space Grotesk (headings, numbers) are bundled in `res/font/` as static, Latin-only instances cut from the Google Fonts variable fonts (about 530 KB). Licenses are in `assets/licenses/`. The weights in use are Inter 400/500/600/700 and Space Grotesk 500/600/700. Load them through `Fonts` (works below API 26).

## Decisions already made (do not redo unprompted)

- **Static `MainActivity.singleton` / `TimerActivity.instance` pattern is left as is.** It is a real leak risk in theory, but refactoring it (for example with `WeakReference`) was judged not worth the risk for this app. Only touch it if the user asks.
- **Theme: the editor and Settings are still Holo** (`ThemeX` in `res/values/themes_themex.xml`); the running timer uses `ThemeTimer`. `AppTheme`/`AppBaseTheme` in `styles.xml` are dead leftovers. The Material 3 migration is stage 2 of the redesign above.
- `NumberPicker` wheels for time entry are fine to keep. It is the same pattern as Google's own Clock timer.
- The manifest requires `android.hardware.touchscreen` so Play hides the app from non-touch devices. There is intentionally no Wear OS / TV build, so do not add `android.hardware.type.watch`.
- The Data Safety form, content rating and ads declaration already exist in Play Console (the app has been published since ~2015). Do not treat them as pending work.

## Gotchas

- **Back button on Android 13+**: once an app targets API 36, Android 16 stops calling `onBackPressed()` and the key event. The old exit-confirmation dialog silently stopped showing on Android 16 devices (it still showed on the Android 14 emulator, which hid the bug). `TimerActivity` now registers an `OnBackInvokedCallback` (API 33+) and sets `android:enableOnBackInvokedCallback="true"`; `onBackPressed()` stays for older versions. Test back-button behavior on an API 36 emulator, not only API 34.
- The status and navigation bars are colored by hand below API 35 (`applyColor()`), while from API 35 the transparent bars let the window background show through. Check both.

- `android.R.string.yes` resolves to **"OK"**, not "Yes". Use `R.string.yes`. (`android.R.string.ok` is fine.)
- `SoundPool.load()` is asynchronous. Keep loading kicked off early (at `startTimer()`), well before the first alert plays.
- `DialogFragment` subclasses have no `Context` at construction time. Store string resource IDs and resolve them in `onCreateDialog()`.
- `build.gradle` forces `kotlin-stdlib` versions in `resolutionStrategy` to avoid a duplicate-class build error. Leave that block alone.
- Play Store privacy policy lives at https://moriest.fi/IntervalTimer/ (served from `docs/` via GitHub Pages, which is why the repo must stay public). It is also linked from the in-app Settings screen, and Play policy requires both.
- Icon sources are SVGs in `design/icon/`. The Play Store 512x512 listing icon is a separate file the user keeps outside the repo.
