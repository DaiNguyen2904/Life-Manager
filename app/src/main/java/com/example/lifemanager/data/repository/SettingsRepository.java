package com.example.lifemanager.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.notification.ReminderScheduler;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.PrefsManager;

/**
 * Các thao tác dữ liệu trong màn hình Cài đặt: xóa dữ liệu, lập lại lịch nhắc.
 */
public class SettingsRepository {

    private static volatile SettingsRepository instance;

    private final Context appContext;
    private final AppExecutors executors;

    private SettingsRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        executors = AppExecutors.getInstance();
    }

    @NonNull
    public static SettingsRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SettingsRepository.class) {
                if (instance == null) {
                    instance = new SettingsRepository(context);
                }
            }
        }
        return instance;
    }

    /** Xóa toàn bộ dữ liệu người dùng và khởi tạo lại dữ liệu mặc định. */
    public void resetAllData(@Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            ReminderScheduler.cancelAll(appContext);
            AppDatabase.getInstance(appContext).resetAllData();
            PrefsManager.getInstance(appContext).setLastNoteId(0);
            ReminderScheduler.rescheduleAll(appContext);
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    /** Lập lại (hoặc hủy) toàn bộ lịch nhắc theo cài đặt hiện tại. */
    public void refreshReminders() {
        executors.diskIO().execute(() -> ReminderScheduler.rescheduleAll(appContext));
    }
}
