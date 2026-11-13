package org.leeko.intervaltimer;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.leeko.intervaltimer.contentprovider.MyContentProvider;
import org.leeko.intervaltimer.database.WorkoutTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WorkoutModel {


    private HashMap<Integer, Workout> hash; // Tab id  and workout
    private static WorkoutModel singleton;

    // Counts every change to the saved workouts, so a screen that was covered by another
    // one (Main behind Manage) can tell when it has to redraw.
    private int revision = 0;

    // The application context, handed over by MyContentProvider when the process starts
    // (a provider is created before any activity). The model used to reach the database
    // through MainActivity, which does not exist when Android restores the app straight
    // into another screen.
    private static Context appContext;

    public static void attach(Context context) {
        appContext = context.getApplicationContext();
    }

    private static ContentResolver resolver() {
        return appContext.getContentResolver();
    }

    /** Redraws the main screen on the given workout, if the main screen exists. */
    private static void showOnMainScreen(int tabId) {
        MainActivity main = MainActivity.getInstance();
        if (main != null) {
            main.updateWholeTabView(tabId);
        }
    }

    private WorkoutModel() {

        hash = new HashMap<Integer, Workout>();  // Tab id  and workout
        loadAllWorkouts();
    }

    public static WorkoutModel getInstance() {

        if (singleton == null) {
            singleton = new WorkoutModel();
        }
        return singleton;
    }


    public int getRevision() {
        return revision;
    }

    /** @return tab id of the workout with this database id, or -1 */
    public int findTabIdByWorkoutId(int workoutId) {
        for (Map.Entry<Integer, Workout> entry : hash.entrySet()) {
            if (entry.getValue().getId() == workoutId) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public Workout getWorkoutCached(int tabId) {

        if (hash.get(tabId) == null) {

            // Only a workout that really is in the database goes into the cache. A miss
            // gives back an empty Workout on tab 0, and caching that would replace the
            // first workout with a blank one that is later saved as a new row.
            Workout w = getWorkoutByTabId(tabId);
            if (w.getId() > 0) {
                hash.put(w.getTabId(), w);
            }
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

        revision++;


        ContentValues values = workout.getContentValues();

        Uri uri = null;

        // cache only if workout already has database id
        if (workout.getId() > 0) {
            hash.put(workout.getTabId(), workout);
            uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + workout.getId());
        }

        if (uri == null) {
            resolver().insert(MyContentProvider.CONTENT_URI, values);

            // reload all workouts. we get proper database id for this new workout
            loadAllWorkouts();
        } else {
            resolver().update(uri, values, null, null);
        }
    }


    /**
     * Loads all workout objects from the database and puts them into hash cache
     */
    private void loadAllWorkouts() {

        revision++;

        // Clear the cache
        if (hash != null) {
            hash.clear();
        } else {
            hash = new HashMap<Integer, Workout>();  // Tab id  and workout
        }

        // The tab ids are the workouts' positions and must be exactly 0, 1, 2... The screens
        // crash on a missing position, and of two workouts on the same position one would
        // never be shown. So the rows are read in position order and numbered again here;
        // any row whose saved position was off (a database damaged by an older version or
        // an interrupted write) is corrected in the database as well.
        String positionOrder = WorkoutTable.COLUMN_TAB + ", " + WorkoutTable.COLUMN_ID;
        ArrayList<ContentProviderOperation> repairs = new ArrayList<ContentProviderOperation>();

        try (Cursor cursor = resolver().query(MyContentProvider.CONTENT_URI, WorkoutTable.projection, null, null, positionOrder)) {

            if (cursor != null) {
                int position = 0;
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

                    Workout workout = new Workout();
                    workout.importFromCursor(cursor);
                    if (workout.getTabId() != position) {
                        workout.setTabId(position);
                        repairs.add(ContentProviderOperation.newUpdate(MyContentProvider.CONTENT_URI)
                                .withSelection(WorkoutTable.COLUMN_ID + " = ? ",
                                        new String[]{String.valueOf(workout.getId())})
                                .withValue(WorkoutTable.COLUMN_TAB, position)
                                .build());
                    }
                    hash.put(position, workout);
                    position++;
                }
            }
        }

        if (!repairs.isEmpty()) {
            try {
                resolver().
                        applyBatch(MyContentProvider.AUTHORITY, repairs);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
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

        try (Cursor cursor = resolver().query(MyContentProvider.CONTENT_URI, WorkoutTable.projection, mSelectionClause, mSelectionArgs, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                workout.importFromCursor(cursor);
            }
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

       // final int delete = resolver().delete(MyContentProvider.CONTENT_URI, mSelectionClause, mSelectionArgs);


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

            if (key > tabId) {

                int newTabId = key - 1;
                String orgWhere = WorkoutTable.COLUMN_ID + " = ? ";
                int id = hash.get(key).getId();
                String[] orgWhereParams = new String[]{String.valueOf(id)};

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
            resolver().
                    applyBatch(MyContentProvider.AUTHORITY, ops);
        } catch (RemoteException e) {
            // do s.th.
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // do s.th.
            e.printStackTrace();
        }


        loadAllWorkouts();


        // Show the workout that took the deleted one's place (or the last one).
        showOnMainScreen(tabId);

    }


    /**
     * Moves a workout from one place to another (drag and drop on the manage screen): the
     * ones in between shift by one. Does not touch the screens, the caller redraws.
     */
    public void moveWorkout(int from, int to) {
        if (from == to || from < 0 || to < 0) {
            return;
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (Map.Entry<Integer, Workout> entry : hash.entrySet()) {
            int tab = entry.getKey();
            int newTab = tab;
            if (tab == from) {
                newTab = to;
            } else if (from < to && tab > from && tab <= to) {
                newTab = tab - 1;
            } else if (from > to && tab >= to && tab < from) {
                newTab = tab + 1;
            }
            if (newTab != tab) {
                ops.add(ContentProviderOperation.newUpdate(MyContentProvider.CONTENT_URI)
                        .withSelection(WorkoutTable.COLUMN_ID + " = ? ",
                                new String[]{String.valueOf(entry.getValue().getId())})
                        .withValue(WorkoutTable.COLUMN_TAB, newTab)
                        .build());
            }
        }

        try {
            resolver().
                    applyBatch(MyContentProvider.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

        loadAllWorkouts();
    }

    /**
     * Puts a workout at the given place, moving the workouts from there on one place to the
     * right. Used by Duplicate and by Undo after a delete. Shows and selects it afterwards.
     */
    public void insertWorkoutAt(Workout workout, int tabId) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        for (Map.Entry<Integer, Workout> entry : hash.entrySet()) {
            if (entry.getKey() >= tabId) {
                ops.add(ContentProviderOperation.newUpdate(MyContentProvider.CONTENT_URI)
                        .withSelection(WorkoutTable.COLUMN_ID + " = ? ",
                                new String[]{String.valueOf(entry.getValue().getId())})
                        .withValue(WorkoutTable.COLUMN_TAB, entry.getKey() + 1)
                        .build());
            }
        }

        workout.setId(0);
        workout.setTabId(tabId);
        ops.add(ContentProviderOperation.newInsert(MyContentProvider.CONTENT_URI)
                .withValues(workout.getContentValues())
                .build());

        try {
            resolver().
                    applyBatch(MyContentProvider.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

        loadAllWorkouts();
        showOnMainScreen(tabId);
    }

    /**
     * Inserts a copy of the workout right after it and selects the copy.
     *
     * @return tab id of the copy
     */
    public int duplicateWorkout(int tabId) {
        Workout copy = getWorkoutCached(tabId).copy();
        copy.setName(appContext.getString(R.string.workout_copy_name, copy.getName()));
        insertWorkoutAt(copy, tabId + 1);
        return tabId + 1;
    }


    /**
     * Move a workout to "right". Increase it's tab id.
     *
     * @param tabId
     */
    public void moveRight(int tabId) {
        switchPlaces(tabId + 1, tabId);
        showOnMainScreen(tabId + 1);

    }

    /**
     * Move a workout to "left". Decrease it's tab id.
     */
    public void moveLeft(int tabId) {
        switchPlaces(tabId, tabId - 1);
        showOnMainScreen(tabId - 1);

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
            resolver().
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
      //  resolver().insert(MyContentProvider.CONTENT_URI, values);

       // loadAllWorkouts();
        showOnMainScreen(newTabId);

    }


}
