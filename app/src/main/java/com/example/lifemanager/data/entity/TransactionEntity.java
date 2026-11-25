package com.example.lifemanager.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng transactions: giao dịch thu / chi.
 * <p>
 * - amount: số tiền (VNĐ, số nguyên dương).
 * - date: ngày giao dịch lưu dạng epochDay (LocalDate.toEpochDay()) để lọc theo ngày / tháng dễ dàng.
 * - category_id: khóa ngoại tới categories, SET NULL khi danh mục bị xóa.
 */
@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "category_id",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index("category_id"), @Index("date"), @Index("type")})
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @ColumnInfo(name = "amount")
    private long amount;

    @NonNull
    @ColumnInfo(name = "type")
    private String type = TransactionType.EXPENSE;

    @Nullable
    @ColumnInfo(name = "category_id")
    private Long categoryId;

    @ColumnInfo(name = "date")
    private long date;

    @Nullable
    @ColumnInfo(name = "note")
    private String note;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @Nullable
    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@Nullable Long categoryId) {
        this.categoryId = categoryId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isIncome() {
        return TransactionType.INCOME.equals(type);
    }
}
