package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.model.TransactionFilter;
import com.example.lifemanager.data.model.TransactionWithCategory;
import com.example.lifemanager.data.repository.FinanceRepository;
import com.example.lifemanager.utils.LiveDataUtils;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.TextNormalizer;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel cho màn hình Thu Chi: tổng quan tháng, ngân sách, lịch sử giao dịch có tìm kiếm / lọc.
 */
public class FinanceViewModel extends AndroidViewModel {

    private final FinanceRepository repository;

    private final MutableLiveData<YearMonth> currentMonth = new MutableLiveData<>(YearMonth.now());
    private final MutableLiveData<TransactionFilter> filter =
            new MutableLiveData<>(TransactionFilter.defaultFilter());

    private final LiveData<Long> monthIncome;
    private final LiveData<Long> monthExpense;
    private final LiveData<List<CategorySpending>> categorySpending;
    private final LiveData<List<CategorySpending>> budgetItems;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<Category>> expenseCategories;
    private final MediatorLiveData<List<TransactionWithCategory>> transactions = new MediatorLiveData<>();
    private List<TransactionWithCategory> lastQueryResult;

    public FinanceViewModel(@NonNull Application application) {
        super(application);
        repository = FinanceRepository.getInstance(application);

        monthIncome = LiveDataUtils.switchMap(currentMonth, ym -> repository.observeTotal(
                TransactionType.INCOME, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        monthExpense = LiveDataUtils.switchMap(currentMonth, ym -> repository.observeTotal(
                TransactionType.EXPENSE, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        categorySpending = LiveDataUtils.switchMap(currentMonth, ym -> repository.observeCategorySpending(
                ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        budgetItems = LiveDataUtils.map(categorySpending, list -> {
            List<CategorySpending> result = new ArrayList<>();
            if (list != null) {
                for (CategorySpending item : list) {
                    if (item.hasBudget()) {
                        result.add(item);
                    }
                }
            }
            return result;
        });
        allCategories = repository.observeCategories();
        expenseCategories = repository.observeCategoriesByType(TransactionType.EXPENSE);

        // Chỉ truy vấn lại database khi các điều kiện lọc (ngoài từ khóa) thay đổi
        LiveData<TransactionFilter> dbFilter = LiveDataUtils.distinctUntilChanged(
                LiveDataUtils.map(filter, f -> f.withQuery("")));
        LiveData<List<TransactionWithCategory>> queryResult =
                LiveDataUtils.switchMap(dbFilter, repository::filterTransactions);
        transactions.addSource(queryResult, list -> {
            lastQueryResult = list;
            applySearch();
        });
        transactions.addSource(filter, f -> applySearch());
    }

    private void applySearch() {
        TransactionFilter f = filter.getValue();
        if (lastQueryResult == null || f == null) {
            return;
        }
        String query = TextNormalizer.normalize(f.query);
        if (query.isEmpty()) {
            transactions.setValue(lastQueryResult);
            return;
        }
        List<TransactionWithCategory> result = new ArrayList<>();
        for (TransactionWithCategory item : lastQueryResult) {
            if (TextNormalizer.matches(query, item.transaction.getTitle(), item.transaction.getNote(),
                    item.categoryName)) {
                result.add(item);
            }
        }
        transactions.setValue(result);
    }

    // ------------------------------------------------------------------ Getter

    public LiveData<YearMonth> getCurrentMonth() {
        return currentMonth;
    }

    public LiveData<Long> getMonthIncome() {
        return monthIncome;
    }

    public LiveData<Long> getMonthExpense() {
        return monthExpense;
    }

    public LiveData<List<CategorySpending>> getBudgetItems() {
        return budgetItems;
    }

    public LiveData<List<CategorySpending>> getCategorySpending() {
        return categorySpending;
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<Category>> getExpenseCategories() {
        return expenseCategories;
    }

    public LiveData<List<TransactionWithCategory>> getTransactions() {
        return transactions;
    }

    public LiveData<TransactionFilter> getFilter() {
        return filter;
    }

    @NonNull
    private TransactionFilter currentFilter() {
        TransactionFilter f = filter.getValue();
        return f == null ? TransactionFilter.defaultFilter() : f;
    }

    // ------------------------------------------------------------------ Bộ lọc

    public void setQuery(@NonNull String query) {
        TransactionFilter updated = currentFilter().withQuery(query);
        if (!updated.equals(filter.getValue())) {
            filter.setValue(updated);
        }
    }

    public void setType(@NonNull String type) {
        filter.setValue(currentFilter().withType(type));
    }

    public void setCategory(long categoryId) {
        filter.setValue(currentFilter().withCategory(categoryId));
    }

    public void setPeriod(@NonNull TransactionFilter.PeriodMode mode) {
        filter.setValue(currentFilter().withPeriod(mode));
    }

    public void setDay(long epochDay) {
        filter.setValue(currentFilter().withDay(epochDay));
    }

    public void setMonth(@NonNull YearMonth month) {
        filter.setValue(currentFilter().withMonth(month));
    }

    public void resetFilter() {
        filter.setValue(TransactionFilter.defaultFilter().withQuery(currentFilter().query));
    }

    /** Cập nhật tháng hiện tại khi sang tháng mới. */
    public void refreshMonth() {
        YearMonth now = YearMonth.now();
        if (!now.equals(currentMonth.getValue())) {
            currentMonth.setValue(now);
        }
    }

    // ------------------------------------------------------------------ Thao tác dữ liệu

    public void deleteTransaction(long id, @Nullable Runnable onDone) {
        repository.deleteTransaction(id, onDone);
    }

    public void saveBudget(long categoryId, long amount, @NonNull ResultCallback<OperationResult> callback) {
        repository.saveBudget(categoryId, amount, callback);
    }

    public void deleteBudget(long categoryId) {
        repository.deleteBudget(categoryId, null);
    }
}
