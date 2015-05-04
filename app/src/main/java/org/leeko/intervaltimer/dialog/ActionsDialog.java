package org.leeko.intervaltimer.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import org.leeko.intervaltimer.MainActivity;
import org.leeko.intervaltimer.R;
import org.leeko.intervaltimer.WorkoutModel;

/**
 * Actions dialog. List of actions. Opens when pressing name tab of a workout.
 */
public class ActionsDialog extends DialogFragment {

    String name;
    int workoutTabID;

    boolean first;
    boolean last;
    boolean oneWorkout;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        int itemsId = R.array.actions_array;

        if (oneWorkout) {
            itemsId = R.array.actions_array_last_one;
        }
        else if (first) {
            itemsId = R.array.actions_array_leftie;
        }
        else if (last) {
            itemsId = R.array.actions_array_righty;
        }



        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.actions_dialog_title)
                .setItems(itemsId, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item

                        // Rename workout is the first one in all lists
                        if (which == 0) {
                            renameWorkout();
                            return;
                        }

                        if (oneWorkout) {
                            actionOnly(which);
                            return;
                        }

                        if (last) {
                            actionRighty(which);
                            return;
                        }

                        if (first) {
                            actionLefty(which);
                            return;
                        }

                        actionNormal(which);

                    }
                });
        return builder.create();
    }


    public void setArguments(Bundle bundle) {
        super.setArguments(bundle);
        this.name = getArguments().getString("name");
        this.workoutTabID = getArguments().getInt("id");

        this.first = getArguments().getBoolean("first");
        this.last = getArguments().getBoolean("last");
        this.oneWorkout = getArguments().getBoolean("oneWorkout");

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

        switch (which) {
            case 1:
                WorkoutModel.getInstance().deleteWorkout(workoutTabID);
                break;
        }
    }


    /**
     * Launch the rename workout dialog
     */
    private void renameWorkout() {
        DialogFragment newFragment = new RenameDialog();
        Bundle args = new Bundle();
        args.putString("name", WorkoutModel.getInstance().getWorkoutCached(workoutTabID).getName());
        args.putInt("id", workoutTabID);
        newFragment.setArguments(args);
        newFragment.show(MainActivity.getInstance().getFragmentManager(), "rename");
    }

}



