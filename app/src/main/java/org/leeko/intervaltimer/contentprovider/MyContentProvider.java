package org.leeko.intervaltimer.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.leeko.intervaltimer.database.DatabaseHelper;
import org.leeko.intervaltimer.database.WorkoutTable;

import java.util.Arrays;
import java.util.HashSet;

public class MyContentProvider extends ContentProvider {

    // database
    private DatabaseHelper database;

    // used for the UriMacher
    private static final int WORKOUTS = 10;
    private static final int WORKOUT_ID = 20;

    public static final String AUTHORITY = "org.leeko.intervaltimer.contentprovider";

    private static final String BASE_PATH = "workouts";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    /*
    public static final Uri CONTENT_URI_TEST = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH + "/6");

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/roundsets";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/roundset";
*/
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, WORKOUTS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", WORKOUT_ID);
    }

    @Override
    public boolean onCreate() {
        database = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(WorkoutTable.TABLE_WORKOUT);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case WORKOUTS:
                break;
            case WORKOUT_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(WorkoutTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {


        Log.d("mikko", " INSERT:  " + uri);


        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        long id = 0;
        switch (uriType) {
            case WORKOUTS:
                id = sqlDB.insert(WorkoutTable.TABLE_WORKOUT, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case WORKOUTS:
                rowsDeleted = sqlDB.delete(WorkoutTable.TABLE_WORKOUT, selection,
                        selectionArgs);
                break;
            case WORKOUT_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(WorkoutTable.TABLE_WORKOUT,
                            WorkoutTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(WorkoutTable.TABLE_WORKOUT,
                            WorkoutTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {


        Log.d("mikko", " UPDATE:  " + uri);

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case WORKOUTS:
                rowsUpdated = sqlDB.update(WorkoutTable.TABLE_WORKOUT,
                        values,
                        selection,
                        selectionArgs);
                break;
            case WORKOUT_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {

                    rowsUpdated = sqlDB.update(WorkoutTable.TABLE_WORKOUT,
                            values,
                            WorkoutTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(WorkoutTable.TABLE_WORKOUT,
                            values,
                            WorkoutTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                WorkoutTable.COLUMN_ID,
                WorkoutTable.COLUMN_TAB,
                WorkoutTable.COLUMN_NAME,
                WorkoutTable.COLUMN_WARM_UP_MIN,
                WorkoutTable.COLUMN_WARM_UP_SEC,
                WorkoutTable.COLUMN_ROUNDS,
                WorkoutTable.COLUMN_WORK_MIN,
                WorkoutTable.COLUMN_WORK_SEC,
                WorkoutTable.COLUMN_REST_MIN,
                WorkoutTable.COLUMN_REST_SEC,
                WorkoutTable.COLUMN_MANUAL
        };


        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }


}