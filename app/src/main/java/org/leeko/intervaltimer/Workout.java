package org.leeko.intervaltimer;


/*
 * Class that has all the info of a workout
 */
public class Workout {

    private int id = 0;
    private String name = "workout";
    private int roundAmount = 3;
    private int workMin = 0;
    private int workSec = 20;
    private int restMin = 0;
    private int restSec = 10;
    private int warmupMin = 0;
    private int warmupSec = 0;
    private boolean manual = false;
    // private int countdown = 0;


//        public void import(Hashtable data)
//        {
//             data.TryGetValue("roundAmount", out roundAmount);
//             data.TryGetValue("workMin", out workMin);
//             data.TryGetValue("workSec", out workSec);
//             data.TryGetValue("restMin", out restMin);
//             data.TryGetValue("restSec", out restSec);
//             data.TryGetValue("warmupMin", out warmupMin);
//             data.TryGetValue("warmupSec", out warmupSec);
//             data.TryGetValue("countdown", out countdown);
//
//             int temp = 0;
//             data.TryGetValue("manual", out temp);
//
//             if (temp == 1)
//             {
//                 manual = true;
//             }
//             else
//             {
//                 manual = false;
//             }
//
//
//
//        }
//
//
//        public Hashtable export()
//        {
//            Dictionary<string, int> AuthorList = new Dictionary<string, int>();
//
//            AuthorList.Add("roundAmount", roundAmount);
//            AuthorList.Add("workMin", workMin);
//            AuthorList.Add("workSec", workSec);
//            AuthorList.Add("restMin", restMin);
//            AuthorList.Add("restSec", restSec);
//            AuthorList.Add("warmupMin", warmupMin);
//            AuthorList.Add("warmupSec", warmupSec);
//            AuthorList.Add("countdown", countdown);
//
//            if (manual)
//            {
//                AuthorList.Add("manual", 1);
//            }
//            else
//            {
//                AuthorList.Add("manual", 0);
//            }
//            
//
//            return AuthorList;
//        }

    public int getId() {
        return id;
    }

    public void setId(int aId) {
        id = aId;
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        name = aName;
    }

    public int getRoundAmount() {
        return roundAmount;
    }

    public void setRoundAmount(int aRounds) {
        roundAmount = aRounds;
    }

    public int getWorkMin() {
        return workMin;
    }

    public void setWorkMin(int aMin) {
        workMin = aMin;
    }

    public int getWorkSec() {
        return workSec;
    }

    public void setWorkSec(int aSec) {
        workSec = aSec;
    }

      /* public void setCountdown(int aCountdown)
        {
            countdown = aCountdown;
        }*/

    public int getRestMin() {
        return restMin;
    }


    // Getters

    public void setRestMin(int aMin) {
        restMin = aMin;
    }

    public int getRestSec() {
        return restSec;
    }

    public void setRestSec(int aSec) {
        restSec = aSec;
    }

    public int getWarmupMin() {
        return warmupMin;
    }

    public void setWarmupMin(int aMin) {
        warmupMin = aMin;
    }

    public int getWarmupSec() {
        return warmupSec;
    }

    public void setWarmupSec(int aSec) {
        warmupSec = aSec;
    }

    public boolean getManual() {
        return manual;
    }

    public void setManual(boolean aManual) {
        manual = aManual;
    }


 /*
       public int getCountdown()
        {
            return countdown;
        }
*/

    public int getManualInteger() {
        if (manual) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setManualInteger(int aManual) {
        if (aManual == 0) {
            manual = false;
        } else {
            manual = true;
        }
    }

    public int getWorkInSec() {
        return (workMin * 60) + workSec;
    }

    public int getRestInSec() {
        return (restMin * 60) + restSec;
    }

    public int getWarmupInSec() {
        return (warmupMin * 60) + warmupSec;
    }


    public String getWarmupInString() {
        return Utils.intsToStringPadAll(warmupMin, warmupSec);
    }

    public String getWorkInString() {
        return Utils.intsToStringPadAll(workMin, workSec);
    }

    public String getRestInString() {
        return Utils.intsToStringPadAll(restMin, restSec);
    }





    /**
     * @return total time of the workout in String   X:XX
     */
    public String getTotal() {

        if (roundAmount == 0) {
            return "unlimited";
        }


        int work = getWorkMin() * 60 + getWorkSec();

        work = work * roundAmount;


        int rest = getRestMin() * 60 + getRestSec();

        if (roundAmount > 1) {
            rest = rest * (roundAmount - 1);
        }

        if (getManual()) {
            rest = 0;
        }


        int totalInSec = rest + work;

        String formed = Utils.secondsToString(totalInSec);


        if (getManual()) {
            return formed + " + rest";
        }


        return formed;


    }


}

