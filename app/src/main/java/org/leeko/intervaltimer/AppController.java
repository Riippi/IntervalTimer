package org.leeko.intervaltimer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import org.leeko.intervaltimer.counter.BaseTimer;
import org.leeko.intervaltimer.counter.CounterFactory;
import org.leeko.intervaltimer.counter.ICounter;
import org.leeko.intervaltimer.counter.ITickerInterface;

public class AppController implements ITickerInterface {


    private static Workout workout;
    private static ICounter counter;

    private static AppController singleton;

    private static boolean vibra = true;
    private static boolean sound = false;

    private static int sound_effect = 0;

    private static boolean incomingCalls = true;

    private MediaPlayer mediaPlayer;

    // needed for logging
    private static final String TAG = "AppController";

    // Pausing on incoming calls used to be done with a PhoneStateListener, which needs
    // the restricted READ_PHONE_STATE permission (Play Store rejects apps requesting it
    // without an approved use case). Audio focus achieves the same practical result: the
    // ringtone for an incoming call takes audio focus away from us, which we treat the
    // same way as an interruption, with no special permission needed.
    @SuppressWarnings("deprecation")
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            Log.i(TAG, "Audio focus lost, pausing timer");
            killMediaPlayer();
            pauseTimer();
            TimerActivity.getInstance().switchState();
            TimerActivity.getInstance().updateView();
        }
    };

    private boolean holdingAudioFocus = false;


    static boolean blockScrensaver = false;

    private static int countdown = 0;

    private Vibrator v;


    private AppController() {
    }


    public static AppController getInstance() {

        if (singleton == null) {
            singleton = new AppController();
        }

        return singleton;

    }

    @SuppressWarnings("deprecation")
    private void requestAudioFocus() {
        if (holdingAudioFocus) {
            return;
        }
        AudioManager audioManager = (AudioManager) TimerActivity.getInstance()
                .getSystemService(Activity.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        holdingAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @SuppressWarnings("deprecation")
    private void abandonAudioFocus() {
        if (!holdingAudioFocus) {
            return;
        }
        AudioManager audioManager = (AudioManager) TimerActivity.getInstance()
                .getSystemService(Activity.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        holdingAudioFocus = false;
    }


    public static void setTimer(int id) {
        workout = WorkoutModel.getInstance().getWorkoutCached(id);
    }


    /**
     * @return Amount of countdown beeps, 0 if none.
     */
    public int getCountdown() {
        return countdown;
    }

    public void notifyCountDownBeep() {
        alert(R.raw.countdown, 0);
    }


    public void startTimer() {
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(TimerActivity.getInstance());
        vibra = pm.getBoolean("vibrate", true);
        sound = pm.getBoolean("sound", true);

        blockScrensaver = pm.getBoolean("prevent_screensaver_checkbox", true);

        incomingCalls = pm.getBoolean("pause_on_incoming_call", true);
        if (incomingCalls) {
            requestAudioFocus();
        } else {
            abandonAudioFocus();
        }

        String temp = pm.getString("sound_list", "0");
        try {
            sound_effect = Integer.parseInt(temp);
        } catch (NumberFormatException nfe) {
            // do nothing
        }

        String tempC = pm.getString("countdown", "0");
        try {
            countdown = Integer.parseInt(tempC);
        } catch (NumberFormatException nfe) {
            // do nothing
        }


        if (workout == null) {
            workout = new Workout();
        }

        if (counter != null) {
            counter.stopTimer();
            counter = null;
        }

        counter = CounterFactory.makeCounter(workout);
        counter.startRounds(workout, getInstance());
    }


    public void stopTimer() {

        if (counter != null) {
            counter.stopTimer();
        }

        abandonAudioFocus();
        killMediaPlayer();
        killVibra();
    }

    public void pauseTimer() {
        counter.pauseTimer();
        killMediaPlayer();
        killVibra();
    }

    public static void resumeTimer() {
        counter.resumeTimer();
    }


    @Override
    public void notifyState() {

        int aState = counter.getState();


        // When you need to modify a UI element, do so on the UI thread.
        // 'getActivity()' is required as this is being ran from a Fragment.
        TimerActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                TimerActivity.getInstance().switchState();
            }
        });



        //TimerActivity.getInstance().switchState();

        switch (aState) {
            case BaseTimer.WORK: {
                alert(getAlert1Id(), 1);
                break;
            }
            case BaseTimer.REST: {
                alert(getAlert2Id(), 2);
                break;
            }
            case BaseTimer.WARMUP: {
                break;
            }
            case BaseTimer.MANUAL_REST: {
                alert(getAlert2Id(), 2);
                break;
            }
            case BaseTimer.OVER: {
                alert(getAlert4Id(), 4);
                break;
            }
        }
    }


    public int getTimerState() {

        if (counter != null) {
            return counter.getState();
        }

        return BaseTimer.WARMUP;

    }

    public TimerStats getTimerTime() {

        if (counter != null) {
            return counter.getTime();
        }
        return null;
    }


    @Override
    public void notifyTick() {

        TimerActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                TimerActivity.getInstance().updateView();
            }
        });

    }

    public boolean isTimerPaused() {
        if (counter != null) {
            return counter.isPaused();
        }
        return false;
    }


    private int getAlert1Id() {

        switch (sound_effect) {
            case 1: {
                return R.raw.beep1;
            }
            case 2: {
                return R.raw.beep_high1;
            }
            case 3: {
                return R.raw.zen1;
            }
        }
        return R.raw.b1;
    }


    private int getAlert2Id() {
        switch (sound_effect) {
            case 1: {
                return R.raw.beep2;
            }
            case 2: {
                return R.raw.beep_high2;
            }
            case 3: {
                return R.raw.zen2;
            }
        }
        return R.raw.b2;
    }


    private int getAlert4Id() {
        switch (sound_effect) {
            case 1: {
                return R.raw.beep4;
            }
            case 2: {
                return R.raw.beep_high4;
            }
            case 3: {
                return R.raw.zen4;
            }
        }
        // case 0
        return R.raw.b4;
    }


    private void alert(int resId, int type) {
        // Check sound setting
        if (sound) {
            playSound(resId);
        }

        if (vibra) {
            vibrate(type);
        }

    }


    private void playSound(int resId) {

        killMediaPlayer();
        mediaPlayer = MediaPlayer.create(TimerActivity.getInstance().getBaseContext(), resId);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }


    private void vibrate(int count) {


        v = (Vibrator) TimerActivity.getInstance().getSystemService(Activity.VIBRATOR_SERVICE);
        // Does the device actually have a vibrator?
        if (v == null || !v.hasVibrator()) {
            return;
        }

        if (count == 1) {
            v.vibrate(400);
        } else if (count == 2) {
            long[] pattern = {0, 300, 200, 300};
            v.vibrate(pattern, -1);
        } else if (count == 4) {
            long[] pattern = {0, 300, 200, 300, 200, 300, 200, 300};
            v.vibrate(pattern, -1);
        } else { // countdown beep
            v.vibrate(130);
        }
    }

    // Kill the vibra  (for example when exiting the timer view during alert)
    private void killVibra() {

        if (v != null) {
            v.cancel();
        }
    }


    private void killMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Error e) {
                // Nothing
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


}





