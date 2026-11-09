package org.leeko.intervaltimer;

import java.util.ArrayList;
import java.util.List;

/**
 * The ordered phases (warm-up, work, rest) of one workout, as the running timer
 * screen needs them: one segment per phase for the progress bar, and what comes next.
 * <p>
 * This only describes the workout, it never drives the timer. The counters in
 * {@code counter/} still do the counting; the screen asks this class where the
 * elapsed time falls.
 * <ul>
 * <li>Rest segments are left out when rest is 0 or when manual rest is on (a manual
 * rest has no fixed length, so it takes no room in the bar).</li>
 * <li>With unlimited rounds (0) there is no end, so the plan is the warm-up plus one
 * work/rest cycle, and the cycle repeats.</li>
 * </ul>
 * Plain Java with no Android classes, so it can be unit tested on the computer.
 */
public final class PhasePlan {

    public static final int WARMUP = 0;
    public static final int WORK = 1;
    public static final int REST = 2;

    private final int[] kinds;
    private final int[] seconds;
    private final int[] starts;
    private final boolean unlimited;
    private final boolean manualRest;
    private final int warmupSeconds;
    private final int length;

    private PhasePlan(int[] kinds, int[] seconds, boolean unlimited, boolean manualRest) {
        this.kinds = kinds;
        this.seconds = seconds;
        this.unlimited = unlimited;
        this.manualRest = manualRest;

        starts = new int[kinds.length];
        int total = 0;
        for (int i = 0; i < kinds.length; i++) {
            starts[i] = total;
            total += seconds[i];
        }
        length = total;
        warmupSeconds = kinds.length > 0 && kinds[0] == WARMUP ? seconds[0] : 0;
    }

    /**
     * @param rounds 0 means unlimited
     */
    public static PhasePlan build(int warmupSec, int workSec, int restSec, int rounds, boolean manualRest) {
        boolean unlimited = rounds <= 0;
        int workCount = unlimited ? 1 : rounds;

        List<int[]> segments = new ArrayList<>();
        if (warmupSec > 0) {
            segments.add(new int[]{WARMUP, warmupSec});
        }
        for (int round = 1; round <= workCount; round++) {
            if (workSec > 0) {
                segments.add(new int[]{WORK, workSec});
            }
            boolean moreToCome = unlimited || round < workCount;
            if (moreToCome && !manualRest && restSec > 0) {
                segments.add(new int[]{REST, restSec});
            }
        }

        int[] kinds = new int[segments.size()];
        int[] seconds = new int[segments.size()];
        for (int i = 0; i < kinds.length; i++) {
            kinds[i] = segments.get(i)[0];
            seconds[i] = segments.get(i)[1];
        }
        return new PhasePlan(kinds, seconds, unlimited, manualRest);
    }

    public int count() {
        return kinds.length;
    }

    public int kind(int index) {
        return kinds[index];
    }

    public int seconds(int index) {
        return seconds[index];
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public boolean isManualRest() {
        return manualRest;
    }

    /** Total seconds of all segments (for unlimited rounds: the warm-up plus one cycle). */
    public int totalSeconds() {
        return length;
    }

    /** Length of one repeating work/rest cycle when rounds are unlimited. */
    private int cycleSeconds() {
        return length - warmupSeconds;
    }

    private int firstCycleIndex() {
        return warmupSeconds > 0 ? 1 : 0;
    }

    /**
     * The segment that the given elapsed time falls in, or {@link #count()} when the
     * workout is finished (never for unlimited rounds).
     */
    public int locate(int elapsed) {
        if (kinds.length == 0) {
            return 0;
        }
        int e = Math.max(0, elapsed);

        if (unlimited) {
            if (warmupSeconds > 0 && e < warmupSeconds) {
                return 0;
            }
            int cycle = cycleSeconds();
            if (cycle <= 0) {
                return kinds.length;
            }
            int local = (e - warmupSeconds) % cycle;
            for (int i = firstCycleIndex(); i < kinds.length; i++) {
                if (local < starts[i] - warmupSeconds + seconds[i]) {
                    return i;
                }
            }
            return kinds.length - 1;
        }

        for (int i = 0; i < kinds.length; i++) {
            if (e < starts[i] + seconds[i]) {
                return i;
            }
        }
        return kinds.length;
    }

    /** How much of the segment is done, 0 to 1, at the given elapsed time. */
    public float fill(int index, int elapsed) {
        int e = Math.max(0, elapsed);

        if (unlimited) {
            if (kinds[index] == WARMUP) {
                return clamp(e / (float) seconds[index]);
            }
            int cycle = cycleSeconds();
            if (e < warmupSeconds || cycle <= 0) {
                return 0f;
            }
            int local = (e - warmupSeconds) % cycle;
            return clamp((local - (starts[index] - warmupSeconds)) / (float) seconds[index]);
        }

        return clamp((e - starts[index]) / (float) seconds[index]);
    }

    /**
     * The segment after the given one, or -1 when it is the last one. With unlimited
     * rounds the cycle starts over, so there is always a next one.
     */
    public int next(int index) {
        if (index < 0 || index >= kinds.length) {
            return -1;
        }
        if (index + 1 < kinds.length) {
            return index + 1;
        }
        return unlimited && cycleSeconds() > 0 ? firstCycleIndex() : -1;
    }

    /**
     * True when a manual rest (wait for a tap) sits after this work phase, before the next
     * work phase. Manual rests have no length so they are not segments of their own.
     */
    public boolean hasManualRestAfter(int index) {
        if (!manualRest || index < 0 || index >= kinds.length || kinds[index] != PhasePlan.WORK) {
            return false;
        }
        if (unlimited) {
            return true;
        }
        return index + 1 < kinds.length && kinds[index + 1] == PhasePlan.WORK;
    }

    private static float clamp(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }
}
