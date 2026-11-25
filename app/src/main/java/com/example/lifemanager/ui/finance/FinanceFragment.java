package com.example.lifemanager.ui.finance;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lifemanager.R;
import com.example.lifemanager.adapter.CategoryBudgetAdapter;
import com.example.lifemanager.adapter.TransactionAdapter;
import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.data.model.TransactionFilter;
import com.example.lifemanager.data.model.TransactionWithCategory;
import com.example.lifemanager.databinding.BottomSheetTransactionDetailBinding;
import com.example.lifemanager.databinding.DialogBudgetBinding;
import com.example.lifemanager.databinding.FragmentFinanceBinding;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.MoneyTextWatcher;
import com.example.lifemanager.viewmodel.FinanceViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Màn hình Quản lý Thu Chi: tổng quan tháng, ngân sách theo danh mục, lịch sử giao dịch (tìm kiếm, lọc).
 */
public class FinanceFragment extends Fragment {

    private static final int MENU_PERIOD_ALL = 1;
    private static final int MENU_PERIOD_TODAY = 2;
    private static final int MENU_PERIOD_THIS_MONTH = 3;
    private static final int MENU_PERIOD_DAY = 4;
    private static final int MENU_PERIOD_MONTH = 5;

    private FragmentFinanceBinding binding;
    private FinanceViewModel viewModel;
    private CategoryBudgetAdapter budgetAdapter;
    private TransactionAdapter transactionAdapter;

    private long monthIncome;
    private long monthExpense;
    private List<Category> allCategories = Collections.emptyList();
    private List<Category> expenseCategories = Collections.emptyList();
    private boolean syncingTypeChips;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FinanceViewModel.class);

        budgetAdapter = new CategoryBudgetAdapter(this::showBudgetDialog);
        binding.rvBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBudgets.setAdapter(budgetAdapter);

        transactionAdapter = new TransactionAdapter(this::showTransactionDetail);
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransactions.setAdapter(transactionAdapter);

        binding.btnManageCategories.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoryManagerActivity.class)));
        binding.btnAddBudget.setOnClickListener(v -> showBudgetDialog(null));
        binding.fabAdd.setOnClickListener(v ->
                startActivity(TransactionEditorActivity.newIntent(requireContext())));

        setupFilters();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshMonth();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ------------------------------------------------------------------ Bộ lọc

    private void setupFilters() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s.toString());
            }
        });

        binding.chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (syncingTypeChips || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipIncome) {
                viewModel.setType(TransactionType.INCOME);
            } else if (id == R.id.chipExpense) {
                viewModel.setType(TransactionType.EXPENSE);
            } else {
                viewModel.setType("");
            }
        });

        binding.chipPeriod.setOnClickListener(this::showPeriodMenu);
        binding.chipCategory.setOnClickListener(this::showCategoryMenu);
        binding.chipClearFilter.setOnClickListener(v -> viewModel.resetFilter());
    }

    private void showPeriodMenu(@NonNull View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        Menu menu = popup.getMenu();
        menu.add(0, MENU_PERIOD_ALL, 0, R.string.filter_period_all_label);
        menu.add(0, MENU_PERIOD_TODAY, 1, R.string.filter_period_today);
        menu.add(0, MENU_PERIOD_THIS_MONTH, 2, R.string.filter_period_this_month);
        menu.add(0, MENU_PERIOD_DAY, 3, R.string.filter_period_pick_day);
        menu.add(0, MENU_PERIOD_MONTH, 4, R.string.filter_period_pick_month);
        popup.setOnMenuItemClickListener(item -> {
            TransactionFilter current = viewModel.getFilter().getValue();
            switch (item.getItemId()) {
                case MENU_PERIOD_TODAY:
                    viewModel.setPeriod(TransactionFilter.PeriodMode.TODAY);
                    return true;
                case MENU_PERIOD_THIS_MONTH:
                    viewModel.setPeriod(TransactionFilter.PeriodMode.THIS_MONTH);
                    return true;
                case MENU_PERIOD_DAY:
                    pickDay(current == null ? DateUtils.todayEpochDay() : current.selectedDay);
                    return true;
                case MENU_PERIOD_MONTH:
                    DialogUtils.showMonthPicker(requireContext(),
                            current == null ? java.time.YearMonth.now() : current.selectedMonth,
                            month -> viewModel.setMonth(month));
                    return true;
                case MENU_PERIOD_ALL:
                default:
                    viewModel.setPeriod(TransactionFilter.PeriodMode.ALL);
                    return true;
            }
        });
        popup.show();
    }

    private void pickDay(long initialEpochDay) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.filter_period_pick_day)
                .setSelection(DateUtils.epochDayToUtcMillis(initialEpochDay))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                viewModel.setDay(DateUtils.utcMillisToEpochDay(selection));
            }
        });
        picker.show(getChildFragmentManager(), "filter_day_picker");
    }

    private void showCategoryMenu(@NonNull View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        Menu menu = popup.getMenu();
        menu.add(0, 0, 0, R.string.filter_category_all_label);
        int order = 1;
        for (Category category : allCategories) {
            String prefix = category.isExpense() ? getString(R.string.filter_prefix_expense)
                    : getString(R.string.filter_prefix_income);
            menu.add(0, (int) category.getId(), order++, prefix + " " + category.getName());
        }
        popup.setOnMenuItemClickListener(item -> {
            viewModel.setCategory(item.getItemId() == 0
                    ? TransactionFilter.NO_CATEGORY_FILTER : item.getItemId());
            return true;
        });
        popup.show();
    }

    private void renderFilter(@Nullable TransactionFilter filter) {
        if (filter == null) {
            return;
        }
        // Thời gian
        String period;
        switch (filter.periodMode) {
            case TODAY:
                period = getString(R.string.filter_period_today);
                break;
            case THIS_MONTH:
                period = getString(R.string.filter_period_this_month);
                break;
            case DAY:
                period = DateUtils.formatDate(filter.selectedDay);
                break;
            case MONTH:
                period = DateUtils.formatMonth(filter.selectedMonth);
                break;
            case ALL:
            default:
                period = getString(R.string.filter_period_all_label);
                break;
        }
        binding.chipPeriod.setText(getString(R.string.filter_period_format, period));

        // Danh mục
        String categoryLabel = getString(R.string.filter_category_all_label);
        if (filter.categoryId != TransactionFilter.NO_CATEGORY_FILTER) {
            for (Category c : allCategories) {
                if (c.getId() == filter.categoryId) {
                    categoryLabel = c.getName();
                    break;
                }
            }
        }
        binding.chipCategory.setText(getString(R.string.filter_category_format, categoryLabel));

        // Loại giao dịch
        syncingTypeChips = true;
        if (TransactionType.INCOME.equals(filter.type)) {
            binding.chipGroupType.check(R.id.chipIncome);
        } else if (TransactionType.EXPENSE.equals(filter.type)) {
            binding.chipGroupType.check(R.id.chipExpense);
        } else {
            binding.chipGroupType.check(R.id.chipAll);
        }
        syncingTypeChips = false;

        boolean hasExtraFilter = !filter.type.isEmpty()
                || filter.categoryId != TransactionFilter.NO_CATEGORY_FILTER
                || filter.periodMode != TransactionFilter.PeriodMode.ALL;
        binding.chipClearFilter.setVisibility(hasExtraFilter ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------ Quan sát dữ liệu

    private void observeViewModel() {
        viewModel.getMonthIncome().observe(getViewLifecycleOwner(), value -> {
            monthIncome = value == null ? 0 : value;
            renderOverview();
        });
        viewModel.getMonthExpense().observe(getViewLifecycleOwner(), value -> {
            monthExpense = value == null ? 0 : value;
            renderOverview();
        });

        viewModel.getBudgetItems().observe(getViewLifecycleOwner(), items -> {
            budgetAdapter.submitList(items);
            boolean empty = items == null || items.isEmpty();
            binding.tvBudgetEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            int over = 0;
            if (items != null) {
                for (CategorySpending item : items) {
                    if (item.isOverBudget()) {
                        over++;
                    }
                }
            }
            binding.cardOverBudget.setVisibility(over > 0 ? View.VISIBLE : View.GONE);
            binding.tvOverBudget.setText(getString(R.string.finance_over_budget_warning, over));
        });

        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories == null ? Collections.emptyList() : categories;
            renderFilter(viewModel.getFilter().getValue());
        });
        viewModel.getExpenseCategories().observe(getViewLifecycleOwner(), categories ->
                expenseCategories = categories == null ? Collections.emptyList() : categories);

        viewModel.getFilter().observe(getViewLifecycleOwner(), this::renderFilter);

        viewModel.getTransactions().observe(getViewLifecycleOwner(), list -> {
            transactionAdapter.submitList(list);
            int count = list == null ? 0 : list.size();
            binding.tvTransactionCount.setText(getString(R.string.finance_transaction_count, count));
            binding.cardEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            TransactionFilter filter = viewModel.getFilter().getValue();
            boolean filtered = filter != null && !filter.isDefault();
            binding.tvEmpty.setText(filtered ? R.string.finance_empty_filtered : R.string.finance_empty);
        });
    }

    private void renderOverview() {
        binding.tvTotalIncome.setText(CurrencyUtils.format(monthIncome));
        binding.tvTotalExpense.setText(CurrencyUtils.format(monthExpense));
        long balance = monthIncome - monthExpense;
        binding.tvBalance.setText(CurrencyUtils.format(balance));
        binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(),
                balance < 0 ? R.color.lm_expense : R.color.lm_balance));
    }

    // ------------------------------------------------------------------ Chi tiết giao dịch

    private void showTransactionDetail(@NonNull TransactionWithCategory item) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        BottomSheetTransactionDetailBinding b = BottomSheetTransactionDetailBinding.inflate(getLayoutInflater());
        TransactionAdapter.bindIcon(b.ivIcon, item);
        b.tvTitle.setText(item.transaction.getTitle());
        b.tvCategory.setText(item.categoryName != null ? item.categoryName : getString(R.string.no_category));
        boolean income = item.transaction.isIncome();
        b.tvAmount.setText(CurrencyUtils.formatSigned(item.transaction.getAmount(), income));
        b.tvAmount.setTextColor(ContextCompat.getColor(requireContext(),
                income ? R.color.lm_income : R.color.lm_expense));
        b.tvType.setText(income ? R.string.type_income : R.string.type_expense);
        b.tvDate.setText(DateUtils.formatDate(item.transaction.getDate()));
        String note = item.transaction.getNote();
        b.tvNote.setText(note == null || note.trim().isEmpty() ? getString(R.string.empty_value) : note);
        b.tvUpdated.setText(DateUtils.formatDateTime(item.transaction.getUpdatedAt()));

        b.btnEdit.setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(TransactionEditorActivity.editIntent(requireContext(), item.transaction.getId()));
        });
        b.btnDelete.setOnClickListener(v -> {
            sheet.dismiss();
            DialogUtils.confirm(requireContext(), R.string.transaction_delete_title,
                    getString(R.string.transaction_delete_message, item.transaction.getTitle()),
                    R.string.action_delete, () -> viewModel.deleteTransaction(item.transaction.getId(), () -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                        }
                    }));
        });
        sheet.setContentView(b.getRoot());
        sheet.show();
    }

    // ------------------------------------------------------------------ Ngân sách

    /**
     * Hộp thoại đặt / sửa ngân sách.
     *
     * @param existing null = thêm ngân sách mới cho danh mục chưa có ngân sách.
     */
    private void showBudgetDialog(@Nullable CategorySpending existing) {
        DialogBudgetBinding b = DialogBudgetBinding.inflate(getLayoutInflater());
        b.tilBudgetAmount.setSuffixText(CurrencyUtils.suffix());
        b.etBudgetAmount.addTextChangedListener(new MoneyTextWatcher(b.etBudgetAmount));
        final long[] selectedCategory = {existing != null ? existing.categoryId : -1};

        if (existing != null) {
            b.actBudgetCategory.setText(existing.name, false);
            b.tilBudgetCategory.setEnabled(false);
            if (existing.budget != null) {
                b.etBudgetAmount.setText(CurrencyUtils.formatNumber(existing.budget));
            }
        } else {
            List<Category> options = new ArrayList<>(expenseCategories);
            ArrayAdapter<Category> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, options);
            b.actBudgetCategory.setAdapter(adapter);
            b.actBudgetCategory.setOnItemClickListener((parent, view, position, id) -> {
                Category category = adapter.getItem(position);
                if (category != null) {
                    selectedCategory[0] = category.getId();
                    b.tilBudgetCategory.setError(null);
                }
            });
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing != null ? R.string.budget_edit_title : R.string.budget_add_title)
                .setView(b.getRoot())
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.budget_remove, (dialog, which) -> {
                viewModel.deleteBudget(existing.categoryId);
                Toast.makeText(requireContext(), R.string.msg_budget_removed, Toast.LENGTH_SHORT).show();
            });
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (selectedCategory[0] <= 0) {
                b.tilBudgetCategory.setError(getString(R.string.error_category_required));
                return;
            }
            long amount = CurrencyUtils.parse(b.etBudgetAmount.getText() == null ? null
                    : b.etBudgetAmount.getText().toString());
            if (amount <= 0) {
                b.tilBudgetAmount.setError(getString(R.string.error_amount_invalid));
                return;
            }
            b.tilBudgetAmount.setError(null);
            viewModel.saveBudget(selectedCategory[0], amount, result -> {
                if (!isAdded()) {
                    return;
                }
                if (result.success) {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.msg_budget_saved, Toast.LENGTH_SHORT).show();
                } else {
                    b.tilBudgetAmount.setError(result.error);
                }
            });
        }));
        dialog.show();
    }
}
