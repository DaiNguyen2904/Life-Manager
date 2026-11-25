package com.example.lifemanager.data.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.data.entity.TransactionType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Khởi tạo dữ liệu mặc định khi tạo database lần đầu hoặc sau khi xóa dữ liệu:
 * - Danh mục chi / thu mặc định.
 * - Ngân sách mặc định cho một số danh mục chi.
 * - 4 thói quen mặc định.
 * <p>
 * Không tạo giao dịch / công việc giả: các dữ liệu đó do người dùng tự nhập.
 */
public final class DatabaseSeeder {

    /** {tên, màu, icon} của danh mục chi mặc định. */
    private static final Object[][] DEFAULT_EXPENSE_CATEGORIES = {
            {"Di chuyển", 0xFF42A5F5, "car"},
            {"Giải trí", 0xFFAB47BC, "movie"},
            {"Hóa đơn & Tiện ích", 0xFFFFA726, "receipt"},
            {"Mua sắm", 0xFFEC407A, "cart"},
            {"Sức khỏe", 0xFF66BB6A, "health"},
            {"Ăn uống", 0xFFEF5350, "food"},
            {"Giáo dục", 0xFF5C6BC0, "school"},
            {"Khác", 0xFF78909C, "category"}
    };

    /** {tên, màu, icon} của danh mục thu mặc định. */
    private static final Object[][] DEFAULT_INCOME_CATEGORIES = {
            {"Lương", 0xFF26A69A, "work"},
            {"Thưởng", 0xFFFFCA28, "gift"},
            {"Đầu tư", 0xFF29B6F6, "trending"},
            {"Thu nhập khác", 0xFF8D6E63, "money"}
    };

    /** {tên danh mục, ngân sách} mặc định (VNĐ / tháng). */
    private static final Object[][] DEFAULT_BUDGETS = {
            {"Di chuyển", 1_500_000L},
            {"Giải trí", 1_000_000L},
            {"Hóa đơn & Tiện ích", 2_000_000L},
            {"Mua sắm", 3_000_000L},
            {"Sức khỏe", 1_000_000L},
            {"Ăn uống", 5_000_000L}
    };

    /** {tên, mô tả, mục tiêu, đơn vị} của thói quen mặc định. */
    private static final Object[][] DEFAULT_HABITS = {
            {"Tập thể dục 30 phút", "Vận động để nâng cao sức khỏe", 30, "phút"},
            {"Đọc sách 20 trang", "Đọc sách mỗi ngày để mở mang kiến thức", 20, "trang"},
            {"Uống 2L nước", "Uống đủ nước mỗi ngày", 2, "lít"},
            {"Học lập trình / Kỹ năng", "Dành thời gian học kỹ năng mới", 60, "phút"}
    };

    private DatabaseSeeder() {
    }

    public static void seed(@NonNull SupportSQLiteDatabase db) {
        long now = System.currentTimeMillis();
        long today = LocalDate.now().toEpochDay();

        Map<String, Long> categoryIds = new HashMap<>();
        insertCategories(db, DEFAULT_EXPENSE_CATEGORIES, TransactionType.EXPENSE, now, categoryIds);
        insertCategories(db, DEFAULT_INCOME_CATEGORIES, TransactionType.INCOME, now, categoryIds);

        for (Object[] row : DEFAULT_BUDGETS) {
            Long categoryId = categoryIds.get((String) row[0]);
            if (categoryId == null || categoryId <= 0) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put("category_id", categoryId);
            values.put("amount", (Long) row[1]);
            values.put("updated_at", now);
            db.insert("budgets", SQLiteDatabase.CONFLICT_IGNORE, values);
        }

        for (Object[] row : DEFAULT_HABITS) {
            ContentValues values = new ContentValues();
            values.put("name", (String) row[0]);
            values.put("description", (String) row[1]);
            values.put("daily_target", (Integer) row[2]);
            values.put("target_unit", (String) row[3]);
            values.putNull("reminder_minutes");
            values.put("start_date", today);
            values.put("frequency_mask", Habit.MASK_DAILY);
            values.put("created_at", now);
            db.insert("habits", SQLiteDatabase.CONFLICT_IGNORE, values);
        }
    }

    private static void insertCategories(@NonNull SupportSQLiteDatabase db, @NonNull Object[][] rows,
                                         @NonNull String type, long now,
                                         @NonNull Map<String, Long> outIds) {
        for (Object[] row : rows) {
            ContentValues values = new ContentValues();
            values.put("name", (String) row[0]);
            values.put("type", type);
            values.put("color", (Integer) row[1]);
            values.put("icon", (String) row[2]);
            values.put("is_default", 1);
            values.put("created_at", now);
            long id = db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values);
            outIds.put((String) row[0], id);
        }
    }
}
