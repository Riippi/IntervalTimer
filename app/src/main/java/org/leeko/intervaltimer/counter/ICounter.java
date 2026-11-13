package org.leeko.intervaltimer.counter;

import org.leeko.intervaltimer.TimerStats;
import org.leeko.intervaltimer.Workout;

public interface ICounter {
	
	
	
	void startRounds(Workout aSet, ITickerInterface aListener);
	
	void stopTimer();

	void pauseTimer();

	void resumeTimer();

    int getState();

    TimerStats getTime();

    boolean isPaused();

    /** True while a tick is being replayed late (after the app was frozen); alerts are skipped then. */
    boolean isCatchingUp();

}
	

