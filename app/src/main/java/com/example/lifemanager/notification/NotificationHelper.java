package com.example.lifemanager.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.lifemanager.MainActivity;
import com.example.lifemanager.R;
import com.example.lifemanager.utils.PrefsManager;

/**
 * Tạo kênh thông báo và hiển thị thông báo nhắc nhở.
 */
public final class NotificationHelper {

    public static final String CHANNEL_TASKS = "task_reminders";
    public static final String CHANNEL_HABITS = "habit_reminders";

    public static final int ID_DAILY_TASKS = 1001;
    public static final int ID_DAILY_HABITS = 1002;
    public static final int ID_TASK_BASE = 20_000;
    public static final int ID_HABIT_BASE = 40_000;

    private NotificationHelper() {
    }

    /** Tạo kênh thông báo (minSdk 26 nên luôn hỗ trợ NotificationChannel). */
    public static void createChannels(@NonNull Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel tasks = new NotificationChannel(CHANNEL_TASKS,
                context.getString(R.string.channel_tasks_name), NotificationManager.IMPORTANCE_HIGH);
        tasks.setDescription(context.getString(R.string.channel_tasks_desc));

        NotificationChannel habits = new NotificationChannel(CHANNEL_HABITS,
                context.getString(R.string.channel_habits_name), NotificationManager.IMPORTANCE_DEFAULT);
        habits.setDescription(context.getString(R.string.channel_habits_desc));

        manager.createNotificationChannel(tasks);
        manager.createNotificationChannel(habits);
    }

    /** Kiểm tra quyền thông báo (Android 13+). */
    public static boolean hasPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Được phép hiển thị thông báo: người dùng bật trong Cài đặt + hệ thống cho phép. */
    public static boolean canNotify(@NonNull Context context) {
        return PrefsManager.getInstance(context).isNotificationsEnabled()
                && hasPermission(context)
                && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Hiển thị thông báo.
     *
     * @param openTab id mục BottomNavigation sẽ mở khi chạm vào thông báo.
     */
    public static void show(@NonNull Context context, @NonNull String channelId, int notificationId,
                            @NonNull String title, @NonNull String text, int openTab) {
        if (!canNotify(context)) {
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, openTab);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_task_alt)
                .setColor(ContextCompat.getColor(context, R.color.lm_primary))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(CHANNEL_TASKS.equals(channelId)
                        ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Người dùng đã thu hồi quyền thông báo
        }
    }
}
