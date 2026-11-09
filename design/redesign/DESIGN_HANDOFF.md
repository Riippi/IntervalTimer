# Interval Timer X — UI redesign handoff

Source of truth: the Design canvas "Interval Timer X redesign"
(https://claude.ai/artifact/Yaep7JqVv4Kd4nZHivGPxE). The `screens/*.dc.html` files next to this
document are that canvas's HTML mockups. Open them for exact spacing and values, but
**do not ship them**: rebuild the screens natively (Material 3, dark theme only).

## Goals
- Modern Material 3 look, stays dark.
- Keep the fast swipe-between-workouts model, but replace "tap the tab name to open a menu"
  with a visible ⋮ button + bottom sheet.
- Running timer readable from across the room: full-screen phase color, maximum-size digits.

## Design tokens

### Colors (dark only)
| Token | Hex | Use |
|---|---|---|
| background | #0E0F11 | screen background |
| surface | #17191C | cards (hero card, settings card, list rows) |
| surfaceHigh | #1E2125 | bottom sheet |
| surfaceButton | #23262A | round stepper / icon buttons |
| outline | #34383E | unselected chip border |
| divider | #24272B | row dividers |
| onSurface | #ECEDEE | primary text |
| onSurfaceVariant | #A1A7AE | secondary text |
| onSurfaceMuted | #8E949C | tertiary text, captions |
| phaseWarmup | #FFD60A | warm-up (yellow) |
| phaseWork | #2BE04F | work (green), also primary/Start |
| phaseRest | #FF3B30 | rest (red) |
| phaseDone | #EDEEEF | finished state |
| onPhase | #0A0A0A | all text/icons on phase colors and on the green Start button |
| destructive | #FF8080 | "Delete workout" text in sheet |

Traffic-light colors mean the same thing everywhere (setup timeline, legend, row dots, running screen).

### Typography
- Body/UI: **Inter** (400/500/600/700).
- Headings and all numbers: **Space Grotesk** (500/600/700), numbers with tabular figures
  (`fontFeatureSettings = "tnum"`).
- Key sizes: app bar title 20sp/600 · workout name 24sp/600 · setup total 56sp/600 ·
  stepper value 22sp/600 · row label 16sp/500 · caption 13sp.
- Running screen: phase label 64sp/700 (+0.04em) · round line 24sp/600 ·
  **digits 180sp/700, letter-spacing −0.05em, line-height 0.88** (portrait, `m:ss`);
  140sp when the string is 5 chars (`10:44`). Landscape: 250sp / 200sp.
  Better: auto-size the digits to the available width (largest size that fits).

### Shape & spacing
- Cards radius 24dp, bottom sheet top radius 28dp, chips/pills fully rounded.
- Screen side padding 16dp (cards), 20dp (text).
- Touch targets ≥ 44–48dp. Steppers 44dp round. Start button 64dp tall, Pause 72dp.

## Screens

### A1 · Workout setup (main screen)
- Top app bar: "Interval Timer", actions: New workout (+), Settings.
- **Workout chips** row (horizontally scrollable, replaces the old tab bar): selected chip filled
  #ECEDEE with dark text, others outlined. Swiping the content left/right switches workout and
  keeps the selected chip scrolled into view.
- **Hero card**: caption "Workout 1 of 5 · swipe to switch", workout name, ⋮ button (opens A2),
  total duration (big), **timeline bar** = one segment per phase, width ∝ seconds, colored by phase
  (warm-up yellow, work green, rest red; with manual rest a thin red marker between rounds),
  legend below.
- **Settings card**, 4 rows: Warm-up, Rounds, Work, Rest. Each row: phase dot, label, − value +.
  Time step: 5 s up to 1:00, 15 s above. Rounds 1–99. Work min 0:05.
  Tapping the value should open a duration picker for big jumps.
- **Manual rest** card: label + "Wait for a tap before each round" + switch. When on, the Rest row
  is dimmed/disabled and shows "Tap", and total reads "total + manual rests".
- **Start** button: full-width, pinned bottom, green, play icon.
- Total = warm + rounds × work + (rounds − 1) × rest (no rest after the last round;
  rest excluded when manual).

### A2 · Workout menu (modal bottom sheet from ⋮)
Header: name + "8 rounds · 0:20 work · 0:10 rest · 3:50". Actions: Rename (inline text field +
Cancel/Save in the sheet), Duplicate (inserted right after, selected), Move left / Move right
(disabled at the ends, sheet stays open), "Reorder & manage all" → A3, divider, Delete workout
(disabled when only one workout). Delete closes the sheet and shows a **snackbar with Undo** (no
confirm dialog).

### A3 · Manage workouts
List with drag handles (drag to reorder = swipe order), name, summary, total, ⋮ per row.
Swipe a row left to delete (with Undo). "New workout" outlined button at the bottom.

### B · Alternative home (not chosen by default)
Workout list as home with a quick-start play button per card; "Last used" highlighted.
Kept on the canvas for comparison only.

### R1–R4 · Running timer
- **Whole screen background = phase color** (animate color change ~350 ms). No card.
- Top: workout name (left), ✕ End workout (right, small).
- Center: phase label (WARM-UP / WORK / REST / DONE), round line
  ("Get ready · 4 rounds" / "Round 1 of 4" / "Next: round 2 of 4"), **huge digits** (remaining
  time in this phase), "Next: Rest 1:00".
- Paused: digits at 45 % opacity + black "PAUSED" pill.
- Bottom: segmented progress bar (one segment per phase, width ∝ duration, done = black,
  todo = black 18 %), Elapsed / Remaining, controls: Previous phase · Pause/Resume (big black pill)
  · Next phase. Previous jumps to the start of the current phase, or to the previous phase if
  within the first 2 s.
- Finished: DONE screen, button "Restart".
- **Landscape layout (R4)**: digits on the left at max size, side panel with next/remaining/
  controls, progress bar across the bottom. Support rotation while running.
- Keep the screen on while running (`FLAG_KEEP_SCREEN_ON`).

## Accessibility
- Phase is always shown as a word as well as a color (red/green colorblindness).
- Icon-only buttons need content descriptions ("Workout options", "Decrease Rounds", …).
- Switch exposes role switch + state. Respect font scaling, except let the digits auto-fit.

## Out of scope / open
- Duration picker dialog design, settings screen, sounds/vibration, new-workout flow.
