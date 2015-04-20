package org.leeko.intervaltimer;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.leeko.intervaltimer.counter.BaseCounter;

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

    int m_state = BaseCounter.WORK;  // TODO should be the initial state like warmup in some cases
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

        instance = this;

        getActionBar().hide();

        Log.d("TIMER", " onCreate");
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

        trafficLight = (View) findViewById(R.id.timerTrafficLight);

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

        Log.d("TIMER", " OWN INIT");

        switchState();
        updateView();

    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        Log.d("TIMER", " Layout changed");

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
        stateText.setText("kk");
        timeText.setVisibility(View.VISIBLE);

        resetView();

        switch (m_state) {
            case BaseCounter.WORK: {
                stateText.setText("WORK");
                trafficLight.setBackground(shape_green);
                break;
            }
            case BaseCounter.REST: {
                stateText.setText("REST");
                trafficLight.setBackground(shape_red);
                break;
            }
            case BaseCounter.WARMUP: {
                trafficLight.setBackground(shape_yellow);
                stateText.setText("WARM-UP");
                break;
            }
            case BaseCounter.MANUAL_REST: {
                timeText.setVisibility(View.GONE);
                trafficLight.setBackground(shape_red);
                stateText.setText("START WHEN READY");
                pauseButton.setText("START");
                break;
            }
            case BaseCounter.OVER: {
                trafficLight.setBackground(shape_red);
                timeText.setVisibility(View.GONE);
                stateText.setText("WORKOUT OVER");
                pauseButton.setText("RESTART");
                break;
            }
        }

        if(AppController.getInstance().isTimerPaused() && m_state != BaseCounter.MANUAL_REST) {
            setPaused();
        }

    }


    private void setPaused() {
        pauseButton.setText("RESUME");
        stateText.setText("PAUSED");
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

        String lastPart = aRound + " of " + timest.roundsAmount;

        if (timest.roundsAmount == 0) {
            lastPart = aRound + "";
            remainingText.setText("-");
        }

        if (m_state == BaseCounter.REST || m_state == BaseCounter.MANUAL_REST) {
            roundText.setText("NEXT: ROUND " + lastPart);
        } else {
            roundText.setText("ROUND " + lastPart);
        }

        progressBar.setMax(timest.total);
        progressBar.setProgress(elapsed);

    }


    private void resetView() {
        pauseButton.setText("PAUSE");
    }

    private void buttonAction() {

        int state = AppController.getInstance().getTimerState();

        if (state == BaseCounter.OVER) {
            AppController.getInstance().startTimer();

        } else if (state == BaseCounter.MANUAL_REST) {
            AppController.resumeTimer();

        } else if (AppController.getInstance().isTimerPaused()) {
            AppController.resumeTimer();

        } else {
            AppController.getInstance().pauseTimer();
        }

        switchState();
        updateView();
    }


}
