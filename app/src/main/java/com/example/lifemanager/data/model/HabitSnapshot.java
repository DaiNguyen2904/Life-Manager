package com.example.lifemanager.data.model;

import androidx.annotation.NonNull;

import com.example.lifemanager.data.entity.Habit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ảnh chụp dữ liệu thói quen tại một thời điểm: danh sách thói quen, lịch sử hoàn thành
 * và các chỉ số đã tính sẵn. Được tạo ở background thread.
 */
public class HabitSnapshot {

    @NonNull
    public final List<Habit> habits;
    /** habitId -> tập các ngày (epochDay) đã hoàn thành. */
    @NonNull
    public final Map<Long, Set<Long>> logsByHabit;
    @NonNull
    public final List<HabitWithStats> stats;
    public final long todayEpochDay;

    public HabitSnapshot(@NonNull List<Habit> habits, @NonNull Map<Long, Set<Long>> logsByHabit,
                         @NonNull List<HabitWithStats> stats, long todayEpochDay) {
        this.habits = Collections.unmodifiableList(habits);
        this.logsByHabit = Collections.unmodifiableMap(logsByHabit);
        this.stats = Collections.unmodifiableList(stats);
        this.todayEpochDay = todayEpochDay;
    }

    public int getScheduledTodayCount() {
        int count = 0;
        for (HabitWithStats s : stats) {
            if (s.scheduledToday) {
                count++;
            }
        }
        return count;
    }

    public int getDoneTodayCount() {
        int count = 0;
        for (HabitWithStats s : stats) {
            if (s.scheduledToday && s.doneToday) {
                count++;
            }
        }
        return count;
    }

    /** Tỷ lệ hoàn thành thói quen hôm nay (0..100). */
    public int getTodayRate() {
        int scheduled = getScheduledTodayCount();
        return scheduled == 0 ? 0 : Math.round(getDoneTodayCount() * 100f / scheduled);
    }
}
