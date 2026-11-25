package com.example.lifemanager.utils;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.lifemanager.R;
import com.example.lifemanager.databinding.DialogMonthPickerBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.YearMonth;

/**
 * Các hộp thoại dùng chung.
 */
public final class DialogUtils {

    private DialogUtils() {
    }

    /** Hộp thoại xác nhận (thường dùng trước khi xóa). */
    public static void confirm(@NonNull Context context, @StringRes int title, @NonNull String message,
                               @StringRes int positiveText, @NonNull Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(positiveText, (dialog, which) -> onConfirm.run())
                .show();
    }

    /** Hộp thoại chọn tháng / năm. */
    public static void showMonthPicker(@NonNull Context context, @NonNull YearMonth initial,
                                       @NonNull ResultCallback<YearMonth> callback) {
        DialogMonthPickerBinding binding = DialogMonthPickerBinding.inflate(LayoutInflater.from(context));
        int currentYear = YearMonth.now().getYear();
        binding.npMonth.setMinValue(1);
        binding.npMonth.setMaxValue(12);
        binding.npMonth.setValue(initial.getMonthValue());
        binding.npYear.setMinValue(currentYear - 20);
        binding.npYear.setMaxValue(currentYear + 5);
        binding.npYear.setValue(initial.getYear());
        binding.npMonth.setWrapSelectorWheel(true);
        binding.npYear.setWrapSelectorWheel(false);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.month_picker_title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_ok, (dialog, which) ->
                        callback.onResult(YearMonth.of(binding.npYear.getValue(), binding.npMonth.getValue())))
                .show();
    }
}
