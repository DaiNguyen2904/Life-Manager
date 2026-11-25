package com.example.lifemanager.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.HabitLog;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.utils.HabitStatsCalculator;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LiveData kết hợp danh sách thói quen và lịch sử hoàn thành.
 * Mỗi khi dữ liệu thay đổi, các chỉ số (Streak, tỷ lệ tuần / tháng) được tính lại ở background
 * và chỉ kết quả mới nhất được phát ra.
 */
public class HabitSnapshotLiveData extends MediatorLiveData<HabitSnapshot> {

    private final Executor executor;
    private final AtomicInteger version = new AtomicInteger();
    private List<Habit> habits;
    private List<HabitLog> logs;

    HabitSnapshotLiveData(@NonNull LiveData<List<Habit>> habitsSource,
                          @NonNull LiveData<List<HabitLog>> logsSource,
                          @NonNull Executor executor) {
        this.executor = executor;
        addSource(habitsSource, list -> {
            habits = list;
            recompute();
        });
        addSource(logsSource, list -> {
            logs = list;
            recompute();
        });
    }

    /** Tính lại (ví dụ khi sang ngày mới). */
    public void refresh() {
        recompute();
    }

    private void recompute() {
        if (habits == null || logs == null) {
            return;
        }
        final List<Habit> habitsCopy = habits;
        final List<HabitLog> logsCopy = logs;
        final int currentVersion = version.incrementAndGet();
        executor.execute(() -> {
            HabitSnapshot snapshot = HabitStatsCalculator.buildSnapshot(habitsCopy, logsCopy, LocalDate.now());
            if (currentVersion == version.get()) {
                postValue(snapshot);
            }
        });
    }
}
