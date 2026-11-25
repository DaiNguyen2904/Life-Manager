package com.example.lifemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Quản lý phiên đăng nhập trên thiết bị (SharedPreferences riêng, không phụ thuộc tài khoản).
 * <p>
 * Lưu id và tên đăng nhập của tài khoản đang đăng nhập, tên đăng nhập gần nhất (để điền sẵn ở
 * màn hình Đăng nhập) và bộ đếm số lần nhập sai mật khẩu để tạm khóa đăng nhập.
 * Không lưu mật khẩu.
 */
public final class SessionManager {

    private static final String PREFS_NAME = "life_manager_session";

    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_USERNAME = "current_username";
    private static final String KEY_LAST_USERNAME = "last_username";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCK_UNTIL = "lock_until";

    /** Số lần nhập sai liên tiếp tối đa trước khi tạm khóa đăng nhập. */
    public static final int MAX_FAILED_ATTEMPTS = 5;
    /** Thời gian tạm khóa đăng nhập. */
    public static final long LOCK_DURATION_MS = 30_000L;

    private static volatile SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static SessionManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------ Phiên đăng nhập

    /** Id tài khoản đang đăng nhập, 0 nếu chưa đăng nhập. */
    public long getCurrentUserId() {
        return prefs.getLong(KEY_USER_ID, 0L);
    }

    public boolean isLoggedIn() {
        return getCurrentUserId() > 0;
    }

    @Nullable
    public String getCurrentUsername() {
        return isLoggedIn() ? prefs.getString(KEY_USERNAME, null) : null;
    }

    /** Bắt đầu phiên cho tài khoản vừa đăng nhập / đăng ký thành công. */
    public void startSession(long userId, @NonNull String username) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_LAST_USERNAME, username)
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCK_UNTIL)
                .apply();
    }

    /** Kết thúc phiên (đăng xuất). Tên đăng nhập gần nhất vẫn được giữ để điền sẵn. */
    public void endSession() {
        prefs.edit().remove(KEY_USER_ID).remove(KEY_USERNAME).apply();
    }

    @NonNull
    public String getLastUsername() {
        String name = prefs.getString(KEY_LAST_USERNAME, "");
        return name == null ? "" : name;
    }

    // ------------------------------------------------------------------ Tạm khóa khi nhập sai

    /** Số mili giây còn lại của thời gian tạm khóa, 0 nếu không bị khóa. */
    public long getLockRemainingMillis(long now) {
        long until = prefs.getLong(KEY_LOCK_UNTIL, 0L);
        return Math.max(0L, until - now);
    }

    /**
     * Ghi nhận một lần nhập sai.
     *
     * @return số lần thử còn lại; 0 nghĩa là vừa bị tạm khóa {@link #LOCK_DURATION_MS}.
     */
    public int recordFailedAttempt(long now) {
        int failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        if (failed >= MAX_FAILED_ATTEMPTS) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCK_UNTIL, now + LOCK_DURATION_MS).apply();
            return 0;
        }
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed).apply();
        return MAX_FAILED_ATTEMPTS - failed;
    }
}
