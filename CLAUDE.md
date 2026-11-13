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
- Phone-call pausing can be tested with `adb emu gsm call <number>` (then `gsm accept` / `gsm cancel`). `dumpsys audio | grep mMode` shows the audio mode the app reacts to.
- Before `adb emu kill`, run `adb shell sync`. Without it a `sqlite3` change made just before can be lost, and the next boot still has the old test workout.
- Rotation: `adb shell settings put system accelerometer_rotation 0` then `user_rotation 1` (landscape) / `0` (portrait). The system may reset `user_rotation` to 0 after an app forces an orientation, so re-apply it before checking other screens. Put `accelerometer_rotation` back to 1 afterward.
- Large-screen behavior without a tablet AVD: `adb shell wm density 240` makes the phone AVD report sw720dp. Undo with `adb shell wm density reset`.
- Foreground `sleep` is blocked in the harness, so wait with `adb shell 'sleep N'` instead.
- zsh: never name a shell variable `path` (or `PATH`-like names); in zsh `path` is tied to `$PATH` and the rest of the script then fails with "command not found".
- The Undo snackbar lasts 6 seconds. When testing Undo over adb, tap it in the same command as the delete; looking at a screenshot in between is too slow.
- Fast test workouts: the debug build allows `adb shell "run-as org.leeko.intervaltimer sqlite3 databases/workouts.db \"update workout set warm_up_sec=5,rounds=2,work_sec=6,rest_sec=4 where _id=1;\""` (table `workout`, columns in `database/WorkoutTable.java`; `force-stop` the app first). A 20 second workout reaches every timer state quickly. The app reopens on the last viewed tab, so rewriting that tab's row between runs is more reliable than tapping tabs.
- Alert sounds can be checked without app logs: `adb shell dumpsys audio | grep "event:started"` lists every SoundPool play with a timestamp (find the app's `piid` on its `new player ... package:org.leeko.intervaltimer` line). The same dump shows the audio focus stack.
- Background behavior: after Home, open two other apps and wait about 10 seconds; `dumpsys activity processes org.leeko.intervaltimer | grep isFrozen` shows when Android froze the app. `adb shell am kill org.leeko.intervaltimer` (app in the background) simulates the system killing it. Activity rebuilds mid-workout: `settings put system font_scale 1.15`, `cmd uimode night yes`, `wm size 900x2400`.
- Unit tests (pure Java, no emulator): `./gradlew testDebugUnitTest`. Only `PhasePlan` has tests so far.

## Release rules

- The release build is shrunk with R8 (`minifyEnabled` and `shrinkResources` in `app/build.gradle`): release APK about 4.3 MB, bundle about 3.9 MB, versus 7.1 / 6.2 MB unshrunk. `proguard-rules.txt` has no custom rules and none were needed. **Before uploading, run the release build (`assembleRelease`, install `app-release.apk`) through every screen**, because R8 problems only show up there. The release app is not debuggable, so `run-as` (the sqlite trick above) only works on the debug build. `app/build/outputs/mapping/release/mapping.txt` decodes crash traces; Play Console gets it automatically from the bundle.

- Every upload to Play Console needs a **new `versionCode`** in `app/build.gradle`. Play rejects a re-upload of a versionCode that was already used, even on a different track. Bump `versionName` alongside it.
- Signing config reads `keystore.properties` (gitignored, along with `*.jks` / `*.keystore`). If the file is absent the project still builds, unsigned. **Never read, print, echo or commit its contents or the keystore.** Passwords and passphrases are the user's: they run `keytool` / `ssh-keygen` themselves in their own terminal.
- Play Console and account-level settings are done by the user in the browser. Claude cannot reach them.
- Release notes and store listing copy: **no em dashes**, keep them short.

## Conventions

- Commit messages end with the `Co-Authored-By` trailer given in the session attribution instructions.
- Check `git status` before any destructive git operation.
- Verify changes empirically (emulator screenshots, logcat) rather than assuming "it compiles" is enough. Every change in this project's history was tested that way.

## Architecture

- `MainActivity` (an `AppCompatActivity`, theme `Theme.IntervalTimer`, Material 3 dark): the workout setup screen, already in the new design. One chip per saved workout (max 10, built in code from `chip_workout.xml`), a `ViewPager` of workout pages (`page_workout.xml`, filled by `WorkoutPageBinder`), and a pinned START button. `layout-land/` has a compact variant. Each page: hero card (name, total from `PhasePlan`, `TimelineView`), a card of four tappable setting rows (`row_setting.xml`), and a manual-rest `MaterialSwitch`. Tapping a row opens the roller dialogs in `dialog/` (`TimeDialog`, `RoundsDialog`), `MaterialAlertDialogBuilder` dialogs that read their arguments in `onCreate` so they survive rotation. The card's menu button opens `dialog/WorkoutMenuSheet` (a `BottomSheetDialogFragment`; its host activity implements `WorkoutMenuSheet.Host`, which both `MainActivity` and `ManageActivity` do): inline rename (name field plus Cancel/Save inside the sheet), Duplicate (copy goes in right after and is selected; disabled at the 10-workout limit), Move left/right (the sheet stays open and follows the workout), Delete (closes the sheet, no confirmation; `MainActivity.deleteWorkoutWithUndo()` shows a 6 second Undo snackbar anchored above START). Delete is disabled when only one workout is left. Rounds value `0` means **unlimited**. `WorkoutModel` calls `MainActivity.updateWholeTabView()` after add/delete/move/insert. `WorkoutModel.insertWorkoutAt()` (used by Duplicate and Undo) shifts the later workouts one place right; `Workout.copy()` makes a copy without a database id. After a delete the workout that took its place is selected, as in the design.
- `TimerActivity`: the running-workout screen, already in the new design (see Redesign below). The whole window takes the phase color (warm-up yellow, work green, rest red, done grey; fades in 350 ms), digits auto-fit the screen, one full-width Pause/Resume/Start/Restart pill. `AppController` still calls `switchState()` / `updateView()`; both just call `render()`, which redraws everything from the counter's current state. Portrait in `layout/`, landscape in `layout-land/`; rotation swaps layouts in `onConfigurationChanged` (the manifest declares `configChanges`). Launched from `MainActivity` through an `ActivityResultLauncher`; uses its own `ThemeTimer`.
- `PhasePlan`: the ordered warm-up/work/rest phases of a workout (plain Java, unit tested). It only describes the workout for the progress bar and the "Next: ..." line; it never drives the timer. Rest is left out when rest is 0 or manual rest is on; unlimited rounds (0) is one repeating cycle. Do not make the counters depend on it.
- `view/FitTextView` (largest text size that fits its box, sized from a digits-as-zeros template so it stays still while ticking) and `view/SegmentedProgressView` (one rounded segment per phase). `Fonts` loads the bundled fonts.
- `SettingsActivity`: an `AppCompatActivity` with its own header (back arrow, title) hosting a `PreferenceFragmentCompat` (androidx.preference), screen defined in `res/xml/pref_general.xml`. The preference keys and the default shared preferences file are unchanged from the legacy version, so saved settings carry over (verified by upgrading in place). Switches use `layout/preference_widget_switch.xml` (a `MaterialSwitch`), the list dialogs get their look from `ThemeOverlay.IntervalTimer.AlertDialog` (androidx.preference builds them with AppCompat's `AlertDialog`, not `MaterialAlertDialogBuilder`, so the theme has to style them), and disabled rows dim through `pref_text_*` color state lists. The "Lock timer to portrait" checkbox (`Utils.PREF_LOCK_PORTRAIT`, default off) only affects `TimerActivity`. It is hidden on screens >= 600dp (`Utils.isLargeScreen()`), because Android 16 ignores orientation locks there for apps targeting API 36.
- `AppController` (singleton): owns the active counter, alert sounds (`SoundPool`, preloaded by `ensureSoundPoolLoaded()` at `startTimer()`), vibration (`vibrateCompat()`, `VibrationEffect` on API 26+), and pausing on calls. It takes **no audio focus**: the beeps play on top of the user's music. (Holding focus for the whole workout stopped Spotify when a workout started, and starting Spotify paused the workout.) Calls are detected by polling `AudioManager.getMode()` once per tick: a change away from `MODE_NORMAL` pauses the workout. Only the change counts, so resuming during a call works. On a silent phone the mode only changes when the call is answered (the old focus approach behaved the same). Do not reintroduce `READ_PHONE_STATE`, which Play restricts, and do not request audio focus for the whole workout.
- `counter/`: `CounterFactory` picks a `BaseTimer` subclass from (manual rest?) x (unlimited rounds?): `DefaultTimer`, `DefaultManual`, `UnlimitedNormal`, `UnlimitedManual`. `BaseTimer` ticks once per second on the main-thread `Handler`, scheduled against a fixed `SystemClock.elapsedRealtime()` anchor so it cannot drift.
- `dialog/PickerText` makes the digits in the roller dialogs 26sp (`NumberPicker.setTextSize()` on Android 10+, a best-effort reflection fallback on Android 5-9 that silently keeps the default size if it fails). The START / PAUSE / RESUME icons are 28dp (26dp in the landscape timer side button).
- `WorkoutModel` + `contentprovider/MyContentProvider` + `database/`: SQLite persistence behind a `ContentProvider`, with an in-memory `HashMap` cache keyed by tab id. Default workouts are seeded when the database is first created.
- `EdgeToEdgeHelper`: applies system-bar insets as padding on each activity's content root. Required because targeting API 35+ forces edge-to-edge.
- All UI strings live in `res/values/strings*.xml`. A few preference labels in `pref_general.xml` are still inline literals.

## Redesign (in progress)

Source of truth: `design/redesign/DESIGN_HANDOFF.md` (mockups next to it are not shipped; rebuild natively, Material 3 dark). Work happens on the `redesign` branch so `main` stays releasable. Order: (1) running timer, done; (2) Material 3 foundation (AppCompat + Material libraries, `Theme.IntervalTimer`, dialogs), done for `MainActivity` and the dialogs, while `SettingsActivity` is still Holo; (3) main screen with workout chips + swipe, hero card, tappable setting rows, done; (4) the bottom sheet from the menu button, done; (5) manage screen, done: `ManageActivity` (RecyclerView + `ItemTouchHelper`: drag by the handle only, swipe left to delete with the shared `UndoDelete` snackbar, tap a row to open it on the main screen, per-row menu button opens the same sheet without its manage row; the last workout cannot be swiped away; reorder is saved on drop through `WorkoutModel.moveWorkout()`; TalkBack gets Move left/right actions instead of dragging). The main screen redraws itself in `onResume()` when `WorkoutModel.getRevision()` changed. The old tab-id based model is still the storage; `ManageActivity` goes back to `MainActivity` first if Android restarted the app with it on top (`MainActivity.getInstance()` is null then); (6) polish: Settings in Material, large screens, R8 shrinking and the accessibility pass are all done, so the redesign is feature complete. The redesign shipped as 2.5 (versionCode 8): the bundle was built from `redesign`, the release build was clicked through on an API 36 emulator, and `redesign` was merged into `main`. Default workouts (seeded in `WorkoutTable.onCreate`, fresh installs only): Tabata Protocol, HIIT 40/20, EMOM 10, Boxing 3x3, Norwegian 4x4, Manual Rest. Play Store screenshots are in `design/store-screenshots/`. The exit confirmation dialog in `TimerActivity` is `setCancelable(false)`, so Back does not dismiss it, only Cancel / Yes do (kept on purpose).
- **Large screens**: on screens 600dp wide or more the setup screens (main page, START, manage list, Settings) are capped at 640dp and centered (`dimen/content_max_width`: 10000dp by default, 640dp in `values-sw600dp`; `view/MaxWidthLinearLayout` for LinearLayouts, `layout_constraintWidth_max` in the manage screen). The timer stays full screen; its digits are `FitTextView`s with an 800sp upper limit, so they fill whatever space there is. Test without a tablet AVD with `adb shell wm size 1600x2400` + `wm density 240` (undo with `wm size reset`, `wm density reset`).
- Previous/Next phase buttons were removed from the design on purpose. Do not add them, and do not give the counters a "jump to phase" method.
- Rounds `0` stays "Unlimited" even though the handoff says rounds 1-99.
- **Time settings stay tap-to-pick, no − / + steppers** (user decision). On the main screen each setting row (warm-up, rounds, work, rest) is one tappable row: phase dot, label, value on the right in Space Grotesk on a small button-grey pill with a chevron so it reads as tappable. Tapping opens the existing roller dialogs (`TimeDialog`, `RoundsDialog`; minutes and seconds wheels, rounds wheel with "Unlimited" at 0), restyled for the dark theme (surfaceHigh, 28dp corners, new fonts). Keep the callbacks (`onDialogPositiveClick`, `onRoundsDialogOk`) and the 1-second precision. With manual rest on, the Rest row is dimmed, not tappable, and shows "Manual". Show the user a screenshot of the restyled dialog before calling it done (the handoff leaves the picker dialog design open).
- Manual rest while running: REST color, big "READY?", START pill (the design did not cover this state).
- Two deliberate deviations from the mockup text: "Last one, finish strong" has a comma instead of an em dash, and the exit confirmation dialog is kept when ending a workout.
- Fonts: Inter (text) and Space Grotesk (headings, numbers) are bundled in `res/font/` as static, Latin-only instances cut from the Google Fonts variable fonts (about 530 KB). Licenses are in `assets/licenses/`. The weights in use are Inter 400/500/600/700 and Space Grotesk 500/600/700. Load them through `Fonts` (works below API 26).

## Accessibility (done, keep it that way)

- Contrast was calculated, not eyeballed. Text on the phase colors is `on_phase` (#0A0A0A); the secondary text is 80% of it (the design said 72%, which is only 4.0:1 on the red rest color, 80% gives 4.6:1) and the paused digits are 60% (the design's 45% is 2.4:1 on red, 60% is 3:1 or better). Everything on the dark surfaces is 5.8:1 or better. Recompute with the luminance formula if a color changes.
- Icon-only buttons have content descriptions (set in code where the text contains the workout name). Screen titles and the workout name are marked as headings. The workout rows on the manage screen offer Move left / Move right actions because dragging does not work with a screen reader; the settings rows read as buttons ("Work, 0:20, tap to change").
- System text size up to 200% was tested on every setup screen: rows grow (`minHeight`, not fixed `height`), labels may wrap (`maxLines` 2, `singleLineTitle="false"` in Settings), the main page and Settings scroll. The timer digits are not scaled by the system font size, they fit the screen instead.
- Lint is at 0 errors (it was 16 before the redesign). The 52 warnings left are old ones plus ones that do not apply (for example hard-coded `layout_marginLeft` in layouts that were deleted).

## Decisions already made (do not redo unprompted)

- **Static `MainActivity.singleton` / `TimerActivity.instance` pattern is left as is.** It is a real leak risk in theory, but refactoring it (for example with `WeakReference`) was judged not worth the risk for this app. Only touch it if the user asks.
- **Themes**: every screen is Material 3 dark now. `Theme.IntervalTimer` is the application theme (set in the manifest and on each activity), `ThemeTimer` is a small variation for the running timer. The old Holo theme files (`ThemeX`, `values-v11`, `values-v14`, `AppTheme`) were deleted.
- `NumberPicker` wheels for time entry are fine to keep. It is the same pattern as Google's own Clock timer.
- The manifest requires `android.hardware.touchscreen` so Play hides the app from non-touch devices. There is intentionally no Wear OS / TV build, so do not add `android.hardware.type.watch`.
- The Data Safety form, content rating and ads declaration already exist in Play Console (the app has been published since ~2015). Do not treat them as pending work.

## Gotchas

- **`TimerActivity.onCreate` must only start the timer when `savedInstanceState == null`** (opened by START). Android rebuilds the activity on font size, dark mode and window size changes, and starting the timer there restarted the workout from zero. When it is rebuilt and `AppController.hasTimer()` is false, the process was killed in the background: the activity finishes and the user lands on the main screen, because the running workout only lives in memory.
- **Late ticks are silent.** After the app was frozen in the background, `BaseTimer` replays the missed ticks at once to get the time right. Ticks more than 2 seconds late report `isCatchingUp()` and `AppController.alert()` skips their sound and vibration. Without that every missed beep fires within a second.

- **Do not set `android:fontFamily` / `fontFamily` as a theme item.** It overrides the fonts that individual views set in their layouts (everything rendered in Inter Regular). Per-view `android:fontFamily="@font/..."` works; Material components get their fonts from the `textAppearance*` overrides in `themes_app.xml`.

- **Back button on Android 13+**: once an app targets API 36, Android 16 stops calling `onBackPressed()` and the key event. The old exit-confirmation dialog silently stopped showing on Android 16 devices (it still showed on the Android 14 emulator, which hid the bug). `TimerActivity` now registers an `OnBackInvokedCallback` (API 33+) and sets `android:enableOnBackInvokedCallback="true"`; `onBackPressed()` stays for older versions. Test back-button behavior on an API 36 emulator, not only API 34.
- The status and navigation bars are colored by hand below API 35 (`applyColor()`), while from API 35 the transparent bars let the window background show through. Check both.

- `android.R.string.yes` resolves to **"OK"**, not "Yes". Use `R.string.yes`. (`android.R.string.ok` is fine.)
- `SoundPool.load()` is asynchronous. Keep loading kicked off early (at `startTimer()`), well before the first alert plays.
- `DialogFragment` subclasses have no `Context` at construction time. Store string resource IDs and resolve them in `onCreateDialog()`.
- `build.gradle` forces `kotlin-stdlib` versions in `resolutionStrategy` to avoid a duplicate-class build error. Leave that block alone.
- Play Store privacy policy lives at https://moriest.fi/IntervalTimer/ (served from `docs/` via GitHub Pages, which is why the repo must stay public). It is also linked from the in-app Settings screen, and Play policy requires both.
- Icon sources are SVGs in `design/icon/`. The Play Store 512x512 listing icon is a separate file the user keeps outside the repo.
