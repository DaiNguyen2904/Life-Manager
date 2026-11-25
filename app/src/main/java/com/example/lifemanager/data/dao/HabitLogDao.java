package com.example.lifemanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.lifemanager.data.entity.HabitLog;

import java.util.List;

@Dao
public interface HabitLogDao {

    /** IGNORE: nếu đã ghi nhận trong ngày (trùng habit_id + date) thì bỏ qua, không tạo bản ghi trùng. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(HabitLog log);

    @Query("DELETE FROM habit_logs WHERE habit_id = :habitId AND date = :date")
    int delete(long habitId, long date);

    @Query("SELECT * FROM habit_logs ORDER BY date ASC")
    LiveData<List<HabitLog>> observeAll();

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habit_id = :habitId AND date = :date")
    int countForDay(long habitId, long date);
}
