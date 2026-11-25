package com.example.lifemanager.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/**
 * Tổng chi tiêu của một danh mục trong một khoảng thời gian, kèm ngân sách (nếu có).
 */
public class CategorySpending {

    @ColumnInfo(name = "category_id")
    public long categoryId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "color")
    public int color;

    @ColumnInfo(name = "icon")
    public String icon;

    @ColumnInfo(name = "spent")
    public long spent;

    @Nullable
    @ColumnInfo(name = "budget")
    public Long budget;

    public boolean hasBudget() {
        return budget != null && budget > 0;
    }

    public boolean isOverBudget() {
        return hasBudget() && spent > budget;
    }

    /** Số tiền còn lại (âm nếu vượt ngân sách). */
    public long getRemaining() {
        return hasBudget() ? budget - spent : 0;
    }

    /** Phần trăm sử dụng ngân sách (có thể > 100). */
    public int getUsagePercent() {
        if (!hasBudget()) {
            return 0;
        }
        return (int) Math.round(spent * 100.0 / budget);
    }
}
