package org.leeko.intervaltimer.counter;

public class UnlimitedManual extends BaseTimer {

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


            restIsOn = true;
            paused = true;
            setState(MANUAL_REST);

        }
    }

    
    @Override
    public void resumeTimer()
    {


        if (restIsOn)
        {
            minutes = iSet.getWorkMin();
            seconds = iSet.getWorkSec();
            restIsOn = false;
            setState(WORK);
            paused = false;

            elapsedSeconds--;

        }
        else
        {
            paused = false;
        }
    }

}

