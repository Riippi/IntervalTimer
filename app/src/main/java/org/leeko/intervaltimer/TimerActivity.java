package org.leeko.intervaltimer;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.leeko.intervaltimer.counter.BaseTimer;
import org.leeko.intervaltimer.view.FitTextView;
import org.leeko.intervaltimer.view.SegmentedProgressView;

/**
 * The running-workout screen. The whole window takes the color of the current phase
 * (yellow warm-up, green work, red rest, light grey when done) and the digits are as
 * big as the screen allows. The counters in counter/ keep time; AppController calls
 * {@link #switchState()} when the phase changes and {@link #updateView()} every second,
 * and both just redraw from the counter's current state.
 */
public class TimerActivity extends Activity {

    private static final long COLOR_ANIMATION_MS = 350;
    // The design says 0.45, which is only 2.4:1 against the red rest color; 0.6 keeps
    // the digits clearly dimmed while staying readable (3:1 or better on every phase).
    private static final float PAUSED_DIGITS_ALPHA = 0.6f;

    private TextView workoutName;
    private TextView phaseLabel;
    private TextView roundText;
    private FitTextView digits;
    private TextView nextText;
    private TextView pausedPill;
    private TextView elapsedText;     // portrait only
    private TextView remainingText;
    private TextView pauseLabel;
    private ImageView pauseIcon;
    private View pauseButton;
    private SegmentedProgressView progressBar;

    private PhasePlan plan;
    private boolean rendered;
    private int targetColor;
    private int displayedColor;
    private ValueAnimator colorAnimator;
    private int lastProgressPercent = -1;

    // OnBackInvokedCallback on Android 13+, kept as Object so older devices never load the class.
    private Object backCallback;

    private static TimerActivity instance;

    public static TimerActivity getInstance() {
        return instance;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional setting: keep the timer upright even if the phone is rotated.
        // Requested before inflating so the portrait layout is the one that loads.
        if (!Utils.isLargeScreen(this)
                && PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean(Utils.PREF_LOCK_PORTRAIT, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_timer);
        EdgeToEdgeHelper.applySystemBarPadding(this);

        instance = this;

        // The phase colors are all light, so the status and navigation bar icons stay dark.
        WindowInsetsControllerCompat bars =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(true);
        bars.setAppearanceLightNavigationBars(true);

        bindViews();
        registerBackHandler();

        // Start the timer. It calls back into switchState()/updateView() right away.
        AppController.getInstance().startTimer();
        render();

        // Keep the screen on
        if (AppController.blockScrensaver) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
        if (backCallback != null && Build.VERSION.SDK_INT >= 33) {
            BackApi33.unregister(this, backCallback);
            backCallback = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Rotation: swap in the portrait or landscape layout and fill it again.
        setContentView(R.layout.activity_timer);
        bindViews();
        lastProgressPercent = -1;
        render();
    }


    private void bindViews() {
        workoutName = (TextView) findViewById(R.id.workoutName);
        phaseLabel = (TextView) findViewById(R.id.phaseLabel);
        roundText = (TextView) findViewById(R.id.roundText);
        digits = (FitTextView) findViewById(R.id.digits);
        nextText = (TextView) findViewById(R.id.nextText);
        pausedPill = (TextView) findViewById(R.id.pausedPill);
        elapsedText = (TextView) findViewById(R.id.elapsedText);
        remainingText = (TextView) findViewById(R.id.remainingText);
        pauseLabel = (TextView) findViewById(R.id.pauseLabel);
        pauseIcon = (ImageView) findViewById(R.id.pauseIcon);
        pauseButton = findViewById(R.id.pauseButton);
        progressBar = (SegmentedProgressView) findViewById(R.id.progressBar);
        if (plan != null) {
            // After a rotation this is a brand new view that has not seen the plan yet.
            progressBar.setPlan(plan);
        }

        if (workoutName != null) {
            workoutName.setTypeface(Fonts.inter(this, 600));
        }
        phaseLabel.setTypeface(Fonts.spaceGrotesk(this, 700));
        roundText.setTypeface(Fonts.inter(this, 600));
        digits.setTypeface(Fonts.spaceGrotesk(this, 700));
        digits.setHeightFactor(0.85f);
        nextText.setTypeface(Fonts.inter(this, 500));
        pausedPill.setTypeface(Fonts.inter(this, 700));
        remainingText.setTypeface(Fonts.spaceGrotesk(this, 600));
        pauseLabel.setTypeface(Fonts.spaceGrotesk(this, 700));
        ((TextView) findViewById(R.id.remainingLabel)).setTypeface(Fonts.inter(this, 700));
        if (elapsedText != null) {
            elapsedText.setTypeface(Fonts.spaceGrotesk(this, 600));
            ((TextView) findViewById(R.id.elapsedLabel)).setTypeface(Fonts.inter(this, 700));
        }

        // TalkBack: announce phase changes, and describe the pill as a button.
        ViewCompat.setAccessibilityLiveRegion(phaseLabel, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        ViewCompat.setAccessibilityDelegate(pauseButton, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonAction();
            }
        });
        findViewById(R.id.endButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestExit();
            }
        });
    }


    /** Called by AppController when the phase changes. */
    public void switchState() {
        render();
    }

    /** Called by AppController every second. */
    public void updateView() {
        render();
    }


    /** Redraws the whole screen from the counter's current state. */
    private void render() {
        Workout workout = AppController.getWorkout();
        if (workout == null || phaseLabel == null) {
            return;
        }
        if (plan == null) {
            plan = PhasePlan.build(workout.getWarmupInSec(), workout.getWorkInSec(),
                    workout.getRestInSec(), workout.getRoundAmount(), workout.getManual());
            progressBar.setPlan(plan);
        }

        AppController app = AppController.getInstance();
        int state = app.getTimerState();
        TimerStats stats = app.getTimerTime();
        boolean paused = app.isTimerPaused();

        boolean over = state == BaseTimer.OVER;
        boolean manualWait = state == BaseTimer.MANUAL_REST;
        boolean showPaused = paused && !manualWait && !over;

        int rounds = workout.getRoundAmount();   // 0 = unlimited
        int round = stats != null ? stats.round : 1;
        int elapsed = stats != null ? stats.elapsed : 0;

        // Phase: color + word (the word matters for people who can't tell red from green)
        int colorRes;
        int labelRes;
        switch (state) {
            case BaseTimer.WORK:
                colorRes = R.color.phase_work;
                labelRes = R.string.state_work;
                break;
            case BaseTimer.REST:
            case BaseTimer.MANUAL_REST:
                colorRes = R.color.phase_rest;
                labelRes = R.string.state_rest;
                break;
            case BaseTimer.OVER:
                colorRes = R.color.phase_done;
                labelRes = R.string.state_done;
                break;
            default:
                colorRes = R.color.phase_warmup;
                labelRes = R.string.state_warmup;
                break;
        }
        showColor(ContextCompat.getColor(this, colorRes), rendered);
        rendered = true;
        phaseLabel.setText(labelRes);

        if (workoutName != null) {
            workoutName.setText(workout.getName());
        }
        roundText.setText(roundLine(state, rounds, round));

        // The big digits: time left in this phase.
        if (over) {
            digits.setText(Utils.secondsToString(stats != null ? stats.total : 0));
        } else if (manualWait) {
            digits.setText(R.string.ready_prompt);
        } else if (stats != null) {
            digits.setText(Utils.integersToString(stats.min, stats.sec));
        } else {
            digits.setText(Utils.secondsToString(0));
        }
        digits.setAlpha(showPaused ? PAUSED_DIGITS_ALPHA : 1f);
        pausedPill.setVisibility(showPaused ? View.VISIBLE : View.INVISIBLE);

        nextText.setText(nextLine(state, elapsed));

        // Progress bar and the two totals
        progressBar.setElapsedSeconds(over ? plan.totalSeconds() + 1 : elapsed);
        if (!plan.isUnlimited() && plan.totalSeconds() > 0) {
            int percent = Math.min(100, (over ? plan.totalSeconds() : elapsed) * 100 / plan.totalSeconds());
            if (percent != lastProgressPercent) {
                lastProgressPercent = percent;
                progressBar.setContentDescription(
                        getString(R.string.cd_workout_progress_percent, percent));
            }
        }
        if (elapsedText != null) {
            elapsedText.setText(Utils.secondsToString(elapsed));
        }
        remainingText.setText(rounds == 0
                ? getString(R.string.remaining_unlimited)
                : Utils.secondsToString(stats != null ? stats.remain : 0));

        // The pill: what tapping it will do
        int pauseLabelRes;
        int pauseIconRes = R.drawable.ic_play;
        if (over) {
            pauseLabelRes = R.string.button_restart;
        } else if (manualWait) {
            pauseLabelRes = R.string.start_button;
        } else if (paused) {
            pauseLabelRes = R.string.button_resume;
        } else {
            pauseLabelRes = R.string.button_pause;
            pauseIconRes = R.drawable.ic_pause;
        }
        pauseLabel.setText(pauseLabelRes);
        pauseIcon.setImageResource(pauseIconRes);
    }

    private String roundLine(int state, int rounds, int round) {
        boolean unlimited = rounds == 0;
        switch (state) {
            case BaseTimer.WORK:
                return unlimited
                        ? getString(R.string.round_unlimited, round)
                        : getString(R.string.round_of, round, rounds);
            case BaseTimer.REST:
            case BaseTimer.MANUAL_REST:
                // The counter has already moved on to the next round during a rest.
                return unlimited
                        ? getString(R.string.next_round_unlimited, round)
                        : getString(R.string.next_round_of, round, rounds);
            case BaseTimer.OVER:
                return getResources().getQuantityString(R.plurals.rounds_complete, rounds, rounds);
            default:
                return unlimited
                        ? getString(R.string.get_ready)
                        : getResources().getQuantityString(R.plurals.get_ready_rounds, rounds, rounds);
        }
    }

    private String nextLine(int state, int elapsed) {
        if (state == BaseTimer.OVER) {
            return getString(R.string.great_work);
        }
        if (state == BaseTimer.MANUAL_REST) {
            return getString(R.string.rest_tap_to_continue);
        }

        int index = plan.locate(elapsed);
        if (index >= plan.count()) {
            return "";
        }
        int next = plan.next(index);

        // With manual rest the rests are not in the plan: after each work phase comes a
        // rest that waits for a tap, except after the very last one.
        if (plan.isManualRest() && plan.kind(index) == PhasePlan.WORK) {
            return getString(next == -1 ? R.string.last_one : R.string.next_rest_manual);
        }
        if (next == -1) {
            return getString(R.string.last_one);
        }

        String time = Utils.secondsToString(plan.seconds(next));
        if (plan.kind(next) == PhasePlan.REST) {
            return getString(R.string.next_rest, time);
        }
        return getString(R.string.next_work, time);
    }


    /** Fades the whole window to the new phase color. */
    private void showColor(int color, boolean animate) {
        if (color == targetColor && displayedColor != 0) {
            return;
        }
        targetColor = color;
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
        if (!animate || displayedColor == 0) {
            applyColor(color);
            return;
        }
        colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), displayedColor, color);
        colorAnimator.setDuration(COLOR_ANIMATION_MS);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                applyColor((Integer) animation.getAnimatedValue());
            }
        });
        colorAnimator.start();
    }

    @SuppressWarnings("deprecation")
    private void applyColor(int color) {
        displayedColor = color;
        getWindow().getDecorView().setBackgroundColor(color);
        if (Build.VERSION.SDK_INT < 35) {
            // From Android 15 the bars are transparent and the window shows through;
            // before that they have to be colored by hand.
            getWindow().setStatusBarColor(color);
            getWindow().setNavigationBarColor(color);
        }
    }


    private void buttonAction() {
        AppController app = AppController.getInstance();
        int state = app.getTimerState();

        if (state == BaseTimer.OVER) {
            app.startTimer();
        } else if (state == BaseTimer.MANUAL_REST) {
            AppController.resumeTimer();
        } else if (app.isTimerPaused()) {
            AppController.resumeTimer();
        } else {
            app.pauseTimer();
        }

        render();
    }


    /**
     * Ends the workout. A finished workout just closes; a running one is paused and the
     * user is asked first, and Cancel puts it back the way it was.
     */
    private void requestExit() {
        final AppController app = AppController.getInstance();
        int state = app.getTimerState();

        if (state == BaseTimer.OVER) {
            goBack();
            return;
        }

        // Only resume afterwards if it was really running: not if the user had already
        // paused it, and not while a manual rest waits (resuming would start the next round).
        final boolean resumeOnCancel = !app.isTimerPaused() && state != BaseTimer.MANUAL_REST;

        app.pauseTimer();
        render();

        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.exit_workout_confirm)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface dialog, int id) {
                        goBack();
                    }
                })
                .setNegativeButton(R.string.cancel, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface dialog, int id) {
                        if (resumeOnCancel) {
                            AppController.resumeTimer();
                        }
                        render();
                    }
                })
                .show();
    }

    public void goBack() {
        finish();
    }


    // Back button. Android 13+ ignores onBackPressed() once the app opts in to the
    // predictive back gesture, and Android 16 opts apps targeting API 36 in by default,
    // so a callback is registered there. Older versions still call onBackPressed().
    @Override
    public void onBackPressed() {
        requestExit();
    }

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = BackApi33.register(this);
        }
    }

    @RequiresApi(33)
    private static final class BackApi33 {
        static Object register(final TimerActivity activity) {
            OnBackInvokedCallback callback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    activity.requestExit();
                }
            };
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }

        static void unregister(TimerActivity activity, Object callback) {
            activity.getOnBackInvokedDispatcher()
                    .unregisterOnBackInvokedCallback((OnBackInvokedCallback) callback);
        }
    }

}
