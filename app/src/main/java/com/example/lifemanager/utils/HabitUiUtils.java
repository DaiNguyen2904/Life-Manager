package com.example.lifemanager.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Habit;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * Chuỗi mô tả hiển thị cho thói quen.
 */
public final class HabitUiUtils {

    private HabitUiUtils() {
    }

    /** Mô tả tần suất: Hằng ngày / Thứ 2 - Thứ 6 / Cuối tuần / T2, T4, T6. */
    @NonNull
    public static String describeFrequency(@NonNull Context context, int mask) {
        int normalized = mask & Habit.MASK_DAILY;
        if (normalized == Habit.MASK_DAILY) {
            return context.getString(R.string.frequency_daily);
        }
        if (normalized == Habit.MASK_WEEKDAYS) {
            return context.getString(R.string.frequency_weekdays_long);
        }
        if (normalized == Habit.MASK_WEEKENDS) {
            return context.getString(R.string.frequency_weekends);
        }
        List<String> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if ((normalized & (1 << (day.getValue() - 1))) != 0) {
                days.add(DateUtils.shortDayOfWeek(day));
            }
        }
        return days.isEmpty() ? context.getString(R.string.frequency_none) : String.join(", ", days);
    }

    /** "Mục tiêu: 30 phút". */
    @NonNull
    public static String describeTarget(@NonNull Context context, @NonNull Habit habit) {
        String unit = habit.getTargetUnit();
        if (unit == null || unit.trim().isEmpty()) {
            unit = context.getString(R.string.habit_default_unit);
        }
        return context.getString(R.string.habit_target_format, habit.getDailyTarget(), unit.trim());
    }

    /** "Mục tiêu: 30 phút · Hằng ngày · Nhắc lúc 07:00". */
    @NonNull
    public static String describeDetail(@NonNull Context context, @NonNull Habit habit) {
        StringBuilder builder = new StringBuilder(describeTarget(context, habit))
                .append(" · ")
                .append(describeFrequency(context, habit.getFrequencyMask()));
        Integer minutes = habit.getReminderMinutes();
        if (minutes != null) {
            builder.append(" · ").append(context.getString(R.string.habit_reminder_at,
                    DateUtils.formatMinutesOfDay(minutes)));
        }
        return builder.toString();
    }
}
