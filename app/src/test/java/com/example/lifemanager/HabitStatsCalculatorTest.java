package com.example.lifemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.utils.HabitStatsCalculator;

import org.junit.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Kiểm thử logic Streak dựa trên lịch sử hoàn thành.
 */
public class HabitStatsCalculatorTest {

    /** Thứ Ba, 17/11/2026. */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    private static Habit dailyHabit(LocalDate start) {
        Habit habit = new Habit("Đọc sách", null, 20, "trang", start.toEpochDay());
        habit.setId(1);
        return habit;
    }

    private static Set<Long> days(LocalDate... dates) {
        Set<Long> set = new HashSet<>();
        for (LocalDate d : dates) {
            set.add(d.toEpochDay());
        }
        return set;
    }

    @Test
    public void streak_countsConsecutiveDaysIncludingToday() {
        Habit habit = dailyHabit(TODAY.minusDays(10));
        Set<Long> done = days(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(3, HabitStatsCalculator.currentStreak(habit, done, TODAY));
    }

    @Test
    public void streak_notBrokenWhenTodayNotYetDone() {
        Habit habit = dailyHabit(TODAY.minusDays(10));
        Set<Long> done = days(TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(2, HabitStatsCalculator.currentStreak(habit, done, TODAY));
    }

    @Test
    public void streak_brokenByMissedDay() {
        // Ngày 1, 2, 3 hoàn thành; ngày 4 bỏ lỡ; ngày 5 (hôm nay) hoàn thành -> streak = 1
        Habit habit = dailyHabit(TODAY.minusDays(4));
        Set<Long> done = days(TODAY.minusDays(4), TODAY.minusDays(3), TODAY.minusDays(2), TODAY);
        assertEquals(1, HabitStatsCalculator.currentStreak(habit, done, TODAY));
        assertEquals(3, HabitStatsCalculator.longestStreak(habit, done, TODAY));
    }

    @Test
    public void streak_zeroWhenNoHistory() {
        Habit habit = dailyHabit(TODAY.minusDays(5));
        assertEquals(0, HabitStatsCalculator.currentStreak(habit, new HashSet<>(), TODAY));
        assertEquals(0, HabitStatsCalculator.longestStreak(habit, new HashSet<>(), TODAY));
    }

    @Test
    public void streak_skipsUnscheduledDays() {
        // Chỉ thực hiện Thứ Hai - Thứ Sáu. Hôm nay Thứ Ba 17/11.
        Habit habit = dailyHabit(TODAY.minusDays(14));
        habit.setFrequencyMask(Habit.MASK_WEEKDAYS);
        // T2 16/11, T6 13/11, T5 12/11 -> cuối tuần 14-15/11 không có lịch nên không làm đứt chuỗi
        Set<Long> done = days(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 12));
        assertEquals(3, HabitStatsCalculator.currentStreak(habit, done, TODAY));
    }

    @Test
    public void streak_ignoresDaysBeforeStartDate() {
        Habit habit = dailyHabit(TODAY.minusDays(1));
        Set<Long> done = days(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3));
        assertEquals(2, HabitStatsCalculator.currentStreak(habit, done, TODAY));
    }

    @Test
    public void computeStats_weekAndMonthRates() {
        Habit habit = dailyHabit(LocalDate.of(2026, 11, 1));
        // Tuần này: T2 16/11, T3 17/11 (2 ngày có lịch), hoàn thành 1 ngày
        Set<Long> done = days(LocalDate.of(2026, 11, 16));
        HabitWithStats stats = HabitStatsCalculator.computeStats(habit, done, TODAY);
        assertTrue(stats.scheduledToday);
        assertFalse(stats.doneToday);
        assertEquals(1, stats.weekDone);
        assertEquals(2, stats.weekScheduled);
        assertEquals(50, stats.getWeekRate());
        assertEquals(17, stats.monthScheduled);
        assertEquals(1, stats.monthDone);
    }

    @Test
    public void habit_notScheduledBeforeStartDate() {
        Habit habit = dailyHabit(TODAY.plusDays(2));
        assertFalse(habit.isScheduledOn(TODAY));
        assertTrue(habit.isScheduledOn(TODAY.plusDays(2)));
    }
}
