package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.repository.TaskRepository;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;

import java.time.LocalDate;

/**
 * ViewModel cho màn hình thêm / sửa công việc.
 * Giữ toàn bộ trạng thái form trong một đối tượng Task nháp (draft).
 */
public class TaskEditorViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final MutableLiveData<Task> draft = new MutableLiveData<>();
    private boolean initialized;

    public TaskEditorViewModel(@NonNull Application application) {
        super(application);
        repository = TaskRepository.getInstance(application);
    }

    public LiveData<Task> getDraft() {
        return draft;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isEditing() {
        Task task = draft.getValue();
        return task != null && task.getId() != 0;
    }

    /** Khởi tạo công việc mới với giá trị mặc định. */
    public void initNew(@Nullable String initialStatus) {
        initialized = true;
        Task task = new Task();
        long today = LocalDate.now().toEpochDay();
        task.setStartDate(today);
        task.setDueDate(today);
        task.setPriority(Task.PRIORITY_MEDIUM);
        if (Task.STATUS_IN_PROGRESS.equals(initialStatus) || Task.STATUS_DONE.equals(initialStatus)) {
            task.setStatus(initialStatus);
        } else {
            task.setStatus(Task.STATUS_TODO);
        }
        draft.setValue(task);
    }

    /** Tải công việc cần sửa. */
    public void load(long id, @NonNull ResultCallback<Boolean> callback) {
        initialized = true;
        repository.loadTask(id, task -> {
            if (task != null) {
                draft.setValue(task.copy());
            }
            callback.onResult(task != null);
        });
    }

    @Nullable
    public Task current() {
        return draft.getValue();
    }

    /** Cập nhật lại draft để giao diện vẽ lại. */
    public void notifyDraftChanged() {
        draft.setValue(draft.getValue());
    }

    public void save(@NonNull ResultCallback<OperationResult> callback) {
        Task task = draft.getValue();
        if (task == null) {
            return;
        }
        repository.saveTask(task.copy(), callback);
    }

    public void delete(@NonNull Runnable onDone) {
        Task task = draft.getValue();
        if (task != null && task.getId() != 0) {
            repository.deleteTask(task, onDone);
        }
    }
}
