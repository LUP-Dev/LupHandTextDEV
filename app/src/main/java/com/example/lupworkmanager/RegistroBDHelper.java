package com.example.lupworkmanager;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class RegistroBDHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RegistroLecturas.db";

    public RegistroBDHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //COmandos SQL

        sqLiteDatabase.execSQL("CREATE TABLE " + RegistroEsquemaBDD.RegistroEntry.TABLE_NAME + " ("
                + RegistroEsquemaBDD.RegistroEntry.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RegistroEsquemaBDD.RegistroEntry.EXECUTION_HOUR + " DATE NOT NULL,"
                + RegistroEsquemaBDD.RegistroEntry.TEXT_DETECTED + " TEXT NOT NULL,"
                + RegistroEsquemaBDD.RegistroEntry.EXECUTION_TIME + " NUMERIC NOT NULL,"
                + "UNIQUE (" + RegistroEsquemaBDD.RegistroEntry.ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    //INSERTAR DATOS EN BDD
    public void insertar(String hour, String stringResult, String timeS){

        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys

        ContentValues values = new ContentValues();
        //values.put(RegistroEsquemaBDD.RegistroEntry.ID, cont);
        values.put(RegistroEsquemaBDD.RegistroEntry.EXECUTION_HOUR, hour);
        values.put(RegistroEsquemaBDD.RegistroEntry.TEXT_DETECTED, stringResult);
        values.put(RegistroEsquemaBDD.RegistroEntry.EXECUTION_TIME, timeS);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(RegistroEsquemaBDD.RegistroEntry.TABLE_NAME,null,values);

        db.close();
    }

    //GET DATOS DE LA BDD
    //Se hace la llamada SELECT y se guardan los datos en un ArrayList de HASHMAP
    @SuppressLint("Range")
    public ArrayList<HashMap<String, String>> getLectures(){
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> lectureList = new ArrayList<>();
        String query = "SELECT id ,date, texto, time FROM "+ RegistroEsquemaBDD.RegistroEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(query,null);
        while (cursor.moveToNext()){
            HashMap<String,String> user = new HashMap<>();
            user.put("id",cursor.getString(cursor.getColumnIndex(RegistroEsquemaBDD.RegistroEntry.ID)));
            user.put("date",cursor.getString(cursor.getColumnIndex(RegistroEsquemaBDD.RegistroEntry.EXECUTION_HOUR)));
            user.put("text",cursor.getString(cursor.getColumnIndex(RegistroEsquemaBDD.RegistroEntry.TEXT_DETECTED)));
            user.put("time",cursor.getString(cursor.getColumnIndex(RegistroEsquemaBDD.RegistroEntry.EXECUTION_TIME)));
            lectureList.add(user);
        }

        return  lectureList;
    }

    // Delete por ID
    public void deleteById(String userid){
        SQLiteDatabase db = this.getWritableDatabase();

        String table = RegistroEsquemaBDD.RegistroEntry.TABLE_NAME;
        String whereClause = "id=?";
        String[] whereArgs = new String[] { String.valueOf(userid) };
        db.delete(table, whereClause, whereArgs);

        db.close();
    }

    // Update
    public int UpdateUserDetails(String location, String designation, int id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        //cVals.put(KEY_LOC, location);
        //cVals.put(KEY_DESG, designation);
        //int count = db.update(TABLE_Users, cVals, KEY_ID+" = ?",new String[]{String.valueOf(id)});
        return  0;
    }

    // BORRAR TODA LA BASE DE DATOS
    public void deleteAll(){

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RegistroEsquemaBDD.RegistroEntry.TABLE_NAME,null,null);
        db.close();

    }
}
