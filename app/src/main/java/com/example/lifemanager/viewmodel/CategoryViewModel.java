package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.repository.FinanceRepository;
import com.example.lifemanager.utils.LiveDataUtils;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel cho màn hình Quản lý danh mục.
 */
public class CategoryViewModel extends AndroidViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<String> selectedType = new MutableLiveData<>(TransactionType.EXPENSE);
    private final LiveData<List<Category>> categories;
    /** categoryId -> ngân sách tháng. */
    private final LiveData<Map<Long, Long>> budgets;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = FinanceRepository.getInstance(application);
        categories = LiveDataUtils.switchMap(selectedType, repository::observeCategoriesByType);
        YearMonth month = YearMonth.now();
        LiveData<List<CategorySpending>> spending = repository.observeCategorySpending(
                month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay());
        budgets = LiveDataUtils.map(spending, list -> {
            Map<Long, Long> map = new HashMap<>();
            if (list != null) {
                for (CategorySpending item : list) {
                    if (item.hasBudget()) {
                        map.put(item.categoryId, item.budget);
                    }
                }
            }
            return map;
        });
    }

    public LiveData<String> getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(@NonNull String type) {
        if (!type.equals(selectedType.getValue())) {
            selectedType.setValue(type);
        }
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Map<Long, Long>> getBudgets() {
        return budgets;
    }

    public void saveCategory(@NonNull Category category, @Nullable Long budget,
                             @NonNull ResultCallback<OperationResult> callback) {
        repository.saveCategory(category, budget, callback);
    }

    public void deleteCategory(@NonNull Category category) {
        repository.deleteCategory(category, null);
    }
}
