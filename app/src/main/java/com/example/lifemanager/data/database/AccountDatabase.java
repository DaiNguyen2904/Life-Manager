package com.example.lifemanager.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.lifemanager.data.dao.UserDao;
import com.example.lifemanager.data.entity.UserAccount;

/**
 * Cơ sở dữ liệu tài khoản (accounts.db) dùng chung cho cả thiết bị - Singleton.
 * Tách riêng khỏi dữ liệu cá nhân: mỗi tài khoản có một tệp CSDL riêng (xem {@link AppDatabase}).
 */
@Database(entities = {UserAccount.class}, version = 1, exportSchema = false)
public abstract class AccountDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "accounts.db";

    private static volatile AccountDatabase instance;

    public abstract UserDao userDao();

    @NonNull
    public static AccountDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AccountDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AccountDatabase.class, DATABASE_NAME).build();
                }
            }
        }
        return instance;
    }
}
