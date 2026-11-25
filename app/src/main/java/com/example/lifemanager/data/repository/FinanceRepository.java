package com.example.lifemanager.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.lifemanager.R;
import com.example.lifemanager.data.dao.BudgetDao;
import com.example.lifemanager.data.dao.CategoryDao;
import com.example.lifemanager.data.dao.TransactionDao;
import com.example.lifemanager.data.database.AppDatabase;
import com.example.lifemanager.data.entity.Budget;
import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionEntity;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.model.TransactionFilter;
import com.example.lifemanager.data.model.TransactionWithCategory;
import com.example.lifemanager.utils.AppExecutors;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.SessionManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Repository quản lý dữ liệu tài chính: giao dịch, danh mục, ngân sách.
 * Mọi thao tác ghi được thực hiện ở background thread.
 */
public class FinanceRepository {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CATEGORY_NAME_LENGTH = 40;

    private static volatile FinanceRepository instance;

    /** Id tài khoản sở hữu dữ liệu mà Repository đang làm việc. */
    private final long ownerId;

    private final Context appContext;
    private final AppDatabase database;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final BudgetDao budgetDao;
    private final AppExecutors executors;

    private FinanceRepository(@NonNull Context context) {
        ownerId = SessionManager.getInstance(context).getCurrentUserId();
        appContext = context.getApplicationContext();
        database = AppDatabase.getInstance(appContext);
        transactionDao = database.transactionDao();
        categoryDao = database.categoryDao();
        budgetDao = database.budgetDao();
        executors = AppExecutors.getInstance();
    }

    /** Repository của tài khoản đang đăng nhập (tạo lại khi tài khoản thay đổi). */
    @NonNull
    public static FinanceRepository getInstance(@NonNull Context context) {
        long userId = SessionManager.getInstance(context).getCurrentUserId();
        FinanceRepository current = instance;
        if (current == null || current.ownerId != userId) {
            synchronized (FinanceRepository.class) {
                current = instance;
                if (current == null || current.ownerId != userId) {
                    current = new FinanceRepository(context);
                    instance = current;
                }
            }
        }
        return current;
    }

    // ------------------------------------------------------------------ Truy vấn

    @NonNull
    public LiveData<List<TransactionWithCategory>> filterTransactions(@NonNull TransactionFilter filter) {
        return transactionDao.filter(filter.type, filter.categoryId, filter.getFromDay(), filter.getToDay());
    }

    @NonNull
    public LiveData<TransactionWithCategory> observeTransaction(long id) {
        return transactionDao.observeById(id);
    }

    @NonNull
    public LiveData<Long> observeTotal(@NonNull String type, long fromDay, long toDay) {
        return transactionDao.observeTotal(type, fromDay, toDay);
    }

    @NonNull
    public LiveData<Long> observeBalance() {
        return transactionDao.observeBalance();
    }

    @NonNull
    public LiveData<List<CategorySpending>> observeCategorySpending(long fromDay, long toDay) {
        return transactionDao.observeCategorySpending(fromDay, toDay);
    }

    @NonNull
    public LiveData<List<Category>> observeCategories() {
        return categoryDao.observeAll();
    }

    @NonNull
    public LiveData<List<Category>> observeCategoriesByType(@NonNull String type) {
        return categoryDao.observeByType(type);
    }

    public void loadTransaction(long id, @NonNull ResultCallback<TransactionEntity> callback) {
        executors.diskIO().execute(() -> {
            TransactionEntity entity = transactionDao.getByIdSync(id);
            executors.mainThread().execute(() -> callback.onResult(entity));
        });
    }

    // ------------------------------------------------------------------ Giao dịch

    /** Kiểm tra dữ liệu giao dịch, trả về thông báo lỗi hoặc null nếu hợp lệ. */
    @Nullable
    public String validateTransaction(@NonNull TransactionEntity t) {
        String title = t.getTitle().trim();
        if (title.isEmpty()) {
            return appContext.getString(R.string.error_title_required);
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return appContext.getString(R.string.error_title_too_long, MAX_TITLE_LENGTH);
        }
        if (t.getAmount() <= 0) {
            return appContext.getString(R.string.error_amount_invalid);
        }
        if (t.getAmount() > CurrencyUtils.MAX_AMOUNT) {
            return appContext.getString(R.string.error_amount_too_large);
        }
        if (!TransactionType.isValid(t.getType())) {
            return appContext.getString(R.string.error_type_invalid);
        }
        if (t.getCategoryId() == null) {
            return appContext.getString(R.string.error_category_required);
        }
        return null;
    }

    public void saveTransaction(@NonNull TransactionEntity transaction,
                                @NonNull ResultCallback<OperationResult> callback) {
        transaction.setTitle(transaction.getTitle().trim());
        String error = validateTransaction(transaction);
        if (error != null) {
            callback.onResult(OperationResult.failure(error));
            return;
        }
        executors.diskIO().execute(() -> {
            OperationResult result;
            try {
                Category category = categoryDao.getByIdSync(transaction.getCategoryId());
                if (category == null || !category.getType().equals(transaction.getType())) {
                    result = OperationResult.failure(appContext.getString(R.string.error_category_required));
                } else {
                    long now = System.currentTimeMillis();
                    long id;
                    if (transaction.getId() == 0) {
                        transaction.setCreatedAt(now);
                        transaction.setUpdatedAt(now);
                        id = transactionDao.insert(transaction);
                    } else {
                        transaction.setUpdatedAt(now);
                        transactionDao.update(transaction);
                        id = transaction.getId();
                    }
                    String warning = null;
                    if (!transaction.isIncome()) {
                        warning = checkBudgetWarning(category, transaction.getDate());
                    }
                    result = OperationResult.successWithWarning(id, warning);
                }
            } catch (Exception e) {
                result = OperationResult.failure(appContext.getString(R.string.error_save_failed));
            }
            final OperationResult finalResult = result;
            executors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    /** Trả về cảnh báo nếu chi tiêu của danh mục trong tháng đã vượt ngân sách. */
    @Nullable
    private String checkBudgetWarning(@NonNull Category category, long epochDay) {
        Budget budget = budgetDao.getByCategorySync(category.getId());
        if (budget == null || budget.getAmount() <= 0) {
            return null;
        }
        YearMonth month = YearMonth.from(LocalDate.ofEpochDay(epochDay));
        long spent = transactionDao.getCategoryExpenseSync(category.getId(),
                month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay());
        if (spent > budget.getAmount()) {
            return appContext.getString(R.string.warning_budget_exceeded, category.getName(),
                    CurrencyUtils.format(spent), CurrencyUtils.format(budget.getAmount()));
        }
        return null;
    }

    public void deleteTransaction(long id, @Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            transactionDao.deleteById(id);
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    // ------------------------------------------------------------------ Danh mục

    /**
     * Thêm / sửa danh mục.
     *
     * @param budgetAmount ngân sách tháng cho danh mục chi (null hoặc 0 = không đặt ngân sách).
     */
    public void saveCategory(@NonNull Category category, @Nullable Long budgetAmount,
                             @NonNull ResultCallback<OperationResult> callback) {
        String name = category.getName().trim();
        category.setName(name);
        if (name.isEmpty()) {
            callback.onResult(OperationResult.failure(appContext.getString(R.string.error_name_required)));
            return;
        }
        if (name.length() > MAX_CATEGORY_NAME_LENGTH) {
            callback.onResult(OperationResult.failure(
                    appContext.getString(R.string.error_title_too_long, MAX_CATEGORY_NAME_LENGTH)));
            return;
        }
        if (!TransactionType.isValid(category.getType())) {
            callback.onResult(OperationResult.failure(appContext.getString(R.string.error_type_invalid)));
            return;
        }
        if (budgetAmount != null && (budgetAmount < 0 || budgetAmount > CurrencyUtils.MAX_AMOUNT)) {
            callback.onResult(OperationResult.failure(appContext.getString(R.string.error_amount_too_large)));
            return;
        }
        executors.diskIO().execute(() -> {
            OperationResult result;
            try {
                if (categoryDao.countByName(name, category.getType(), category.getId()) > 0) {
                    result = OperationResult.failure(appContext.getString(R.string.error_category_exists, name));
                } else {
                    long[] idHolder = new long[1];
                    database.runInTransaction(() -> {
                        long id;
                        if (category.getId() == 0) {
                            category.setCreatedAt(System.currentTimeMillis());
                            id = categoryDao.insert(category);
                        } else {
                            categoryDao.update(category);
                            id = category.getId();
                        }
                        if (category.isExpense()) {
                            if (budgetAmount != null && budgetAmount > 0) {
                                budgetDao.upsert(new Budget(id, budgetAmount));
                            } else {
                                budgetDao.deleteByCategory(id);
                            }
                        }
                        idHolder[0] = id;
                    });
                    result = OperationResult.success(idHolder[0]);
                }
            } catch (SQLiteConstraintException e) {
                result = OperationResult.failure(appContext.getString(R.string.error_category_exists, name));
            } catch (Exception e) {
                result = OperationResult.failure(appContext.getString(R.string.error_save_failed));
            }
            final OperationResult finalResult = result;
            executors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    /** Xóa danh mục: ngân sách bị xóa theo (CASCADE), giao dịch chuyển về "không có danh mục" (SET NULL). */
    public void deleteCategory(@NonNull Category category, @Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            categoryDao.delete(category);
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }

    // ------------------------------------------------------------------ Ngân sách

    public void saveBudget(long categoryId, long amount, @NonNull ResultCallback<OperationResult> callback) {
        if (amount <= 0) {
            callback.onResult(OperationResult.failure(appContext.getString(R.string.error_amount_invalid)));
            return;
        }
        if (amount > CurrencyUtils.MAX_AMOUNT) {
            callback.onResult(OperationResult.failure(appContext.getString(R.string.error_amount_too_large)));
            return;
        }
        executors.diskIO().execute(() -> {
            OperationResult result;
            Category category = categoryDao.getByIdSync(categoryId);
            if (category == null || !category.isExpense()) {
                result = OperationResult.failure(appContext.getString(R.string.error_category_required));
            } else {
                long id = budgetDao.upsert(new Budget(categoryId, amount));
                result = OperationResult.success(id);
            }
            final OperationResult finalResult = result;
            executors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    public void deleteBudget(long categoryId, @Nullable Runnable onDone) {
        executors.diskIO().execute(() -> {
            budgetDao.deleteByCategory(categoryId);
            if (onDone != null) {
                executors.mainThread().execute(onDone);
            }
        });
    }
}
