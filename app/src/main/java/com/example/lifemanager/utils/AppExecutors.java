package com.example.lifemanager.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quản lý các luồng thực thi của ứng dụng.
 * <p>
 * - diskIO: luồng nền đơn (single thread) cho mọi thao tác database, đảm bảo thứ tự ghi.
 * - mainThread: đẩy kết quả về luồng giao diện.
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO;
    private final Executor mainThread;

    private AppExecutors() {
        diskIO = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lifemanager-disk-io");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        mainThread = new MainThreadExecutor();
    }

    @NonNull
    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    @NonNull
    public ExecutorService diskIO() {
        return diskIO;
    }

    @NonNull
    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    }
}
