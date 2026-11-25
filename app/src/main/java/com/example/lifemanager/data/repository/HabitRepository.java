package com.example.lifemanager.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lifemanager.R;
import com.example.lifemanager.data.dao.HabitDao;
import com.example.lifemanager.data.dao.HabitLogDao;
import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.HabitLog;
import com.example.lifemanager.notification.ReminderScheduler;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.SessionManager;

import java.time.LocalDate;

/**
 * Repository quản lý thói quen và lịch sử hoàn thành thói quen.
 */
public class HabitRepository {

    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_TARGET = 100_000;

    private static volatile HabitRepository instance;

    /** Id tài khoản sở hữu dữ liệu mà Repository đang làm việc. */
    private final long ownerId;

    private final Context appContext;
    private final HabitDao habitDao;
    private final HabitLogDao habitLogDao;
    private final AppExecutors executors;

    private HabitRepository(@NonNull Context context) {
        ownerId = SessionManager.getInstance(context).getCurrentUserId();
        appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        habitDao = db.habitDao();
        habitLogDao = db.habitLogDao();
        executors = AppExecutors.getInstance();
    }

    /** Repository của tài khoản đang đăng nhập (tạo lại khi tài khoản thay đổi). */
    @NonNull
    public static HabitRepository getInstance(@NonNull Context context) {
        long userId = SessionManager.getInstance(context).getCurrentUserId();
        HabitRepository current = instance;
        if (current == null || current.ownerId != userId) {
            synchronized (HabitRepository.class) {
                current = instance;
                if (current == null || current.ownerId != userId) {
                    current = new HabitRepository(context);
                    instance = current;
                }
            }
        }
        return current;
    }

    /** Tạo LiveData chứa thói quen + lịch sử + chỉ số Streak (tính ở background). */
    @NonNull
    public HabitSnapshotLiveData createSnapshotLiveData() {
        return new HabitSnapshotLiveData(habitDao.observeAll(), habitLogDao.observeAll(),
                executors.diskIO());
    }

    public void loadHabit(long id, @NonNull ResultCallback<Habit> callback) {
        executors.diskIO().execute(() -> {
            Habit habit = habitDao.getByIdSync(id);
            executors.mainThread().execute(() -> callback.onResult(habit));
        });
    }

    /**
     * Đánh dấu / bỏ đánh dấu hoàn thành thói quen cho một ngày.
     * Unique index (habit_id, date) + IGNORE đảm bảo không ghi nhận trùng trong cùng ngày.
     */
    public void setCompleted(long habitId, long epochDay, boolean completed, @Nullable Runnable onDone) {
        if (epochDay > LocalDate.now().toEpochDay()) {
            return; // Không cho phép đánh dấu trước cho ngày tương lai
        }
        executors.diskIO().execute(() -> {
            if (completed) {
                habitLogDao.insert(new HabitLog(habitId, epochDay));
            } else {
                habitLogDao.delete(habitId, epochDay);
            }
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    @Nullable
    public String validate(@NonNull Habit habit) {
        String name = habit.getName().trim();
        if (name.isEmpty()) {
            return appContext.getString(R.string.error_name_required);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return appContext.getString(R.string.error_title_too_long, MAX_NAME_LENGTH);
        }
        if (habit.getDailyTarget() <= 0 || habit.getDailyTarget() > MAX_TARGET) {
            return appContext.getString(R.string.error_target_invalid);
        }
        if ((habit.getFrequencyMask() & Habit.MASK_DAILY) == 0) {
            return appContext.getString(R.string.error_frequency_required);
        }
        Integer minutes = habit.getReminderMinutes();
        if (minutes != null && (minutes < 0 || minutes >= 24 * 60)) {
            habit.setReminderMinutes(null);
        }
        return null;
    }

    public void saveHabit(@NonNull Habit habit, @NonNull ResultCallback<OperationResult> callback) {
        habit.setName(habit.getName().trim());
        String error = validate(habit);
        if (error != null) {
            callback.onResult(OperationResult.failure(error));
            return;
        }
        executors.diskIO().execute(() -> {
            OperationResult result;
            try {
                if (habitDao.countByName(habit.getName(), habit.getId()) > 0) {
                    result = OperationResult.failure(
                            appContext.getString(R.string.error_habit_exists, habit.getName()));
                } else {
                    long id;
                    if (habit.getId() == 0) {
                        habit.setCreatedAt(System.currentTimeMillis());
                        id = habitDao.insert(habit);
                        habit.setId(id);
                    } else {
                        habitDao.update(habit);
                        id = habit.getId();
                    }
                    ReminderScheduler.scheduleHabit(appContext, habit);
                    result = OperationResult.success(id);
                }
            } catch (SQLiteConstraintException e) {
                result = OperationResult.failure(
                        appContext.getString(R.string.error_habit_exists, habit.getName()));
            } catch (Exception e) {
                result = OperationResult.failure(appContext.getString(R.string.error_save_failed));
            }
            final OperationResult finalResult = result;
            executors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    /** Xóa thói quen (lịch sử hoàn thành bị xóa theo nhờ CASCADE). */
    public void deleteHabit(@NonNull Habit habit, @Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            habitDao.delete(habit);
            ReminderScheduler.cancelHabit(appContext, habit.getId());
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }
}
