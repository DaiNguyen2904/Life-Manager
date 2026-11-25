package com.example.lifemanager.data.model;

import androidx.annotation.NonNull;

import com.example.lifemanager.data.entity.Habit;

/**
 * Thói quen kèm các chỉ số được tính từ lịch sử hoàn thành (habit_logs).
 */
public class HabitWithStats {

    @NonNull
    public final Habit habit;
    /** Hôm nay có lịch thực hiện. */
    public boolean scheduledToday;
    /** Hôm nay đã hoàn thành. */
    public boolean doneToday;
    /** Chuỗi ngày liên tiếp hiện tại. */
    public int currentStreak;
    /** Kỷ lục chuỗi dài nhất. */
    public int longestStreak;
    public int weekDone;
    public int weekScheduled;
    public int monthDone;
    public int monthScheduled;
    /** Tổng số lần hoàn thành. */
    public int totalCompletions;

    public HabitWithStats(@NonNull Habit habit) {
        this.habit = habit;
    }

    public int getWeekRate() {
        return weekScheduled == 0 ? 0 : Math.round(weekDone * 100f / weekScheduled);
    }

    public int getMonthRate() {
        return monthScheduled == 0 ? 0 : Math.round(monthDone * 100f / monthScheduled);
    }
}
