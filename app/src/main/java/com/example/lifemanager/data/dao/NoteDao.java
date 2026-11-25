package com.example.lifemanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.lifemanager.data.entity.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Note note);

    @Query("UPDATE notes SET content = :content, updated_at = :updatedAt WHERE id = :id")
    int updateContent(long id, String content, long updatedAt);

    @Query("DELETE FROM notes WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    LiveData<List<Note>> observeAll();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getByIdSync(long id);
}
