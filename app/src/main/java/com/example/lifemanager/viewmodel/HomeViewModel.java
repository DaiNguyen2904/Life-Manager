package com.example.lifemanager.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.Note;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.data.repository.FinanceRepository;
import com.example.lifemanager.data.repository.HabitRepository;
import com.example.lifemanager.data.repository.HabitSnapshotLiveData;
import com.example.lifemanager.data.repository.NoteRepository;
import com.example.lifemanager.data.repository.TaskRepository;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.LiveDataUtils;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.PrefsManager;
import com.example.lifemanager.utils.ResultCallback;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel cho màn hình Trang chủ: ngày tháng, việc hôm nay, thói quen, ghi chú nhanh, thống kê nhanh.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final long AUTO_SAVE_DELAY_MS = 700L;

    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final NoteRepository noteRepository;
    private final PrefsManager prefs;

    private final MutableLiveData<LocalDate> today = new MutableLiveData<>(LocalDate.now());
    private final HabitSnapshotLiveData habitSnapshot;
    private final LiveData<List<HabitWithStats>> todayHabits;
    private final LiveData<List<Task>> allTasks;
    private final MediatorLiveData<List<Task>> todayTasks = new MediatorLiveData<>();
    private final MediatorLiveData<TaskStats> taskStats = new MediatorLiveData<>();
    private final LiveData<Long> monthIncome;
    private final LiveData<Long> monthExpense;
    private final LiveData<Long> balance;
    private final LiveData<List<Note>> notes;

    // ---- Ghi chú nhanh
    private final MutableLiveData<String> editorText = new MutableLiveData<>("");
    private final MutableLiveData<Long> currentNoteId = new MutableLiveData<>(0L);
    private final MutableLiveData<String> noteStatus = new MutableLiveData<>("");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = this::saveNow;
    private boolean dirty;
    private boolean insertInFlight;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        FinanceRepository financeRepository = FinanceRepository.getInstance(application);
        taskRepository = TaskRepository.getInstance(application);
        habitRepository = HabitRepository.getInstance(application);
        noteRepository = NoteRepository.getInstance(application);
        prefs = PrefsManager.getInstance(application);

        habitSnapshot = habitRepository.createSnapshotLiveData();
        todayHabits = LiveDataUtils.map(habitSnapshot, snapshot -> {
            List<HabitWithStats> list = new ArrayList<>();
            for (HabitWithStats s : snapshot.stats) {
                if (s.scheduledToday) {
                    list.add(s);
                }
            }
            return list;
        });

        allTasks = taskRepository.observeAll();
        todayTasks.addSource(allTasks, list -> recomputeTasks());
        todayTasks.addSource(today, day -> recomputeTasks());
        taskStats.addSource(allTasks, list -> recomputeTaskStats());
        taskStats.addSource(today, day -> recomputeTaskStats());

        LiveData<YearMonth> month = LiveDataUtils.distinctUntilChanged(
                LiveDataUtils.map(today, YearMonth::from));
        monthIncome = LiveDataUtils.switchMap(month, ym -> financeRepository.observeTotal(
                TransactionType.INCOME, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        monthExpense = LiveDataUtils.switchMap(month, ym -> financeRepository.observeTotal(
                TransactionType.EXPENSE, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        balance = financeRepository.observeBalance();
        notes = noteRepository.observeAll();

        restoreLastNote();
    }

    // ------------------------------------------------------------------ Getter LiveData

    public LiveData<LocalDate> getToday() {
        return today;
    }

    public LiveData<HabitSnapshot> getHabitSnapshot() {
        return habitSnapshot;
    }

    public LiveData<List<HabitWithStats>> getTodayHabits() {
        return todayHabits;
    }

    public LiveData<List<Task>> getTodayTasks() {
        return todayTasks;
    }

    public LiveData<TaskStats> getTaskStats() {
        return taskStats;
    }

    public LiveData<Long> getMonthIncome() {
        return monthIncome;
    }

    public LiveData<Long> getMonthExpense() {
        return monthExpense;
    }

    public LiveData<Long> getBalance() {
        return balance;
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    public LiveData<String> getEditorText() {
        return editorText;
    }

    public LiveData<Long> getCurrentNoteId() {
        return currentNoteId;
    }

    public LiveData<String> getNoteStatus() {
        return noteStatus;
    }

    // ------------------------------------------------------------------ Ngày

    /** Gọi khi màn hình hiển thị lại: cập nhật nếu đã sang ngày mới. */
    public void refreshDay() {
        LocalDate now = LocalDate.now();
        if (!now.equals(today.getValue())) {
            today.setValue(now);
        }
        habitSnapshot.refresh();
    }

    // ------------------------------------------------------------------ Công việc hôm nay

    private void recomputeTasks() {
        List<Task> source = allTasks.getValue();
        LocalDate day = today.getValue();
        if (source == null || day == null) {
            return;
        }
        long todayEpoch = day.toEpochDay();
        List<Task> pending = new ArrayList<>();
        List<Task> doneToday = new ArrayList<>();
        for (Task task : source) {
            if (!task.isDone()) {
                pending.add(task);
            } else if (task.getCompletedAt() != null
                    && DateUtils.epochMillisToEpochDay(task.getCompletedAt()) == todayEpoch) {
                doneToday.add(task);
            }
        }
        // Quá hạn trước, sau đó theo hạn gần nhất, ưu tiên cao trước
        Collections.sort(pending, (a, b) -> {
            boolean ao = a.isOverdue(todayEpoch);
            boolean bo = b.isOverdue(todayEpoch);
            if (ao != bo) {
                return ao ? -1 : 1;
            }
            long ad = a.getDueDate() == null ? Long.MAX_VALUE : a.getDueDate();
            long bd = b.getDueDate() == null ? Long.MAX_VALUE : b.getDueDate();
            if (ad != bd) {
                return Long.compare(ad, bd);
            }
            return Integer.compare(b.getPriority(), a.getPriority());
        });
        List<Task> result = new ArrayList<>(pending);
        result.addAll(doneToday);
        todayTasks.setValue(result);
    }

    private void recomputeTaskStats() {
        List<Task> source = allTasks.getValue();
        LocalDate day = today.getValue();
        if (source != null && day != null) {
            taskStats.setValue(TaskStats.from(source, day.toEpochDay()));
        }
    }

    public void toggleTask(@NonNull Task task) {
        taskRepository.updateStatus(task.getId(), task.isDone() ? Task.STATUS_TODO : Task.STATUS_DONE, null);
    }

    public void toggleHabit(@NonNull HabitWithStats stats) {
        LocalDate day = today.getValue() == null ? LocalDate.now() : today.getValue();
        habitRepository.setCompleted(stats.habit.getId(), day.toEpochDay(), !stats.doneToday, null);
    }

    /** Thêm nhanh công việc với hạn hoàn thành là hôm nay. */
    public void quickAddTask(@NonNull String title, @NonNull ResultCallback<OperationResult> callback) {
        Task task = new Task();
        task.setTitle(title);
        long todayEpoch = LocalDate.now().toEpochDay();
        task.setStartDate(todayEpoch);
        task.setDueDate(todayEpoch);
        task.setPriority(Task.PRIORITY_MEDIUM);
        task.setStatus(Task.STATUS_TODO);
        taskRepository.saveTask(task, callback);
    }

    /** Thêm nhanh thói quen hằng ngày. */
    public void quickAddHabit(@NonNull String name, @NonNull ResultCallback<OperationResult> callback) {
        Habit habit = new Habit(name, null, 1, getApplication().getString(R.string.habit_default_unit),
                LocalDate.now().toEpochDay());
        habitRepository.saveHabit(habit, callback);
    }

    // ------------------------------------------------------------------ Ghi chú nhanh

    private void restoreLastNote() {
        long lastId = prefs.getLastNoteId();
        if (lastId <= 0) {
            return;
        }
        noteRepository.load(lastId, note -> {
            if (note != null && !dirty) {
                currentNoteId.setValue(note.getId());
                editorText.setValue(note.getContent());
            } else if (note == null) {
                prefs.setLastNoteId(0);
            }
        });
    }

    /** Người dùng gõ nội dung ghi chú: lưu tự động sau một khoảng trễ ngắn. */
    public void onNoteTextChanged(@NonNull String text) {
        if (text.equals(editorText.getValue())) {
            return;
        }
        editorText.setValue(text);
        dirty = true;
        noteStatus.setValue(getApplication().getString(R.string.note_status_saving));
        handler.removeCallbacks(autoSaveRunnable);
        handler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY_MS);
    }

    /** Lưu ngay nội dung đang soạn (khi rời màn hình, chuyển ghi chú...). */
    public void flushNote() {
        handler.removeCallbacks(autoSaveRunnable);
        if (dirty) {
            saveNow();
        }
    }

    private void saveNow() {
        String content = editorText.getValue() == null ? "" : editorText.getValue();
        long id = currentNoteId.getValue() == null ? 0 : currentNoteId.getValue();
        if (id == 0) {
            if (content.trim().isEmpty()) {
                dirty = false;
                noteStatus.setValue("");
                return;
            }
            if (insertInFlight) {
                // Đang tạo bản ghi: lưu lại sau khi có id
                handler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY_MS);
                return;
            }
            insertInFlight = true;
            dirty = false;
            noteRepository.insert(content, newId -> {
                insertInFlight = false;
                currentNoteId.setValue(newId);
                prefs.setLastNoteId(newId);
                markSaved();
                if (dirty) {
                    saveNow();
                }
            });
        } else {
            dirty = false;
            noteRepository.updateContent(id, content, this::markSaved);
        }
    }

    private void markSaved() {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        noteStatus.setValue(getApplication().getString(R.string.note_status_saved, time));
    }

    /** Mở một ghi chú đã lưu vào khung soạn thảo. */
    public void openNote(@NonNull Note note) {
        flushNote();
        currentNoteId.setValue(note.getId());
        editorText.setValue(note.getContent());
        prefs.setLastNoteId(note.getId());
        noteStatus.setValue("");
    }

    /** Tạo ghi chú mới (xóa trắng khung soạn thảo). */
    public void newNote() {
        flushNote();
        currentNoteId.setValue(0L);
        editorText.setValue("");
        prefs.setLastNoteId(0);
        noteStatus.setValue("");
    }

    /** Xóa ghi chú theo id; nếu là ghi chú đang mở thì xóa trắng khung soạn thảo. */
    public void deleteNote(long id) {
        Long current = currentNoteId.getValue();
        if (current != null && current == id) {
            handler.removeCallbacks(autoSaveRunnable);
            dirty = false;
            currentNoteId.setValue(0L);
            editorText.setValue("");
            prefs.setLastNoteId(0);
            noteStatus.setValue("");
        }
        if (id > 0) {
            noteRepository.delete(id, null);
        }
    }

    public void deleteCurrentNote() {
        Long current = currentNoteId.getValue();
        deleteNote(current == null ? 0 : current);
    }

    @Override
    protected void onCleared() {
        flushNote();
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
