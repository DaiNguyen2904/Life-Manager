package com.example.lifemanager.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng habit_logs: lịch sử hoàn thành thói quen.
 * Chỉ mục duy nhất (habit_id, date) đảm bảo một thói quen chỉ được ghi nhận
 * hoàn thành tối đa một lần trong một ngày.
 */
@Entity(tableName = "habit_logs",
        foreignKeys = @ForeignKey(entity = Habit.class,
                parentColumns = "id",
                childColumns = "habit_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"habit_id", "date"}, unique = true)})
public class HabitLog {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "habit_id")
    private long habitId;

    /** Ngày hoàn thành (epochDay). */
    @ColumnInfo(name = "date")
    private long date;

    @ColumnInfo(name = "completed_at")
    private long completedAt;

    public HabitLog() {
    }

    @Ignore
    public HabitLog(long habitId, long date) {
        this.habitId = habitId;
        this.date = date;
        this.completedAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getHabitId() {
        return habitId;
    }

    public void setHabitId(long habitId) {
        this.habitId = habitId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
}
