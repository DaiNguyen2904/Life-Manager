package com.example.lifemanager.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.SessionManager;

/**
 * Lập lại lịch nhắc nhở sau khi khởi động lại thiết bị, cập nhật ứng dụng
 * hoặc thay đổi giờ hệ thống (AlarmManager bị xóa trong các trường hợp này).
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }
        if (!SessionManager.getInstance(context).isLoggedIn()) {
            // Chưa đăng nhập: lịch nhắc sẽ được lập khi người dùng đăng nhập
            return;
        }
        final PendingResult pendingResult = goAsync();
        final Context app = context.getApplicationContext();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                ReminderScheduler.rescheduleAll(app);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
