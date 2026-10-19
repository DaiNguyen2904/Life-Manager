package com.life.management.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "life_management.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USER = "users";
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_NOTES = "notes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USER + " (" +
                "userId TEXT PRIMARY KEY, " +
                "email TEXT, " +
                "displayName TEXT, " +
                "photoUrl TEXT, " +
                "isLoggedIn INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                "expenseId TEXT PRIMARY KEY, " +
                "userId TEXT, " +
                "categoryId TEXT, " +
                "type TEXT, " +
                "amount REAL, " +
                "note TEXT, " +
                "date INTEGER, " +
                "paymentMethod TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_TASKS + " (" +
                "taskId TEXT PRIMARY KEY, " +
                "userId TEXT, " +
                "title TEXT, " +
                "done INTEGER, " +
                "due TEXT, " +
                "priority TEXT, " +
                "status TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_NOTES + " (" +
                "userId TEXT PRIMARY KEY, " +
                "content TEXT, " +
                "updatedAt INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public void saveUser(String userId, String email, String displayName, String photoUrl, boolean isLoggedIn) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", userId);
        cv.put("email", email);
        cv.put("displayName", displayName);
        cv.put("photoUrl", photoUrl);
        cv.put("isLoggedIn", isLoggedIn ? 1 : 0);
        db.insertWithOnConflict(TABLE_USER, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USER + " LIMIT 1", null);
    }

    public void clearUser() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER, null, null);
    }
}
