package com.example.lifemanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lifemanager.data.entity.TransactionEntity;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.model.TransactionWithCategory;

import java.util.List;

@Dao
public interface TransactionDao {

    String SELECT_WITH_CATEGORY = "SELECT t.*, c.name AS category_name, c.color AS category_color, "
            + "c.icon AS category_icon FROM transactions t "
            + "LEFT JOIN categories c ON t.category_id = c.id ";

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(TransactionEntity transaction);

    @Update
    int update(TransactionEntity transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    TransactionEntity getByIdSync(long id);

    @Query(SELECT_WITH_CATEGORY + "WHERE t.id = :id LIMIT 1")
    LiveData<TransactionWithCategory> observeById(long id);

    /**
     * Lọc giao dịch theo loại, danh mục và khoảng ngày.
     * type = '' nghĩa là tất cả; categoryId = -1 nghĩa là tất cả danh mục.
     * Tìm kiếm theo từ khóa (không dấu) được xử lý ở ViewModel.
     */
    @Query(SELECT_WITH_CATEGORY
            + "WHERE (:type = '' OR t.type = :type) "
            + "AND (:categoryId = -1 OR t.category_id = :categoryId) "
            + "AND t.date BETWEEN :fromDay AND :toDay "
            + "ORDER BY t.date DESC, t.id DESC")
    LiveData<List<TransactionWithCategory>> filter(String type, long categoryId, long fromDay, long toDay);

    /** Tổng tiền theo loại giao dịch trong khoảng ngày. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions "
            + "WHERE type = :type AND date BETWEEN :fromDay AND :toDay")
    LiveData<Long> observeTotal(String type, long fromDay, long toDay);

    /** Số dư hiện tại = tổng thu - tổng chi (toàn bộ thời gian). */
    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) "
            + "FROM transactions")
    LiveData<Long> observeBalance();

    /** Tổng chi của một danh mục trong khoảng ngày (dùng để cảnh báo vượt ngân sách). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions "
            + "WHERE type = 'EXPENSE' AND category_id = :categoryId AND date BETWEEN :fromDay AND :toDay")
    long getCategoryExpenseSync(long categoryId, long fromDay, long toDay);

    /** Chi tiêu theo từng danh mục chi trong khoảng ngày, kèm ngân sách. */
    @Query("SELECT c.id AS category_id, c.name AS name, c.color AS color, c.icon AS icon, "
            + "COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.category_id = c.id "
            + "AND t.type = 'EXPENSE' AND t.date BETWEEN :fromDay AND :toDay), 0) AS spent, "
            + "b.amount AS budget "
            + "FROM categories c LEFT JOIN budgets b ON b.category_id = c.id "
            + "WHERE c.type = 'EXPENSE' ORDER BY c.id")
    LiveData<List<CategorySpending>> observeCategorySpending(long fromDay, long toDay);
}
