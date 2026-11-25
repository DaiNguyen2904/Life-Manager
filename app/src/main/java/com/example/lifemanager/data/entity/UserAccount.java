package com.example.lifemanager.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng users: tài khoản người dùng trên thiết bị (lưu trong accounts.db).
 * Mật khẩu chỉ được lưu dưới dạng băm PBKDF2 kèm salt riêng của từng tài khoản.
 */
@Entity(tableName = "users", indices = {@Index(value = "username", unique = true)})
public class UserAccount {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    /** Tên đăng nhập, lưu ở dạng chữ thường. */
    @NonNull
    @ColumnInfo(name = "username")
    private String username = "";

    @NonNull
    @ColumnInfo(name = "display_name")
    private String displayName = "";

    @NonNull
    @ColumnInfo(name = "password_hash")
    private String passwordHash = "";

    @NonNull
    @ColumnInfo(name = "password_salt")
    private String passwordSalt = "";

    @ColumnInfo(name = "hash_iterations")
    private int hashIterations;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @Nullable
    @ColumnInfo(name = "last_login_at")
    private Long lastLoginAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    @NonNull
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@NonNull String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @NonNull
    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(@NonNull String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(int hashIterations) {
        this.hashIterations = hashIterations;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Nullable
    public Long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(@Nullable Long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
