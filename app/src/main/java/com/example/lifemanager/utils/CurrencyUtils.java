package com.example.lifemanager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng tiền tệ VNĐ theo lựa chọn của người dùng trong Cài đặt.
 * Ví dụ: 1500000 -> "1.500.000 ₫" | "1.500.000 VNĐ" | "1.500.000 đ".
 */
public final class CurrencyUtils {

    public static final int STYLE_SYMBOL = 0;   // 1.500.000 ₫
    public static final int STYLE_CODE = 1;     // 1.500.000 VNĐ
    public static final int STYLE_SHORT = 2;    // 1.500.000 đ

    /** Giới hạn số tiền tối đa cho một giao dịch / ngân sách (1.000 tỷ). */
    public static final long MAX_AMOUNT = 1_000_000_000_000L;

    private static volatile int currentStyle = STYLE_SYMBOL;

    private CurrencyUtils() {
    }

    public static void setStyle(int style) {
        currentStyle = style;
    }

    public static int getStyle() {
        return currentStyle;
    }

    @NonNull
    private static DecimalFormat newFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setGroupingUsed(true);
        return format;
    }

    /** Định dạng số có dấu phân cách hàng nghìn: 1500000 -> "1.500.000". */
    @NonNull
    public static String formatNumber(long value) {
        return newFormatter().format(value);
    }

    @NonNull
    public static String suffix() {
        switch (currentStyle) {
            case STYLE_CODE:
                return "VNĐ";
            case STYLE_SHORT:
                return "đ";
            case STYLE_SYMBOL:
            default:
                return "₫";
        }
    }

    /** Định dạng số tiền kèm đơn vị. */
    @NonNull
    public static String format(long amount) {
        return formatNumber(amount) + " " + suffix();
    }

    /** Định dạng số tiền có dấu +/- (dùng cho danh sách giao dịch). */
    @NonNull
    public static String formatSigned(long amount, boolean income) {
        return (income ? "+" : "-") + format(Math.abs(amount));
    }

    /** Định dạng rút gọn cho biểu đồ: 1.5Tr, 500K. */
    @NonNull
    public static String formatCompact(float amount) {
        float abs = Math.abs(amount);
        if (abs >= 1_000_000_000f) {
            return trim(amount / 1_000_000_000f) + " tỷ";
        } else if (abs >= 1_000_000f) {
            return trim(amount / 1_000_000f) + "tr";
        } else if (abs >= 1_000f) {
            return trim(amount / 1_000f) + "k";
        }
        return trim(amount);
    }

    @NonNull
    private static String trim(float value) {
        if (value == Math.round(value)) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    /**
     * Chuyển chuỗi người dùng nhập ("1.500.000") thành số.
     *
     * @return số tiền, hoặc -1 nếu chuỗi rỗng / không hợp lệ.
     */
    public static long parse(@Nullable String input) {
        if (input == null) {
            return -1;
        }
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 15) {
            return -1;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
