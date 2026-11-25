package com.example.lifemanager.utils;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Task;

/**
 * Nhãn và màu sắc hiển thị cho trạng thái / mức độ ưu tiên công việc.
 */
public final class TaskUiUtils {

    private TaskUiUtils() {
    }

    @NonNull
    public static String priorityLabel(@NonNull Context context, int priority) {
        switch (priority) {
            case Task.PRIORITY_HIGH:
                return context.getString(R.string.priority_high);
            case Task.PRIORITY_LOW:
                return context.getString(R.string.priority_low);
            case Task.PRIORITY_MEDIUM:
            default:
                return context.getString(R.string.priority_medium);
        }
    }

    @ColorInt
    public static int priorityColor(@NonNull Context context, int priority) {
        switch (priority) {
            case Task.PRIORITY_HIGH:
                return ContextCompat.getColor(context, R.color.priority_high);
            case Task.PRIORITY_LOW:
                return ContextCompat.getColor(context, R.color.priority_low);
            case Task.PRIORITY_MEDIUM:
            default:
                return ContextCompat.getColor(context, R.color.priority_medium);
        }
    }

    @NonNull
    public static String statusLabel(@NonNull Context context, @NonNull String status) {
        switch (status) {
            case Task.STATUS_IN_PROGRESS:
                return context.getString(R.string.status_in_progress);
            case Task.STATUS_DONE:
                return context.getString(R.string.status_done);
            case Task.STATUS_OVERDUE:
                return context.getString(R.string.status_overdue);
            case Task.STATUS_TODO:
            default:
                return context.getString(R.string.status_todo);
        }
    }

    @ColorInt
    public static int statusTextColor(@NonNull Context context, @NonNull String status) {
        switch (status) {
            case Task.STATUS_IN_PROGRESS:
                return ContextCompat.getColor(context, R.color.lm_progress_text);
            case Task.STATUS_DONE:
                return ContextCompat.getColor(context, R.color.lm_done_text);
            case Task.STATUS_OVERDUE:
                return ContextCompat.getColor(context, R.color.lm_warning);
            case Task.STATUS_TODO:
            default:
                return ContextCompat.getColor(context, R.color.lm_todo_text);
        }
    }

    @ColorInt
    public static int statusBackgroundColor(@NonNull Context context, @NonNull String status) {
        switch (status) {
            case Task.STATUS_IN_PROGRESS:
                return ContextCompat.getColor(context, R.color.lm_yellow);
            case Task.STATUS_DONE:
                return ContextCompat.getColor(context, R.color.lm_green);
            case Task.STATUS_OVERDUE:
                return ContextCompat.getColor(context, R.color.lm_orange_soft);
            case Task.STATUS_TODO:
            default:
                return ContextCompat.getColor(context, R.color.lm_red);
        }
    }

    /** Hiển thị nhãn bo tròn với màu chữ và màu nền. */
    public static void stylePill(@NonNull TextView view, @NonNull String text,
                                 @ColorInt int textColor, @ColorInt int backgroundColor) {
        view.setText(text);
        view.setTextColor(textColor);
        ViewUtils.setBackgroundTint(view, backgroundColor);
    }

    public static void stylePriority(@NonNull TextView view, int priority) {
        Context context = view.getContext();
        int color = priorityColor(context, priority);
        stylePill(view, priorityLabel(context, priority), color, ViewUtils.softColor(color, 0.82f));
    }

    public static void styleStatus(@NonNull TextView view, @NonNull String effectiveStatus) {
        Context context = view.getContext();
        stylePill(view, statusLabel(context, effectiveStatus),
                statusTextColor(context, effectiveStatus), statusBackgroundColor(context, effectiveStatus));
    }
}
