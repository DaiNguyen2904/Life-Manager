package com.example.lifemanager.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.lifemanager.data.dao.NoteDao;
import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.Note;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.SessionManager;

import java.util.List;

/**
 * Repository quản lý ghi chú nhanh.
 */
public class NoteRepository {

    public static final int MAX_CONTENT_LENGTH = 5000;

    private static volatile NoteRepository instance;

    /** Id tài khoản sở hữu dữ liệu mà Repository đang làm việc. */
    private final long ownerId;

    private final NoteDao noteDao;
    private final AppExecutors executors;

    private NoteRepository(@NonNull Context context) {
        ownerId = SessionManager.getInstance(context).getCurrentUserId();
        noteDao = AppDatabase.getInstance(context.getApplicationContext()).noteDao();
        executors = AppExecutors.getInstance();
    }

    /** Repository của tài khoản đang đăng nhập (tạo lại khi tài khoản thay đổi). */
    @NonNull
    public static NoteRepository getInstance(@NonNull Context context) {
        long userId = SessionManager.getInstance(context).getCurrentUserId();
        NoteRepository current = instance;
        if (current == null || current.ownerId != userId) {
            synchronized (NoteRepository.class) {
                current = instance;
                if (current == null || current.ownerId != userId) {
                    current = new NoteRepository(context);
                    instance = current;
                }
            }
        }
        return current;
    }

    @NonNull
    public LiveData<List<Note>> observeAll() {
        return noteDao.observeAll();
    }

    public void load(long id, @NonNull ResultCallback<Note> callback) {
        executors.diskIO().execute(() -> {
            Note note = noteDao.getByIdSync(id);
            executors.mainThread().execute(() -> callback.onResult(note));
        });
    }

    /** Thêm ghi chú mới, trả về id qua callback. */
    public void insert(@NonNull String content, @NonNull ResultCallback<Long> callback) {
        final String safe = limit(content);
        executors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            Note note = new Note();
            note.setContent(safe);
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
            long id = noteDao.insert(note);
            executors.mainThread().execute(() -> callback.onResult(id));
        });
    }

    public void updateContent(long id, @NonNull String content, @Nullable Runnable onDone) {
        final String safe = limit(content);
        executors.diskIO().execute(() -> {
            noteDao.updateContent(id, safe, System.currentTimeMillis());
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    public void delete(long id, @Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            noteDao.deleteById(id);
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    @NonNull
    private static String limit(@NonNull String content) {
        return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
    }
}
