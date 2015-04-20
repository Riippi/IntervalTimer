package org.leeko.intervaltimer;

/**
 * Created by Mikko on 12.4.2015.
 */
public class TimerStats {



    int min;
    int sec;
    int round;
    int roundsAmount;
    int remain;
    int elapsed;
    int total;


    public void setValues(int aMin, int aSec, int aRound, int aRounds, int aRemain, int aElapsed, int aTotal) {

        min = aMin;
        sec = aSec;
        round = aRound;
        roundsAmount = aRounds;
        remain = aRemain;
        elapsed = aElapsed;
        total = aTotal;
    }


}
