package com.example.lifemanager.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.example.lifemanager.data.entity.TransactionEntity;

/**
 * Kết quả truy vấn giao dịch kèm thông tin danh mục (LEFT JOIN categories).
 */
public class TransactionWithCategory {

    @Embedded
    public TransactionEntity transaction;

    @Nullable
    @ColumnInfo(name = "category_name")
    public String categoryName;

    @Nullable
    @ColumnInfo(name = "category_color")
    public Integer categoryColor;

    @Nullable
    @ColumnInfo(name = "category_icon")
    public String categoryIcon;
}
