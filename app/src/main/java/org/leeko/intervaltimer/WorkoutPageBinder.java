package org.leeko.intervaltimer;

import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.leeko.intervaltimer.view.TimelineView;

/**
 * Fills one workout page of the main screen (hero card, setting rows, manual rest switch)
 * from the saved workout, and wires up what tapping each part does.
 */
final class WorkoutPageBinder {

    private static final float TOTAL_SIZE_SP = 56f;
    private static final float TOTAL_UNLIMITED_SIZE_SP = 36f;
    private static final float DIMMED_ALPHA = 0.45f;

    private WorkoutPageBinder() {
    }

    static void bind(final MainActivity activity, View page, final int position) {
        final Workout workout = WorkoutModel.getInstance().getWorkoutCached(position);
        int count = WorkoutModel.getInstance().getWorkoutAmount();
        boolean unlimited = workout.getRoundAmount() == 0;

        PhasePlan plan = PhasePlan.build(workout.getWarmupInSec(), workout.getWorkInSec(),
                workout.getRestInSec(), workout.getRoundAmount(), workout.getManual());

        // Hero card
        ((TextView) page.findViewById(R.id.heroCaption))
                .setText(activity.getString(R.string.workout_position, position + 1, count));
        ((TextView) page.findViewById(R.id.heroName)).setText(workout.getName());

        TextView total = (TextView) page.findViewById(R.id.heroTotal);
        TextView suffix = (TextView) page.findViewById(R.id.heroTotalSuffix);
        if (unlimited) {
            total.setTextSize(TypedValue.COMPLEX_UNIT_SP, TOTAL_UNLIMITED_SIZE_SP);
            total.setText(R.string.rounds_unlimited);
            suffix.setText(R.string.total_suffix_unlimited);
        } else {
            total.setTextSize(TypedValue.COMPLEX_UNIT_SP, TOTAL_SIZE_SP);
            total.setText(Utils.secondsToString(plan.totalSeconds()));
            suffix.setText(workout.getManual() ? R.string.total_suffix_manual : R.string.total_suffix);
        }
        ((TimelineView) page.findViewById(R.id.timeline)).setPlan(plan);

        page.findViewById(R.id.optionsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showWorkoutMenu(position);
            }
        });

        // Setting rows. Tapping a row opens the roller dialog for it.
        bindRow(activity, page.findViewById(R.id.rowWarmup), R.string.setting_warmup,
                R.drawable.dot_warmup, Utils.secondsToString(workout.getWarmupInSec()), true,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.editWarmup(position);
                    }
                });
        bindRow(activity, page.findViewById(R.id.rowRounds), R.string.setting_rounds,
                R.drawable.dot_rounds,
                unlimited ? activity.getString(R.string.rounds_unlimited)
                        : String.valueOf(workout.getRoundAmount()),
                true,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.editRounds(position);
                    }
                });
        bindRow(activity, page.findViewById(R.id.rowWork), R.string.setting_work,
                R.drawable.dot_work, Utils.secondsToString(workout.getWorkInSec()), true,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.editWork(position);
                    }
                });
        // With manual rest the rest has no fixed length: show it dimmed and not tappable.
        boolean restEditable = !workout.getManual();
        bindRow(activity, page.findViewById(R.id.rowRest), R.string.setting_rest,
                R.drawable.dot_rest,
                restEditable ? Utils.secondsToString(workout.getRestInSec())
                        : activity.getString(R.string.value_manual),
                restEditable,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.editRest(position);
                    }
                });

        // Manual rest switch
        MaterialSwitch manual = (MaterialSwitch) page.findViewById(R.id.manualSwitch);
        manual.setOnCheckedChangeListener(null);
        manual.setChecked(workout.getManual());
        manual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                workout.setManual(isChecked);
                WorkoutModel.getInstance().saveWorkout(workout);
                activity.refreshPage(position);
            }
        });
    }

    private static void bindRow(MainActivity activity, View row, int labelRes, int dotRes,
                                String value, boolean editable, View.OnClickListener onClick) {
        String label = activity.getString(labelRes);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        row.findViewById(R.id.rowDot).setBackgroundResource(dotRes);

        row.setAlpha(editable ? 1f : DIMMED_ALPHA);
        row.findViewById(R.id.rowChevron).setVisibility(editable ? View.VISIBLE : View.GONE);
        row.setEnabled(editable);
        row.setClickable(editable);
        row.setOnClickListener(editable ? onClick : null);

        row.setContentDescription(editable
                ? activity.getString(R.string.cd_setting_row, label, value)
                : label + ", " + value);
        ViewCompat.setAccessibilityDelegate(row, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
    }
}
