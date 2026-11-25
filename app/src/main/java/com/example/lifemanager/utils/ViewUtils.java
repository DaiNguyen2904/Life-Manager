package com.example.lifemanager.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Tiện ích thao tác với View.
 */
public final class ViewUtils {

    private ViewUtils() {
    }

    public static int dp(@NonNull Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Thêm padding theo thanh hệ thống (status bar / navigation bar / bàn phím)
     * vì ứng dụng hiển thị tràn viền (edge-to-edge) trên Android 15+.
     */
    public static void applySystemBarInsets(@NonNull View view, boolean applyTop, boolean applyBottom) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(bars.bottom, ime.bottom);
            v.setPadding(left + bars.left,
                    top + (applyTop ? bars.top : 0),
                    right + bars.right,
                    bottom + (applyBottom ? bottomInset : 0));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void setBackgroundTint(@NonNull View view, @ColorInt int color) {
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(color));
    }

    /** Pha màu với trắng để tạo nền nhạt. */
    @ColorInt
    public static int softColor(@ColorInt int color, float whiteRatio) {
        return ColorUtils.blendARGB(color, Color.WHITE, whiteRatio);
    }

    public static void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
