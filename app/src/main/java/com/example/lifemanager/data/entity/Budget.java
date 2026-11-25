package com.example.lifemanager.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng budgets: ngân sách chi tiêu hằng tháng cho từng danh mục chi.
 * Mỗi danh mục chỉ có tối đa một ngân sách (unique category_id).
 */
@Entity(tableName = "budgets",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "category_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "category_id", unique = true)})
public class Budget {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "category_id")
    private long categoryId;

    @ColumnInfo(name = "amount")
    private long amount;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public Budget() {
    }

    @Ignore
    public Budget(long categoryId, long amount) {
        this.categoryId = categoryId;
        this.amount = amount;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
