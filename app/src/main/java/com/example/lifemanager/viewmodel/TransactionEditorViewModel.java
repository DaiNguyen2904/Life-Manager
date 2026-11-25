package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionEntity;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.repository.FinanceRepository;
import com.example.lifemanager.utils.LiveDataUtils;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;

import java.time.LocalDate;
import java.util.List;

/**
 * ViewModel cho màn hình thêm / sửa giao dịch.
 * Giữ trạng thái form (loại, danh mục, ngày) để không mất khi xoay màn hình.
 */
public class TransactionEditorViewModel extends AndroidViewModel {

    private final FinanceRepository repository;
    private final MutableLiveData<String> type = new MutableLiveData<>(TransactionType.EXPENSE);
    private final MutableLiveData<Long> date = new MutableLiveData<>(LocalDate.now().toEpochDay());
    private final LiveData<List<Category>> categories;

    @Nullable
    private Long selectedCategoryId;
    @Nullable
    private TransactionEntity editing;
    private boolean loaded;

    public TransactionEditorViewModel(@NonNull Application application) {
        super(application);
        repository = FinanceRepository.getInstance(application);
        categories = LiveDataUtils.switchMap(LiveDataUtils.distinctUntilChanged(type),
                repository::observeCategoriesByType);
    }

    public LiveData<String> getType() {
        return type;
    }

    public LiveData<Long> getDate() {
        return date;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    @Nullable
    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(@Nullable Long id) {
        selectedCategoryId = id;
    }

    public void setType(@NonNull String newType) {
        if (!newType.equals(type.getValue())) {
            type.setValue(newType);
            selectedCategoryId = null;
        }
    }

    public void setDate(long epochDay) {
        date.setValue(epochDay);
    }

    @Nullable
    public TransactionEntity getEditing() {
        return editing;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Tải giao dịch cần sửa (chỉ tải một lần). */
    public void load(long id, @NonNull ResultCallback<TransactionEntity> callback) {
        repository.loadTransaction(id, entity -> {
            loaded = true;
            editing = entity;
            if (entity != null) {
                type.setValue(entity.getType());
                date.setValue(entity.getDate());
                selectedCategoryId = entity.getCategoryId();
            }
            callback.onResult(entity);
        });
    }

    public void save(@NonNull String title, long amount, @Nullable String note,
                     @NonNull ResultCallback<OperationResult> callback) {
        TransactionEntity entity = new TransactionEntity();
        if (editing != null) {
            entity.setId(editing.getId());
            entity.setCreatedAt(editing.getCreatedAt());
        }
        entity.setTitle(title);
        entity.setAmount(amount);
        entity.setType(type.getValue() == null ? TransactionType.EXPENSE : type.getValue());
        entity.setCategoryId(selectedCategoryId);
        entity.setDate(date.getValue() == null ? LocalDate.now().toEpochDay() : date.getValue());
        entity.setNote(note == null || note.trim().isEmpty() ? null : note.trim());
        repository.saveTransaction(entity, callback);
    }

    public void delete(@NonNull Runnable onDone) {
        if (editing != null) {
            repository.deleteTransaction(editing.getId(), onDone);
        }
    }
}
