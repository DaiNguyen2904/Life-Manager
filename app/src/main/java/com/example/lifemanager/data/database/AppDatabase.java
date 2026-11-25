package com.example.lifemanager.data.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.lifemanager.data.dao.BudgetDao;
import com.example.lifemanager.data.dao.CategoryDao;
import com.example.lifemanager.data.dao.HabitDao;
import com.example.lifemanager.data.dao.HabitLogDao;
import com.example.lifemanager.data.dao.NoteDao;
import com.example.lifemanager.data.dao.TaskDao;
import com.example.lifemanager.data.dao.TransactionDao;
import com.example.lifemanager.data.entity.Budget;
import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.HabitLog;
import com.example.lifemanager.data.entity.Note;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.entity.TransactionEntity;
import com.example.lifemanager.utils.SessionManager;

import java.io.File;

/**
 * Cơ sở dữ liệu Room (SQLite) chứa dữ liệu cá nhân của tài khoản đang đăng nhập - Singleton.
 * <p>
 * Mỗi tài khoản có một tệp riêng {@code life_manager_u<id>.db} nên dữ liệu của các tài khoản
 * trên cùng thiết bị tách biệt hoàn toàn. Khi tài khoản đăng nhập thay đổi, CSDL của tài khoản
 * trước được đóng và CSDL của tài khoản mới được mở.
 */
@Database(entities = {
        TransactionEntity.class,
        Category.class,
        Budget.class,
        Task.class,
        Habit.class,
        HabitLog.class,
        Note.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** Tên tệp CSDL của phiên bản chưa có tài khoản. */
    private static final String LEGACY_DATABASE_NAME = "life_manager.db";
    private static final String[] SQLITE_FILE_SUFFIXES = {"", "-wal", "-shm", "-journal"};

    private static volatile AppDatabase instance;

    /** Id tài khoản sở hữu CSDL đang mở. */
    private long ownerId;

    public abstract TransactionDao transactionDao();

    public abstract CategoryDao categoryDao();

    public abstract BudgetDao budgetDao();

    public abstract TaskDao taskDao();

    public abstract HabitDao habitDao();

    public abstract HabitLogDao habitLogDao();

    public abstract NoteDao noteDao();

    /**
     * CSDL của tài khoản đang đăng nhập.
     *
     * @throws IllegalStateException nếu chưa có tài khoản nào đăng nhập.
     */
    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        long userId = SessionManager.getInstance(context).getCurrentUserId();
        if (userId <= 0) {
            throw new IllegalStateException("Chưa đăng nhập: không thể mở dữ liệu cá nhân");
        }
        AppDatabase current = instance;
        if (current != null && current.ownerId == userId) {
            return current;
        }
        synchronized (AppDatabase.class) {
            current = instance;
            if (current != null && current.ownerId == userId) {
                return current;
            }
            if (current != null) {
                // Đã chuyển sang tài khoản khác: đóng CSDL của tài khoản trước
                current.close();
            }
            current = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, databaseNameFor(userId))
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Tạo dữ liệu mặc định ngay khi database được tạo lần đầu
                            DatabaseSeeder.seed(db);
                        }
                    })
                    .build();
            current.ownerId = userId;
            instance = current;
            return current;
        }
    }

    @NonNull
    public static String databaseNameFor(long userId) {
        return "life_manager_u" + userId + ".db";
    }

    /**
     * Chuyển CSDL của phiên bản chưa có tài khoản (life_manager.db) thành CSDL của tài khoản
     * đầu tiên để người dùng không mất dữ liệu sau khi cập nhật ứng dụng. Gọi ở background thread,
     * trước khi CSDL của tài khoản được mở.
     *
     * @return true nếu đã chuyển dữ liệu.
     */
    public static boolean adoptLegacyDatabase(@NonNull Context context, long userId) {
        File legacy = context.getDatabasePath(LEGACY_DATABASE_NAME);
        File target = context.getDatabasePath(databaseNameFor(userId));
        if (!legacy.exists() || target.exists()) {
            return false;
        }
        // Mở rồi đóng CSDL cũ để SQLite ghi toàn bộ nhật ký WAL vào tệp chính trước khi đổi tên
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(legacy.getPath(), null,
                SQLiteDatabase.OPEN_READWRITE)) {
            try (Cursor cursor = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) {
                cursor.moveToFirst();
            }
        } catch (RuntimeException e) {
            return false;
        }
        for (String suffix : SQLITE_FILE_SUFFIXES) {
            File from = new File(legacy.getPath() + suffix);
            if (from.exists() && !from.renameTo(new File(target.getPath() + suffix))) {
                return false;
            }
        }
        return target.exists();
    }

    /**
     * Xóa toàn bộ dữ liệu và khởi tạo lại dữ liệu mặc định.
     * Phải gọi ở background thread.
     */
    public void resetAllData() {
        clearAllTables();
        runInTransaction(() -> DatabaseSeeder.seed(getOpenHelper().getWritableDatabase()));
    }
}
