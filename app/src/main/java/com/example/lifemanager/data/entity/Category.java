package com.example.lifemanager.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng categories: danh mục thu / chi.
 * Tên danh mục là duy nhất trong cùng một loại (thu hoặc chi).
 */
@Entity(tableName = "categories",
        indices = {@Index(value = {"name", "type"}, unique = true)})
public class Category {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @NonNull
    @ColumnInfo(name = "type")
    private String type = TransactionType.EXPENSE;

    @ColumnInfo(name = "color")
    private int color;

    @NonNull
    @ColumnInfo(name = "icon")
    private String icon = "category";

    @ColumnInfo(name = "is_default")
    private boolean builtIn;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public Category() {
    }

    @Ignore
    public Category(@NonNull String name, @NonNull String type, int color, @NonNull String icon) {
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @NonNull
    public String getIcon() {
        return icon;
    }

    public void setIcon(@NonNull String icon) {
        this.icon = icon;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExpense() {
        return TransactionType.EXPENSE.equals(type);
    }

    /** Dùng để hiển thị trong dropdown (ArrayAdapter gọi toString). */
    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
