package com.example.lifemanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lifemanager.data.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Category category);

    @Update
    int update(Category category);

    @Delete
    int delete(Category category);

    /** Danh mục chi trước, danh mục thu sau. */
    @Query("SELECT * FROM categories "
            + "ORDER BY CASE WHEN type = 'EXPENSE' THEN 0 ELSE 1 END, id")
    LiveData<List<Category>> observeAll();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    LiveData<List<Category>> observeByType(String type);

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getByIdSync(long id);

    /** Đếm danh mục trùng tên (không phân biệt hoa thường) trong cùng loại, bỏ qua bản ghi excludeId. */
    @Query("SELECT COUNT(*) FROM categories "
            + "WHERE name = :name COLLATE NOCASE AND type = :type AND id != :excludeId")
    int countByName(String name, String type, long excludeId);

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    List<Category> getByTypeSync(String type);
}
