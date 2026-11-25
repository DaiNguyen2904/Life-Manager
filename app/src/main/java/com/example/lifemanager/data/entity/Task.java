package com.example.lifemanager.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng tasks: công việc cá nhân theo mô hình Kanban.
 * <p>
 * Trạng thái lưu trong DB: TODO, IN_PROGRESS, DONE.
 * Trạng thái OVERDUE (quá hạn) không lưu mà được tính tự động:
 * hạn hoàn thành nhỏ hơn ngày hiện tại và công việc chưa hoàn thành.
 */
@Entity(tableName = "tasks",
        indices = {@Index("status"), @Index("due_date")})
public class Task {

    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE = "DONE";
    /** Trạng thái hiển thị, không lưu xuống database. */
    public static final String STATUS_OVERDUE = "OVERDUE";

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    /** Ngày bắt đầu (epochDay), có thể null. */
    @Nullable
    @ColumnInfo(name = "start_date")
    private Long startDate;

    /** Hạn hoàn thành (epochDay), có thể null. */
    @Nullable
    @ColumnInfo(name = "due_date")
    private Long dueDate;

    @ColumnInfo(name = "priority")
    private int priority = PRIORITY_MEDIUM;

    @NonNull
    @ColumnInfo(name = "status")
    private String status = STATUS_TODO;

    @Nullable
    @ColumnInfo(name = "note")
    private String note;

    /** Thời điểm nhắc nhở (epoch millis), có thể null. */
    @Nullable
    @ColumnInfo(name = "reminder_at")
    private Long reminderAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    /** Thời điểm hoàn thành (epoch millis), null nếu chưa hoàn thành. */
    @Nullable
    @ColumnInfo(name = "completed_at")
    private Long completedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable Long startDate) {
        this.startDate = startDate;
    }

    @Nullable
    public Long getDueDate() {
        return dueDate;
    }

    public void setDueDate(@Nullable Long dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    @Nullable
    public Long getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(@Nullable Long reminderAt) {
        this.reminderAt = reminderAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(@Nullable Long completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isDone() {
        return STATUS_DONE.equals(status);
    }

    /** Công việc quá hạn: chưa hoàn thành và hạn hoàn thành nhỏ hơn hôm nay. */
    public boolean isOverdue(long todayEpochDay) {
        return !isDone() && dueDate != null && dueDate < todayEpochDay;
    }

    /** Trạng thái hiển thị trên bảng Kanban (có tính cột Quá hạn). */
    @NonNull
    public String getEffectiveStatus(long todayEpochDay) {
        if (isDone()) {
            return STATUS_DONE;
        }
        if (isOverdue(todayEpochDay)) {
            return STATUS_OVERDUE;
        }
        return STATUS_IN_PROGRESS.equals(status) ? STATUS_IN_PROGRESS : STATUS_TODO;
    }

    /** Tạo bản sao để chỉnh sửa mà không ảnh hưởng đối tượng đang hiển thị trong Adapter. */
    @NonNull
    public Task copy() {
        Task t = new Task();
        t.id = id;
        t.title = title;
        t.description = description;
        t.startDate = startDate;
        t.dueDate = dueDate;
        t.priority = priority;
        t.status = status;
        t.note = note;
        t.reminderAt = reminderAt;
        t.createdAt = createdAt;
        t.updatedAt = updatedAt;
        t.completedAt = completedAt;
        return t;
    }
}
