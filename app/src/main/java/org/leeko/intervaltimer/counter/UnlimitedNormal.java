package org.leeko.intervaltimer.counter;

public class UnlimitedNormal extends BaseTimer {


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

