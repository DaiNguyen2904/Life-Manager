package com.example.lifemanager.data.model;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Bộ lọc danh sách giao dịch: từ khóa, loại, danh mục, khoảng thời gian.
 * Đối tượng bất biến, mỗi lần thay đổi tạo bản mới.
 */
public final class TransactionFilter {

    public enum PeriodMode {ALL, TODAY, THIS_MONTH, DAY, MONTH}

    public static final long NO_CATEGORY_FILTER = -1L;

    @NonNull
    public final String query;
    /** "" = tất cả, INCOME hoặc EXPENSE. */
    @NonNull
    public final String type;
    public final long categoryId;
    @NonNull
    public final PeriodMode periodMode;
    /** Ngày được chọn khi periodMode = DAY. */
    public final long selectedDay;
    /** Tháng được chọn khi periodMode = MONTH. */
    @NonNull
    public final YearMonth selectedMonth;

    private TransactionFilter(@NonNull String query, @NonNull String type, long categoryId,
                              @NonNull PeriodMode periodMode, long selectedDay,
                              @NonNull YearMonth selectedMonth) {
        this.query = query;
        this.type = type;
        this.categoryId = categoryId;
        this.periodMode = periodMode;
        this.selectedDay = selectedDay;
        this.selectedMonth = selectedMonth;
    }

    @NonNull
    public static TransactionFilter defaultFilter() {
        return new TransactionFilter("", "", NO_CATEGORY_FILTER, PeriodMode.ALL,
                LocalDate.now().toEpochDay(), YearMonth.now());
    }

    @NonNull
    public TransactionFilter withQuery(@NonNull String newQuery) {
        return new TransactionFilter(newQuery.trim(), type, categoryId, periodMode, selectedDay, selectedMonth);
    }

    @NonNull
    public TransactionFilter withType(@NonNull String newType) {
        return new TransactionFilter(query, newType, categoryId, periodMode, selectedDay, selectedMonth);
    }

    @NonNull
    public TransactionFilter withCategory(long newCategoryId) {
        return new TransactionFilter(query, type, newCategoryId, periodMode, selectedDay, selectedMonth);
    }

    @NonNull
    public TransactionFilter withPeriod(@NonNull PeriodMode mode) {
        return new TransactionFilter(query, type, categoryId, mode, selectedDay, selectedMonth);
    }

    @NonNull
    public TransactionFilter withDay(long epochDay) {
        return new TransactionFilter(query, type, categoryId, PeriodMode.DAY, epochDay, selectedMonth);
    }

    @NonNull
    public TransactionFilter withMonth(@NonNull YearMonth month) {
        return new TransactionFilter(query, type, categoryId, PeriodMode.MONTH, selectedDay, month);
    }

    /** Ngày bắt đầu (epochDay) của khoảng lọc. */
    public long getFromDay() {
        switch (periodMode) {
            case TODAY:
                return LocalDate.now().toEpochDay();
            case THIS_MONTH:
                return YearMonth.now().atDay(1).toEpochDay();
            case DAY:
                return selectedDay;
            case MONTH:
                return selectedMonth.atDay(1).toEpochDay();
            case ALL:
            default:
                return Long.MIN_VALUE;
        }
    }

    /** Ngày kết thúc (epochDay) của khoảng lọc. */
    public long getToDay() {
        switch (periodMode) {
            case TODAY:
                return LocalDate.now().toEpochDay();
            case THIS_MONTH:
                return YearMonth.now().atEndOfMonth().toEpochDay();
            case DAY:
                return selectedDay;
            case MONTH:
                return selectedMonth.atEndOfMonth().toEpochDay();
            case ALL:
            default:
                return Long.MAX_VALUE;
        }
    }

    public boolean isDefault() {
        return query.isEmpty() && type.isEmpty() && categoryId == NO_CATEGORY_FILTER
                && periodMode == PeriodMode.ALL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionFilter)) return false;
        TransactionFilter that = (TransactionFilter) o;
        return categoryId == that.categoryId
                && selectedDay == that.selectedDay
                && query.equals(that.query)
                && type.equals(that.type)
                && periodMode == that.periodMode
                && selectedMonth.equals(that.selectedMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, type, categoryId, periodMode, selectedDay, selectedMonth);
    }
}
