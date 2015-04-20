package org.leeko.intervaltimer.counter;

public interface ITickerInterface {

        void notifyState();
        void notifyTick();
        int getCountdown();
        void notifyCountDownBeep();

}
