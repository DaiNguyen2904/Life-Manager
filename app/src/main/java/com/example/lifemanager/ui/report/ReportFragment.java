package com.example.lifemanager.ui.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lifemanager.R;
import com.example.lifemanager.adapter.BudgetCompareAdapter;
import com.example.lifemanager.adapter.HabitStatsAdapter;
import com.example.lifemanager.adapter.SpendingShareAdapter;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.model.DailyHabitRate;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.databinding.FragmentReportBinding;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.HabitStatsCalculator;
import com.example.lifemanager.utils.TaskUiUtils;
import com.example.lifemanager.viewmodel.ReportViewModel;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Màn hình Báo cáo & Thống kê: 3 tab Thu Chi (tỷ trọng chi tiêu / so sánh ngân sách), Công việc, Thói quen.
 */
public class ReportFragment extends Fragment {

    private static final String KEY_MAIN_TAB = "report_main_tab";
    private static final String KEY_SUB_TAB = "report_sub_tab";

    private FragmentReportBinding binding;
    private ReportViewModel viewModel;
    private SpendingShareAdapter shareAdapter;
    private BudgetCompareAdapter compareAdapter;
    private HabitStatsAdapter streakAdapter;

    private List<CategorySpending> spending = Collections.emptyList();
    private long totalExpense;
    private long totalIncome;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        int mainTab = savedInstanceState == null ? 0 : savedInstanceState.getInt(KEY_MAIN_TAB, 0);
        int subTab = savedInstanceState == null ? 0 : savedInstanceState.getInt(KEY_SUB_TAB, 0);
        setupTabs(mainTab, subTab);
        setupLists();
        setupCharts();

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());
        binding.chipGroupHabitRange.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) {
                viewModel.setHabitRange(ids.get(0) == binding.chip30Days.getId()
                        ? ReportViewModel.RANGE_MONTH : ReportViewModel.RANGE_WEEK);
            }
        });

        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putInt(KEY_MAIN_TAB, binding.tabReport.getSelectedTabPosition());
            outState.putInt(KEY_SUB_TAB, binding.tabFinanceSub.getSelectedTabPosition());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ------------------------------------------------------------------ Thiết lập

    private void setupTabs(int mainTab, int subTab) {
        TabLayout main = binding.tabReport;
        main.addTab(main.newTab().setText(R.string.report_tab_finance));
        main.addTab(main.newTab().setText(R.string.report_tab_tasks));
        main.addTab(main.newTab().setText(R.string.report_tab_habits));
        selectTab(main, mainTab);
        showMainPage(mainTab);
        main.addOnTabSelectedListener(new SimpleTabListener(this::showMainPage));

        TabLayout sub = binding.tabFinanceSub;
        sub.addTab(sub.newTab().setText(R.string.report_sub_share));
        sub.addTab(sub.newTab().setText(R.string.report_sub_budget));
        selectTab(sub, subTab);
        showSubPage(subTab);
        sub.addOnTabSelectedListener(new SimpleTabListener(this::showSubPage));
    }

    private static void selectTab(@NonNull TabLayout tabs, int position) {
        TabLayout.Tab tab = tabs.getTabAt(position);
        if (tab != null) {
            tab.select();
        }
    }

    private void showMainPage(int position) {
        binding.scrollFinance.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.scrollTasks.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        binding.scrollHabits.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }

    private void showSubPage(int position) {
        binding.layoutShare.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.layoutBudget.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
    }

    private void setupLists() {
        shareAdapter = new SpendingShareAdapter();
        binding.rvShare.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShare.setAdapter(shareAdapter);

        compareAdapter = new BudgetCompareAdapter();
        binding.rvBudgetCompare.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBudgetCompare.setAdapter(compareAdapter);

        streakAdapter = new HabitStatsAdapter(true, null);
        binding.rvHabitStreaks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHabitStreaks.setAdapter(streakAdapter);
    }

    private void setupCharts() {
        ChartHelper.setupPieChart(binding.pieExpense, requireContext());
        ChartHelper.setupPieChart(binding.pieTasks, requireContext());
        ChartHelper.setupBarChart(binding.barBudget, requireContext());
        ChartHelper.setupBarChart(binding.barHabits, requireContext());
        binding.barBudget.getAxisLeft().setValueFormatter(ChartHelper.MONEY_AXIS_FORMATTER);
        binding.barHabits.getAxisLeft().setValueFormatter(ChartHelper.PERCENT_AXIS_FORMATTER);
        binding.barHabits.getAxisLeft().setAxisMaximum(100f);
        binding.barHabits.getLegend().setEnabled(false);
    }

    // ------------------------------------------------------------------ Quan sát

    private void observeViewModel() {
        viewModel.getSelectedMonth().observe(getViewLifecycleOwner(), this::renderMonth);
        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), list -> {
            spending = list == null ? Collections.emptyList() : list;
            renderFinance();
        });
        viewModel.getMonthExpense().observe(getViewLifecycleOwner(), value -> {
            totalExpense = value == null ? 0 : value;
            renderFinance();
        });
        viewModel.getMonthIncome().observe(getViewLifecycleOwner(), value -> {
            totalIncome = value == null ? 0 : value;
            renderFinance();
        });
        viewModel.getTaskStats().observe(getViewLifecycleOwner(), this::renderTasks);
        viewModel.getHabitSnapshot().observe(getViewLifecycleOwner(), this::renderHabits);
        viewModel.getHabitDailyRates().observe(getViewLifecycleOwner(), this::renderHabitChart);
    }

    // ------------------------------------------------------------------ Tab Thu Chi

    private void renderMonth(@Nullable YearMonth month) {
        if (month == null) {
            return;
        }
        binding.tvMonth.setText(DateUtils.formatMonth(month));
        boolean canNext = viewModel.canGoNext();
        binding.btnNextMonth.setEnabled(canNext);
        binding.btnNextMonth.setAlpha(canNext ? 1f : 0.3f);
        binding.tvExpenseLabel.setText(month.equals(YearMonth.now())
                ? getString(R.string.report_total_expense_this_month)
                : getString(R.string.report_total_expense_month, DateUtils.formatMonth(month)));
    }

    private void renderFinance() {
        binding.tvTotalExpense.setText(CurrencyUtils.format(totalExpense));
        binding.tvIncomeCompare.setText(getString(R.string.report_income_compare,
                CurrencyUtils.format(totalIncome), CurrencyUtils.format(totalIncome - totalExpense)));

        // Tỷ trọng chi tiêu theo danh mục
        List<CategorySpending> share = new ArrayList<>(spending);
        long categorized = 0;
        for (CategorySpending item : share) {
            categorized += item.spent;
        }
        long uncategorized = totalExpense - categorized;
        if (uncategorized > 0) {
            CategorySpending other = new CategorySpending();
            other.categoryId = -1;
            other.name = getString(R.string.no_category);
            other.color = ContextCompat.getColor(requireContext(), R.color.lm_text_secondary);
            other.icon = "category";
            other.spent = uncategorized;
            share.add(other);
        }
        Collections.sort(share, (a, b) -> Long.compare(b.spent, a.spent));
        shareAdapter.submit(share, totalExpense);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (CategorySpending item : share) {
            if (item.spent > 0) {
                entries.add(new PieEntry(item.spent, item.name));
                colors.add(item.color);
            }
        }
        ChartHelper.setPieData(binding.pieExpense, entries, colors,
                getString(R.string.chart_center_expense, CurrencyUtils.format(totalExpense)));

        // So sánh ngân sách
        List<CategorySpending> budgets = new ArrayList<>();
        for (CategorySpending item : spending) {
            if (item.hasBudget()) {
                budgets.add(item);
            }
        }
        compareAdapter.submitList(budgets);
        binding.tvBudgetCompareEmpty.setVisibility(budgets.isEmpty() ? View.VISIBLE : View.GONE);
        renderBudgetChart(budgets);
    }

    private void renderBudgetChart(@NonNull List<CategorySpending> budgets) {
        if (budgets.isEmpty()) {
            binding.barBudget.clear();
            return;
        }
        List<BarEntry> budgetEntries = new ArrayList<>();
        List<BarEntry> spentEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < budgets.size(); i++) {
            CategorySpending item = budgets.get(i);
            budgetEntries.add(new BarEntry(i, item.budget == null ? 0 : item.budget));
            spentEntries.add(new BarEntry(i, item.spent));
            labels.add(ChartHelper.shorten(item.name, 10));
        }
        BarDataSet budgetSet = new BarDataSet(budgetEntries, getString(R.string.chart_budget));
        budgetSet.setColor(ContextCompat.getColor(requireContext(), R.color.lm_blue));
        budgetSet.setDrawValues(false);
        BarDataSet spentSet = new BarDataSet(spentEntries, getString(R.string.chart_spent));
        spentSet.setColor(ContextCompat.getColor(requireContext(), R.color.lm_primary));
        spentSet.setDrawValues(false);

        float groupSpace = 0.3f;
        float barSpace = 0.04f;
        float barWidth = 0.31f; // (0.31 + 0.04) * 2 + 0.3 = 1.0
        BarData data = new BarData(budgetSet, spentSet);
        data.setBarWidth(barWidth);
        binding.barBudget.setData(data);

        int groupCount = budgets.size();
        binding.barBudget.getXAxis().setAxisMinimum(0f);
        binding.barBudget.getXAxis().setAxisMaximum(data.getGroupWidth(groupSpace, barSpace) * groupCount);
        binding.barBudget.getXAxis().setCenterAxisLabels(true);
        binding.barBudget.getXAxis().setLabelCount(groupCount);
        binding.barBudget.getXAxis().setLabelRotationAngle(groupCount > 4 ? -30f : 0f);
        binding.barBudget.getXAxis().setValueFormatter(ChartHelper.labelFormatter(labels, true));
        binding.barBudget.groupBars(0f, groupSpace, barSpace);
        binding.barBudget.animateY(600);
        binding.barBudget.invalidate();
    }

    // ------------------------------------------------------------------ Tab Công việc

    private void renderTasks(@Nullable TaskStats stats) {
        if (stats == null) {
            return;
        }
        int rate = stats.getCompletionRate();
        binding.tvTaskRate.setText(getString(R.string.report_task_rate, rate));
        binding.tvTaskSummary.setText(getString(R.string.report_task_summary, stats.done, stats.total));
        binding.progressTasks.setProgressCompat(rate, true);
        binding.tvTodoCount.setText(getString(R.string.report_task_count, stats.todo));
        binding.tvInProgressCount.setText(getString(R.string.report_task_count, stats.inProgress));
        binding.tvDoneCount.setText(getString(R.string.report_task_count, stats.done));
        binding.tvOverdueCount.setText(getString(R.string.report_task_count, stats.overdue));

        binding.tvPriorityHigh.setText(getString(R.string.report_priority_count,
                getString(R.string.priority_high), stats.highPriority));
        binding.tvPriorityMedium.setText(getString(R.string.report_priority_count,
                getString(R.string.priority_medium), stats.mediumPriority));
        binding.tvPriorityLow.setText(getString(R.string.report_priority_count,
                getString(R.string.priority_low), stats.lowPriority));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        addTaskSlice(entries, colors, stats.todo, com.example.lifemanager.data.entity.Task.STATUS_TODO);
        addTaskSlice(entries, colors, stats.inProgress, com.example.lifemanager.data.entity.Task.STATUS_IN_PROGRESS);
        addTaskSlice(entries, colors, stats.done, com.example.lifemanager.data.entity.Task.STATUS_DONE);
        addTaskSlice(entries, colors, stats.overdue, com.example.lifemanager.data.entity.Task.STATUS_OVERDUE);
        ChartHelper.setPieData(binding.pieTasks, entries, colors,
                getString(R.string.chart_center_tasks, stats.total));
    }

    private void addTaskSlice(@NonNull List<PieEntry> entries, @NonNull List<Integer> colors,
                              int count, @NonNull String status) {
        if (count > 0) {
            entries.add(new PieEntry(count, TaskUiUtils.statusLabel(requireContext(), status)));
            colors.add(TaskUiUtils.statusTextColor(requireContext(), status));
        }
    }

    // ------------------------------------------------------------------ Tab Thói quen

    private void renderHabits(@Nullable HabitSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        int rate = snapshot.getTodayRate();
        binding.tvHabitTodayRate.setText(getString(R.string.report_habit_today_rate, rate));
        binding.tvHabitTodaySummary.setText(getString(R.string.report_habit_today_summary,
                snapshot.getDoneTodayCount(), snapshot.getScheduledTodayCount()));
        binding.progressHabitsToday.setProgressCompat(rate, true);

        int weekDone = 0;
        int weekScheduled = 0;
        int monthDone = 0;
        int monthScheduled = 0;
        for (HabitWithStats s : snapshot.stats) {
            weekDone += s.weekDone;
            weekScheduled += s.weekScheduled;
            monthDone += s.monthDone;
            monthScheduled += s.monthScheduled;
        }
        binding.tvHabitWeekRate.setText(getString(R.string.percent_format,
                weekScheduled == 0 ? 0 : Math.round(weekDone * 100f / weekScheduled)));
        binding.tvHabitMonthRate.setText(getString(R.string.percent_format,
                monthScheduled == 0 ? 0 : Math.round(monthDone * 100f / monthScheduled)));

        streakAdapter.submitList(snapshot.stats);
        binding.tvHabitsEmpty.setVisibility(snapshot.stats.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderHabitChart(@Nullable List<DailyHabitRate> rates) {
        HabitSnapshot snapshot = viewModel.getHabitSnapshot().getValue();
        if (rates == null || rates.isEmpty() || snapshot == null || snapshot.habits.isEmpty()) {
            binding.barHabits.clear();
            binding.tvHabitRangeRate.setText("");
            return;
        }
        boolean week = rates.size() <= ReportViewModel.RANGE_WEEK;
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < rates.size(); i++) {
            DailyHabitRate rate = rates.get(i);
            entries.add(new BarEntry(i, rate.getPercent(), rate));
            LocalDate date = LocalDate.ofEpochDay(rate.epochDay);
            labels.add(week ? DateUtils.shortDayOfWeek(date.getDayOfWeek())
                    : DateUtils.formatShortDay(rate.epochDay));
        }
        BarDataSet set = new BarDataSet(entries, getString(R.string.chart_habit_rate));
        set.setColor(ContextCompat.getColor(requireContext(), R.color.lm_success));
        set.setDrawValues(week);
        set.setValueTextSize(9f);
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.lm_text_primary));
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry entry) {
                // Ngày không có thói quen nào theo lịch: không hiển thị nhãn
                Object data = entry.getData();
                if (data instanceof DailyHabitRate && ((DailyHabitRate) data).scheduled == 0) {
                    return "";
                }
                return Math.round(entry.getY()) + "%";
            }
        });
        BarData data = new BarData(set);
        data.setBarWidth(0.6f);
        binding.barHabits.setData(data);
        binding.barHabits.getXAxis().setAxisMinimum(-0.5f);
        binding.barHabits.getXAxis().setAxisMaximum(rates.size() - 0.5f);
        binding.barHabits.getXAxis().setLabelCount(week ? rates.size() : 6);
        binding.barHabits.getXAxis().setValueFormatter(ChartHelper.labelFormatter(labels, false));
        binding.barHabits.animateY(600);
        binding.barHabits.invalidate();

        binding.tvHabitRangeRate.setText(getString(R.string.report_habit_range_rate,
                rates.size(), HabitStatsCalculator.aggregateRate(rates)));
    }

    /** Listener rút gọn cho TabLayout. */
    private static final class SimpleTabListener implements TabLayout.OnTabSelectedListener {
        interface OnSelected {
            void onSelected(int position);
        }

        private final OnSelected callback;

        SimpleTabListener(@NonNull OnSelected callback) {
            this.callback = callback;
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            callback.onSelected(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
    }
}
