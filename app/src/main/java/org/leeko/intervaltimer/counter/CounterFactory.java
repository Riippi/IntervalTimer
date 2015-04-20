package org.leeko.intervaltimer.counter;

import org.leeko.intervaltimer.Workout;

public class CounterFactory {


	public static ICounter makeCounter(Workout rs) {

		if (rs.getManual() == true)
		{
			if (rs.getRoundAmount() == 0)
			{
				return new UnlimitedManual();
			}
			else
			{
				return new DefaultManual();
			}
	
		}
		else if (rs.getRoundAmount() == 0)
		{
			return new UnlimitedNormal();
		}
		else
		{
			return new DefaultCounter();
		}
	
	}
	
}
