package org.leeko.intervaltimer;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

/** Deleting a workout without asking: a snackbar offers Undo for a few seconds. */
final class UndoDelete {

    private static final int DURATION_MS = 6000;

    private UndoDelete() {
    }

    /**
     * @param anchorViewId the view the snackbar sits above (START, New workout)
     * @param afterChange  runs after the delete and again after an Undo; may be null
     */
    static void delete(final AppCompatActivity activity, final int position, int anchorViewId,
                       final Runnable afterChange) {
        WorkoutModel model = WorkoutModel.getInstance();
        if (model.getWorkoutAmount() <= 1) {
            return;
        }
        final Workout removed = model.getWorkoutCached(position).copy();
        model.deleteWorkout(position);
        if (afterChange != null) {
            afterChange.run();
        }

        Snackbar.make(activity.findViewById(android.R.id.content),
                        activity.getString(R.string.workout_deleted, removed.getName()), DURATION_MS)
                .setAnchorView(anchorViewId)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WorkoutModel.getInstance().insertWorkoutAt(removed, position);
                        if (afterChange != null) {
                            afterChange.run();
                        }
                    }
                })
                .show();
    }
}
