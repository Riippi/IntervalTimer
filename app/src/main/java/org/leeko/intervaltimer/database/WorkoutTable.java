package org.leeko.intervaltimer.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class WorkoutTable {

	public static final String[] projection = { 
		WorkoutTable.COLUMN_ID,
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


	//	  String sql = "INSERT INTO Tasks (_id, Aircraft, Station, Discrepancy,DateCreated, CreatedBy, Status, DateClosed, ClosedBy,
	//ArrivalFlightID, RecordChangedByUI) VALUES ('" + tasks[i]._id + "','" + tasks[i].Aircraft + "','" + tasks[i].Station + "','" + tasks[i].Discrepancy + "','" + tasks[i].DateCreated + "','" + tasks[i].CreatedBy + "','" + tasks[i].Status + "','" + tasks[i].DateClosed + "','" + tasks[i].ClosedBy + "','" + tasks[i].ArrivalFlightID + "','N')"; 




	private static final String DATABASE_INSERT = "INSERT INTO " 
			+ TABLE_WORKOUT
			+ "(" 
			//	      + COLUMN_ID + ", " 
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
			//		      + "3"
			+ "'Intervals', "
			+ "3, "
			+ "0, "
			+ "6, "
			+ "1, "
			+ "0, "
			+ "3, "
			+ "0, "
			+ "0"
			+ ");";


	private static final String INSERT_TABATA = "INSERT INTO " 
			+ TABLE_WORKOUT
			+ "(" 
			//	      + COLUMN_ID + ", " 
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
			//		      + "3"
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


	private static final String INSERT_LITTLE = "INSERT INTO " 
			+ TABLE_WORKOUT
			+ "(" 
			//	      + COLUMN_ID + ", " 
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
			//		      + "3"
			+ "'Little Method', "
			+ "3, "
			+ "0, "
			+ "8, "
			+ "1, "
			+ "0, "
			+ "1, "
			+ "15, "
			+ "0"
			+ ");";


	private static final String INSERT_B4x2 = "INSERT INTO " 
			+ TABLE_WORKOUT
			+ "(" 
			//	      + COLUMN_ID + ", " 
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
			//		      + "3"
			+ "'Boxing 4x2', "
			+ "0, "
			+ "0, "
			+ "4, "
			+ "2, "
			+ "0, "
			+ "1, "
			+ "0, "
			+ "0"
			+ ");";


	private static final String INSERT_MANUAL = "INSERT INTO " 
			+ TABLE_WORKOUT
			+ "(" 
			//	      + COLUMN_ID + ", " 
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
			//		      + "3"
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
	
	private static final String INSERT_MMA = "INSERT INTO " 
			+ TABLE_WORKOUT
			+ "(" 
			//	      + COLUMN_ID + ", " 
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
			//		      + "3"
			+ "'MMA 3x3', "
			+ "0, "
			+ "0, "
			+ "3, "
			+ "3, "
			+ "0, "
			+ "1, "
			+ "0, "
			+ "0"
			+ ");";

	//	  setIntValues("Little", 8, 1, 0, 1, 15, 0, 0, false);

	//	  setIntValues("Boxing 4x2", 4, 2, 0, 1, 0, 0, 0, false);
	//	  setIntValues("Boxing 3x3", 3, 3, 0, 1, 0, 0, 0, false);
	// setIntValues("Manual", 10, 1, 0, 0, 30, 1, 0, true);

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);


		database.execSQL(INSERT_TABATA);
		database.execSQL(INSERT_LITTLE);
		database.execSQL(INSERT_B4x2);
		database.execSQL(INSERT_MANUAL);
		database.execSQL(DATABASE_INSERT);
		database.execSQL(INSERT_MMA);
		//	    database.execSQL(DATABASE_INSERT);

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
