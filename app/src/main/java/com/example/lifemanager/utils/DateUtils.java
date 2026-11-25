package com.example.lifemanager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Tiện ích xử lý ngày giờ bằng Java Time API.
 * Ngày được lưu trong database dưới dạng epochDay (số ngày kể từ 01/01/1970).
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", Locale.US);
    private static final DateTimeFormatter SHORT_DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM", Locale.US);

    private DateUtils() {
    }

    public static long todayEpochDay() {
        return LocalDate.now().toEpochDay();
    }

    @NonNull
    public static LocalDate fromEpochDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay);
    }

    /** "dd/MM/yyyy" */
    @NonNull
    public static String formatDate(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DATE_FORMAT);
    }

    /** "dd/MM" */
    @NonNull
    public static String formatShortDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(SHORT_DAY_FORMAT);
    }

    /** "HH:mm, dd/MM/yyyy" từ epoch millis. */
    @NonNull
    public static String formatDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                .format(DATE_TIME_FORMAT);
    }

    /** "HH:mm" từ số phút trong ngày. */
    @NonNull
    public static String formatMinutesOfDay(int minutes) {
        return LocalTime.of(minutes / 60, minutes % 60).format(TIME_FORMAT);
    }

    /** Tên thứ tiếng Việt: Thứ Hai ... Chủ Nhật. */
    @NonNull
    public static String vietnameseDayOfWeek(@NonNull DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "Thứ Hai";
            case TUESDAY:
                return "Thứ Ba";
            case WEDNESDAY:
                return "Thứ Tư";
            case THURSDAY:
                return "Thứ Năm";
            case FRIDAY:
                return "Thứ Sáu";
            case SATURDAY:
                return "Thứ Bảy";
            case SUNDAY:
            default:
                return "Chủ Nhật";
        }
    }

    /** Tên thứ viết tắt: T2 ... CN. */
    @NonNull
    public static String shortDayOfWeek(@NonNull DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SUNDAY ? "CN" : "T" + (dayOfWeek.getValue() + 1);
    }

    /** "Thứ Ba, 17 Tháng 11, 2026" */
    @NonNull
    public static String formatFullDate(@NonNull LocalDate date) {
        return vietnameseDayOfWeek(date.getDayOfWeek()) + ", " + date.getDayOfMonth()
                + " Tháng " + date.getMonthValue() + ", " + date.getYear();
    }

    /** "Tháng 11/2026" */
    @NonNull
    public static String formatMonth(@NonNull YearMonth month) {
        return "Tháng " + month.getMonthValue() + "/" + month.getYear();
    }

    /** Chuyển giá trị MaterialDatePicker (UTC millis) sang epochDay. */
    public static long utcMillisToEpochDay(long utcMillis) {
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay();
    }

    /** Chuyển epochDay sang UTC millis để đặt lựa chọn cho MaterialDatePicker. */
    public static long epochDayToUtcMillis(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /** Epoch millis của một ngày + giờ theo múi giờ thiết bị. */
    public static long toEpochMillis(long epochDay, int hour, int minute) {
        return LocalDate.ofEpochDay(epochDay).atTime(hour, minute)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @NonNull
    public static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    /** epochDay tương ứng với một thời điểm epoch millis. */
    public static long epochMillisToEpochDay(long epochMillis) {
        return toLocalDateTime(epochMillis).toLocalDate().toEpochDay();
    }

    /** Ngày thứ mấy trong năm. */
    public static int dayOfYear(@NonNull LocalDate date) {
        return date.getDayOfYear();
    }

    /** Tổng số ngày của năm. */
    public static int daysInYear(@NonNull LocalDate date) {
        return date.lengthOfYear();
    }

    /** Ngày đầu tuần (Thứ Hai) chứa ngày đã cho. */
    @NonNull
    public static LocalDate startOfWeek(@NonNull LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    /** Mô tả thời hạn so với hôm nay: "Hôm nay", "Ngày mai", "Còn 3 ngày", "Quá hạn 2 ngày". */
    @NonNull
    public static String describeDue(@Nullable Long dueEpochDay, long todayEpochDay) {
        if (dueEpochDay == null) {
            return "Không có hạn";
        }
        long diff = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(todayEpochDay),
                LocalDate.ofEpochDay(dueEpochDay));
        if (diff == 0) {
            return "Hôm nay";
        } else if (diff == 1) {
            return "Ngày mai";
        } else if (diff > 1) {
            return "Còn " + diff + " ngày";
        } else {
            return "Quá hạn " + (-diff) + " ngày";
        }
    }
}
