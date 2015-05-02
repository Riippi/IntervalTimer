package org.leeko.intervaltimer;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.leeko.intervaltimer.contentprovider.MyContentProvider;
import org.leeko.intervaltimer.database.WorkoutTable;

import java.util.Hashtable;

public class WorkoutModel {
	
	

	private Hashtable<Integer, Workout> hash;
	private static WorkoutModel singleton;

    private WorkoutModel() {
        hash = new Hashtable<Integer, Workout>();
    }
	
	public static WorkoutModel getInstance() {
		
		if (singleton == null) {
			singleton = new WorkoutModel();
		}
		return singleton;
	}
	


	public Workout getWorkoutCached(int id) {

        // Actually no idea if this hash-caching has any use
        if (hash.get(id) == null) {
            hash.put(id, getWorkout(id));
        }
			
		return hash.get(id);
	}



    private Workout getWorkout(int id) {

        Workout workout = new Workout();
        Cursor cursor = MainActivity.getInstance().getContentResolver().query(MyContentProvider.CONTENT_URI, WorkoutTable.projection, null, null, null);

        if (cursor != null) {

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                // do what you need with the cursor here

                // In database rows start from 1
                int rowId = id + 1;

                if (cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_ID)) == rowId) {
                    workout.setId(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_ID)));
                    workout.setName(cursor.getString(cursor.getColumnIndex(WorkoutTable.COLUMN_NAME)));
                    workout.setWarmupMin(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WARM_UP_MIN)));
                    workout.setWarmupSec(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WARM_UP_SEC)));

                    workout.setRoundAmount(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_ROUNDS)));

                    workout.setWorkMin(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WORK_MIN)));
                    workout.setWorkSec(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_WORK_SEC)));

                    workout.setRestMin(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_REST_MIN)));
                    workout.setRestSec(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_REST_SEC)));

                    workout.setManualInteger(cursor.getInt(cursor.getColumnIndex(WorkoutTable.COLUMN_MANUAL)));

                    break;
                }
            }

            cursor.close();
        }



        return workout;
    }


    public void saveWorkout(Workout rs, int id) {

        // only save if
        if (rs == null) {
            return;
        }

        // cached
        hash.put(id, rs);

        ContentValues values = new ContentValues();
        values.put(WorkoutTable.COLUMN_NAME, rs.getName());
        values.put(WorkoutTable.COLUMN_ROUNDS, rs.getRoundAmount());
        values.put(WorkoutTable.COLUMN_WARM_UP_MIN, rs.getWarmupMin());
        values.put(WorkoutTable.COLUMN_WARM_UP_SEC, rs.getWarmupSec());
        values.put(WorkoutTable.COLUMN_WORK_MIN, rs.getWorkMin());
        values.put(WorkoutTable.COLUMN_WORK_SEC, rs.getWorkSec());
        values.put(WorkoutTable.COLUMN_REST_MIN, rs.getRestMin());
        values.put(WorkoutTable.COLUMN_REST_SEC, rs.getRestSec());
        values.put(WorkoutTable.COLUMN_MANUAL, rs.getManualInteger());


        Uri uri = null;

        String[] projection = { WorkoutTable.COLUMN_ID  };

        Cursor cursor = MainActivity.getInstance().getContentResolver().query(MyContentProvider.CONTENT_URI, projection, null, null, null);

        // In database row id:s start from 1
        int rowId = id + 1;

        if (cursor != null && cursor.moveToFirst()) {
            uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + rowId);
            cursor.close();
        }


        if (uri == null) {
            MainActivity.getInstance().getContentResolver().insert(MyContentProvider.CONTENT_URI, values);
        } else {
            MainActivity.getInstance().getContentResolver().update(uri, values, null, null);
        }
    }
}
