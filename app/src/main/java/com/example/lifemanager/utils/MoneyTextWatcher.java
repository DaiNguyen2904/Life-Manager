package com.example.lifemanager.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

/**
 * Tự động thêm dấu phân cách hàng nghìn khi người dùng nhập số tiền.
 * Ví dụ: gõ 1500000 -> hiển thị 1.500.000
 */
public class MoneyTextWatcher implements TextWatcher {

    private static final int MAX_DIGITS = 13;

    private final EditText editText;
    private boolean selfChange;

    public MoneyTextWatcher(@NonNull EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Không cần xử lý
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Không cần xử lý
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (selfChange) {
            return;
        }
        String digits = s.toString().replaceAll("[^0-9]", "");
        if (digits.length() > MAX_DIGITS) {
            digits = digits.substring(0, MAX_DIGITS);
        }
        String formatted = digits.isEmpty() ? "" : CurrencyUtils.formatNumber(Long.parseLong(digits));
        if (!formatted.equals(s.toString())) {
            selfChange = true;
            editText.setText(formatted);
            editText.setSelection(formatted.length());
            selfChange = false;
        }
    }
}
