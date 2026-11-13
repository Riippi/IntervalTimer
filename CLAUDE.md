# Interval Timer X

Android interval/workout timer (`org.leeko.intervaltimer`), originally built in 2015, modernized and redesigned in 2026. Published on Google Play. Single Gradle module (`app`), Java, classic View system (no Compose, no Kotlin sources), Material 3 dark. `minSdk 21`, `compileSdk`/`targetSdk 36`.

This repo is **public**. Never put secrets, passwords or account details in any tracked file.

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home   # AGP 9.x needs JDK 17, not the newer openjdk on this machine
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/
./gradlew bundleRelease      # signed AAB -> app/build/outputs/bundle/release/app-release.aab
./gradlew testDebugUnitTest lintDebug
```

- Gradle 9.6.1, Android Gradle plugin 9.5.1. AGP 9 requires `proguard-android-optimize.txt` as the default rules file.
- The Android SDK is the Homebrew cask install (`/opt/homebrew/Caskroom/android-commandlinetools/<version>`), not `~/Library/Android/sdk`. `local.properties` (gitignored) points at it. `adb` is not on the PATH: use `<sdk>/platform-tools/adb`.
- Libraries are at the newest versions that still support `minSdk 21`. The latest `core`, `appcompat`, `fragment` and Material need `minSdk 23`; raising it is the user's decision.
- Do not add a `resolutionStrategy` that forces `kotlin-stdlib` versions. An old one was removed: it downgraded the plugin's own lint and broke the release build.
- CI: `.github/workflows/android.yml` runs `assembleDebug testDebugUnitTest lintDebug` on every push to `main` and every pull request.
- Unit tests (pure JVM): `PhasePlanTest` and `counter/CounterTest`. `CounterTest` drives the four counters one second at a time through the package-private `BaseTimer.tick()` and checks every second against `PhasePlan`. It needs `unitTests.returnDefaultValues = true` in `app/build.gradle` so `Handler` and `SystemClock` do nothing on the JVM.
- Lint is at 0 errors. The 36 warnings left are deliberate choices (static activity instances, required touchscreen, portrait lock, `minSdk 21` library versions, target API 36) or cosmetic (overdraw, nested weights, launcher icon shape). Two findings are suppressed in code with a comment each: `GestureBackNavigation` on `TimerActivity.onBackPressed()` and `SoonBlockedPrivateApi` on the `PickerText` reflection.

## Testing on an emulator

Setup
- AVDs: `IntervalTimerTest36` (API 36, the one to use) and `IntervalTimerTest` (API 34). Boot headless: `<sdk>/emulator/emulator -avd IntervalTimerTest36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot`
- `avdmanager` is broken on this SDK install, so AVDs are created by hand-writing `~/.android/avd/<name>.ini` and `config.ini`. Do not try to fix `avdmanager`.
- Edge-to-edge and back-button behavior only reproduce on API 35+ / 36. Do not rely on the API 34 AVD for them.
- With a phone plugged in as well, always pass `-s emulator-5554` to `adb` so test commands never reach the phone.
- Switching between debug-signed and release-signed builds on the same device fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. `adb uninstall org.leeko.intervaltimer` first. The user's phone takes the release APK with `adb -d install -r` (signatures match the Play build, data is kept).
- Before `adb emu kill`, run `adb shell sync`. Without it a change made just before can be lost.

Harness and shell
- Foreground `sleep` is blocked in the harness, so wait with `adb shell 'sleep N'` instead.
- zsh: never name a shell variable `path` (or `PATH`-like names); in zsh `path` is tied to `$PATH` and the rest of the script then fails with "command not found".

Driving the app
- `uiautomator dump` fails with "could not get idle state" on the live timer screen because the UI never idles, and the dump file then still holds the previous screen. Dump from a static screen (main screen, paused or DONE state), or use screenshots.
- The app logs nothing on purpose. Verify behavior from screenshots, UI dumps and `logcat | grep FATAL`.
- The Undo snackbar lasts 6 seconds. When testing Undo over adb, tap it in the same command as the delete.
- Fast test workouts (debug build only): `adb shell "run-as org.leeko.intervaltimer sqlite3 databases/workouts.db \"update workout set warm_up_sec=5,rounds=2,work_sec=6,rest_sec=4 where _id=1;\""` (table `workout`, columns in `database/WorkoutTable.java`; `force-stop` the app first). The app reopens on the last viewed tab.
- The release build is not debuggable, so no sqlite there. For a short workout set Rounds to 1 in the rounds dialog (tap the number above the selected one to step down): Tabata is then 30 seconds and reaches DONE.
- Rotation: `adb shell settings put system accelerometer_rotation 0` then `user_rotation 1` (landscape) / `0` (portrait). The system may reset `user_rotation` to 0 after an app forces an orientation, so re-apply it before checking other screens. Put `accelerometer_rotation` back to 1 afterward.
- Large screens without a tablet AVD: `adb shell wm size 1600x2400` + `wm density 240` (undo with `wm size reset`, `wm density reset`).
- Activity rebuilds mid-workout: `settings put system font_scale 1.15`, `cmd uimode night yes`, `wm size 900x2400`.

Sound, calls and background
- Alert sounds: `adb shell dumpsys audio | grep "event:started"` lists every SoundPool play with a timestamp (find the app's `piid` on its `new player ... package:org.leeko.intervaltimer` line). The same dump shows the audio focus stack and the audio mode (`grep mMode`).
- Phone calls: `adb emu gsm call <number>`, then `gsm accept` / `gsm cancel`.
- Frozen in the background: after Home, open two other apps and wait about 10 seconds; `dumpsys activity processes org.leeko.intervaltimer | grep isFrozen` shows when Android froze the app. `adb shell am kill org.leeko.intervaltimer` (app in the background) simulates the system killing it.
- Backup and restore: `bmgr` with the local transport (`bmgr transport com.android.localtransport/.LocalTransport`, `bmgr backupnow <package>`, `pm clear`, `bmgr restore 1 <package>`). `backupnow` answers "Backup is not allowed" for a force-stopped app, so launch it first. Put the transport and `bmgr enable` back afterward.

## Release rules

- The release build is shrunk with R8 (`minifyEnabled` and `shrinkResources`): APK about 4.0 MB, bundle about 4.4 MB. `proguard-rules.txt` has no custom rules and none were needed. **Before uploading, run the release build (`assembleRelease`, install `app-release.apk`) through every screen**, because R8 problems only show up there. `app/build/outputs/mapping/release/mapping.txt` decodes crash traces; Play Console gets it automatically from the bundle.
- Every upload to Play Console needs a **new `versionCode`** in `app/build.gradle`. Play rejects a re-upload of a versionCode that was already used, even on a different track. Bump `versionName` alongside it. The current one is 2.6 (versionCode 9).
- Signing config reads `keystore.properties` (gitignored, along with `*.jks` / `*.keystore`). If the file is absent the project still builds, unsigned. **Never read, print, echo or commit its contents or the keystore.** Passwords and passphrases are the user's: they run `keytool` / `ssh-keygen` themselves in their own terminal.
- Play Console and account-level settings are done by the user in the browser. Claude cannot reach them.
- Release notes and store listing copy: **no em dashes**, keep them short.
- The Data Safety form, content rating and ads declaration already exist in Play Console. Do not treat them as pending work.
- The privacy policy lives at https://moriest.fi/IntervalTimer/ (served from `docs/` via GitHub Pages, which is why the repo must stay public). It is also linked from the in-app Settings screen, and Play policy requires both.
- Play is expected to require target API 37 from August 2027. That is a separate piece of work.

## Conventions

- Commit messages end with the `Co-Authored-By` trailer given in the session attribution instructions.
- Check `git status` before any destructive git operation.
- Verify changes empirically (emulator screenshots, UI dumps, logcat) rather than assuming "it compiles" is enough. Every change in this project's history was tested that way.

## Architecture

Screens
- `MainActivity` (`AppCompatActivity`): the workout setup screen. One chip per saved workout (max 10, built in code from `chip_workout.xml`), a `ViewPager` of workout pages (`page_workout.xml`, filled by `WorkoutPageBinder`), and a pinned START button. `layout-land/` has a compact variant. Each page: hero card (name, total from `PhasePlan`, `TimelineView`), a card of four tappable setting rows (`row_setting.xml`), and a manual-rest `MaterialSwitch`. With manual rest on, the Rest row is dimmed, not tappable, and shows "Manual". The screen redraws itself in `onResume()` when `WorkoutModel.getRevision()` changed.
- Roller dialogs in `dialog/` (`TimeDialog`, `RoundsDialog`): `MaterialAlertDialogBuilder` dialogs with `NumberPicker` wheels that read their arguments in `onCreate` so they survive rotation. They close only with Set, Cancel or Back (`setCanceledOnTouchOutside(false)`), because a wheel swipe that ended next to the dialog used to close it and lose the change. The rounds wheel is as wide as the dialog so "Unlimited" fits. `dialog/PickerText` makes the digits 26sp (`NumberPicker.setTextSize()` on Android 10+, a best-effort reflection fallback on Android 5-9 that keeps the default size if it fails).
- `dialog/WorkoutMenuSheet` (a `BottomSheetDialogFragment`; its host implements `WorkoutMenuSheet.Host`, which `MainActivity` and `ManageActivity` do): inline rename, Duplicate (copy goes in right after and is selected; disabled at the 10-workout limit), Move left/right (the sheet stays open and follows the workout), Delete (no confirmation; the shared `UndoDelete` shows a 6 second Undo snackbar). Delete is disabled when only one workout is left. After a delete the workout that took its place is selected. Undo after re-adding at the limit can give 11 workouts; that is accepted, since refusing the Undo would lose the workout.
- `ManageActivity`: `RecyclerView` + `ItemTouchHelper`. Drag by the handle only, swipe left to delete with Undo, tap a row to open it on the main screen, per-row menu button opens the same sheet. The last workout cannot be swiped away. Reorder is saved on drop through `WorkoutModel.moveWorkout()`. It goes back to `MainActivity` first if Android restarted the app with it on top.
- `TimerActivity` (a plain `Activity`, theme `ThemeTimer`): the running-workout screen. The whole window takes the phase color (warm-up yellow, work green, rest red, done grey; fades in 350 ms), digits auto-fit the screen, one full-width Pause/Resume/Start/Restart pill. `AppController` calls `switchState()` / `updateView()`; both just call `render()`, which redraws everything from the counter's current state. Portrait in `layout/`, landscape in `layout-land/`; rotation swaps layouts in `onConfigurationChanged` (the manifest declares `configChanges`). Manual rest while running: REST color, big "READY?", START pill. The exit confirmation dialog is `setCancelable(false)` on purpose: only Cancel / Yes close it.
- `SettingsActivity` (`AppCompatActivity`) hosting a `PreferenceFragmentCompat`, screen defined in `res/xml/pref_general.xml`. The preference keys and the default shared preferences file are unchanged from the 2015 version, so saved settings carry over. Switches use `layout/pref_switch_widget.xml` (a `MaterialSwitch`); the list dialogs get their look from `ThemeOverlay.IntervalTimer.AlertDialog` (androidx.preference builds them with AppCompat's `AlertDialog`, so the theme has to style them); disabled rows dim through `pref_text_*` color state lists. "Lock timer to portrait" (`Utils.PREF_LOCK_PORTRAIT`, default off) only affects `TimerActivity` and is hidden on screens >= 600dp (`Utils.isLargeScreen()`), because Android 16 ignores orientation locks there.

Timer core
- `counter/`: `CounterFactory` picks a `BaseTimer` subclass from (manual rest?) x (unlimited rounds?): `DefaultTimer`, `DefaultManual`, `UnlimitedNormal`, `UnlimitedManual`. Rounds value `0` means **unlimited**. `BaseTimer` ticks once per second on the main-thread `Handler`, scheduled against a fixed `SystemClock.elapsedRealtime()` anchor so it cannot drift.
- `PhasePlan`: the ordered warm-up/work/rest phases of a workout (plain Java). It only describes the workout for the progress bar and the "Next: ..." line; it never drives the timer. Rest is left out when rest is 0 or manual rest is on; unlimited rounds is one repeating cycle. Do not make the counters depend on it. `BaseTimer.getTotalSeconds()` (and the override in `DefaultManual`) and `PhasePlan.totalSeconds()` compute the same total in two places, so change both together; `CounterTest` checks that they agree.
- `AppController` (singleton): owns the active counter, alert sounds (`SoundPool`, preloaded by `ensureSoundPoolLoaded()` at `startTimer()`), vibration (`vibrateCompat()`, `VibrationEffect` on API 26+), and pausing on calls. It takes **no audio focus**: the beeps play on top of the user's music. (Holding focus for the whole workout stopped Spotify when a workout started, and starting Spotify paused the workout.) Calls are detected by polling `AudioManager.getMode()` once per tick: a change away from `MODE_NORMAL` pauses the workout. Only the change counts, so resuming during a call works. On a silent phone the mode only changes when the call is answered.
- There is no foreground service. The timer keeps running in the background only as long as Android leaves the process alone. Android 16 logs "background playback would be muted" for beeps played in the background. Adding a service is the user's decision.

Data
- `WorkoutModel` + `contentprovider/MyContentProvider` + `database/`: SQLite behind a `ContentProvider`, with an in-memory `HashMap` cache keyed by tab id. Default workouts are seeded when the database is first created (`WorkoutTable.onCreate`): Tabata Protocol, HIIT 40/20, EMOM 10, Boxing 3x3, Norwegian 4x4, Manual Rest.
- The tab ids are the workouts' positions and must be exactly 0, 1, 2...: a gap crashes the main screen at launch and a duplicate hides a workout. Two things keep them that way: `MyContentProvider.applyBatch()` runs each batch (delete, reorder, duplicate, undo) in one transaction, and `WorkoutModel.loadAllWorkouts()` reads the rows in position order, renumbers them and writes any correction back.
- **`WorkoutTable.onUpgrade()` must never drop the table.** When the schema changes, raise `DATABASE_VERSION` and add an `ALTER TABLE` step there (it is an empty hook today).
- `WorkoutModel` reaches the database through the application context, which `MyContentProvider.onCreate()` hands it with `WorkoutModel.attach()` (a provider is created before any activity). It only calls `MainActivity` to redraw, and skips that when the main screen does not exist. `WorkoutModel.insertWorkoutAt()` (used by Duplicate and Undo) shifts the later workouts one place right; `Workout.copy()` makes a copy without a database id.
- Auto Backup is on with no rules: the database and both preference files are backed up and restored.

Shared pieces
- `view/FitTextView` (largest text size that fits its box, sized from a digits-as-zeros template so it stays still while ticking; 800sp upper limit), `view/SegmentedProgressView` (one rounded segment per phase), `view/MaxWidthLinearLayout`.
- `EdgeToEdgeHelper`: applies system-bar insets as padding on each activity's content root. Required because targeting API 35+ forces edge-to-edge.
- Large screens: on screens 600dp wide or more the setup screens are capped at 640dp and centered (`dimen/content_max_width`: 10000dp by default, 640dp in `values-sw600dp`; `layout_constraintWidth_max` in the manage screen). The timer stays full screen.
- Themes: `Theme.IntervalTimer` is the application theme, `ThemeTimer` a small variation for the running timer.
- Fonts: Inter (text) and Space Grotesk (headings, numbers) are bundled in `res/font/` as static, Latin-only instances (about 530 KB). Licenses are in `assets/licenses/`. Weights in use: Inter 400/500/600/700, Space Grotesk 500/600/700. Load them through `Fonts` (works below API 26).
- All UI strings live in `res/values/strings*.xml`. The app is English only and declares `android:supportsRtl="false"`.
- The only permission is `VIBRATE`. Do not reintroduce `READ_PHONE_STATE` (Play restricts it) or `WAKE_LOCK` (nothing uses it).

## Design

- `design/redesign/DESIGN_HANDOFF.md` is the source for the look (the mockups next to it are not shipped). The redesign shipped as 2.5. Play Store screenshots are in `design/store-screenshots/`, icon sources (SVG) in `design/icon/`. The Play Store 512x512 listing icon is a separate file the user keeps outside the repo.
- Deliberate deviations from the handoff, all user decisions:
  - No Previous/Next phase buttons. Do not add them, and do not give the counters a "jump to phase" method.
  - Rounds `0` is "Unlimited" even though the handoff says rounds 1-99.
  - Time settings are tap-to-pick with `NumberPicker` wheels, no − / + steppers, with 1-second precision.
  - "Last one, finish strong" has a comma instead of an em dash.
  - The exit confirmation dialog is kept when ending a workout.

## Accessibility (done, keep it that way)

- Contrast was calculated, not eyeballed. Text on the phase colors is `on_phase` (#0A0A0A); the secondary text is 80% of it (the design's 72% is only 4.0:1 on the red rest color) and the paused digits are 60% (the design's 45% is 2.4:1 on red). Everything on the dark surfaces is 5.8:1 or better. Recompute with the luminance formula if a color changes.
- Icon-only buttons have content descriptions (set in code where the text contains the workout name). Screen titles and the workout name are marked as headings. The manage rows offer Move left / Move right actions because dragging does not work with a screen reader; the settings rows read as buttons ("Work, 0:20, tap to change").
- System text size up to 200% works on every setup screen: rows grow (`minHeight`, not fixed `height`), labels may wrap, the main page and Settings scroll. The timer digits are not scaled by the system font size, they fit the screen instead.

## Decisions already made (do not redo unprompted)

- **Static `MainActivity.singleton` / `TimerActivity.instance` pattern is left as is.** It is a leak risk in theory, but refactoring it was judged not worth the risk. Only touch it if the user asks.
- **The `ContentProvider` stays**, even though the database is private to the app.
- **`ViewPager` 1 stays.** Moving to `ViewPager2` is a rewrite of the main screen's paging with no user benefit.
- The manifest requires `android.hardware.touchscreen` so Play hides the app from non-touch devices. There is intentionally no Wear OS / TV build.

## Gotchas

- **`TimerActivity.onCreate` must only start the timer when `savedInstanceState == null`** (opened by START). Android rebuilds the activity on font size, dark mode and window size changes, and starting the timer there restarts the workout from zero. When it is rebuilt and `AppController.hasTimer()` is false, the process was killed in the background: the activity finishes and the user lands on the main screen, because the running workout only lives in memory.
- **Late ticks are silent.** After the app was frozen in the background, `BaseTimer` replays the missed ticks at once to get the time right. Ticks more than 2 seconds late report `isCatchingUp()` and `AppController.alert()` skips their sound and vibration. Without that every missed beep fires within a second.
- **Back button**: once an app targets API 36, Android 16 stops calling `onBackPressed()`. `TimerActivity` registers an `OnBackInvokedCallback` (API 33+) and sets `android:enableOnBackInvokedCallback="true"`; `onBackPressed()` stays for older versions. Test back-button behavior on the API 36 emulator.
- **Do not set `android:fontFamily` / `fontFamily` as a theme item.** It overrides the fonts that individual views set in their layouts. Per-view `android:fontFamily="@font/..."` works; Material components get their fonts from the `textAppearance*` overrides in `themes_app.xml`.
- The status and navigation bars are colored by hand below API 35 (`applyColor()`), while from API 35 the transparent bars let the window background show through. Check both.
- `android.R.string.yes` resolves to **"OK"**, not "Yes". Use `R.string.yes`. (`android.R.string.ok` is fine.)
- `SoundPool.load()` is asynchronous. Keep loading kicked off early (at `startTimer()`), well before the first alert plays.
- `DialogFragment` subclasses have no `Context` at construction time. Store string resource IDs and resolve them in `onCreateDialog()`.
