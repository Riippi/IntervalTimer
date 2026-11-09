package org.leeko.intervaltimer;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.leeko.intervaltimer.dialog.WorkoutMenuSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage workouts: the order of this list is the swipe order on the main screen. Drag the
 * handle to reorder, swipe a row left to delete (with Undo), tap a row to open it on the
 * main screen, or use the row's menu button for rename, duplicate, move and delete.
 */
public class ManageActivity extends AppCompatActivity implements WorkoutMenuSheet.Host {

    /** In and out: database id of the workout the main screen should show. */
    public static final String EXTRA_SELECTED_ID = "selected_id";

    private static final int MAX_WORKOUTS = 10;

    private RecyclerView list;
    private ManageAdapter adapter;
    private ItemTouchHelper touchHelper;
    private int selectedId = -1;

    private final Runnable reload = new Runnable() {
        @Override
        public void run() {
            adapter.reload();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The saved workouts are reached through the main screen. If Android has restarted
        // the app with this screen on top, go through the main screen first.
        if (MainActivity.getInstance() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_manage);
        EdgeToEdgeHelper.applySystemBarPadding(this);

        selectedId = getIntent().getIntExtra(EXTRA_SELECTED_ID, -1);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.manageTitle), true);

        list = (RecyclerView) findViewById(R.id.manageList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageAdapter();
        list.setAdapter(adapter);
        adapter.reload();

        touchHelper = new ItemTouchHelper(new TouchCallback());
        touchHelper.attachToRecyclerView(list);

        findViewById(R.id.manageBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        View.OnClickListener add = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWorkout();
            }
        };
        findViewById(R.id.manageAdd).setOnClickListener(add);
        findViewById(R.id.manageNew).setOnClickListener(add);
    }

    @Override
    public void finish() {
        // Tell the main screen which workout to show.
        Intent result = new Intent();
        result.putExtra(EXTRA_SELECTED_ID, selectedId);
        setResult(RESULT_OK, result);
        super.finish();
    }

    private void addWorkout() {
        if (WorkoutModel.getInstance().getWorkoutAmount() >= MAX_WORKOUTS) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cant_add_workout_title)
                    .setMessage(R.string.max_workouts_reached)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        WorkoutModel.getInstance().addWorkout();
        adapter.reload();
        list.scrollToPosition(adapter.getItemCount() - 1);
    }


    // ----- WorkoutMenuSheet.Host -----

    @Override
    public void duplicateWorkout(int position) {
        if (WorkoutModel.getInstance().getWorkoutAmount() >= MAX_WORKOUTS) {
            return;
        }
        WorkoutModel.getInstance().duplicateWorkout(position);
        adapter.reload();
    }

    @Override
    public void deleteWorkoutWithUndo(int position) {
        UndoDelete.delete(this, position, R.id.manageNew, reload);
    }

    @Override
    public void renameWorkout(int position, String name) {
        Workout workout = WorkoutModel.getInstance().getWorkoutCached(position);
        workout.setName(name);
        WorkoutModel.getInstance().saveWorkout(workout);
        adapter.reload();
    }

    @Override
    public void onWorkoutsChanged() {
        adapter.reload();
    }

    @Override
    public void openManageScreen() {
        // already here
    }


    // ----- The list -----

    private class RowHolder extends RecyclerView.ViewHolder {
        final View row;
        final ImageView handle;
        final TextView name;
        final TextView summary;
        final TextView total;
        final View options;
        final View divider;

        RowHolder(View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.manageRow);
            handle = (ImageView) itemView.findViewById(R.id.manageHandle);
            name = (TextView) itemView.findViewById(R.id.manageName);
            summary = (TextView) itemView.findViewById(R.id.manageSummary);
            total = (TextView) itemView.findViewById(R.id.manageTotal);
            options = itemView.findViewById(R.id.manageOptions);
            divider = itemView.findViewById(R.id.manageDivider);
        }
    }

    private class ManageAdapter extends RecyclerView.Adapter<RowHolder> {

        private final List<Workout> items = new ArrayList<>();

        /** Reads the saved workouts again, in their saved order. */
        void reload() {
            items.clear();
            int count = WorkoutModel.getInstance().getWorkoutAmount();
            for (int i = 0; i < count; i++) {
                items.add(WorkoutModel.getInstance().getWorkoutCached(i));
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RowHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_manage, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final RowHolder holder, int position) {
            final Workout workout = items.get(position);
            PhasePlan plan = PhasePlan.build(workout.getWarmupInSec(), workout.getWorkInSec(),
                    workout.getRestInSec(), workout.getRoundAmount(), workout.getManual());
            boolean unlimited = workout.getRoundAmount() == 0;

            holder.name.setText(workout.getName());

            String work = Utils.secondsToString(workout.getWorkInSec());
            String rest = workout.getManual()
                    ? getString(R.string.meta_rest_manual)
                    : getString(R.string.meta_rest, Utils.secondsToString(workout.getRestInSec()));
            holder.summary.setText(unlimited
                    ? getString(R.string.manage_summary_unlimited, work, rest)
                    : getString(R.string.manage_summary, workout.getRoundAmount(), work, rest));

            if (unlimited) {
                holder.total.setText(R.string.total_unlimited_symbol);
                holder.total.setContentDescription(getString(R.string.cd_total_unlimited));
            } else {
                // A trailing + means the manual rests come on top of this.
                holder.total.setText(Utils.secondsToString(plan.totalSeconds())
                        + (workout.getManual() ? "+" : ""));
                holder.total.setContentDescription(null);
            }

            holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);

            holder.handle.setContentDescription(getString(R.string.cd_reorder, workout.getName()));
            holder.handle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        touchHelper.startDrag(holder);
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return false;
                }
            });
            holder.options.setContentDescription(getString(R.string.cd_options_for, workout.getName()));
            holder.options.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WorkoutMenuSheet.newInstance(holder.getBindingAdapterPosition(), false)
                            .show(getSupportFragmentManager(), "menu");
                }
            });
            holder.row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedId = workout.getId();
                    finish();
                }
            });

            // Dragging is not possible with a screen reader, so offer move actions instead.
            addMoveAction(holder, getString(R.string.sheet_move_left), -1);
            addMoveAction(holder, getString(R.string.sheet_move_right), 1);
        }

        private void addMoveAction(final RowHolder holder, String label, final int step) {
            ViewCompat.addAccessibilityAction(holder.row, label, new AccessibilityViewCommand() {
                @Override
                public boolean perform(@NonNull View view, AccessibilityViewCommand.CommandArguments arguments) {
                    int from = holder.getBindingAdapterPosition();
                    int to = from + step;
                    if (from < 0 || to < 0 || to >= items.size()) {
                        return false;
                    }
                    WorkoutModel.getInstance().moveWorkout(from, to);
                    reload();
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        /** While dragging the list only changes on screen; it is saved when the row is dropped. */
        void moveItem(int from, int to) {
            Workout moved = items.remove(from);
            items.add(to, moved);
            notifyItemMoved(from, to);
        }
    }

    private class TouchCallback extends ItemTouchHelper.SimpleCallback {

        private int dragFrom = -1;
        private int dragTo = -1;
        private final Paint swipePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable trash;

        TouchCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
            swipePaint.setColor(ContextCompat.getColor(ManageActivity.this, R.color.swipe_delete_background));
            trash = ContextCompat.getDrawable(ManageActivity.this, R.drawable.ic_delete).mutate();
            trash.setTint(ContextCompat.getColor(ManageActivity.this, R.color.destructive));
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;   // only the handle starts a drag
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // The last workout can't be deleted.
            return adapter.getItemCount() <= 1 ? 0 : super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            int from = viewHolder.getBindingAdapterPosition();
            int to = target.getBindingAdapterPosition();
            adapter.moveItem(from, to);
            dragTo = to;
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            deleteWorkoutWithUndo(viewHolder.getBindingAdapterPosition());
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                dragFrom = viewHolder.getBindingAdapterPosition();
                dragTo = dragFrom;
                RowHolder holder = (RowHolder) viewHolder;
                holder.itemView.setBackgroundResource(R.drawable.bg_manage_row_drag);
                holder.divider.setVisibility(View.INVISIBLE);
                ViewCompat.setElevation(holder.itemView, 12 * getResources().getDisplayMetrics().density);
                holder.itemView.setRotation(-1f);
                holder.handle.setColorFilter(ContextCompat.getColor(ManageActivity.this, R.color.phase_work));
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            RowHolder holder = (RowHolder) viewHolder;
            holder.itemView.setBackground(null);
            ViewCompat.setElevation(holder.itemView, 0f);
            holder.itemView.setRotation(0f);
            holder.handle.setColorFilter(ContextCompat.getColor(ManageActivity.this, R.color.on_surface_muted));

            if (dragFrom >= 0 && dragTo != dragFrom) {
                WorkoutModel.getInstance().moveWorkout(dragFrom, dragTo);
            }
            dragFrom = -1;
            dragTo = -1;
            recyclerView.post(reload);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                View item = viewHolder.itemView;
                c.drawRect(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom(), swipePaint);

                int size = (int) (24 * getResources().getDisplayMetrics().density);
                int margin = (int) (24 * getResources().getDisplayMetrics().density);
                int top = item.getTop() + (item.getHeight() - size) / 2;
                int right = item.getRight() - margin;
                if (-dX > size + margin) {
                    trash.setBounds(right - size, top, right, top + size);
                    trash.draw(c);
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}
