package org.leeko.intervaltimer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import android.util.SparseIntArray;

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

    // All alert sounds are short beeps/chimes, so a SoundPool (sounds pre-decoded
    // and kept in memory, played with low latency) is a better fit than MediaPlayer
    // (which decodes from scratch on every playSound() call - overkill for this, and
    // MediaPlayer.create() failures are also silent, unlike SoundPool's load callback).
    private static final int[] SOUND_RES_IDS = {
            R.raw.countdown,
            R.raw.b1, R.raw.b2, R.raw.b4,
            R.raw.beep1, R.raw.beep2, R.raw.beep4,
            R.raw.beep_high1, R.raw.beep_high2, R.raw.beep_high4,
            R.raw.zen1, R.raw.zen2, R.raw.zen4,
    };

    private SoundPool soundPool;
    private final SparseIntArray soundIds = new SparseIntArray();
    private int activeStreamId = 0;

    // Pausing on incoming calls. A PhoneStateListener would need the restricted
    // READ_PHONE_STATE permission (Play Store rejects apps requesting it without an approved
    // use case). Holding audio focus for the whole workout also caught the ringtone, but it
    // made the app act like a music player: starting a workout stopped the user's music, and
    // starting music paused the workout. So the app takes no audio focus at all (the beeps
    // just play on top of whatever is playing) and watches the audio mode instead, which
    // needs no permission: it leaves MODE_NORMAL when the phone rings (on a silent phone,
    // when the call is answered) and during VoIP calls. Checked once per tick.
    private AudioManager audioManager;
    private int lastAudioMode = AudioManager.MODE_NORMAL;


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

    private int currentAudioMode() {
        return audioManager != null ? audioManager.getMode() : AudioManager.MODE_NORMAL;
    }

    /**
     * Pauses the workout when a call starts. Only the change from "no call" counts, so a
     * workout that is started or resumed during a call keeps running.
     */
    private void pauseIfCallStarted() {
        if (!incomingCalls) {
            return;
        }
        int mode = currentAudioMode();
        boolean callStarted = lastAudioMode == AudioManager.MODE_NORMAL
                && mode != AudioManager.MODE_NORMAL;
        lastAudioMode = mode;
        if (callStarted) {
            pauseTimer();
        }
    }


    public static void setTimer(int id) {
        workout = WorkoutModel.getInstance().getWorkoutCached(id);
    }

    /** The workout the timer is running (set by setTimer, or a default one once started). */
    public static Workout getWorkout() {
        return workout;
    }


    /**
     * @return Amount of countdown beeps, 0 if none.
     */
    public int getCountdown() {
        return countdown;
    }

    public void notifyCountDownBeep() {
        // The tick that noticed a call has just paused the workout: no beep after that.
        if (counter != null && counter.isPaused()) {
            return;
        }
        alert(R.raw.countdown, 0);
    }


    public void startTimer() {
        // Kick off SoundPool loading as early as possible so the (async) decode has
        // the whole warm-up/first interval to finish before the first alert() call.
        ensureSoundPoolLoaded(TimerActivity.getInstance());

        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(TimerActivity.getInstance());
        vibra = pm.getBoolean("vibrate", true);
        sound = pm.getBoolean("sound", true);

        blockScrensaver = pm.getBoolean("prevent_screensaver_checkbox", true);

        incomingCalls = pm.getBoolean("pause_on_incoming_call", true);
        audioManager = (AudioManager) TimerActivity.getInstance().getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        lastAudioMode = currentAudioMode();

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


    /** True once a workout has been started in this process (running, paused or finished). */
    public boolean hasTimer() {
        return counter != null;
    }

    public void stopTimer() {

        if (counter != null) {
            counter.stopTimer();
        }

        stopSound();
        killVibra();
    }

    public void pauseTimer() {
        counter.pauseTimer();
        stopSound();
        killVibra();
    }

    public static void resumeTimer() {
        // A call that is already going on when the user resumes must not pause it again.
        AppController app = getInstance();
        app.lastAudioMode = app.currentAudioMode();
        counter.resumeTimer();
    }


    @Override
    public void notifyState() {

        int aState = counter.getState();


        // The counter ticks on the main thread, so the screen can be updated directly.
        TimerActivity.getInstance().switchState();

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

        pauseIfCallStarted();

        TimerActivity.getInstance().updateView();

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
        // No beeps or vibration for ticks that are replayed after the app was frozen.
        if (counter != null && counter.isCatchingUp()) {
            return;
        }

        // Check sound setting
        if (sound) {
            playSound(resId);
        }

        if (vibra) {
            vibrate(type);
        }

    }


    private void ensureSoundPoolLoaded(Context context) {
        if (soundPool != null) {
            return;
        }

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build();

        for (int resId : SOUND_RES_IDS) {
            soundIds.put(resId, soundPool.load(context, resId, 1));
        }
    }

    private void playSound(int resId) {

        stopSound();

        if (soundPool == null) {
            return;
        }

        int soundId = soundIds.get(resId, 0);
        if (soundId == 0) {
            return;
        }

        activeStreamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }


    private void vibrate(int count) {


        v = (Vibrator) TimerActivity.getInstance().getSystemService(Activity.VIBRATOR_SERVICE);
        // Does the device actually have a vibrator?
        if (v == null || !v.hasVibrator()) {
            return;
        }

        if (count == 1) {
            vibrateCompat(400);
        } else if (count == 2) {
            vibrateCompat(new long[]{0, 300, 200, 300}, -1);
        } else if (count == 4) {
            vibrateCompat(new long[]{0, 300, 200, 300, 200, 300, 200, 300}, -1);
        } else { // countdown beep
            vibrateCompat(130);
        }
    }

    // Vibrator.vibrate(long) / vibrate(long[], int) are deprecated since API 26 in
    // favor of VibrationEffect, but VibrationEffect itself doesn't exist before API 26 -
    // so both paths are kept, gated on the running device's API level.
    @SuppressWarnings("deprecation")
    private void vibrateCompat(long milliseconds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(milliseconds);
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrateCompat(long[] pattern, int repeat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, repeat));
        } else {
            v.vibrate(pattern, repeat);
        }
    }

    // Kill the vibra  (for example when exiting the timer view during alert)
    private void killVibra() {

        if (v != null) {
            v.cancel();
        }
    }


    // Stop whatever alert sound is currently playing (for example when exiting
    // the timer view, or when a new alert needs to cut off the previous one).
    private void stopSound() {
        if (soundPool != null && activeStreamId != 0) {
            soundPool.stop(activeStreamId);
            activeStreamId = 0;
        }
    }


}





