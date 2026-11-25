package com.example.lifemanager.data.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.lifemanager.data.entity.UserAccount;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(UserAccount user);

    @Nullable
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserAccount getByUsernameSync(String username);

    @Nullable
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserAccount getByIdSync(long id);

    @Query("SELECT COUNT(*) FROM users")
    int countSync();

    @Query("UPDATE users SET last_login_at = :time WHERE id = :id")
    int updateLastLogin(long id, long time);

    @Query("UPDATE users SET display_name = :displayName WHERE id = :id")
    int updateDisplayName(long id, String displayName);
}
