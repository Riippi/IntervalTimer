package org.leeko.intervaltimer.counter;

import android.os.AsyncTask;

import org.leeko.intervaltimer.TimerStats;
import org.leeko.intervaltimer.Workout;


public abstract class BaseTimer extends AsyncTask<Void, Void, Void> implements ICounter {

    // States
    public static final int WARMUP = 1;
    public static final int WORK = 2;
    public static final int REST = 3;
    //public static final int COUNTDOWN = 4;
    public static final int MANUAL_REST = 5;
    public static final int OVER = 6;
    public boolean restIsOn;
    public boolean paused;
    public boolean warmupIsOn;
    public int seconds;

    //runs without a timer by reposting this handler at the end of the runnable
    //Handler timerHandler;
    //Runnable timer;
    public int minutes;
    public int currentRound;
    public int elapsedSeconds;
    public Workout iSet;
    public ITickerInterface listener;
    //public static final int PAUSED = 7;
    int currentState;


    TimerStats timerStats;


    boolean timerOn = false;


    @Override
    protected Void doInBackground(Void... params) {

        // timerHandler = new Handler();
        // creating timer instance
        doStartTimer();
        // starting the timer
//		timer.Start();
        publishProgress();

        return null;
    }


    public void startRounds(Workout aSet, ITickerInterface aListener) {
        listener = aListener;
        iSet = aSet;
        paused = false;

        execute();
    }


    private void doStartTimer() {

        timerOn = true;
        startTimer();
        onTimerTick();

        while (timerOn) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Do nothing
            }
            onTimerTick();
        }
    }


    private void onTimerTick() {
        if (!paused) {
            publishProgress();
        }
    }

    public void stopTimer() {
        // TODO
        timerOn = false;
        //timerHandler.removeCallbacks(timer);
        cancel(true);
    }

    public void restartTimer() {
        // TODO
        startTimer();
        doStartTimer();
    }

    public void pauseTimer() {
        paused = true;
    }

    public void resumeTimer() {
        paused = false;
    }


    private void startTimer() {

        // Does the workout have a warmup period?
        if (iSet.getWarmupMin() + iSet.getWarmupSec() > 0) {
            minutes = iSet.getWarmupMin();
            seconds = iSet.getWarmupSec();
            setState(WARMUP);
            warmupIsOn = true;
        } // If not, go straight to the work
        else {
            minutes = iSet.getWorkMin();
            seconds = iSet.getWorkSec();
            setState(WORK);
        }

        currentRound = 1;
        elapsedSeconds = 0;
        restIsOn = false;
    }


    private void FFtick() {
        publishProgress();
    }

    @Override
    protected void onProgressUpdate(Void... value) {
        super.onProgressUpdate(value);
        // The Heart
        if (seconds <= 0 && minutes <= 0) {
            changeTimerState();
        }
        if (seconds < 0) {
            minutes--;
            seconds = 59;
        }

        if (timerStats == null) {
            timerStats = new TimerStats();
        }

        timerStats.setValues(minutes, seconds, currentRound, iSet.getRoundAmount(), getRemainingSeconds(), getElapsedSeconds(), getTotalSeconds());
        listener.notifyTick();

        logShit();


        if (listener.getCountdown() > 0) {
            if (minutes == 0 && seconds <= listener.getCountdown() && seconds >= 1) {
                //setState(COUNTDOWN);
                listener.notifyCountDownBeep();
            }
        }

        seconds--;
        elapsedSeconds++;
    }


    // return time elapsed of the workout in seconds
    int getElapsedSeconds() {
        return elapsedSeconds;
    }

    // return the total time of the workout in seconds
    int getTotalSeconds() {
        int totalWork = iSet.getWorkInSec() * iSet.getRoundAmount();
        int totalRest = iSet.getRestInSec() * (iSet.getRoundAmount() - 1);

        return totalRest + totalWork + iSet.getWarmupInSec();
    }

    // return time remaining in seconds
    int getRemainingSeconds() {
        return getTotalSeconds() - getElapsedSeconds();
    }

    // For subclasses to handle
    public abstract void changeTimerState();


    void logShit() {

        String dd = "";
        String number = "";


        if (restIsOn) {
            dd = dd + "rest ";
        } else {
            dd += "work ";
        }

        number += currentRound;
        dd += number;

        dd += " - ";


        number = "" + minutes;

        dd += number;
        dd += ":";
        number = "" + seconds;


        if (number.length() < 2) {
            dd += "0";
        }

        dd += number;

        System.out.println(dd);
    }

    // For view
    public int getState() {

        /*

        // If manual rest is on  forget the whole pause shenanigans
        if (currentState == MANUAL_REST) {
            return currentState;
        }

        if (paused) {
            return PAUSED;
        } */

        return currentState;
    }

    protected void setState(int aState) {
        currentState = aState;
        listener.notifyState();
    }

    public TimerStats getTime() {

        return timerStats;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

}

