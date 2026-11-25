package com.example.lifemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Map;

/**
 * Lưu các cài đặt của ứng dụng bằng SharedPreferences.
 * Chỉ lưu cài đặt giao diện / thông báo, không lưu thông tin nhạy cảm.
 * Mỗi tài khoản có tệp cài đặt riêng {@code life_manager_settings_u<id>}.
 */
public final class PrefsManager {

    /** Tệp cài đặt của phiên bản chưa có tài khoản (và khi chưa đăng nhập). */
    private static final String PREFS_NAME = "life_manager_settings";

    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_REMINDER_MINUTES = "daily_reminder_minutes";
    private static final String KEY_CURRENCY_STYLE = "currency_style";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LAST_NOTE_ID = "last_note_id";
    private static final String KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked";

    public static final int DEFAULT_REMINDER_MINUTES = 8 * 60; // 08:00
    public static final String DEFAULT_USER_NAME = "Người dùng";

    private static volatile PrefsManager instance;
    private final SharedPreferences prefs;
    private final long ownerId;

    private PrefsManager(@NonNull Context context, long userId) {
        ownerId = userId;
        prefs = context.getApplicationContext().getSharedPreferences(prefsNameFor(userId), Context.MODE_PRIVATE);
    }

    /** Cài đặt của tài khoản đang đăng nhập. */
    @NonNull
    public static PrefsManager getInstance(@NonNull Context context) {
        long userId = SessionManager.getInstance(context).getCurrentUserId();
        PrefsManager current = instance;
        if (current == null || current.ownerId != userId) {
            synchronized (PrefsManager.class) {
                current = instance;
                if (current == null || current.ownerId != userId) {
                    current = new PrefsManager(context, userId);
                    instance = current;
                }
            }
        }
        return current;
    }

    @NonNull
    private static String prefsNameFor(long userId) {
        return userId > 0 ? PREFS_NAME + "_u" + userId : PREFS_NAME;
    }

    /**
     * Chuyển cài đặt của phiên bản chưa có tài khoản sang tài khoản đầu tiên
     * (đi kèm việc chuyển CSDL, xem AppDatabase.adoptLegacyDatabase).
     */
    public static void adoptLegacyPrefs(@NonNull Context context, long userId) {
        Context app = context.getApplicationContext();
        SharedPreferences legacy = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor target = app.getSharedPreferences(prefsNameFor(userId), Context.MODE_PRIVATE).edit();
        for (Map.Entry<String, ?> entry : legacy.getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                target.putBoolean(entry.getKey(), (Boolean) value);
            } else if (value instanceof Integer) {
                target.putInt(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                target.putLong(entry.getKey(), (Long) value);
            } else if (value instanceof String) {
                target.putString(entry.getKey(), (String) value);
            }
        }
        target.apply();
        legacy.edit().clear().apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    /** Giờ nhắc nhở hằng ngày tính bằng số phút trong ngày. */
    public int getDailyReminderMinutes() {
        return prefs.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES);
    }

    public void setDailyReminderMinutes(int minutes) {
        prefs.edit().putInt(KEY_REMINDER_MINUTES, minutes).apply();
    }

    public int getCurrencyStyle() {
        return prefs.getInt(KEY_CURRENCY_STYLE, CurrencyUtils.STYLE_SYMBOL);
    }

    public void setCurrencyStyle(int style) {
        prefs.edit().putInt(KEY_CURRENCY_STYLE, style).apply();
        CurrencyUtils.setStyle(style);
    }

    @NonNull
    public String getUserName() {
        String name = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME);
        return name == null || name.trim().isEmpty() ? DEFAULT_USER_NAME : name;
    }

    public void setUserName(@NonNull String name) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply();
    }

    /** Chữ cái đầu của tên người dùng để hiển thị avatar. */
    @NonNull
    public String getUserInitial() {
        String name = getUserName().trim();
        return name.isEmpty() ? "N" : name.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    public long getLastNoteId() {
        return prefs.getLong(KEY_LAST_NOTE_ID, 0L);
    }

    public void setLastNoteId(long id) {
        prefs.edit().putLong(KEY_LAST_NOTE_ID, id).apply();
    }

    public boolean isNotificationPermissionAsked() {
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false);
    }

    public void setNotificationPermissionAsked(boolean asked) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, asked).apply();
    }
}
