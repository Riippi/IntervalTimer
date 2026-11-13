package org.leeko.intervaltimer.counter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.leeko.intervaltimer.PhasePlan;
import org.leeko.intervaltimer.Workout;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the four counters one second at a time (tick() is what the Handler calls once per
 * second on a device) and checks them against PhasePlan, which describes the same workout
 * for the progress bar. The two must always agree.
 */
public class CounterTest {

    /** Stands in for AppController: records what the counter reports. */
    private static final class Recorder implements ITickerInterface {
        final List<Integer> states = new ArrayList<Integer>();
        BaseTimer timer;
        int countdown;
        int countdownBeeps;

        @Override
        public void notifyState() {
            states.add(timer.getState());
        }

        @Override
        public void notifyTick() {
        }

        @Override
        public int getCountdown() {
            return countdown;
        }

        @Override
        public void notifyCountDownBeep() {
            countdownBeeps++;
        }
    }

    private static Workout workout(int warmupSec, int workSec, int restSec, int rounds, boolean manual) {
        Workout w = new Workout();
        w.setWarmupMin(warmupSec / 60);
        w.setWarmupSec(warmupSec % 60);
        w.setWorkMin(workSec / 60);
        w.setWorkSec(workSec % 60);
        w.setRestMin(restSec / 60);
        w.setRestSec(restSec % 60);
        w.setRoundAmount(rounds);
        w.setManual(manual);
        return w;
    }

    private static BaseTimer start(Workout w, Recorder recorder) {
        BaseTimer timer = (BaseTimer) CounterFactory.makeCounter(w);
        recorder.timer = timer;
        timer.startRounds(w, recorder);   // shows the first second, like a tick
        return timer;
    }

    private static int stateFor(int planKind) {
        switch (planKind) {
            case PhasePlan.WARMUP:
                return BaseTimer.WARMUP;
            case PhasePlan.WORK:
                return BaseTimer.WORK;
            default:
                return BaseTimer.REST;
        }
    }

    /** Runs a timed (not manual) workout to the end and compares every second with the plan. */
    private static void assertFollowsPlan(int warmupSec, int workSec, int restSec, int rounds) {
        Workout w = workout(warmupSec, workSec, restSec, rounds, false);
        PhasePlan plan = PhasePlan.build(warmupSec, workSec, restSec, rounds, false);
        Recorder recorder = new Recorder();
        BaseTimer timer = start(w, recorder);

        assertEquals(plan.totalSeconds(), timer.getTotalSeconds());

        for (int second = 0; second < plan.totalSeconds(); second++) {
            if (second > 0) {
                timer.tick();
            }
            assertEquals("state at second " + second,
                    stateFor(plan.kind(plan.locate(second))), timer.getState());
        }

        timer.tick();
        assertEquals(BaseTimer.OVER, timer.getState());
        assertEquals(plan.totalSeconds(), shownElapsed(timer));
    }

    /**
     * The elapsed time the screen was last given. Each tick reports its values first and
     * only then steps the counter on, so the field is one ahead of what was shown.
     */
    private static int shownElapsed(BaseTimer timer) {
        return timer.getElapsedSeconds() - 1;
    }


    @Test
    public void factoryPicksTheCounterForManualRestAndUnlimitedRounds() {
        assertTrue(CounterFactory.makeCounter(workout(0, 20, 10, 3, false)) instanceof DefaultTimer);
        assertTrue(CounterFactory.makeCounter(workout(0, 20, 10, 3, true)) instanceof DefaultManual);
        assertTrue(CounterFactory.makeCounter(workout(0, 20, 10, 0, false)) instanceof UnlimitedNormal);
        assertTrue(CounterFactory.makeCounter(workout(0, 20, 10, 0, true)) instanceof UnlimitedManual);
    }

    @Test
    public void timedWorkoutFollowsThePlanSecondBySecond() {
        assertFollowsPlan(10, 20, 10, 3);
    }

    @Test
    public void workoutWithoutWarmupStartsWithWork() {
        assertFollowsPlan(0, 20, 10, 3);
    }

    @Test
    public void workoutWithoutRestGoesFromWorkToWork() {
        assertFollowsPlan(5, 30, 0, 4);
    }

    @Test
    public void phasesLongerThanAMinuteRollOverCorrectly() {
        assertFollowsPlan(65, 125, 61, 2);
    }

    @Test
    public void singleRoundHasNoRest() {
        assertFollowsPlan(10, 20, 10, 1);
    }

    @Test
    public void statesAreReportedInOrder() {
        Recorder recorder = new Recorder();
        BaseTimer timer = start(workout(2, 3, 2, 2, false), recorder);
        for (int i = 0; i < 20 && timer.getState() != BaseTimer.OVER; i++) {
            timer.tick();
        }

        List<Integer> expected = new ArrayList<Integer>();
        expected.add(BaseTimer.WARMUP);
        expected.add(BaseTimer.WORK);
        expected.add(BaseTimer.REST);
        expected.add(BaseTimer.WORK);
        expected.add(BaseTimer.OVER);
        assertEquals(expected, recorder.states);
    }

    @Test
    public void countdownBeepsInTheLastSecondsOfEveryPhase() {
        // warm-up 10, work 20, rest 10, 2 rounds = 4 phases, 3 beeps each
        Recorder recorder = new Recorder();
        recorder.countdown = 3;
        BaseTimer timer = start(workout(10, 20, 10, 2, false), recorder);
        for (int i = 0; i < 200 && timer.getState() != BaseTimer.OVER; i++) {
            timer.tick();
        }

        assertEquals(BaseTimer.OVER, timer.getState());
        assertEquals(12, recorder.countdownBeeps);
    }

    @Test
    public void manualRestWaitsForTheUserAfterEachRound() {
        // warm-up 5, work 10, 3 rounds, manual rest
        Workout w = workout(5, 10, 30, 3, true);
        PhasePlan plan = PhasePlan.build(5, 10, 30, 3, true);
        Recorder recorder = new Recorder();
        BaseTimer timer = start(w, recorder);

        assertEquals(plan.totalSeconds(), timer.getTotalSeconds());
        assertEquals(BaseTimer.WARMUP, timer.getState());

        for (int round = 1; round <= 3; round++) {
            // run until the counter stops by itself
            int guard = 0;
            while (!timer.isPaused() && timer.getState() != BaseTimer.OVER && guard++ < 100) {
                timer.tick();
            }
            if (round < 3) {
                assertEquals("after round " + round, BaseTimer.MANUAL_REST, timer.getState());
                assertTrue(timer.isPaused());
                timer.resumeTimer();
                assertEquals(BaseTimer.WORK, timer.getState());
                assertFalse(timer.isPaused());
            }
        }

        assertEquals(BaseTimer.OVER, timer.getState());
        // The waiting itself is not counted: elapsed time is warm-up plus the work.
        assertEquals(plan.totalSeconds(), shownElapsed(timer));
    }

    @Test
    public void unlimitedRoundsNeverFinish() {
        Recorder recorder = new Recorder();
        BaseTimer timer = start(workout(5, 20, 10, 0, false), recorder);
        PhasePlan plan = PhasePlan.build(5, 20, 10, 0, false);

        for (int second = 1; second < 1000; second++) {
            timer.tick();
            assertEquals("state at second " + second,
                    stateFor(plan.kind(plan.locate(second))), timer.getState());
        }
    }

    @Test
    public void unlimitedManualRestKeepsAskingForTheNextRound() {
        Recorder recorder = new Recorder();
        BaseTimer timer = start(workout(0, 10, 0, 0, true), recorder);

        for (int round = 1; round <= 5; round++) {
            int guard = 0;
            while (!timer.isPaused() && guard++ < 100) {
                timer.tick();
            }
            assertEquals(BaseTimer.MANUAL_REST, timer.getState());
            timer.resumeTimer();
            assertEquals(BaseTimer.WORK, timer.getState());
        }
    }

    @Test
    public void pausedCounterKeepsItsPlace() {
        Recorder recorder = new Recorder();
        BaseTimer timer = start(workout(0, 20, 10, 3, false), recorder);
        timer.tick();
        timer.tick();
        int elapsed = timer.getElapsedSeconds();

        timer.pauseTimer();
        assertTrue(timer.isPaused());
        assertEquals(BaseTimer.WORK, timer.getState());
        assertEquals(elapsed, timer.getElapsedSeconds());

        timer.resumeTimer();
        assertFalse(timer.isPaused());
        timer.tick();
        assertEquals(elapsed + 1, timer.getElapsedSeconds());
    }
}
