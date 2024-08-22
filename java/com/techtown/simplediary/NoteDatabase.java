package com.techtown.simplediary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class NoteDatabase {
    private static final String TAG = "NoteDatabase";

    /**
     * 싱글톤 인스턴스
     */
    private static NoteDatabase database;

    /**
     * table name for Notes
     */
    public static String TABLE_NOTE = "NOTE";

    /**
     * VERSION
     */
    public static int DATABASE_VERSION = 1;

    /**
     * Helper class defined
     */
    private DatabaseHelper dbHelper;

    /**
     * SQLiteDatabase 인스턴스
     */
    private SQLiteDatabase db;

    /**
     * 컨텍스트 객체
     */
    Context context;

    public NoteDatabase(Context context) {
        this.context = context;
    }

    /**
     * 인스턴스 생성 및 가져오기
     */
    public static NoteDatabase getInstance(Context context){
        if(database == null){
            database = new NoteDatabase(context);
        }
        return database; //싱글턴 객체 반환
    }

    public boolean open(){
        println("opening database ["+AppConstants.DATABASE_NAME+"]");

        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();

        if(db == null){
            return false;
        }

        return true;
    }

    public void close(){
        println("closing database ["+AppConstants.DATABASE_NAME+"].");
        db.close();
        database = null;

        //dbHelper는 그대로 둔다
    }

    public Cursor rawQuery(String sql){
        println("\nrawQuery called.\n");

        Cursor c1 = null;
        try{
            c1 = db.rawQuery(sql, null);
            println("cursor count : "+c1.getCount());
        }catch (Exception e){
            e.printStackTrace();
        }

        return c1;
    }

    public boolean execSQL(String sql){
        println("\nexeSQL called\n");

        try{
            Log.d(TAG, "SQL : "+sql);
            db.execSQL(sql);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void println(String data){
        Log.d(TAG, data);
    }

    /**
     * Database Helper inner class
     */

    private class DatabaseHelper extends SQLiteOpenHelper{
        public DatabaseHelper(@Nullable Context context) {
            super(context, AppConstants.DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            println("creating database ["+AppConstants.DATABASE_NAME+"]");

            //TABLE_NOTE
            println("creating table ["+TABLE_NOTE+"]");

            //drop existing table
            String DROP_SQL = "DROP TABLE IF EXISTS "+TABLE_NOTE;
            try{
                db.execSQL(DROP_SQL);
            }catch (Exception e){
                Log.d(TAG, "Exeption in DROP_SQL");
                e.printStackTrace();
            }

            //create table
            String CREATE_SQL = "CREATE TABLE "+TABLE_NOTE+"("+
                    "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "+
                    "WEATHER TEXT DEFAULT '',"+
                    "ADDRESS TEXT DEFAULT '', "+
                    "LOCATION_X TEXT DEFAULT '', "+
                    "LOCATION_Y TEXT DEFAULT '', "+
                    "CONTENTS TEXT DEFAULT '', "+
                    "MOOD TEXT, "+
                    "PICTURE TEXT DEFAULT '',"+
                    "CREATE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"+
                    "MODIFY_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP"+
                    ")";
            try{
                db.execSQL(CREATE_SQL);
            }catch (Exception e){
                Log.e(TAG, "Exception in CREATE_SQL",e);
            }

            // create index
            String CREATE_INDEX_SQL = "CREATE INDEX "+TABLE_NOTE+ "_IDX ON "+TABLE_NOTE+"(" +
                    "CREATE_DATE" +
                    ")"; //CREATE_DATE를 기준으로 인덱스 생성

            try{
                db.execSQL(CREATE_INDEX_SQL);
            }catch (Exception e){
                Log.e(TAG, "Exeption in CREATE_INDEX_SQL", e);
            }

        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            println("opened database ["+AppConstants.DATABASE_NAME+"]");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            println("Upgrading database from version "+oldVersion+" to "+newVersion);
        }
    }
}
