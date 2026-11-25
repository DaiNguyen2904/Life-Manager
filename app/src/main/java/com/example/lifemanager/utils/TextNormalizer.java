package com.example.lifemanager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa chuỗi tiếng Việt để tìm kiếm không phân biệt dấu và chữ hoa / thường.
 * Ví dụ: "Ăn uống" -> "an uong".
 */
public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{Mn}+");

    private TextNormalizer() {
    }

    @NonNull
    public static String normalize(@Nullable String input) {
        if (input == null) {
            return "";
        }
        String text = input.replace('đ', 'd').replace('Đ', 'D');
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = DIACRITICS.matcher(text).replaceAll("");
        return text.toLowerCase(Locale.ROOT).trim();
    }

    /** Kiểm tra một trong các trường có chứa từ khóa (đã chuẩn hóa) hay không. */
    public static boolean matches(@NonNull String normalizedQuery, @Nullable String... fields) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        if (fields == null) {
            return false;
        }
        for (String field : fields) {
            if (field != null && normalize(field).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }
}
