package org.leeko.intervaltimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhasePlanTest {

    @Test
    public void finiteWorkoutHasWarmupAndRestsBetweenRoundsOnly() {
        // warm-up 10, work 20, rest 10, 3 rounds: W10 K20 R10 K20 R10 K20 = 90
        PhasePlan plan = PhasePlan.build(10, 20, 10, 3, false);

        assertEquals(6, plan.count());
        assertEquals(PhasePlan.WARMUP, plan.kind(0));
        assertEquals(PhasePlan.WORK, plan.kind(1));
        assertEquals(PhasePlan.REST, plan.kind(2));
        assertEquals(PhasePlan.WORK, plan.kind(5));
        assertEquals(90, plan.totalSeconds());
    }

    @Test
    public void locateFindsThePhaseForElapsedTime() {
        PhasePlan plan = PhasePlan.build(10, 20, 10, 3, false);

        assertEquals(0, plan.locate(0));
        assertEquals(0, plan.locate(9));
        assertEquals(1, plan.locate(10));
        assertEquals(1, plan.locate(29));
        assertEquals(2, plan.locate(30));
        assertEquals(5, plan.locate(89));
        assertEquals(6, plan.locate(90));   // finished
        assertEquals(6, plan.locate(500));
    }

    @Test
    public void fillGrowsInsideTheCurrentPhase() {
        PhasePlan plan = PhasePlan.build(10, 20, 10, 3, false);

        assertEquals(1f, plan.fill(0, 10), 0.001f);   // warm-up done
        assertEquals(0f, plan.fill(1, 10), 0.001f);   // work not started
        assertEquals(0.5f, plan.fill(1, 20), 0.001f); // work halfway
        assertEquals(0f, plan.fill(3, 20), 0.001f);   // later phase untouched
    }

    @Test
    public void nextWalksForwardAndEndsOnTheLastPhase() {
        PhasePlan plan = PhasePlan.build(10, 20, 10, 2, false); // W K R K

        assertEquals(1, plan.next(0));
        assertEquals(2, plan.next(1));
        assertEquals(3, plan.next(2));
        assertEquals(-1, plan.next(3));
    }

    @Test
    public void noWarmupAndNoRestLeavesOnlyWork() {
        PhasePlan plan = PhasePlan.build(0, 30, 0, 4, false);

        assertEquals(4, plan.count());
        for (int i = 0; i < plan.count(); i++) {
            assertEquals(PhasePlan.WORK, plan.kind(i));
        }
        assertEquals(120, plan.totalSeconds());
    }

    @Test
    public void manualRestTakesNoRoomInThePlan() {
        PhasePlan plan = PhasePlan.build(10, 20, 15, 3, true); // W K K K

        assertEquals(4, plan.count());
        assertTrue(plan.isManualRest());
        assertEquals(70, plan.totalSeconds());
    }

    @Test
    public void unlimitedRoundsRepeatOneCycleAfterTheWarmup() {
        // warm-up 10, then repeating work 20 + rest 10: W K R
        PhasePlan plan = PhasePlan.build(10, 20, 10, 0, false);

        assertTrue(plan.isUnlimited());
        assertEquals(3, plan.count());
        assertEquals(0, plan.locate(5));
        assertEquals(1, plan.locate(10));
        assertEquals(2, plan.locate(30));
        assertEquals(1, plan.locate(40));   // second cycle starts over at work
        assertEquals(2, plan.locate(60));   // 10 warm-up + 30 first cycle + 20 work = rest again
        assertEquals(1, plan.locate(70));   // third cycle
        assertEquals(1, plan.next(2));      // after rest comes work again, never the warm-up
    }

    @Test
    public void unlimitedFillRestartsEachCycleButKeepsWarmupDone() {
        PhasePlan plan = PhasePlan.build(10, 20, 10, 0, false);

        assertEquals(1f, plan.fill(0, 500), 0.001f);
        assertEquals(0.5f, plan.fill(1, 10 + 30 + 10), 0.001f); // 10s into the second cycle's work
        assertEquals(0f, plan.fill(2, 10 + 30 + 10), 0.001f);
    }

    @Test
    public void emptyPlanDoesNotCrash() {
        PhasePlan plan = PhasePlan.build(0, 0, 0, 3, false);

        assertEquals(0, plan.count());
        assertEquals(0, plan.locate(5));
        assertEquals(-1, plan.next(0));
    }
}
