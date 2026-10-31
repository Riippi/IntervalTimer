package org.leeko.intervaltimer;

import android.content.Context;

public class Utils
{




	public static int[] splitTime(int time)
	{
		int[] split = {0,0}; 

		if (time < 10)
		{
			split[0] = 0;
			split[1] = time;
		}
		else
		{
			split[0] = time / 10;
			split[1] = time - split[0] * 10;
		}

		return split;
	}


	public static int combineTime(int one, int two)
	{
		return one * 10 + two;
	}







	/**
	 * Converts time that is in seconds to string formatted for the timer view
	 * @param seconds
	 * @return  String for example 06:33
	 */    
	public static String secondsToString(int seconds)
	{
		int remainMins = seconds / 60;
		String str = "";
		str += remainMins;

		int secs = seconds - (remainMins * 60);

		str += ":";

		if (secs < 10)
		{
			str += "0";
		}

		str += secs;

		return str;
	}


    /**
     * Makes a string out of int values. Zero padding for seconds.
     * @param aMin
     * @param aSec
     * @return
     */
	public static String integersToString(int aMin, int aSec) {

		String dd = "";

		dd += aMin;
		dd += ":";

		if (aSec < 10)
		{
			dd += "0";
		}

		dd += aSec;

		return dd;
	}



    /**
     * Makes a string out of integer values with zero padding. Format "02:05".
     *
     * @param min
     * @param sec
     * @return
     */
    public static String intsToStringPadAll(int min, int sec) {

        String part1 = "" + min;
        if (min < 10) {
            part1 = "0" + part1;
        }

        String part2 = "" + sec;
        if (sec < 10) {
            part2 = "0" + part2;
        }

        return part1 + ":" + part2;

    }


    /** Preference key for "lock the timer view to portrait". */
    public static final String PREF_LOCK_PORTRAIT = "lock_portrait";

    /**
     * True on tablets/foldables (smallest width 600dp or more). Android 16 ignores
     * orientation locks for apps targeting API 36 on those screens, so the portrait
     * lock setting is hidden there instead of being a switch that does nothing.
     */
    public static boolean isLargeScreen(Context context) {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

}
