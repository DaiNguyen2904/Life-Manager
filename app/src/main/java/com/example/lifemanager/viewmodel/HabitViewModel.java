package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.data.repository.HabitRepository;
import com.example.lifemanager.data.repository.HabitSnapshotLiveData;

import java.time.LocalDate;

/**
 * ViewModel cho màn hình Quản lý thói quen.
 */
public class HabitViewModel extends AndroidViewModel {

    private final HabitRepository repository;
    private final HabitSnapshotLiveData snapshot;

    public HabitViewModel(@NonNull Application application) {
        super(application);
        repository = HabitRepository.getInstance(application);
        snapshot = repository.createSnapshotLiveData();
    }

    public LiveData<HabitSnapshot> getSnapshot() {
        return snapshot;
    }

    public void refresh() {
        snapshot.refresh();
    }

    /** Đánh dấu / bỏ đánh dấu hoàn thành hôm nay. */
    public void toggleToday(@NonNull HabitWithStats stats) {
        repository.setCompleted(stats.habit.getId(), LocalDate.now().toEpochDay(), !stats.doneToday, null);
    }

    public void deleteHabit(@NonNull Habit habit) {
        repository.deleteHabit(habit, null);
    }
}
