package com.example.lifemanager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Kết quả của một thao tác lưu / xóa dữ liệu.
 */
public final class OperationResult {

    public final boolean success;
    /** Id bản ghi vừa lưu (nếu có). */
    public final long id;
    /** Thông báo lỗi khi thất bại. */
    @Nullable
    public final String error;
    /** Cảnh báo kèm theo khi thành công (ví dụ: vượt ngân sách). */
    @Nullable
    public final String warning;

    private OperationResult(boolean success, long id, @Nullable String error, @Nullable String warning) {
        this.success = success;
        this.id = id;
        this.error = error;
        this.warning = warning;
    }

    @NonNull
    public static OperationResult success(long id) {
        return new OperationResult(true, id, null, null);
    }

    @NonNull
    public static OperationResult successWithWarning(long id, @Nullable String warning) {
        return new OperationResult(true, id, null, warning);
    }

    @NonNull
    public static OperationResult failure(@NonNull String error) {
        return new OperationResult(false, 0, error, null);
    }
}
