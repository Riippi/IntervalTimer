package org.leeko.intervaltimer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.leeko.intervaltimer.dialog.RoundsDialog;
import org.leeko.intervaltimer.dialog.TimeDialog;
import org.leeko.intervaltimer.dialog.WorkoutMenuSheet;

/**
 * The workout setup screen: one chip per workout, swipeable pages with the selected
 * workout's settings, and the Start button.
 */
public class MainActivity extends AppCompatActivity implements TimeDialog.NoticeDialogListener,
        RoundsDialog.NoticeDialogListener, WorkoutMenuSheet.Host {

    public static final String PREFS_NAME = "intervaltimerX";
    public static final String PREFS_TAB = "TAB";
    private static final int MAX_WORKOUTS = 10;
    private static final int CHIP_NAME_LENGTH = 28;   // long enough that "X" and "X copy" stay apart

    private static MainActivity singleton;

    private ViewPager pager;
    private HorizontalScrollView chipsScroll;
    private LinearLayout chipsRow;
    private WorkoutPagerAdapter adapter;
    private int seenRevision;   // WorkoutModel.getRevision() the screen was last drawn for

    // Registered eagerly (required by the Activity Result API: before the activity reaches
    // STARTED, not lazily when the user taps something).
    private final ActivityResultLauncher<Intent> timerActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // TimerActivity has closed so stop the timer
                AppController.getInstance().stopTimer();
            });

    // The manage screen tells which workout to show when it closes.
    private final ActivityResultLauncher<Intent> manageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                int workoutId = data != null ? data.getIntExtra(ManageActivity.EXTRA_SELECTED_ID, -1) : -1;
                int tab = workoutId >= 0 ? WorkoutModel.getInstance().findTabIdByWorkoutId(workoutId) : -1;
                updateWholeTabView(tab >= 0 ? tab : pager.getCurrentItem());
            });

    // Returns the application instance
    public static MainActivity getInstance() {
        return singleton;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleton = this;

        AppController.getInstance().stopTimer();

        setContentView(R.layout.activity_main);
        EdgeToEdgeHelper.applySystemBarPadding(this);

        pager = (ViewPager) findViewById(R.id.pager);
        chipsScroll = (HorizontalScrollView) findViewById(R.id.chipsScroll);
        chipsRow = (LinearLayout) findViewById(R.id.chipsRow);

        adapter = new WorkoutPagerAdapter();
        pager.setAdapter(adapter);

        // Back to the workout that was open last time
        int count = WorkoutModel.getInstance().getWorkoutAmount();
        int tab = getSharedPreferences(PREFS_NAME, 0).getInt(PREFS_TAB, 0);
        tab = Math.max(0, Math.min(tab, count - 1));

        buildChips();
        pager.setCurrentItem(tab, false);
        selectChip(tab, false);

        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                selectChip(position, true);
            }
        });

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimer();
            }
        });
        findViewById(R.id.addWorkoutButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWorkout();
            }
        });
        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Something changed the workouts while this screen was covered (the manage screen).
        if (WorkoutModel.getInstance().getRevision() != seenRevision) {
            updateWholeTabView(pager.getCurrentItem());
        }
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putInt(PREFS_TAB, pager.getCurrentItem());
        editor.apply();
        super.onPause();
    }


    /** Set up the timer and switch to timer view. */
    private void showTimer() {
        // No workouts? Show alert instead of starting something
        if (WorkoutModel.getInstance().getWorkoutAmount() == 0) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cant_start_timer_title)
                    .setMessage(R.string.add_workout_first)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        AppController.setTimer(pager.getCurrentItem());
        timerActivityLauncher.launch(new Intent(this, TimerActivity.class));
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
        // Adds it and calls updateWholeTabView() to show and select it
        WorkoutModel.getInstance().addWorkout();
    }


    // ----- Chips -----

    private void buildChips() {
        seenRevision = WorkoutModel.getInstance().getRevision();
        chipsRow.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int count = WorkoutModel.getInstance().getWorkoutAmount();

        for (int i = 0; i < count; i++) {
            final int position = i;
            TextView chip = (TextView) inflater.inflate(R.layout.chip_workout, chipsRow, false);
            chip.setText(chipLabel(WorkoutModel.getInstance().getWorkoutCached(i).getName()));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pager.setCurrentItem(position);
                }
            });
            chipsRow.addView(chip);
        }
    }

    private static String chipLabel(String name) {
        if (name.length() > CHIP_NAME_LENGTH) {
            return name.substring(0, CHIP_NAME_LENGTH - 1) + "…";
        }
        return name;
    }

    /** Highlights the chip of the shown workout and scrolls it into view. */
    private void selectChip(int position, final boolean animate) {
        for (int i = 0; i < chipsRow.getChildCount(); i++) {
            chipsRow.getChildAt(i).setSelected(i == position);
        }
        final View chip = chipsRow.getChildAt(position);
        if (chip == null) {
            return;
        }
        chipsScroll.post(new Runnable() {
            @Override
            public void run() {
                int target = Math.max(0, chip.getLeft() - (chipsScroll.getWidth() - chip.getWidth()) / 2);
                if (animate) {
                    chipsScroll.smoothScrollTo(target, 0);
                } else {
                    chipsScroll.scrollTo(target, 0);
                }
            }
        });
    }


    // ----- Refreshing -----

    /** Redraws one page after its workout was changed, and the chip names. */
    void refreshPage(int position) {
        View page = pager.findViewWithTag(position);
        if (page != null) {
            WorkoutPageBinder.bind(this, page, position);
        }
        buildChips();
        selectChip(pager.getCurrentItem(), false);
    }

    /**
     * Update the whole view after workouts were added, deleted or moved.
     *
     * @param tabId workout to show afterwards
     */
    public void updateWholeTabView(int tabId) {
        adapter.notifyDataSetChanged();
        buildChips();

        int count = WorkoutModel.getInstance().getWorkoutAmount();
        int tab = Math.max(0, Math.min(tabId, count - 1));
        pager.setCurrentItem(tab, false);
        selectChip(tab, false);
    }


    // ----- Dialogs, opened from the page -----

    void editWarmup(int position) {
        Workout workout = WorkoutModel.getInstance().getWorkoutCached(position);
        showTimeDialog(new TimeDialog.WarmUpTimeDialog(), workout.getWarmupMin(), workout.getWarmupSec(), position);
    }

    void editWork(int position) {
        Workout workout = WorkoutModel.getInstance().getWorkoutCached(position);
        showTimeDialog(new TimeDialog.WorkTimeDialog(), workout.getWorkMin(), workout.getWorkSec(), position);
    }

    void editRest(int position) {
        Workout workout = WorkoutModel.getInstance().getWorkoutCached(position);
        showTimeDialog(new TimeDialog.RestTimeDialog(), workout.getRestMin(), workout.getRestSec(), position);
    }

    private void showTimeDialog(TimeDialog dialog, int min, int sec, int position) {
        Bundle args = new Bundle();
        args.putInt("min", min);
        args.putInt("sec", sec);
        args.putInt("id", position);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "time");
    }

    void editRounds(int position) {
        RoundsDialog dialog = new RoundsDialog();
        Bundle args = new Bundle();
        args.putInt("rounds", WorkoutModel.getInstance().getWorkoutCached(position).getRoundAmount());
        args.putInt("id", position);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "rounds");
    }

    /** The menu button on the workout card: opens the workout menu sheet. */
    void showWorkoutMenu(int position) {
        WorkoutMenuSheet.newInstance(position, true).show(getSupportFragmentManager(), "menu");
    }

    /** Duplicate: the copy goes in right after the original and is selected. */
    @Override
    public void duplicateWorkout(int position) {
        if (WorkoutModel.getInstance().getWorkoutAmount() >= MAX_WORKOUTS) {
            return;
        }
        WorkoutModel.getInstance().duplicateWorkout(position);
    }

    /** Delete without asking; a snackbar offers Undo for a few seconds. */
    @Override
    public void deleteWorkoutWithUndo(int position) {
        UndoDelete.delete(this, position, R.id.startButton, null);
    }

    /** The sheet moved a workout; the model already redrew this screen. */
    @Override
    public void onWorkoutsChanged() {
    }

    /** "Reorder & manage all" in the menu sheet. */
    @Override
    public void openManageScreen() {
        Intent intent = new Intent(this, ManageActivity.class);
        intent.putExtra(ManageActivity.EXTRA_SELECTED_ID,
                WorkoutModel.getInstance().getWorkoutCached(pager.getCurrentItem()).getId());
        manageLauncher.launch(intent);
    }

    /** Called by the menu sheet after the new name was typed in. */
    @Override
    public void renameWorkout(int position, String name) {
        Workout workout = WorkoutModel.getInstance().getWorkoutCached(position);
        workout.setName(name);
        WorkoutModel.getInstance().saveWorkout(workout);
        refreshPage(position);
    }


    // ----- Dialog results -----

    @Override
    public void onDialogPositiveClick(int type, int minutes, int seconds, int id) {
        Workout rs = WorkoutModel.getInstance().getWorkoutCached(id);

        switch (type) {
            case TimeDialog.WARM_UP_TIME:
                rs.setWarmupMin(minutes);
                rs.setWarmupSec(seconds);
                break;

            case TimeDialog.WORK_TIME:
                rs.setWorkMin(minutes);
                rs.setWorkSec(seconds);
                break;

            case TimeDialog.REST_TIME:
                rs.setRestMin(minutes);
                rs.setRestSec(seconds);
                break;

            default:
                break;
        }

        WorkoutModel.getInstance().saveWorkout(rs);
        refreshPage(id);
    }

    @Override
    public void onRoundsDialogOk(int roundsAmount, int workoutTabId) {
        Workout rs = WorkoutModel.getInstance().getWorkoutCached(workoutTabId);
        rs.setRoundAmount(roundsAmount);
        WorkoutModel.getInstance().saveWorkout(rs);
        refreshPage(workoutTabId);
    }

    /** One swipeable page per workout. */
    private class WorkoutPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return WorkoutModel.getInstance().getWorkoutAmount();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public int getItemPosition(Object object) {
            // Pages are rebuilt when workouts are added, deleted or moved.
            return POSITION_NONE;
        }

        @Override
        public Object instantiateItem(android.view.ViewGroup container, int position) {
            View page = getLayoutInflater().inflate(R.layout.page_workout, container, false);
            page.setTag(position);
            WorkoutPageBinder.bind(MainActivity.this, page, position);
            container.addView(page);
            return page;
        }

        @Override
        public void destroyItem(android.view.ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
