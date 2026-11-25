package com.example.lifemanager.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.utils.PrefsManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Lập lịch nhắc nhở bằng AlarmManager:
 * - Nhắc hằng ngày (công việc sắp đến hạn / quá hạn, thói quen chưa hoàn thành).
 * - Nhắc theo thời điểm đã thiết lập của từng công việc.
 * - Nhắc theo giờ đã thiết lập của từng thói quen.
 */
public final class ReminderScheduler {

    public static final String ACTION_DAILY = "com.example.lifemanager.action.DAILY_REMINDER";
    public static final String ACTION_TASK = "com.example.lifemanager.action.TASK_REMINDER";
    public static final String ACTION_HABIT = "com.example.lifemanager.action.HABIT_REMINDER";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_HABIT_ID = "extra_habit_id";

    private ReminderScheduler() {
    }

    // ---------------------------------------------------------------- Nhắc hằng ngày

    public static void scheduleDaily(@NonNull Context context) {
        PrefsManager prefs = PrefsManager.getInstance(context);
        if (!prefs.isNotificationsEnabled()) {
            cancelDaily(context);
            return;
        }
        int minutes = prefs.getDailyReminderMinutes();
        long triggerAt = nextOccurrence(minutes);
        setAlarm(context, triggerAt, dailyIntent(context));
    }

    public static void cancelDaily(@NonNull Context context) {
        cancel(context, dailyIntent(context));
    }

    // ---------------------------------------------------------------- Nhắc công việc

    public static void scheduleTask(@NonNull Context context, @NonNull Task task) {
        Long reminderAt = task.getReminderAt();
        if (task.isDone() || reminderAt == null || reminderAt <= System.currentTimeMillis()
                || !PrefsManager.getInstance(context).isNotificationsEnabled()) {
            cancelTask(context, task.getId());
            return;
        }
        setAlarm(context, reminderAt, taskIntent(context, task.getId()));
    }

    public static void cancelTask(@NonNull Context context, long taskId) {
        cancel(context, taskIntent(context, taskId));
    }

    // ---------------------------------------------------------------- Nhắc thói quen

    public static void scheduleHabit(@NonNull Context context, @NonNull Habit habit) {
        Integer minutes = habit.getReminderMinutes();
        if (minutes == null || !PrefsManager.getInstance(context).isNotificationsEnabled()) {
            cancelHabit(context, habit.getId());
            return;
        }
        setAlarm(context, nextOccurrence(minutes), habitIntent(context, habit.getId()));
    }

    public static void cancelHabit(@NonNull Context context, long habitId) {
        cancel(context, habitIntent(context, habitId));
    }

    // ---------------------------------------------------------------- Toàn bộ

    /** Lập lại toàn bộ lịch nhắc (sau khi khởi động máy, bật lại thông báo...). Gọi ở background. */
    @WorkerThread
    public static void rescheduleAll(@NonNull Context context) {
        Context app = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(app);
        boolean enabled = PrefsManager.getInstance(app).isNotificationsEnabled();
        if (!enabled) {
            cancelAll(app);
            return;
        }
        scheduleDaily(app);
        List<Task> tasks = db.taskDao().getUpcomingRemindersSync(System.currentTimeMillis());
        for (Task task : tasks) {
            scheduleTask(app, task);
        }
        for (Habit habit : db.habitDao().getAllSync()) {
            scheduleHabit(app, habit);
        }
    }

    /** Hủy toàn bộ lịch nhắc. Gọi ở background. */
    @WorkerThread
    public static void cancelAll(@NonNull Context context) {
        Context app = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(app);
        cancelDaily(app);
        for (Task task : db.taskDao().getAllWithReminderSync()) {
            cancelTask(app, task.getId());
        }
        for (Habit habit : db.habitDao().getAllSync()) {
            cancelHabit(app, habit.getId());
        }
    }

    // ---------------------------------------------------------------- Nội bộ

    /** Thời điểm tiếp theo (millis) ứng với giờ:phút trong ngày. */
    static long nextOccurrence(int minutesOfDay) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = LocalDate.now().atTime(LocalTime.of(minutesOfDay / 60, minutesOfDay % 60));
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @NonNull
    private static PendingIntent dailyIntent(@NonNull Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_DAILY);
        intent.setData(Uri.parse("lifemanager://reminder/daily"));
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @NonNull
    private static PendingIntent taskIntent(@NonNull Context context, long taskId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_TASK);
        // Data URI riêng cho từng công việc để PendingIntent không bị trùng
        intent.setData(Uri.parse("lifemanager://reminder/task/" + taskId));
        intent.putExtra(EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @NonNull
    private static PendingIntent habitIntent(@NonNull Context context, long habitId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_HABIT);
        intent.setData(Uri.parse("lifemanager://reminder/habit/" + habitId));
        intent.putExtra(EXTRA_HABIT_ID, habitId);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Ứng dụng có quyền đặt báo thức chính xác hay không (Android 12+ cần người dùng cấp). */
    public static boolean canScheduleExact(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private static void setAlarm(@NonNull Context context, long triggerAt, @NonNull PendingIntent pi) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        try {
            if (canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                // Không có quyền báo thức chính xác: dùng báo thức gần đúng
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private static void cancel(@NonNull Context context, @NonNull PendingIntent pi) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(pi);
        }
        pi.cancel();
    }
}
