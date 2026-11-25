package com.example.lifemanager.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Bảng habits: thói quen hằng ngày.
 * <p>
 * Tần suất thực hiện lưu dạng bitmask 7 bit: bit 0 = Thứ Hai ... bit 6 = Chủ Nhật.
 * Ví dụ: 127 = hằng ngày, 31 = Thứ Hai - Thứ Sáu, 96 = cuối tuần.
 */
@Entity(tableName = "habits",
        indices = {@Index(value = "name", unique = true)})
public class Habit {

    public static final int MASK_DAILY = 0b1111111;
    public static final int MASK_WEEKDAYS = 0b0011111;
    public static final int MASK_WEEKENDS = 0b1100000;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    /** Mục tiêu hằng ngày, ví dụ 30 (phút), 20 (trang). */
    @ColumnInfo(name = "daily_target")
    private int dailyTarget = 1;

    /** Đơn vị mục tiêu, ví dụ "phút", "trang", "lít". */
    @Nullable
    @ColumnInfo(name = "target_unit")
    private String targetUnit;

    /** Giờ nhắc nhở tính bằng số phút trong ngày (0..1439), null nếu không nhắc. */
    @Nullable
    @ColumnInfo(name = "reminder_minutes")
    private Integer reminderMinutes;

    /** Ngày bắt đầu (epochDay). */
    @ColumnInfo(name = "start_date")
    private long startDate;

    @ColumnInfo(name = "frequency_mask")
    private int frequencyMask = MASK_DAILY;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public Habit() {
    }

    @Ignore
    public Habit(@NonNull String name, @Nullable String description, int dailyTarget,
                 @Nullable String targetUnit, long startDate) {
        this.name = name;
        this.description = description;
        this.dailyTarget = dailyTarget;
        this.targetUnit = targetUnit;
        this.startDate = startDate;
        this.frequencyMask = MASK_DAILY;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public int getDailyTarget() {
        return dailyTarget;
    }

    public void setDailyTarget(int dailyTarget) {
        this.dailyTarget = dailyTarget;
    }

    @Nullable
    public String getTargetUnit() {
        return targetUnit;
    }

    public void setTargetUnit(@Nullable String targetUnit) {
        this.targetUnit = targetUnit;
    }

    @Nullable
    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(@Nullable Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public int getFrequencyMask() {
        return frequencyMask;
    }

    public void setFrequencyMask(int frequencyMask) {
        this.frequencyMask = frequencyMask;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /** Thói quen có lịch thực hiện vào ngày này hay không (theo tần suất và ngày bắt đầu). */
    public boolean isScheduledOn(@NonNull LocalDate date) {
        if (date.toEpochDay() < startDate) {
            return false;
        }
        int bit = 1 << (date.getDayOfWeek().getValue() - 1);
        return (frequencyMask & bit) != 0;
    }
}
