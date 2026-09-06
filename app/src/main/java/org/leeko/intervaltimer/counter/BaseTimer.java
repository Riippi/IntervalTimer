package org.leeko.intervaltimer.counter;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.leeko.intervaltimer.TimerStats;
import org.leeko.intervaltimer.Workout;


public abstract class BaseTimer implements ICounter {

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

    public int minutes;
    public int currentRound;
    public int elapsedSeconds;
    public Workout iSet;
    public ITickerInterface listener;
    //public static final int PAUSED = 7;
    int currentState;


    TimerStats timerStats;


    boolean timerOn = false;

    // Ticks once per second on the main thread. Scheduled against a fixed
    // elapsedRealtime() anchor rather than just posting "1000ms from now"
    // each time, so per-tick overhead can't accumulate into drift over a
    // long workout - each tick corrects for however late it actually ran.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long nextTickAt;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!timerOn) {
                return;
            }

            if (!paused) {
                tick();
                nextTickAt += 1000;
            } else {
                // Keep the anchor at "now" while paused, so resuming doesn't
                // fire a burst of catch-up ticks for time spent paused.
                nextTickAt = SystemClock.elapsedRealtime() + 1000;
            }

            long delay = Math.max(0, nextTickAt - SystemClock.elapsedRealtime());
            handler.postDelayed(this, delay);
        }
    };


    public void startRounds(Workout aSet, ITickerInterface aListener) {
        listener = aListener;
        iSet = aSet;
        paused = false;

        timerOn = true;
        startTimer();
        tick();

        nextTickAt = SystemClock.elapsedRealtime() + 1000;
        handler.postDelayed(tickRunnable, 1000);
    }


    private void tick() {

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

        if (listener.getCountdown() > 0) {
            if (minutes == 0 && seconds <= listener.getCountdown() && seconds >= 1) {
                //setState(COUNTDOWN);
                listener.notifyCountDownBeep();
            }
        }

        seconds--;
        elapsedSeconds++;
    }

    public void stopTimer() {
        timerOn = false;
        handler.removeCallbacks(tickRunnable);
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

