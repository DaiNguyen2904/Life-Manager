package com.example.lifemanager.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.lifemanager.data.entity.Budget;

@Dao
public interface BudgetDao {

    /** Thêm hoặc thay thế ngân sách (mỗi danh mục chỉ có một ngân sách). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Budget budget);

    @Query("DELETE FROM budgets WHERE category_id = :categoryId")
    int deleteByCategory(long categoryId);

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId LIMIT 1")
    Budget getByCategorySync(long categoryId);
}
