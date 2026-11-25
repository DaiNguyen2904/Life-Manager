package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.repository.HabitRepository;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;

import java.time.LocalDate;

/**
 * ViewModel cho màn hình thêm / sửa thói quen.
 */
public class HabitEditorViewModel extends AndroidViewModel {

    private final HabitRepository repository;
    private final MutableLiveData<Habit> draft = new MutableLiveData<>();
    private boolean initialized;

    public HabitEditorViewModel(@NonNull Application application) {
        super(application);
        repository = HabitRepository.getInstance(application);
    }

    public LiveData<Habit> getDraft() {
        return draft;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isEditing() {
        Habit habit = draft.getValue();
        return habit != null && habit.getId() != 0;
    }

    public void initNew() {
        initialized = true;
        Habit habit = new Habit();
        habit.setStartDate(LocalDate.now().toEpochDay());
        habit.setFrequencyMask(Habit.MASK_DAILY);
        habit.setDailyTarget(1);
        draft.setValue(habit);
    }

    public void load(long id, @NonNull ResultCallback<Boolean> callback) {
        initialized = true;
        repository.loadHabit(id, habit -> {
            if (habit != null) {
                draft.setValue(habit);
            }
            callback.onResult(habit != null);
        });
    }

    @Nullable
    public Habit current() {
        return draft.getValue();
    }

    public void notifyDraftChanged() {
        draft.setValue(draft.getValue());
    }

    public void save(@NonNull ResultCallback<OperationResult> callback) {
        Habit habit = draft.getValue();
        if (habit != null) {
            repository.saveHabit(habit, callback);
        }
    }

    public void delete(@NonNull Runnable onDone) {
        Habit habit = draft.getValue();
        if (habit != null && habit.getId() != 0) {
            repository.deleteHabit(habit, onDone);
        }
    }
}
