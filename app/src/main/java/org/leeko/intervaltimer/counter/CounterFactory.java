package org.leeko.intervaltimer.counter;

import org.leeko.intervaltimer.Workout;

public class CounterFactory {


	public static ICounter makeCounter(Workout workout) {

		if (workout.getManual())
		{
			if (workout.getRoundAmount() == 0)
			{
				return new UnlimitedManual();
			}
			else
			{
				return new DefaultManual();
			}
	
		}
		else if (workout.getRoundAmount() == 0)
		{
			return new UnlimitedNormal();
		}
		else
		{
			return new DefaultTimer();
		}
	
	}
	
}
