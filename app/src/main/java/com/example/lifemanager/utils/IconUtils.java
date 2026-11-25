package com.example.lifemanager.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.example.lifemanager.R;

/**
 * Ánh xạ khóa icon (lưu trong database) sang drawable và bảng màu cho danh mục.
 */
public final class IconUtils {

    /** Danh sách icon có thể chọn cho danh mục. */
    public static final String[] ICON_KEYS = {
            "car", "movie", "receipt", "cart", "health", "food", "school", "category", "money",
            "gift", "trending", "work", "home", "fitness", "book", "drink", "code", "star"
    };

    /** Bảng màu có thể chọn cho danh mục. */
    public static final int[] PALETTE = {
            0xFF42A5F5, 0xFFAB47BC, 0xFFFFA726, 0xFFEC407A, 0xFF66BB6A, 0xFFEF5350,
            0xFF5C6BC0, 0xFF78909C, 0xFF26A69A, 0xFF8D6E63, 0xFFFFCA28, 0xFF29B6F6
    };

    private IconUtils() {
    }

    @DrawableRes
    public static int getIconRes(@Nullable String key) {
        if (key == null) {
            return R.drawable.ic_cat_category;
        }
        switch (key) {
            case "car":
                return R.drawable.ic_cat_car;
            case "movie":
                return R.drawable.ic_cat_movie;
            case "receipt":
                return R.drawable.ic_cat_receipt;
            case "cart":
                return R.drawable.ic_cat_cart;
            case "health":
                return R.drawable.ic_cat_health;
            case "food":
                return R.drawable.ic_cat_food;
            case "school":
                return R.drawable.ic_cat_school;
            case "money":
                return R.drawable.ic_cat_money;
            case "gift":
                return R.drawable.ic_cat_gift;
            case "trending":
                return R.drawable.ic_cat_trending;
            case "work":
                return R.drawable.ic_cat_work;
            case "home":
                return R.drawable.ic_cat_home;
            case "fitness":
                return R.drawable.ic_cat_fitness;
            case "book":
                return R.drawable.ic_cat_book;
            case "drink":
                return R.drawable.ic_cat_drink;
            case "code":
                return R.drawable.ic_cat_code;
            case "star":
                return R.drawable.ic_cat_star;
            case "category":
            default:
                return R.drawable.ic_cat_category;
        }
    }
}
