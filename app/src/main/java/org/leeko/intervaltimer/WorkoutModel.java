package org.leeko.intervaltimer;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import org.leeko.intervaltimer.contentprovider.MyContentProvider;
import org.leeko.intervaltimer.database.WorkoutTable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class WorkoutModel {


    private Hashtable<Integer, Workout> hash; // Tab id  and workout
    private static WorkoutModel singleton;

    private WorkoutModel() {

        hash = new Hashtable<Integer, Workout>();  // Tab id  and workout
        loadAllWorkouts();
    }

    public static WorkoutModel getInstance() {

        if (singleton == null) {
            singleton = new WorkoutModel();
        }
        return singleton;
    }


    public Workout getWorkoutCached(int tabId) {


        //Log.d("Load", "TAB ID: " + tabId);

        // Actually no idea if this hash-caching has any use
        if (hash.get(tabId) == null) {

            Workout w = getWorkoutByTabId(tabId);
            hash.put(w.getTabId(), w);
        }

        return hash.get(tabId);
    }


    /**
     * Save a workout object to the database and cache.
     *
     * @param workout
     */
    public void saveWorkout(Workout workout) {

        // null check
        if (workout == null) {
            return;
        }


        ContentValues values = workout.getContentValues();

        Uri uri = null;

        // cache only if workout already has database id
        if (workout.getId() > 0) {
            hash.put(workout.getTabId(), workout);
            uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + workout.getId());
        }

        if (uri == null) {
            MainActivity.getInstance().getContentResolver().insert(MyContentProvider.CONTENT_URI, values);

            // reload all workouts. we get proper database id for this new workout
            loadAllWorkouts();
        } else {
            MainActivity.getInstance().getContentResolver().update(uri, values, null, null);
        }
    }


    /**
     * Loads all workout objects from the database and puts them into hash cache
     */
    private void loadAllWorkouts() {


        Cursor cursor = MainActivity.getInstance().getContentResolver().query(MyContentProvider.CONTENT_URI, WorkoutTable.projection, null, null, null);

        // Clear the cache
        if (hash != null) {
            hash.clear();
        } else {
            hash = new Hashtable<Integer, Workout>();  // Tab id  and workout
        }

        if (cursor != null) {

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

                Workout workout = new Workout();
                workout.importFromCursor(cursor);
                hash.put(workout.getTabId(), workout);

                Log.d("Workoutmodel LOADED: ", workout.toString());
            }

            cursor.close();
        }

    }


    /**
     * @return amount of workouts in the cache (and database)
     */
    public int getWorkoutAmount() {

        if (hash != null) {
            return hash.size();
        }

        return 0;
    }


    /**
     * Get workout object from the database
     *
     * @param tabId tab id of the workout
     * @return workout
     */
    private Workout getWorkoutByTabId(int tabId) {


        String mSelectionClause = WorkoutTable.COLUMN_TAB + " = ?";
        String[] mSelectionArgs = {String.valueOf(tabId)};

        Workout workout = new Workout();
        Cursor cursor = MainActivity.getInstance().getContentResolver().query(MyContentProvider.CONTENT_URI, WorkoutTable.projection, mSelectionClause, mSelectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {

            cursor.moveToFirst();
            workout.importFromCursor(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }


        return workout;
    }


    /**
     * Deletes a workout from the database
     *
     * @param tabId
     */
    public void deleteWorkout(int tabId) {


        // Selection clause for tab id
        String mSelectionClause = WorkoutTable.COLUMN_TAB + " = ?";

        // Arguments
        String[] mSelectionArgs = {String.valueOf(tabId)};

       // final int delete = MainActivity.getInstance().getContentResolver().delete(MyContentProvider.CONTENT_URI, mSelectionClause, mSelectionArgs);


        // After deleting a workout we must change tab id:s of all workouts to the right so there won't be empty slot

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(
                ContentProviderOperation.newDelete(MyContentProvider.CONTENT_URI)
                        .withSelection(mSelectionClause, mSelectionArgs)
                        .build());


        Iterator it = hash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            int key = (Integer) pair.getKey();

            Log.d("Workoutmodel update: ", "key= " + key + " iterating: " + hash.get(key).toString());

            if (key > tabId) {

                int newTabId = key - 1;
                String orgWhere = WorkoutTable.COLUMN_ID + " = ? ";
                int id = hash.get(key).getId();
                String[] orgWhereParams = new String[]{String.valueOf(id)};

                Log.d("Workoutmodel update: ", "new TAB: " + newTabId + " for: " + hash.get(key).toString());

                ops.add(
                        ContentProviderOperation.newUpdate(MyContentProvider.CONTENT_URI)
                                .withSelection(orgWhere, orgWhereParams)
                                .withValue(WorkoutTable.COLUMN_TAB, newTabId)
                                .build());

            }
            it.remove(); // avoids a ConcurrentModificationException
        }

        hash.clear();

        try {
            MainActivity.getInstance().getContentResolver().
                    applyBatch(MyContentProvider.AUTHORITY, ops);
        } catch (RemoteException e) {
            // do s.th.
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // do s.th.
            e.printStackTrace();
        }


        loadAllWorkouts();


        int nextTab = tabId - 1;

        if (nextTab < 0) {
            nextTab = 0;
        }

        MainActivity.getInstance().updateWholeTabView(nextTab);

    }


    /**
     * Move a workout to "right". Increase it's tab id.
     *
     * @param tabId
     */
    public void moveRight(int tabId) {
        Log.d("WorkoutModel", "MOVE RIGHT " + tabId);

        switchPlaces(tabId + 1, tabId);
        MainActivity.getInstance().updateWholeTabView(tabId + 1);

    }

    /**
     * Move a workout to "left". Decrease it's tab id.
     */
    public void moveLeft(int tabId) {
        Log.d("WorkoutModel", "MOVE LEFT " + tabId);

        switchPlaces(tabId, tabId - 1);
        MainActivity.getInstance().updateWholeTabView(tabId - 1);

    }


    /**
     * Switch tab id:s of two workout objects in database. Switches their places in tab view.
     *
     * @param toLeft
     * @param toRight
     */
    private void switchPlaces(int toLeft, int toRight) {


        int idToRight = hash.get(toRight).getId();
        int idToLeft = hash.get(toLeft).getId();

        ArrayList<ContentProviderOperation> ops =
                new ArrayList<ContentProviderOperation>();

        // First move

        String[] orgWhereParams = new String[]{String.valueOf(idToRight)};
        String orgWhere = WorkoutTable.COLUMN_ID + " = ? ";

        ops.add(ContentProviderOperation.newUpdate(MyContentProvider.CONTENT_URI)
                .withSelection(orgWhere, orgWhereParams)
                .withValue(WorkoutTable.COLUMN_TAB, toLeft)
                .build());

        // Second move

        orgWhereParams = new String[]{String.valueOf(idToLeft)};
        orgWhere = WorkoutTable.COLUMN_ID + " = ? ";

        ops.add(ContentProviderOperation.newUpdate(MyContentProvider.CONTENT_URI)
                .withSelection(orgWhere, orgWhereParams)
                .withValue(WorkoutTable.COLUMN_TAB, toRight)
                .build());


        try {
            MainActivity.getInstance().getContentResolver().
                    applyBatch(MyContentProvider.AUTHORITY, ops);
        } catch (RemoteException e) {
            // do s.th.
        } catch (OperationApplicationException e) {
            // do s.th.
        }

        loadAllWorkouts();

    }


    /**
     * Create new workout and add it
     */
    public void addWorkout() {

        int newTabId = hash.size();

        Workout workout = new Workout();
        workout.setTabId(newTabId);
        workout.setName("new workout");
        saveWorkout(workout);

       // ContentValues values = workout.getContentValues();
      //  MainActivity.getInstance().getContentResolver().insert(MyContentProvider.CONTENT_URI, values);

       // loadAllWorkouts();
        MainActivity.getInstance().updateWholeTabView(newTabId);

    }


}
