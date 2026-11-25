package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.model.DailyHabitRate;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.data.repository.FinanceRepository;
import com.example.lifemanager.data.repository.HabitRepository;
import com.example.lifemanager.data.repository.HabitSnapshotLiveData;
import com.example.lifemanager.data.repository.TaskRepository;
import com.example.lifemanager.utils.HabitStatsCalculator;
import com.example.lifemanager.utils.LiveDataUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * ViewModel cho màn hình Báo cáo & Thống kê (Thu Chi, Công việc, Thói quen).
 */
public class ReportViewModel extends AndroidViewModel {

    public static final int RANGE_WEEK = 7;
    public static final int RANGE_MONTH = 30;

    private final MutableLiveData<YearMonth> selectedMonth = new MutableLiveData<>(YearMonth.now());
    private final LiveData<List<CategorySpending>> categorySpending;
    private final LiveData<Long> monthExpense;
    private final LiveData<Long> monthIncome;

    private final LiveData<List<Task>> allTasks;
    private final MediatorLiveData<TaskStats> taskStats = new MediatorLiveData<>();

    private final HabitSnapshotLiveData habitSnapshot;
    private final MutableLiveData<Integer> habitRange = new MutableLiveData<>(RANGE_WEEK);
    private final MediatorLiveData<List<DailyHabitRate>> habitDailyRates = new MediatorLiveData<>();

    public ReportViewModel(@NonNull Application application) {
        super(application);
        FinanceRepository financeRepository = FinanceRepository.getInstance(application);
        categorySpending = LiveDataUtils.switchMap(selectedMonth, ym -> financeRepository
                .observeCategorySpending(ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        monthExpense = LiveDataUtils.switchMap(selectedMonth, ym -> financeRepository.observeTotal(
                TransactionType.EXPENSE, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));
        monthIncome = LiveDataUtils.switchMap(selectedMonth, ym -> financeRepository.observeTotal(
                TransactionType.INCOME, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()));

        allTasks = TaskRepository.getInstance(application).observeAll();
        taskStats.addSource(allTasks, list ->
                taskStats.setValue(TaskStats.from(list, LocalDate.now().toEpochDay())));

        habitSnapshot = HabitRepository.getInstance(application).createSnapshotLiveData();
        habitDailyRates.addSource(habitSnapshot, s -> recomputeHabitRates());
        habitDailyRates.addSource(habitRange, r -> recomputeHabitRates());
    }

    private void recomputeHabitRates() {
        HabitSnapshot snapshot = habitSnapshot.getValue();
        Integer range = habitRange.getValue();
        if (snapshot == null || range == null) {
            return;
        }
        LocalDate today = LocalDate.ofEpochDay(snapshot.todayEpochDay);
        habitDailyRates.setValue(HabitStatsCalculator.dailyRates(snapshot,
                today.minusDays(range - 1L), today));
    }

    public LiveData<YearMonth> getSelectedMonth() {
        return selectedMonth;
    }

    public LiveData<List<CategorySpending>> getCategorySpending() {
        return categorySpending;
    }

    public LiveData<Long> getMonthExpense() {
        return monthExpense;
    }

    public LiveData<Long> getMonthIncome() {
        return monthIncome;
    }

    public LiveData<TaskStats> getTaskStats() {
        return taskStats;
    }

    public LiveData<HabitSnapshot> getHabitSnapshot() {
        return habitSnapshot;
    }

    public LiveData<Integer> getHabitRange() {
        return habitRange;
    }

    public LiveData<List<DailyHabitRate>> getHabitDailyRates() {
        return habitDailyRates;
    }

    public void previousMonth() {
        YearMonth current = selectedMonth.getValue() == null ? YearMonth.now() : selectedMonth.getValue();
        selectedMonth.setValue(current.minusMonths(1));
    }

    /** Không cho phép xem tháng trong tương lai. */
    public void nextMonth() {
        YearMonth current = selectedMonth.getValue() == null ? YearMonth.now() : selectedMonth.getValue();
        if (current.isBefore(YearMonth.now())) {
            selectedMonth.setValue(current.plusMonths(1));
        }
    }

    public boolean canGoNext() {
        YearMonth current = selectedMonth.getValue();
        return current != null && current.isBefore(YearMonth.now());
    }

    public void setHabitRange(int days) {
        habitRange.setValue(days);
    }

    public void refresh() {
        habitSnapshot.refresh();
        List<Task> tasks = allTasks.getValue();
        if (tasks != null) {
            taskStats.setValue(TaskStats.from(tasks, LocalDate.now().toEpochDay()));
        }
    }
}
