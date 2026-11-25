package com.example.lifemanager.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.lifemanager.R;
import com.example.lifemanager.data.dao.TaskDao;
import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.notification.ReminderScheduler;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.SessionManager;

import java.util.List;

/**
 * Repository quản lý công việc và lịch nhắc nhở của công việc.
 */
public class TaskRepository {

    public static final int MAX_TITLE_LENGTH = 100;

    private static volatile TaskRepository instance;

    /** Id tài khoản sở hữu dữ liệu mà Repository đang làm việc. */
    private final long ownerId;

    private final Context appContext;
    private final TaskDao taskDao;
    private final AppExecutors executors;

    private TaskRepository(@NonNull Context context) {
        ownerId = SessionManager.getInstance(context).getCurrentUserId();
        appContext = context.getApplicationContext();
        taskDao = AppDatabase.getInstance(appContext).taskDao();
        executors = AppExecutors.getInstance();
    }

    /** Repository của tài khoản đang đăng nhập (tạo lại khi tài khoản thay đổi). */
    @NonNull
    public static TaskRepository getInstance(@NonNull Context context) {
        long userId = SessionManager.getInstance(context).getCurrentUserId();
        TaskRepository current = instance;
        if (current == null || current.ownerId != userId) {
            synchronized (TaskRepository.class) {
                current = instance;
                if (current == null || current.ownerId != userId) {
                    current = new TaskRepository(context);
                    instance = current;
                }
            }
        }
        return current;
    }

    @NonNull
    public LiveData<List<Task>> observeAll() {
        return taskDao.observeAll();
    }

    @NonNull
    public LiveData<Task> observeById(long id) {
        return taskDao.observeById(id);
    }

    public void loadTask(long id, @NonNull ResultCallback<Task> callback) {
        executors.diskIO().execute(() -> {
            Task task = taskDao.getByIdSync(id);
            executors.mainThread().execute(() -> callback.onResult(task));
        });
    }

    /** Kiểm tra dữ liệu công việc, trả về thông báo lỗi hoặc null nếu hợp lệ. */
    @Nullable
    public String validate(@NonNull Task task) {
        String title = task.getTitle().trim();
        if (title.isEmpty()) {
            return appContext.getString(R.string.error_title_required);
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return appContext.getString(R.string.error_title_too_long, MAX_TITLE_LENGTH);
        }
        if (task.getStartDate() != null && task.getDueDate() != null
                && task.getDueDate() < task.getStartDate()) {
            return appContext.getString(R.string.error_due_before_start);
        }
        if (task.getPriority() < Task.PRIORITY_LOW || task.getPriority() > Task.PRIORITY_HIGH) {
            task.setPriority(Task.PRIORITY_MEDIUM);
        }
        return null;
    }

    public void saveTask(@NonNull Task task, @NonNull ResultCallback<OperationResult> callback) {
        task.setTitle(task.getTitle().trim());
        String error = validate(task);
        if (error != null) {
            callback.onResult(OperationResult.failure(error));
            return;
        }
        executors.diskIO().execute(() -> {
            OperationResult result;
            try {
                long now = System.currentTimeMillis();
                if (task.isDone()) {
                    if (task.getCompletedAt() == null) {
                        task.setCompletedAt(now);
                    }
                } else {
                    task.setCompletedAt(null);
                }
                long id;
                if (task.getId() == 0) {
                    task.setCreatedAt(now);
                    task.setUpdatedAt(now);
                    id = taskDao.insert(task);
                    task.setId(id);
                } else {
                    task.setUpdatedAt(now);
                    taskDao.update(task);
                    id = task.getId();
                }
                ReminderScheduler.scheduleTask(appContext, task);
                result = OperationResult.success(id);
            } catch (Exception e) {
                result = OperationResult.failure(appContext.getString(R.string.error_save_failed));
            }
            final OperationResult finalResult = result;
            executors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    /** Thay đổi trạng thái công việc (Cần làm / Đang làm / Hoàn thành). */
    public void updateStatus(long taskId, @NonNull String newStatus, @Nullable Runnable onDone) {
        if (!Task.STATUS_TODO.equals(newStatus) && !Task.STATUS_IN_PROGRESS.equals(newStatus)
                && !Task.STATUS_DONE.equals(newStatus)) {
            return;
        }
        executors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            Long completedAt = Task.STATUS_DONE.equals(newStatus) ? now : null;
            taskDao.updateStatus(taskId, newStatus, completedAt, now);
            Task updated = taskDao.getByIdSync(taskId);
            if (updated != null) {
                ReminderScheduler.scheduleTask(appContext, updated);
            }
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    public void deleteTask(@NonNull Task task, @Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            taskDao.delete(task);
            ReminderScheduler.cancelTask(appContext, task.getId());
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }
}
