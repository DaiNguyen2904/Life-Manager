package com.example.lifemanager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quy tắc kiểm tra dữ liệu đăng ký / đăng nhập (không phụ thuộc Android để kiểm thử đơn vị).
 */
public final class AuthValidator {

    public static final int DISPLAY_NAME_MAX = 40;
    public static final int USERNAME_MIN = 4;
    public static final int USERNAME_MAX = 30;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 64;

    private static final Pattern USERNAME_CHARS = Pattern.compile("[a-z0-9._]+");

    public enum Error {
        NONE,
        DISPLAY_NAME_REQUIRED,
        DISPLAY_NAME_TOO_LONG,
        USERNAME_REQUIRED,
        USERNAME_LENGTH,
        USERNAME_INVALID_CHARS,
        PASSWORD_REQUIRED,
        PASSWORD_LENGTH,
        PASSWORD_WEAK,
        CONFIRM_MISMATCH
    }

    private AuthValidator() {
    }

    /** Tên đăng nhập được lưu ở dạng chữ thường, bỏ khoảng trắng đầu cuối (không phân biệt hoa thường). */
    @NonNull
    public static String normalizeUsername(@Nullable String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    public static Error checkDisplayName(@Nullable String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return Error.DISPLAY_NAME_REQUIRED;
        }
        return name.length() > DISPLAY_NAME_MAX ? Error.DISPLAY_NAME_TOO_LONG : Error.NONE;
    }

    @NonNull
    public static Error checkUsername(@Nullable String raw) {
        String username = normalizeUsername(raw);
        if (username.isEmpty()) {
            return Error.USERNAME_REQUIRED;
        }
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            return Error.USERNAME_LENGTH;
        }
        return USERNAME_CHARS.matcher(username).matches() ? Error.NONE : Error.USERNAME_INVALID_CHARS;
    }

    @NonNull
    public static Error checkPassword(@Nullable String password) {
        if (password == null || password.isEmpty()) {
            return Error.PASSWORD_REQUIRED;
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return Error.PASSWORD_LENGTH;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit ? Error.NONE : Error.PASSWORD_WEAK;
    }

    @NonNull
    public static Error checkConfirm(@Nullable String password, @Nullable String confirm) {
        return password != null && password.equals(confirm) ? Error.NONE : Error.CONFIRM_MISMATCH;
    }
}
