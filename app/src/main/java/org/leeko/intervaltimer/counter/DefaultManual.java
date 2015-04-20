package org.leeko.intervaltimer.counter;

public class DefaultManual extends BaseCounter
{


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
                restIsOn = true;
                paused = true;
                setState(MANUAL_REST);
            }
        }
    }

    @Override
    public void resumeTimer()
    {

        //  qDebug() << "\n\n\n\n WOWOWOWIEIEIEIEJASJAFJASFJASHFHFHASF ";


        if (restIsOn)
        {
            minutes = iSet.getWorkMin();
            seconds = iSet.getWorkSec();
            restIsOn = false;
            setState(WORK);
            paused = false;
            
            // qDebug() << "saatana!: " << elapsedSeconds;

            elapsedSeconds--;
            
            //   qDebug() << "vittur: " << elapsedSeconds;
        }
        else
        {
            paused = false;
        }
    }


    int getTotalSeconds()
    {
        int totalWork = iSet.getWorkInSec() * iSet.getRoundAmount();
        return totalWork + iSet.getWarmupInSec(); 
    }

}
