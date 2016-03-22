package com.ucsf.ui.admin;

/**
 * Created by yanrongli on 3/12/16.
 */
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yanrongli on 3/2/16.
 */
public class SensorTagDBHelper extends SQLiteOpenHelper {
    public static String DATABASE_NAME = "TestSensorTagConfig.db";
    public static String TABLE_NAME = "sensortag_config";
    public static String COL_1 = "ID";
    public static String COL_2 = "SENSORTAG_ID";
    public static String COL_3 = "SENSORTAG_TYPE";
    //.....

    public SensorTagDBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT,SENSORTAG_ID TEXT,SENSORTAG_TYPE TEXT)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    boolean insertData(String sensortag_id, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, sensortag_id);
        contentValues.put(COL_3, type);
        long result = db.insert(TABLE_NAME, null, contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+TABLE_NAME, null);
        return res;
    }

    public JSONObject getJsonFromAllData() {
        Cursor cursor = getAllData();
        JSONObject results = new JSONObject();
        JSONArray resultsArray = new JSONArray();

        cursor.moveToFirst();
        int i = 0;
        while(!cursor.isAfterLast())
        {
            JSONObject entry = new JSONObject();
            try{
                //entry.put("id", cursor.getString(cursor.getColumnIndex(COL_1)));
                entry.put("sensortag_id", cursor.getString(cursor.getColumnIndex(COL_2)));
                entry.put("sensortag_type", cursor.getString(cursor.getColumnIndex(COL_3)));

                cursor.moveToNext();

                resultsArray.put(i, entry);
                i++;
            } catch(JSONException e){
                e.printStackTrace();
            }
        }

        try {
            results.put("all_data", resultsArray);
        } catch(JSONException e){
            e.printStackTrace();
        }

        return results;

    }

    public boolean updateData(String id, String sensortag_id, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, id);
        contentValues.put(COL_2, sensortag_id);
        contentValues.put(COL_3, type);
        db.update(TABLE_NAME, contentValues, "ID = ?", new String[] {id});
        return true;
    }

    public Integer deleteData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ID = ?", new String[] {id});
    }
}

