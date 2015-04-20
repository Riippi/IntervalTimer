package org.leeko.intervaltimer.counter;

import org.leeko.intervaltimer.TimerStats;
import org.leeko.intervaltimer.Workout;

public interface ICounter {
	
	
	
	public void startRounds(Workout aSet, ITickerInterface aListener);
	
	public void stopTimer();

	public void pauseTimer();

	public void resumeTimer();

    public int getState();

    public TimerStats getTime();

    public boolean isPaused();

}
	

