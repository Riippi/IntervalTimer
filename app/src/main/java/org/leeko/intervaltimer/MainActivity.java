package org.leeko.intervaltimer;


import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.leeko.intervaltimer.dialog.RenameDialog;
import org.leeko.intervaltimer.dialog.RoundsDialog;
import org.leeko.intervaltimer.dialog.TimeDialog;

public class MainActivity extends FragmentActivity implements TimeDialog.NoticeDialogListener,
        RoundsDialog.NoticeDialogListener, RenameDialog.NoticeDialogListener {


    public static final String PREFS_NAME = "intervaltimerX";
    public static final String PREFS_TAB = "TAB";
    private static MainActivity singleton;
    private SlidingTabsBasicFragment fragment;
    private String fragmentTag = "frTAG";

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

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            fragment = new SlidingTabsBasicFragment();

            transaction.replace(R.id.sample_content_fragment, fragment, fragmentTag);
            transaction.commit();
        }

        // Set up action bar.
        final ActionBar actionBar = getActionBar();


        if (actionBar != null) {
            // Specify that the Home button should show an "Up" caret, indicating that touching the
            // button will take the user one step up in the application's hierarchy.
            actionBar.setDisplayHomeAsUpEnabled(false);

            actionBar.setIcon(
                    new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        }

        Button startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                showTimer();
            }

        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {


        if (fragment != null) {
            int tab = fragment.getCurrentTab();

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(PREFS_TAB, tab);
            // Commit the edits!
            editor.apply();
        }
        super.onPause();

    }


    private void showTimer() {

        if (fragment == null) {
            fragment = (SlidingTabsBasicFragment) getSupportFragmentManager().findFragmentByTag(fragmentTag);
        }

        if (fragment != null) {
            int currentId = fragment.getCurrentTab();
            AppController.setTimer(currentId);
            startActivityForResult(new Intent(this, TimerActivity.class), 0xe110);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // code for activity closed
        if (requestCode == 0xe110) {
            // TimerActivity has been closed so stop the timer
            AppController.getInstance().stopTimer();
        }
    }


    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface

    @Override
    public void onDialogPositiveClick(int type, int minutes, int seconds, int id) {
        // TODO Auto-generated method stub
        System.out.println(" WOAH!! " + minutes + " - " + seconds);

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

//		saveState();
        WorkoutModel.getInstance().saveWorkout(rs, id);

        updateFragmentCurrentTab();

    }


    @Override
    public void onRoundsDialogOk(int roundsAmount, int workoutID) {

        Log.d(" SAVE ID", " ID: " + workoutID);

        Workout rs = WorkoutModel.getInstance().getWorkoutCached(workoutID);
        rs.setRoundAmount(roundsAmount);
        WorkoutModel.getInstance().saveWorkout(rs, workoutID);

        updateFragmentCurrentTab();
    }

    @Override
    public void onTextDialogOk(String text, int id) {
        // TODO Auto-generated method stub

        Log.d("mikko", "textdialogsave");

        Workout rs = WorkoutModel.getInstance().getWorkoutCached(id);
        rs.setName(text);
        WorkoutModel.getInstance().saveWorkout(rs, id);

        if (fragment == null) {
            fragment = (SlidingTabsBasicFragment) getSupportFragmentManager().findFragmentByTag(fragmentTag);
        }

        if (fragment != null) {
            fragment.setTabName(id, text);
        }

    }

    /**
     * Safe way to update current tab
     */
    private void updateFragmentCurrentTab() {
        if (fragment == null) {
            fragment = (SlidingTabsBasicFragment) getSupportFragmentManager().findFragmentByTag(fragmentTag);
        }

        if (fragment != null) {
            fragment.updateCurrentTab();
        }
    }


}
