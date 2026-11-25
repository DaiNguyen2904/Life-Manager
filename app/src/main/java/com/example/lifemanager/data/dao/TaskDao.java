package com.example.lifemanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lifemanager.data.entity.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Task task);

    @Update
    int update(Task task);

    @Delete
    int delete(Task task);

    /** Sắp xếp mặc định: có hạn trước, hạn gần trước, ưu tiên cao trước. */
    @Query("SELECT * FROM tasks ORDER BY CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, "
            + "due_date ASC, priority DESC, id DESC")
    LiveData<List<Task>> observeAll();

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    LiveData<Task> observeById(long id);

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    Task getByIdSync(long id);

    @Query("UPDATE tasks SET status = :status, completed_at = :completedAt, updated_at = :updatedAt "
            + "WHERE id = :id")
    int updateStatus(long id, String status, Long completedAt, long updatedAt);

    /** Công việc chưa hoàn thành có hạn đến trước hoặc bằng ngày chỉ định (dùng cho thông báo). */
    @Query("SELECT * FROM tasks WHERE status != 'DONE' AND due_date IS NOT NULL "
            + "AND due_date <= :untilDay ORDER BY due_date ASC")
    List<Task> getUnfinishedDueUntilSync(long untilDay);

    /** Công việc chưa hoàn thành có thời điểm nhắc nhở trong tương lai. */
    @Query("SELECT * FROM tasks WHERE status != 'DONE' AND reminder_at IS NOT NULL "
            + "AND reminder_at > :now")
    List<Task> getUpcomingRemindersSync(long now);

    @Query("SELECT * FROM tasks WHERE reminder_at IS NOT NULL")
    List<Task> getAllWithReminderSync();
}
