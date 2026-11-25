package com.example.lifemanager.utils;

/**
 * Callback nhận kết quả từ thao tác nền, luôn được gọi trên luồng giao diện.
 */
public interface ResultCallback<T> {
    void onResult(T result);
}
