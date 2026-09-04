package org.leeko.intervaltimer;


import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
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
    private static final int REQUEST_READ_PHONE_STATE = 0x9001;
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }

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
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            showSettings();
            return true;
        } else if (itemId == R.id.action_add_workout) {

            if (WorkoutModel.getInstance().getWorkoutAmount() > 9) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Can't add workout");
                alertDialog.setMessage("Maximum limit of workouts reached");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

                return true;
            }

            WorkoutModel.getInstance().addWorkout();
            return true;
        } else {
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


    /**
     * Set up the timer and switch to timer view
     */
    private void showTimer() {


        // No workouts? Show alert instead of starting something
        if (WorkoutModel.getInstance().getWorkoutAmount() == 0) {

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Can't start timer");
            alertDialog.setMessage("Add new workout first");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

            return;
        }



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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            AppController.registerPhoneStateListener();
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
        WorkoutModel.getInstance().saveWorkout(rs);

        updateFragmentCurrentTab();

    }


    @Override
    public void onRoundsDialogOk(int roundsAmount, int workoutTabId) {

        Log.d(" SAVE ID", " ID: " + workoutTabId);

        Workout rs = WorkoutModel.getInstance().getWorkoutCached(workoutTabId);
        rs.setRoundAmount(roundsAmount);
        WorkoutModel.getInstance().saveWorkout(rs);

        updateFragmentCurrentTab();
    }

    @Override
    public void onTextDialogOk(String text, int id) {


        Log.d("mikko", "textdialogsave");

        Workout rs = WorkoutModel.getInstance().getWorkoutCached(id);
        rs.setName(text);
        WorkoutModel.getInstance().saveWorkout(rs);

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


    /**
     * Update the whole tab view
     * @param tabId tab id where to focus after refresh
     */
    public void updateWholeTabView(int tabId) {

        if (fragment == null) {
            fragment = (SlidingTabsBasicFragment) getSupportFragmentManager().findFragmentByTag(fragmentTag);
        }

        if (fragment != null) {
            fragment.updateAll();
            fragment.setCurrentTab(tabId);
        }

    }


}
