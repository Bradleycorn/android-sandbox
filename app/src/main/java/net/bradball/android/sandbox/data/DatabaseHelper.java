package net.bradball.android.sandbox.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import net.bradball.android.sandbox.data.DatabaseSchema.RecordingsTable;
import net.bradball.android.sandbox.data.DatabaseSchema.ShowsTable;
import net.bradball.android.sandbox.util.LogHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = LogHelper.makeLogTag(DatabaseHelper.class);
    private static final String DATABASE_NAME = "recordings_database";

    private static final int INIT_VERSION = 100;
    private static final int CURRENT_DATABASE_VERSION = INIT_VERSION;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, CURRENT_DATABASE_VERSION);
        //super(context,"/mnt/sdcard/database_name.db", null, CURRENT_DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LogHelper.v(TAG, "CREATE DATABASE");
        db.execSQL("CREATE TABLE " + ShowsTable.NAME + "(" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ShowsTable.COLUMNS.YEAR + " TEXT NOT NULL, " +
                ShowsTable.COLUMNS.DATE + " TEXT NOT NULL, " +
                ShowsTable.COLUMNS.LOCATION + " TEXT," +
                ShowsTable.COLUMNS.TITLE + " TEXT," +
                ShowsTable.COLUMNS.SETLIST + " TEXT, " +
                ShowsTable.COLUMNS.SOUNDBOARD + " INTEGER NOT NULL DEFAULT 0, " +
                ShowsTable.COLUMNS.DOWNLOADS + " INTEGER NOT NULL DEFAULT 0," +
                " UNIQUE (" + ShowsTable.COLUMNS.DATE + ") ON CONFLICT REPLACE" +
                ")"
        );

        db.execSQL("CREATE TABLE " + RecordingsTable.NAME + "(" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RecordingsTable.COLUMNS.SHOW_ID + " INTEGER NOT NULL, " +
                RecordingsTable.COLUMNS.IDENTIFIER + " TEXT NOT NULL, " +
                RecordingsTable.COLUMNS.DATE + " TEXT NOT NULL, " +
                RecordingsTable.COLUMNS.LOCATION + " TEXT," +
                RecordingsTable.COLUMNS.TITLE + " TEXT, " +
                RecordingsTable.COLUMNS.SETLIST + " TEXT, " +
                RecordingsTable.COLUMNS.SOUNDBOARD + " INTEGER NOT NULL DEFAULT 0, " +
                RecordingsTable.COLUMNS.PUBLISHER + " TEXT, " +
                RecordingsTable.COLUMNS.RATING + " REAL NOT NULL DEFAULT 0, " +
                RecordingsTable.COLUMNS.NUM_REVIEWS + " INTEGER NOT NULL DEFAULT 0, " +
                RecordingsTable.COLUMNS.DOWNLOADS + " INTEGER NOT NULL DEFAULT 0, " +
                RecordingsTable.COLUMNS.AVAILABLE_OFFLINE + " INTEGER NOT NULL DEFAULT 0," +
                RecordingsTable.COLUMNS.SOURCE + " TEXT," +
                " UNIQUE (" + RecordingsTable.COLUMNS.IDENTIFIER + ") ON CONFLICT REPLACE," +
                " FOREIGN KEY(" + RecordingsTable.COLUMNS.SHOW_ID + ") REFERENCES " + ShowsTable.NAME + "(" + BaseColumns._ID + ") ON DELETE CASCADE" +
                ")"
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}