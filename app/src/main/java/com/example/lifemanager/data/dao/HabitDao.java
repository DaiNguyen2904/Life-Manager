package com.example.lifemanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lifemanager.data.entity.Habit;

import java.util.List;

@Dao
public interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Habit habit);

    @Update
    int update(Habit habit);

    @Delete
    int delete(Habit habit);

    @Query("SELECT * FROM habits ORDER BY id")
    LiveData<List<Habit>> observeAll();

    @Query("SELECT * FROM habits ORDER BY id")
    List<Habit> getAllSync();

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    Habit getByIdSync(long id);

    /** Đếm thói quen trùng tên (không phân biệt hoa thường), bỏ qua bản ghi excludeId. */
    @Query("SELECT COUNT(*) FROM habits WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    int countByName(String name, long excludeId);
}
