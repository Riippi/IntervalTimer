package org.leeko.intervaltimer;


import android.content.ContentValues;
import android.database.Cursor;

import org.leeko.intervaltimer.database.WorkoutTable;

/*
 * Class that has all the info of a workout
 */
public class Workout {

    private int id = 0;
    private int tabId = 0;
    private String name = "workout";
    private int roundAmount = 3;
    private int workMin = 0;
    private int workSec = 20;
    private int restMin = 0;
    private int restSec = 10;
    private int warmupMin = 0;
    private int warmupSec = 0;
    private boolean manual = false;


    /**
     * get database column id
     * @return database column id
     */
    public int getId() {
        return id;
    }

    /**
     * set database column id
     * @param aId database column id
     */
    public void setId(int aId) {
        id = aId;
    }

    public int getTabId() {
        return tabId;
    }

    public void setTabId(int tabId) {
        this.tabId = tabId;
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {


        name = aName;

        if (name == null) {
            name = "null";
        }

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


    /**
     *
     * @return  manual setting as integer
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


    /**
     * Imports workout data from database Cursor
     * @param cursor
     */
    public void importFromCursor(Cursor cursor) {

        setId(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_ID)));
        setTabId(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_TAB)));
        setName(cursor.getString(cursor.getColumnIndex(WorkoutTable.COLUMN_NAME)));
        setWarmupMin(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WARM_UP_MIN)));
        setWarmupSec(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WARM_UP_SEC)));

        setRoundAmount(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_ROUNDS)));

        setWorkMin(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WORK_MIN)));
        setWorkSec(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WORK_SEC)));

        setRestMin(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_REST_MIN)));
        setRestSec(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_REST_SEC)));

        setManualInteger(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_MANUAL)));

    }


    /**
     * export workout data to ContentValues for database
     * @return
     */
    public ContentValues getContentValues() {

        ContentValues values = new ContentValues();
        values.put(WorkoutTable.COLUMN_TAB, getTabId());
        values.put(WorkoutTable.COLUMN_NAME, getName());
        values.put(WorkoutTable.COLUMN_ROUNDS, getRoundAmount());
        values.put(WorkoutTable.COLUMN_WARM_UP_MIN, getWarmupMin());
        values.put(WorkoutTable.COLUMN_WARM_UP_SEC, getWarmupSec());
        values.put(WorkoutTable.COLUMN_WORK_MIN, getWorkMin());
        values.put(WorkoutTable.COLUMN_WORK_SEC, getWorkSec());
        values.put(WorkoutTable.COLUMN_REST_MIN, getRestMin());
        values.put(WorkoutTable.COLUMN_REST_SEC, getRestSec());
        values.put(WorkoutTable.COLUMN_MANUAL, getManualInteger());

        return values;
    }


    @Override
    public String toString() {
        return "Name=" + name + " DBID=" + id + " TABID=" + tabId;
    }


}

