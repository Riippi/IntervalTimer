package org.leeko.intervaltimer.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class WorkoutTable {

	public static final String[] projection = { 
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

	public static final String TABLE_WORKOUT = "workout";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TAB = "tab_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_WARM_UP_MIN = "warm_up_min";
	public static final String COLUMN_WARM_UP_SEC = "warm_up_sec";
	public static final String COLUMN_ROUNDS = "rounds";
	public static final String COLUMN_WORK_MIN = "work_min";
	public static final String COLUMN_WORK_SEC = "work_sec";
	public static final String COLUMN_REST_MIN = "rest_min";
	public static final String COLUMN_REST_SEC = "rest_sec";
	public static final String COLUMN_MANUAL = "manual";



	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_WORKOUT
			+ "(" 
			+ COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_TAB + " integer not null, "
			+ COLUMN_NAME + " text not null, " 
			+ COLUMN_WARM_UP_MIN + " integer not null, " 
			+ COLUMN_WARM_UP_SEC  + " integer not null, "
			+ COLUMN_ROUNDS + " integer not null, " 
			+ COLUMN_WORK_MIN  + " integer not null, " 
			+ COLUMN_WORK_SEC + " integer not null, " 
			+ COLUMN_REST_MIN  + " integer not null, " 
			+ COLUMN_REST_SEC + " integer not null, " 
			+ COLUMN_MANUAL  + " integer not null" 
			+ ");";

	private static final String INSERT_FISRT_PART =
			"INSERT INTO "
					+ TABLE_WORKOUT
					+ "("
					+ COLUMN_TAB + ", "
					+ COLUMN_NAME + ", "
					+ COLUMN_WARM_UP_MIN + ", "
					+ COLUMN_WARM_UP_SEC  + ", "
					+ COLUMN_ROUNDS + ", "
					+ COLUMN_WORK_MIN  + ", "
					+ COLUMN_WORK_SEC + ", "
					+ COLUMN_REST_MIN  + ", "
					+ COLUMN_REST_SEC + ", "
					+ COLUMN_MANUAL  + ""
					+ ") VALUES ("
			;


	private static final String INSERT_TABATA = INSERT_FISRT_PART
			+ "0, "
			+ "'Tabata Protocol', "
			+ "0, "
			+ "10, "
			+ "8, "
			+ "0, "
			+ "20, "
			+ "0, "
			+ "10, "
			+ "0"
			+ ");";

	private static final String INSERT_HIIT = INSERT_FISRT_PART
			+ "1, "
			+ "'HIIT 40/20', "
			+ "0, "
			+ "10, "
			+ "8, "
			+ "0, "
			+ "40, "
			+ "0, "
			+ "20, "
			+ "0"
			+ ");";

	private static final String INSERT_EMOM = INSERT_FISRT_PART
			+ "2, "
			+ "'EMOM 10', "
			+ "0, "
			+ "10, "
			+ "10, "
			+ "1, "
			+ "0, "
			+ "0, "
			+ "0, "
			+ "0"
			+ ");";

	private static final String INSERT_BOXING = INSERT_FISRT_PART
			+ "3, "
			+ "'Boxing 3x3', "
			+ "0, "
			+ "0, "
			+ "3, "
			+ "3, "
			+ "0, "
			+ "1, "
			+ "0, "
			+ "0"
			+ ");";

	private static final String INSERT_NORWEGIAN = INSERT_FISRT_PART
			+ "4, "
			+ "'Norwegian 4x4', "
			+ "10, "
			+ "0, "
			+ "4, "
			+ "4, "
			+ "0, "
			+ "3, "
			+ "0, "
			+ "0"
			+ ");";

	private static final String INSERT_MANUAL = INSERT_FISRT_PART
			+ "5, "
			+ "'Manual Rest', "
			+ "1, "
			+ "0, "
			+ "5, "
			+ "1, "
			+ "0, "
			+ "0, "
			+ "30, "
			+ "1"
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(INSERT_TABATA);
		database.execSQL(INSERT_HIIT);
		database.execSQL(INSERT_EMOM);
		database.execSQL(INSERT_BOXING);
		database.execSQL(INSERT_NORWEGIAN);
		database.execSQL(INSERT_MANUAL);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(WorkoutTable.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKOUT);
		onCreate(database);
	}
} 
