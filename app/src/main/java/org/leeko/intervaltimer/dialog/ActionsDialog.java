package org.leeko.intervaltimer.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.leeko.intervaltimer.MainActivity;
import org.leeko.intervaltimer.R;
import org.leeko.intervaltimer.WorkoutModel;

/**
 * List of actions for a workout (rename, move, delete). Opens from the menu button on the
 * workout card. To be replaced by the bottom sheet of the redesign.
 */
public class ActionsDialog extends DialogFragment {

    int workoutTabID;

    boolean first;
    boolean last;
    boolean oneWorkout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            workoutTabID = args.getInt("id");
            first = args.getBoolean("first");
            last = args.getBoolean("last");
            oneWorkout = args.getBoolean("oneWorkout");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int itemsId = R.array.actions_array;

        if (oneWorkout) {
            itemsId = R.array.actions_array_last_one;
        } else if (first) {
            itemsId = R.array.actions_array_leftie;
        } else if (last) {
            itemsId = R.array.actions_array_righty;
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.actions_dialog_title)
                .setItems(itemsId, (dialog, which) -> {
                    // Rename is the first one in all lists
                    if (which == 0) {
                        renameWorkout();
                    } else if (oneWorkout) {
                        actionOnly(which);
                    } else if (last) {
                        actionRighty(which);
                    } else if (first) {
                        actionLefty(which);
                    } else {
                        actionNormal(which);
                    }
                })
                .create();
    }

    private void actionNormal(int which) {
        switch (which) {
            case 1:
                WorkoutModel.getInstance().moveLeft(workoutTabID);
                break;
            case 2:
                WorkoutModel.getInstance().moveRight(workoutTabID);
                break;
            case 3:
                WorkoutModel.getInstance().deleteWorkout(workoutTabID);
                break;
        }
    }

    private void actionLefty(int which) {
        switch (which) {
            case 1:
                WorkoutModel.getInstance().moveRight(workoutTabID);
                break;
            case 2:
                WorkoutModel.getInstance().deleteWorkout(workoutTabID);
                break;
        }
    }

    private void actionRighty(int which) {
        switch (which) {
            case 1:
                WorkoutModel.getInstance().moveLeft(workoutTabID);
                break;
            case 2:
                WorkoutModel.getInstance().deleteWorkout(workoutTabID);
                break;
        }
    }

    private void actionOnly(int which) {
        if (which == 1) {
            WorkoutModel.getInstance().deleteWorkout(workoutTabID);
        }
    }

    /** Launch the rename workout dialog. */
    private void renameWorkout() {
        RenameDialog rename = new RenameDialog();
        Bundle args = new Bundle();
        args.putString("name", WorkoutModel.getInstance().getWorkoutCached(workoutTabID).getName());
        args.putInt("id", workoutTabID);
        rename.setArguments(args);
        rename.show(MainActivity.getInstance().getSupportFragmentManager(), "rename");
    }
}
