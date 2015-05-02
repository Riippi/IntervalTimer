package org.leeko.intervaltimer.counter;


/**
 * The basic counter 
 * @author Mikko
 *
 */
public class DefaultTimer extends BaseTimer {


	@Override
	public void changeTimerState()
	{

		if (warmupIsOn)
		{
			minutes = iSet.getWorkMin();
			seconds = iSet.getWorkSec();
			restIsOn = false;
			setState(WORK);
			warmupIsOn = false;
			return;
		}

		if (restIsOn)
		{
			minutes = iSet.getWorkMin();
			seconds = iSet.getWorkSec();
			restIsOn = false;
			setState(WORK);
		}
		else
		{
			currentRound++;

			if (currentRound > iSet.getRoundAmount())
			{
				currentRound = iSet.getRoundAmount();
				minutes = 0;
				seconds = 0;
				setState(OVER);
				stopTimer();
			}
			else
			{

				if (iSet.getRestInSec() == 0)
				{
					minutes = iSet.getWorkMin();
					seconds = iSet.getWorkSec();
					restIsOn = false;
					setState(WORK);
				}
				else
				{
					minutes = iSet.getRestMin();
					seconds = iSet.getRestSec();
					restIsOn = true;
					setState(REST);
				}

			}

		}

	}
}
