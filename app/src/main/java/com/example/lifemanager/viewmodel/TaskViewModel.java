package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.data.repository.TaskRepository;
import com.example.lifemanager.utils.TextNormalizer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ViewModel cho bảng Kanban công việc: chia cột theo trạng thái, tìm kiếm, lọc ưu tiên, sắp xếp theo hạn.
 */
public class TaskViewModel extends AndroidViewModel {

    public static final int PRIORITY_ALL = -1;

    /** Kết quả hiển thị của bảng Kanban. */
    public static class TaskBoard {
        @NonNull
        public final List<Task> visibleTasks;
        @NonNull
        public final TaskStats stats;
        @NonNull
        public final String column;
        public final long todayEpochDay;

        TaskBoard(@NonNull List<Task> visibleTasks, @NonNull TaskStats stats, @NonNull String column,
                  long todayEpochDay) {
            this.visibleTasks = visibleTasks;
            this.stats = stats;
            this.column = column;
            this.todayEpochDay = todayEpochDay;
        }
    }

    private final TaskRepository repository;
    private final LiveData<List<Task>> allTasks;
    private final MutableLiveData<String> column = new MutableLiveData<>(Task.STATUS_TODO);
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<Integer> priority = new MutableLiveData<>(PRIORITY_ALL);
    private final MutableLiveData<Boolean> sortAscending = new MutableLiveData<>(true);
    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<TaskBoard> board = new MediatorLiveData<>();

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = TaskRepository.getInstance(application);
        allTasks = repository.observeAll();
        board.addSource(allTasks, v -> recompute());
        board.addSource(column, v -> recompute());
        board.addSource(query, v -> recompute());
        board.addSource(priority, v -> recompute());
        board.addSource(sortAscending, v -> recompute());
        board.addSource(today, v -> recompute());
    }

    private void recompute() {
        List<Task> source = allTasks.getValue();
        if (source == null) {
            return;
        }
        long todayEpoch = (today.getValue() == null ? LocalDate.now() : today.getValue()).toEpochDay();
        String selectedColumn = column.getValue() == null ? Task.STATUS_TODO : column.getValue();
        String normalizedQuery = TextNormalizer.normalize(query.getValue());
        int selectedPriority = priority.getValue() == null ? PRIORITY_ALL : priority.getValue();
        boolean ascending = sortAscending.getValue() == null || sortAscending.getValue();

        List<Task> visible = new ArrayList<>();
        for (Task task : source) {
            if (!selectedColumn.equals(task.getEffectiveStatus(todayEpoch))) {
                continue;
            }
            if (selectedPriority != PRIORITY_ALL && task.getPriority() != selectedPriority) {
                continue;
            }
            if (!TextNormalizer.matches(normalizedQuery, task.getTitle(), task.getDescription(),
                    task.getNote())) {
                continue;
            }
            visible.add(task);
        }
        Collections.sort(visible, deadlineComparator(ascending));
        board.setValue(new TaskBoard(visible, TaskStats.from(source, todayEpoch), selectedColumn, todayEpoch));
    }

    /** Sắp xếp theo thời hạn; công việc không có hạn luôn nằm cuối. */
    @NonNull
    private static Comparator<Task> deadlineComparator(boolean ascending) {
        return (a, b) -> {
            Long ad = a.getDueDate();
            Long bd = b.getDueDate();
            if (ad == null && bd != null) {
                return 1;
            }
            if (ad != null && bd == null) {
                return -1;
            }
            if (ad != null && !ad.equals(bd)) {
                return ascending ? Long.compare(ad, bd) : Long.compare(bd, ad);
            }
            if (a.getPriority() != b.getPriority()) {
                return Integer.compare(b.getPriority(), a.getPriority());
            }
            return Long.compare(b.getId(), a.getId());
        };
    }

    public LiveData<TaskBoard> getBoard() {
        return board;
    }

    public LiveData<String> getColumn() {
        return column;
    }

    public LiveData<Integer> getPriority() {
        return priority;
    }

    public LiveData<Boolean> getSortAscending() {
        return sortAscending;
    }

    public void setColumn(@NonNull String status) {
        if (!status.equals(column.getValue())) {
            column.setValue(status);
        }
    }

    public void setQuery(@NonNull String text) {
        if (!text.equals(query.getValue())) {
            query.setValue(text);
        }
    }

    public void setPriority(int value) {
        priority.setValue(value);
    }

    public void toggleSort() {
        Boolean current = sortAscending.getValue();
        sortAscending.setValue(current == null || !current);
    }

    public void refreshDay() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
    }

    public void updateStatus(@NonNull Task task, @NonNull String status) {
        repository.updateStatus(task.getId(), status, null);
    }

    public void toggleDone(@NonNull Task task) {
        repository.updateStatus(task.getId(), task.isDone() ? Task.STATUS_TODO : Task.STATUS_DONE, null);
    }

    public void deleteTask(@NonNull Task task) {
        repository.deleteTask(task, null);
    }
}
