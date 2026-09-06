package org.leeko.intervaltimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.leeko.intervaltimer.counter.BaseTimer;

public class TimerActivity extends Activity {

    //	TextView timerTextView;
    TextView timeText;
    TextView stateText;

    TextView roundText;
    TextView elapsedText;
    TextView remainingText;

    View trafficLight;
    ProgressBar progressBar;

    Button pauseButton;

    int currentRound;
    int nextRound;

    int m_state = BaseTimer.WORK;  // TODO should be the initial state like warmup in some cases
    int m_beforePauseState = -1;


    Drawable shape_yellow;
    Drawable shape_red;
    Drawable shape_green;

    private static TimerActivity instance;
    public static TimerActivity getInstance() {
        return instance;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        EdgeToEdgeHelper.applySystemBarPadding(this);

        instance = this;


        if (getActionBar() != null) {
            getActionBar().hide();
        }


        initStuff();
        // Start the timer
        AppController.getInstance().startTimer();


        // Keep the screen on
        if (AppController.blockScrensaver) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }



    private void initStuff() {
        // UI Components
        stateText = (TextView) findViewById(R.id.textState);
        roundText = (TextView) findViewById(R.id.roundText);
        remainingText = (TextView) findViewById(R.id.textTotalRemaining);
        elapsedText = (TextView) findViewById(R.id.textTotalElapsed);

        trafficLight = findViewById(R.id.timerTrafficLight);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        pauseButton = (Button) findViewById(R.id.pauseButton);

        pauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                buttonAction();
            }
        });

        shape_yellow = getResources().getDrawable(R.drawable.rounded_corner);
        shape_red = getResources().getDrawable(R.drawable.rounded_corner_red);
        shape_green = getResources().getDrawable(R.drawable.rounded_corner_green);


        timeText = (TextView) findViewById(R.id.timeText);

        switchState();
        updateView();

    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        setContentView(R.layout.activity_timer);

        super.onConfigurationChanged(newConfig);
        initStuff();

    }


    @Override
    public void onPause() {
        super.onPause();
        //        timerHandler.removeCallbacks(timerRunnable);
        //        Button b = (Button)findViewById(R.id.button);
        //        b.setText("start");
    }


    public void switchState() {

        m_state = AppController.getInstance().getTimerState();
        timeText.setVisibility(View.VISIBLE);

        resetView();

        switch (m_state) {
            case BaseTimer.WORK: {
                stateText.setText(R.string.state_work);
                trafficLight.setBackground(shape_green);
                break;
            }
            case BaseTimer.REST: {
                stateText.setText(R.string.state_rest);
                trafficLight.setBackground(shape_red);
                break;
            }
            case BaseTimer.WARMUP: {
                trafficLight.setBackground(shape_yellow);
                stateText.setText(R.string.state_warmup);
                break;
            }
            case BaseTimer.MANUAL_REST: {
                timeText.setVisibility(View.GONE);
                trafficLight.setBackground(shape_red);
                stateText.setText(R.string.state_start_when_ready);
                pauseButton.setText(R.string.start_button);
                break;
            }
            case BaseTimer.OVER: {
                trafficLight.setBackground(shape_red);
                timeText.setVisibility(View.GONE);
                stateText.setText(R.string.state_workout_over);
                pauseButton.setText(R.string.button_restart);
                break;
            }
        }

        if(AppController.getInstance().isTimerPaused() && m_state != BaseTimer.MANUAL_REST) {
            setPaused();
        }

    }


    private void setPaused() {
        pauseButton.setText(R.string.button_resume);
        stateText.setText(R.string.state_paused);
        trafficLight.setBackground(shape_yellow);
    }


    public void updateView() {
        // TODO MOAR

        // int aMin, int aSec, int aRound, int aRounds, int remain, int elapsed, int total

        TimerStats timest = AppController.getInstance().getTimerTime();

        if (timest == null) {
            return;
        }

        String time = Utils.integersToString(timest.min, timest.sec);
        timeText.setText(time);

        int aRound = timest.round;
        int elapsed = timest.elapsed;
        int remain = timest.remain;

        currentRound = aRound;
        nextRound = aRound + 1;

        elapsedText.setText(Utils.secondsToString(elapsed));
        remainingText.setText(Utils.secondsToString(remain));

        currentRound = aRound;
        nextRound = aRound + 1;

        String lastPart = getString(R.string.round_of_rounds, aRound, timest.roundsAmount);

        if (timest.roundsAmount == 0) {
            lastPart = String.valueOf(aRound);
            remainingText.setText(R.string.remaining_unlimited);
        }

        if (m_state == BaseTimer.REST || m_state == BaseTimer.MANUAL_REST) {
            roundText.setText(getString(R.string.round_progress_next, lastPart));
        } else {
            roundText.setText(getString(R.string.round_progress, lastPart));
        }

        progressBar.setMax(timest.total);
        progressBar.setProgress(elapsed);

    }


    private void resetView() {
        pauseButton.setText(R.string.button_pause);
    }

    private void buttonAction() {

        int state = AppController.getInstance().getTimerState();

        if (state == BaseTimer.OVER) {
            AppController.getInstance().startTimer();

        } else if (state == BaseTimer.MANUAL_REST) {
            AppController.resumeTimer();

        } else if (AppController.getInstance().isTimerPaused()) {
            AppController.resumeTimer();

        } else {
            AppController.getInstance().pauseTimer();
        }

        switchState();
        updateView();
    }




    @Override
    public void onBackPressed() {


        // If workout is over exit without questions.
        if (AppController.getInstance().getTimerState() == BaseTimer.OVER) {
             goBack();
            return;
        }

        AppController.getInstance().pauseTimer();
        switchState();
        updateView();

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.exit_workout_confirm)
                .setCancelable(false)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // go back
                 goBack();
            }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        AppController.resumeTimer();
                        switchState();
                        updateView();
                    }
                });
        // Create the AlertDialog
        builder.create();
        builder.show();
    }


    public void goBack() {
        super.onBackPressed();
    }


}
