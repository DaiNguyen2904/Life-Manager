package com.example.lifemanager.data.model;

import androidx.annotation.NonNull;

import com.example.lifemanager.data.entity.Task;

import java.util.List;

/**
 * Thống kê công việc theo trạng thái hiển thị (có cột Quá hạn).
 */
public class TaskStats {

    public int total;
    public int todo;
    public int inProgress;
    public int done;
    public int overdue;
    public int highPriority;
    public int mediumPriority;
    public int lowPriority;

    @NonNull
    public static TaskStats from(@NonNull List<Task> tasks, long todayEpochDay) {
        TaskStats stats = new TaskStats();
        for (Task task : tasks) {
            stats.total++;
            switch (task.getEffectiveStatus(todayEpochDay)) {
                case Task.STATUS_DONE:
                    stats.done++;
                    break;
                case Task.STATUS_OVERDUE:
                    stats.overdue++;
                    break;
                case Task.STATUS_IN_PROGRESS:
                    stats.inProgress++;
                    break;
                default:
                    stats.todo++;
                    break;
            }
            if (task.getPriority() == Task.PRIORITY_HIGH) {
                stats.highPriority++;
            } else if (task.getPriority() == Task.PRIORITY_LOW) {
                stats.lowPriority++;
            } else {
                stats.mediumPriority++;
            }
        }
        return stats;
    }

    /** Tỷ lệ hoàn thành = (số công việc hoàn thành / tổng số công việc) x 100%. */
    public int getCompletionRate() {
        return total == 0 ? 0 : Math.round(done * 100f / total);
    }
}
