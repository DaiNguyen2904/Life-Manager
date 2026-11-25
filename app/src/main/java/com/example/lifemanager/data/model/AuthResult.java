package com.example.lifemanager.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lifemanager.data.entity.UserAccount;

/**
 * Kết quả đăng ký / đăng nhập. Khi thất bại, {@link #field} cho biết trường nhập liệu cần hiển thị lỗi.
 */
public final class AuthResult {

    public enum Field {
        NONE, DISPLAY_NAME, USERNAME, PASSWORD, CONFIRM
    }

    public final boolean success;
    @NonNull
    public final Field field;
    @Nullable
    public final String error;
    @Nullable
    public final UserAccount user;
    /** Dữ liệu của phiên bản chưa có tài khoản đã được chuyển vào tài khoản này. */
    public final boolean adoptedExistingData;

    private AuthResult(boolean success, @NonNull Field field, @Nullable String error,
                       @Nullable UserAccount user, boolean adoptedExistingData) {
        this.success = success;
        this.field = field;
        this.error = error;
        this.user = user;
        this.adoptedExistingData = adoptedExistingData;
    }

    @NonNull
    public static AuthResult success(@NonNull UserAccount user, boolean adoptedExistingData) {
        return new AuthResult(true, Field.NONE, null, user, adoptedExistingData);
    }

    @NonNull
    public static AuthResult failure(@NonNull Field field, @NonNull String error) {
        return new AuthResult(false, field, error, null, false);
    }
}
