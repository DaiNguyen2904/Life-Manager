package com.example.lifemanager;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.lifemanager.data.repository.SettingsRepository;
import com.example.lifemanager.notification.NotificationHelper;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.PrefsManager;
import com.example.lifemanager.utils.SessionManager;

/**
 * Lớp Application: khởi tạo cài đặt, kênh thông báo và lịch nhắc nhở khi ứng dụng khởi động.
 */
public class LifeManagerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Giao diện thiết kế theo nền sáng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        NotificationHelper.createChannels(this);

        // Cài đặt và lịch nhắc thuộc về từng tài khoản: chỉ áp dụng khi đã có tài khoản đăng nhập
        if (SessionManager.getInstance(this).isLoggedIn()) {
            CurrencyUtils.setStyle(PrefsManager.getInstance(this).getCurrencyStyle());
            // Đảm bảo lịch nhắc luôn được thiết lập (chạy ở background thread)
            SettingsRepository.getInstance(this).refreshReminders();
        }
    }
}
