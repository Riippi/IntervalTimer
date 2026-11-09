package org.leeko.intervaltimer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.leeko.intervaltimer.MainActivity;
import org.leeko.intervaltimer.PhasePlan;
import org.leeko.intervaltimer.R;
import org.leeko.intervaltimer.Utils;
import org.leeko.intervaltimer.Workout;
import org.leeko.intervaltimer.WorkoutModel;

/**
 * The workout menu: opens from the menu button on the workout card. Rename (inline, in the
 * sheet), Duplicate, Move left / Move right (the sheet stays open) and Delete (closes the
 * sheet, the screen then offers Undo).
 */
public class WorkoutMenuSheet extends BottomSheetDialogFragment {

    private static final String ARG_POSITION = "position";
    private static final int MAX_WORKOUTS = 10;
    private static final float DISABLED_ALPHA = 0.35f;

    private int position;

    private View menuGroup;
    private View renameGroup;
    private EditText renameInput;
    private TextView nameText;
    private TextView metaText;
    private View renameRow;
    private View duplicateRow;
    private View moveLeftRow;
    private View moveRightRow;
    private View deleteRow;

    public static WorkoutMenuSheet newInstance(int position) {
        WorkoutMenuSheet sheet = new WorkoutMenuSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The position changes when the workout is moved, so a saved value wins.
        position = savedInstanceState != null
                ? savedInstanceState.getInt(ARG_POSITION)
                : requireArguments().getInt(ARG_POSITION);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_POSITION, position);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // Lift the sheet above the keyboard while renaming.
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_workout_menu, container, false);

        menuGroup = view.findViewById(R.id.menuGroup);
        renameGroup = view.findViewById(R.id.renameGroup);
        renameInput = (EditText) view.findViewById(R.id.renameInput);
        nameText = (TextView) view.findViewById(R.id.sheetName);
        metaText = (TextView) view.findViewById(R.id.sheetMeta);

        renameRow = setUpRow(view.findViewById(R.id.actionRename), R.drawable.ic_edit, R.string.sheet_rename,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startRename();
                    }
                });
        duplicateRow = setUpRow(view.findViewById(R.id.actionDuplicate), R.drawable.ic_copy, R.string.sheet_duplicate,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        activity().duplicateWorkout(position);
                    }
                });
        moveLeftRow = setUpRow(view.findViewById(R.id.actionMoveLeft), R.drawable.ic_arrow_back, R.string.sheet_move_left,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkoutModel.getInstance().moveLeft(position);
                        position--;
                        bind();
                    }
                });
        moveRightRow = setUpRow(view.findViewById(R.id.actionMoveRight), R.drawable.ic_arrow_forward, R.string.sheet_move_right,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkoutModel.getInstance().moveRight(position);
                        position++;
                        bind();
                    }
                });
        deleteRow = setUpRow(view.findViewById(R.id.actionDelete), R.drawable.ic_delete, R.string.sheet_delete,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        activity().deleteWorkoutWithUndo(position);
                    }
                });
        ((ImageView) deleteRow.findViewById(R.id.sheetIcon)).setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.destructive));
        ((TextView) deleteRow.findViewById(R.id.sheetLabel)).setTextColor(
                ContextCompat.getColor(requireContext(), R.color.destructive));

        view.findViewById(R.id.renameCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRename();
            }
        });
        view.findViewById(R.id.renameSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveName();
            }
        });
        renameInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveName();
                    return true;
                }
                return false;
            }
        });

        bind();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Always open fully (also in landscape, where the collapsed state would be cut off).
        BottomSheetBehavior<FrameLayout> behavior = ((BottomSheetDialog) requireDialog()).getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private View setUpRow(View row, int iconRes, int labelRes, View.OnClickListener onClick) {
        ((ImageView) row.findViewById(R.id.sheetIcon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.sheetLabel)).setText(labelRes);
        row.setTag(onClick);
        return row;
    }

    private void setRowEnabled(View row, boolean enabled) {
        row.setEnabled(enabled);
        row.setClickable(enabled);
        row.setAlpha(enabled ? 1f : DISABLED_ALPHA);
        row.setOnClickListener(enabled ? (View.OnClickListener) row.getTag() : null);
    }

    private MainActivity activity() {
        return (MainActivity) requireActivity();
    }

    /** Fills the header and the enabled state of the actions from the saved workout. */
    private void bind() {
        WorkoutModel model = WorkoutModel.getInstance();
        int count = model.getWorkoutAmount();
        Workout workout = model.getWorkoutCached(position);

        nameText.setText(workout.getName());
        metaText.setText(summary(workout));

        setRowEnabled(renameRow, true);
        setRowEnabled(duplicateRow, count < MAX_WORKOUTS);
        ((TextView) duplicateRow.findViewById(R.id.sheetLabel)).setText(
                count < MAX_WORKOUTS ? R.string.sheet_duplicate : R.string.sheet_duplicate_limit);
        setRowEnabled(moveLeftRow, position > 0);
        setRowEnabled(moveRightRow, position < count - 1);
        setRowEnabled(deleteRow, count > 1);
    }

    /** "8 rounds · 0:20 work · 0:10 rest · 4:00" */
    private String summary(Workout workout) {
        Context context = requireContext();
        PhasePlan plan = PhasePlan.build(workout.getWarmupInSec(), workout.getWorkInSec(),
                workout.getRestInSec(), workout.getRoundAmount(), workout.getManual());
        boolean unlimited = workout.getRoundAmount() == 0;

        StringBuilder text = new StringBuilder();
        text.append(unlimited
                ? context.getString(R.string.meta_rounds_unlimited)
                : getResources().getQuantityString(R.plurals.meta_rounds,
                        workout.getRoundAmount(), workout.getRoundAmount()));
        text.append(" · ").append(context.getString(R.string.meta_work,
                Utils.secondsToString(workout.getWorkInSec())));
        text.append(" · ").append(workout.getManual()
                ? context.getString(R.string.meta_rest_manual)
                : context.getString(R.string.meta_rest, Utils.secondsToString(workout.getRestInSec())));
        if (!unlimited) {
            text.append(" · ").append(Utils.secondsToString(plan.totalSeconds()));
        }
        return text.toString();
    }


    // ----- Rename, inline in the sheet -----

    private void startRename() {
        renameInput.setText(WorkoutModel.getInstance().getWorkoutCached(position).getName());
        renameInput.setSelection(renameInput.getText().length());
        menuGroup.setVisibility(View.GONE);
        renameGroup.setVisibility(View.VISIBLE);
        renameInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(renameInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void stopRename() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(renameInput.getWindowToken(), 0);
        }
        renameGroup.setVisibility(View.GONE);
        menuGroup.setVisibility(View.VISIBLE);
    }

    private void saveName() {
        String name = renameInput.getText().toString().trim();
        if (!name.isEmpty()) {
            activity().renameWorkout(position, name);
        }
        bind();
        stopRename();
    }
}
