package com.example.lifemanager.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.lifemanager.R;
import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhận báo thức từ AlarmManager và hiển thị thông báo tương ứng.
 * Truy vấn database ở background thread (goAsync).
 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (!SessionManager.getInstance(context).isLoggedIn()) {
            // Đã đăng xuất: không hiển thị nhắc nhở của tài khoản nào
            return;
        }
        final PendingResult pendingResult = goAsync();
        final Context app = context.getApplicationContext();
        final Intent data = new Intent(intent);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                handle(app, data);
            } finally {
                pendingResult.finish();
            }
        });
    }

    @WorkerThread
    private void handle(@NonNull Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        if (ReminderScheduler.ACTION_DAILY.equals(action)) {
            // Lập lịch cho ngày hôm sau trước, rồi mới hiển thị thông báo hôm nay
            ReminderScheduler.scheduleDaily(context);
            showDailySummary(context);
        } else if (ReminderScheduler.ACTION_TASK.equals(action)) {
            showTaskReminder(context, intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1));
        } else if (ReminderScheduler.ACTION_HABIT.equals(action)) {
            showHabitReminder(context, intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, -1));
        }
    }

    @WorkerThread
    private void showDailySummary(@NonNull Context context) {
        if (!NotificationHelper.canNotify(context)) {
            return;
        }
        AppDatabase db = AppDatabase.getInstance(context);
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        // 1. Công việc quá hạn / đến hạn hôm nay / ngày mai
        List<Task> tasks = db.taskDao().getUnfinishedDueUntilSync(todayEpoch + 1);
        int overdue = 0;
        int dueToday = 0;
        int dueTomorrow = 0;
        StringBuilder names = new StringBuilder();
        for (Task task : tasks) {
            Long due = task.getDueDate();
            if (due == null) {
                continue;
            }
            if (due < todayEpoch) {
                overdue++;
            } else if (due == todayEpoch) {
                dueToday++;
            } else {
                dueTomorrow++;
            }
            if (names.length() < 300) {
                names.append("\n• ").append(task.getTitle()).append(" (")
                        .append(DateUtils.describeDue(due, todayEpoch)).append(")");
            }
        }
        if (!tasks.isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (overdue > 0) {
                parts.add(context.getString(R.string.notif_overdue_count, overdue));
            }
            if (dueToday > 0) {
                parts.add(context.getString(R.string.notif_due_today_count, dueToday));
            }
            if (dueTomorrow > 0) {
                parts.add(context.getString(R.string.notif_due_tomorrow_count, dueTomorrow));
            }
            String text = String.join(" · ", parts) + names;
            NotificationHelper.show(context, NotificationHelper.CHANNEL_TASKS,
                    NotificationHelper.ID_DAILY_TASKS,
                    context.getString(R.string.notif_daily_tasks_title), text,
                    com.example.lifemanager.R.id.nav_tasks);
        }

        // 2. Thói quen chưa hoàn thành hôm nay
        List<String> pendingHabits = new ArrayList<>();
        for (Habit habit : db.habitDao().getAllSync()) {
            if (habit.isScheduledOn(today)
                    && db.habitLogDao().countForDay(habit.getId(), todayEpoch) == 0) {
                pendingHabits.add(habit.getName());
            }
        }
        if (!pendingHabits.isEmpty()) {
            String text = context.getString(R.string.notif_habits_text, pendingHabits.size())
                    + "\n• " + String.join("\n• ", pendingHabits);
            NotificationHelper.show(context, NotificationHelper.CHANNEL_HABITS,
                    NotificationHelper.ID_DAILY_HABITS,
                    context.getString(R.string.notif_habits_title), text, R.id.nav_home);
        }
    }

    @WorkerThread
    private void showTaskReminder(@NonNull Context context, long taskId) {
        if (taskId <= 0) {
            return;
        }
        Task task = AppDatabase.getInstance(context).taskDao().getByIdSync(taskId);
        if (task == null || task.isDone()) {
            return;
        }
        String text = task.getTitle();
        if (task.getDueDate() != null) {
            text += " - " + context.getString(R.string.task_due_label,
                    DateUtils.formatDate(task.getDueDate()))
                    + " (" + DateUtils.describeDue(task.getDueDate(), DateUtils.todayEpochDay()) + ")";
        }
        NotificationHelper.show(context, NotificationHelper.CHANNEL_TASKS,
                (int) (NotificationHelper.ID_TASK_BASE + (taskId % 10_000)),
                context.getString(R.string.notif_task_title), text, R.id.nav_tasks);
    }

    @WorkerThread
    private void showHabitReminder(@NonNull Context context, long habitId) {
        if (habitId <= 0) {
            return;
        }
        AppDatabase db = AppDatabase.getInstance(context);
        Habit habit = db.habitDao().getByIdSync(habitId);
        if (habit == null) {
            return;
        }
        // Lập lịch nhắc cho ngày hôm sau
        ReminderScheduler.scheduleHabit(context, habit);
        LocalDate today = LocalDate.now();
        if (!habit.isScheduledOn(today)
                || db.habitLogDao().countForDay(habitId, today.toEpochDay()) > 0) {
            return;
        }
        NotificationHelper.show(context, NotificationHelper.CHANNEL_HABITS,
                (int) (NotificationHelper.ID_HABIT_BASE + (habitId % 10_000)),
                context.getString(R.string.notif_habit_title, habit.getName()),
                context.getString(R.string.notif_habit_text), R.id.nav_home);
    }
}
