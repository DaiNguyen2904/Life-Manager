package com.example.lifemanager.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationManagerCompat;

import com.example.lifemanager.R;
import com.example.lifemanager.data.dao.UserDao;
import com.example.lifemanager.data.database.AccountDatabase;
import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.UserAccount;
import com.example.lifemanager.data.model.AuthResult;
import com.example.lifemanager.notification.ReminderScheduler;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.AuthValidator;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.PasswordHasher;
import com.example.lifemanager.utils.PrefsManager;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.SessionManager;

/**
 * Repository tài khoản: đăng ký, đăng nhập, đăng xuất.
 * <p>
 * Tài khoản được lưu cục bộ trong accounts.db (ứng dụng hoạt động ngoại tuyến), mật khẩu được băm
 * PBKDF2 kèm salt. Việc băm mật khẩu và truy vấn được thực hiện ở background thread, kết quả trả về
 * luồng giao diện qua callback.
 */
public class AuthRepository {

    private static volatile AuthRepository instance;

    private final Context appContext;
    private final UserDao userDao;
    private final SessionManager session;
    private final AppExecutors executors;

    private AuthRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        userDao = AccountDatabase.getInstance(appContext).userDao();
        session = SessionManager.getInstance(appContext);
        executors = AppExecutors.getInstance();
    }

    @NonNull
    public static AuthRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(context);
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------ Đăng ký

    public void register(@Nullable String displayName, @Nullable String username, @Nullable String password,
                         @Nullable String confirm, @NonNull ResultCallback<AuthResult> callback) {
        AuthResult invalid = validateRegistration(displayName, username, password, confirm);
        if (invalid != null) {
            callback.onResult(invalid);
            return;
        }
        // Sau khi kiểm tra hợp lệ, các giá trị chắc chắn khác null
        final String name = displayName == null ? "" : displayName.trim();
        final String normalized = AuthValidator.normalizeUsername(username);
        final String secret = password == null ? "" : password;
        executors.diskIO().execute(() -> {
            AuthResult result;
            try {
                result = doRegister(name, normalized, secret);
            } catch (RuntimeException e) {
                result = AuthResult.failure(AuthResult.Field.NONE, appContext.getString(R.string.error_auth_failed));
            }
            post(callback, result);
        });
    }

    @WorkerThread
    @NonNull
    private AuthResult doRegister(@NonNull String displayName, @NonNull String username, @NonNull String password) {
        if (userDao.getByUsernameSync(username) != null) {
            return usernameTaken(username);
        }
        boolean firstAccount = userDao.countSync() == 0;
        long now = System.currentTimeMillis();
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordSalt(PasswordHasher.newSalt());
        user.setHashIterations(PasswordHasher.DEFAULT_ITERATIONS);
        user.setPasswordHash(PasswordHasher.hash(password, user.getPasswordSalt(), user.getHashIterations()));
        user.setCreatedAt(now);
        user.setLastLoginAt(now);
        try {
            user.setId(userDao.insert(user));
        } catch (SQLiteConstraintException e) {
            return usernameTaken(username);
        }
        // Tài khoản đầu tiên nhận lại dữ liệu đã nhập trước khi ứng dụng có chức năng tài khoản
        boolean adopted = firstAccount && AppDatabase.adoptLegacyDatabase(appContext, user.getId());
        if (adopted) {
            PrefsManager.adoptLegacyPrefs(appContext, user.getId());
        }
        startSession(user);
        return AuthResult.success(user, adopted);
    }

    @Nullable
    private AuthResult validateRegistration(@Nullable String displayName, @Nullable String username,
                                            @Nullable String password, @Nullable String confirm) {
        AuthValidator.Error error = AuthValidator.checkDisplayName(displayName);
        if (error != AuthValidator.Error.NONE) {
            return AuthResult.failure(AuthResult.Field.DISPLAY_NAME, message(error));
        }
        error = AuthValidator.checkUsername(username);
        if (error != AuthValidator.Error.NONE) {
            return AuthResult.failure(AuthResult.Field.USERNAME, message(error));
        }
        error = AuthValidator.checkPassword(password);
        if (error != AuthValidator.Error.NONE) {
            return AuthResult.failure(AuthResult.Field.PASSWORD, message(error));
        }
        error = AuthValidator.checkConfirm(password, confirm);
        if (error != AuthValidator.Error.NONE) {
            return AuthResult.failure(AuthResult.Field.CONFIRM, message(error));
        }
        return null;
    }

    // ------------------------------------------------------------------ Đăng nhập

    public void login(@Nullable String username, @Nullable String password,
                      @NonNull ResultCallback<AuthResult> callback) {
        long lockRemaining = session.getLockRemainingMillis(System.currentTimeMillis());
        if (lockRemaining > 0) {
            callback.onResult(AuthResult.failure(AuthResult.Field.NONE, lockedMessage(lockRemaining)));
            return;
        }
        final String normalized = AuthValidator.normalizeUsername(username);
        if (normalized.isEmpty()) {
            callback.onResult(AuthResult.failure(AuthResult.Field.USERNAME,
                    appContext.getString(R.string.error_username_required)));
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onResult(AuthResult.failure(AuthResult.Field.PASSWORD,
                    appContext.getString(R.string.error_password_required)));
            return;
        }
        executors.diskIO().execute(() -> {
            AuthResult result;
            try {
                result = doLogin(normalized, password);
            } catch (RuntimeException e) {
                result = AuthResult.failure(AuthResult.Field.NONE, appContext.getString(R.string.error_auth_failed));
            }
            post(callback, result);
        });
    }

    @WorkerThread
    @NonNull
    private AuthResult doLogin(@NonNull String username, @NonNull String password) {
        UserAccount user = userDao.getByUsernameSync(username);
        boolean valid;
        if (user == null) {
            // Vẫn băm mật khẩu để thời gian phản hồi không làm lộ tên đăng nhập có tồn tại hay không
            PasswordHasher.hash(password, PasswordHasher.newSalt(), PasswordHasher.DEFAULT_ITERATIONS);
            valid = false;
        } else {
            valid = PasswordHasher.verify(password, user.getPasswordSalt(), user.getHashIterations(),
                    user.getPasswordHash());
        }
        long now = System.currentTimeMillis();
        if (!valid) {
            int remaining = session.recordFailedAttempt(now);
            String error = remaining > 0
                    ? appContext.getString(R.string.error_login_failed, remaining)
                    : lockedMessage(SessionManager.LOCK_DURATION_MS);
            return AuthResult.failure(AuthResult.Field.PASSWORD, error);
        }
        userDao.updateLastLogin(user.getId(), now);
        user.setLastLoginAt(now);
        startSession(user);
        return AuthResult.success(user, false);
    }

    // ------------------------------------------------------------------ Đăng xuất, hồ sơ

    /** Hủy lịch nhắc và thông báo của tài khoản hiện tại rồi kết thúc phiên. */
    public void logout(@NonNull Runnable onDone) {
        executors.diskIO().execute(() -> {
            if (session.isLoggedIn()) {
                ReminderScheduler.cancelAll(appContext);
                NotificationManagerCompat.from(appContext).cancelAll();
                session.endSession();
            }
            executors.mainThread().execute(onDone);
        });
    }

    /** Cập nhật tên hiển thị của tài khoản đang đăng nhập. */
    public void updateDisplayName(@NonNull String displayName) {
        final long userId = session.getCurrentUserId();
        if (userId <= 0) {
            return;
        }
        executors.diskIO().execute(() -> userDao.updateDisplayName(userId, displayName.trim()));
    }

    @Nullable
    public String getCurrentUsername() {
        return session.getCurrentUsername();
    }

    @NonNull
    public String getLastUsername() {
        return session.getLastUsername();
    }

    // ------------------------------------------------------------------ Nội bộ

    /**
     * Bắt đầu phiên và chuẩn bị dữ liệu của tài khoản: mở (hoặc tạo kèm dữ liệu mặc định) CSDL riêng,
     * áp dụng cài đặt tiền tệ và lập lại lịch nhắc của tài khoản.
     */
    @WorkerThread
    private void startSession(@NonNull UserAccount user) {
        session.startSession(user.getId(), user.getUsername());
        PrefsManager prefs = PrefsManager.getInstance(appContext);
        prefs.setUserName(user.getDisplayName());
        CurrencyUtils.setStyle(prefs.getCurrencyStyle());
        ReminderScheduler.rescheduleAll(appContext);
    }

    @NonNull
    private AuthResult usernameTaken(@NonNull String username) {
        return AuthResult.failure(AuthResult.Field.USERNAME,
                appContext.getString(R.string.error_username_taken, username));
    }

    @NonNull
    private String lockedMessage(long remainingMillis) {
        int seconds = (int) Math.max(1, (remainingMillis + 999) / 1000);
        return appContext.getString(R.string.error_login_locked, seconds);
    }

    @NonNull
    private String message(@NonNull AuthValidator.Error error) {
        switch (error) {
            case DISPLAY_NAME_REQUIRED:
                return appContext.getString(R.string.error_display_name_required);
            case DISPLAY_NAME_TOO_LONG:
                return appContext.getString(R.string.error_display_name_too_long, AuthValidator.DISPLAY_NAME_MAX);
            case USERNAME_REQUIRED:
                return appContext.getString(R.string.error_username_required);
            case USERNAME_LENGTH:
                return appContext.getString(R.string.error_username_length,
                        AuthValidator.USERNAME_MIN, AuthValidator.USERNAME_MAX);
            case USERNAME_INVALID_CHARS:
                return appContext.getString(R.string.error_username_invalid);
            case PASSWORD_REQUIRED:
                return appContext.getString(R.string.error_password_required);
            case PASSWORD_LENGTH:
                return appContext.getString(R.string.error_password_length,
                        AuthValidator.PASSWORD_MIN, AuthValidator.PASSWORD_MAX);
            case PASSWORD_WEAK:
                return appContext.getString(R.string.error_password_weak);
            case CONFIRM_MISMATCH:
                return appContext.getString(R.string.error_confirm_mismatch);
            default:
                return appContext.getString(R.string.error_auth_failed);
        }
    }

    private void post(@NonNull ResultCallback<AuthResult> callback, @NonNull AuthResult result) {
        executors.mainThread().execute(() -> callback.onResult(result));
    }
}
