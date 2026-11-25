package com.example.lifemanager.utils;

import androidx.annotation.NonNull;

import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.HabitLog;
import com.example.lifemanager.data.model.DailyHabitRate;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.HabitWithStats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tính toán các chỉ số thói quen từ lịch sử hoàn thành lưu trong database.
 * <p>
 * Quy tắc Streak:
 * - Chỉ xét các ngày có lịch thực hiện (theo tần suất và ngày bắt đầu).
 * - Hôm nay chưa hoàn thành thì CHƯA làm đứt chuỗi (người dùng vẫn còn thời gian trong ngày).
 * - Bỏ lỡ một ngày có lịch trong quá khứ thì chuỗi bị ngắt.
 */
public final class HabitStatsCalculator {

    private HabitStatsCalculator() {
    }

    /** Gom lịch sử hoàn thành theo từng thói quen: habitId -> tập epochDay. */
    @NonNull
    public static Map<Long, Set<Long>> groupLogs(@NonNull List<HabitLog> logs) {
        Map<Long, Set<Long>> map = new HashMap<>();
        for (HabitLog log : logs) {
            Set<Long> days = map.get(log.getHabitId());
            if (days == null) {
                days = new HashSet<>();
                map.put(log.getHabitId(), days);
            }
            days.add(log.getDate());
        }
        return map;
    }

    @NonNull
    public static HabitSnapshot buildSnapshot(@NonNull List<Habit> habits, @NonNull List<HabitLog> logs,
                                              @NonNull LocalDate today) {
        Map<Long, Set<Long>> logsByHabit = groupLogs(logs);
        List<HabitWithStats> stats = new ArrayList<>(habits.size());
        for (Habit habit : habits) {
            Set<Long> days = logsByHabit.get(habit.getId());
            stats.add(computeStats(habit, days == null ? Collections.emptySet() : days, today));
        }
        return new HabitSnapshot(new ArrayList<>(habits), logsByHabit, stats, today.toEpochDay());
    }

    @NonNull
    public static HabitWithStats computeStats(@NonNull Habit habit, @NonNull Set<Long> doneDays,
                                              @NonNull LocalDate today) {
        HabitWithStats stats = new HabitWithStats(habit);
        long todayEpoch = today.toEpochDay();
        stats.scheduledToday = habit.isScheduledOn(today);
        stats.doneToday = doneDays.contains(todayEpoch);
        stats.currentStreak = currentStreak(habit, doneDays, today);
        stats.longestStreak = Math.max(longestStreak(habit, doneDays, today), stats.currentStreak);
        stats.totalCompletions = doneDays.size();

        // Tỷ lệ tuần: từ Thứ Hai tuần này đến hôm nay
        int[] week = countInRange(habit, doneDays, DateUtils.startOfWeek(today), today);
        stats.weekDone = week[0];
        stats.weekScheduled = week[1];

        // Tỷ lệ tháng: từ ngày 1 của tháng đến hôm nay
        int[] month = countInRange(habit, doneDays, today.withDayOfMonth(1), today);
        stats.monthDone = month[0];
        stats.monthScheduled = month[1];
        return stats;
    }

    /** Chuỗi ngày liên tiếp hiện tại. */
    public static int currentStreak(@NonNull Habit habit, @NonNull Set<Long> doneDays,
                                    @NonNull LocalDate today) {
        int streak = 0;
        if (habit.isScheduledOn(today) && doneDays.contains(today.toEpochDay())) {
            streak++;
        }
        LocalDate day = today.minusDays(1);
        while (day.toEpochDay() >= habit.getStartDate()) {
            if (habit.isScheduledOn(day)) {
                if (doneDays.contains(day.toEpochDay())) {
                    streak++;
                } else {
                    break;
                }
            }
            day = day.minusDays(1);
        }
        return streak;
    }

    /** Kỷ lục chuỗi dài nhất từ ngày bắt đầu đến hôm nay. */
    public static int longestStreak(@NonNull Habit habit, @NonNull Set<Long> doneDays,
                                    @NonNull LocalDate today) {
        int best = 0;
        int run = 0;
        long todayEpoch = today.toEpochDay();
        for (long d = habit.getStartDate(); d <= todayEpoch; d++) {
            LocalDate day = LocalDate.ofEpochDay(d);
            if (!habit.isScheduledOn(day)) {
                continue;
            }
            if (doneDays.contains(d)) {
                run++;
                best = Math.max(best, run);
            } else if (d != todayEpoch) {
                run = 0;
            }
        }
        return best;
    }

    /**
     * Đếm số ngày hoàn thành / số ngày có lịch trong khoảng [from, to].
     *
     * @return mảng {done, scheduled}
     */
    @NonNull
    public static int[] countInRange(@NonNull Habit habit, @NonNull Set<Long> doneDays,
                                     @NonNull LocalDate from, @NonNull LocalDate to) {
        int done = 0;
        int scheduled = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (habit.isScheduledOn(day)) {
                scheduled++;
                if (doneDays.contains(day.toEpochDay())) {
                    done++;
                }
            }
        }
        return new int[]{done, scheduled};
    }

    /** Tỷ lệ hoàn thành của tất cả thói quen theo từng ngày trong khoảng [from, to]. */
    @NonNull
    public static List<DailyHabitRate> dailyRates(@NonNull HabitSnapshot snapshot,
                                                  @NonNull LocalDate from, @NonNull LocalDate to) {
        List<DailyHabitRate> result = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int done = 0;
            int scheduled = 0;
            for (Habit habit : snapshot.habits) {
                if (habit.isScheduledOn(day)) {
                    scheduled++;
                    Set<Long> days = snapshot.logsByHabit.get(habit.getId());
                    if (days != null && days.contains(day.toEpochDay())) {
                        done++;
                    }
                }
            }
            result.add(new DailyHabitRate(day.toEpochDay(), done, scheduled));
        }
        return result;
    }

    /** Tỷ lệ tổng hợp (0..100) của danh sách ngày. */
    public static int aggregateRate(@NonNull List<DailyHabitRate> rates) {
        int done = 0;
        int scheduled = 0;
        for (DailyHabitRate rate : rates) {
            done += rate.done;
            scheduled += rate.scheduled;
        }
        return scheduled == 0 ? 0 : Math.round(done * 100f / scheduled);
    }
}
