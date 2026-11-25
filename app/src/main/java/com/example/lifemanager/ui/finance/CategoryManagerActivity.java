package com.example.lifemanager.ui.finance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lifemanager.R;
import com.example.lifemanager.adapter.CategoryAdapter;
import com.example.lifemanager.adapter.ColorOptionAdapter;
import com.example.lifemanager.adapter.IconOptionAdapter;
import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.databinding.ActivityCategoryManagerBinding;
import com.example.lifemanager.databinding.DialogCategoryBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.IconUtils;
import com.example.lifemanager.utils.MoneyTextWatcher;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.CategoryViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.Map;

/**
 * Màn hình Quản lý danh mục thu / chi: thêm, sửa, xóa, chọn màu, icon và ngân sách.
 */
public class CategoryManagerActivity extends BaseActivity {

    private ActivityCategoryManagerBinding binding;
    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        TabLayout tabs = binding.tabType;
        tabs.addTab(tabs.newTab().setText(R.string.category_tab_expense));
        tabs.addTab(tabs.newTab().setText(R.string.category_tab_income));
        boolean income = TransactionType.INCOME.equals(viewModel.getSelectedType().getValue());
        TabLayout.Tab initial = tabs.getTabAt(income ? 1 : 0);
        if (initial != null) {
            initial.select();
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setSelectedType(tab.getPosition() == 1 ? TransactionType.INCOME : TransactionType.EXPENSE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        adapter = new CategoryAdapter(new CategoryAdapter.Listener() {
            @Override
            public void onEdit(@NonNull Category category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDelete(@NonNull Category category) {
                DialogUtils.confirm(CategoryManagerActivity.this, R.string.category_delete_title,
                        getString(R.string.category_delete_message, category.getName()),
                        R.string.action_delete, () -> {
                            viewModel.deleteCategory(category);
                            Toast.makeText(CategoryManagerActivity.this, R.string.msg_deleted,
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> showCategoryDialog(null));

        viewModel.getCategories().observe(this, list -> {
            adapter.submitList(list);
            binding.tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getBudgets().observe(this, budgets -> {
            if (budgets != null) {
                adapter.setBudgets(budgets);
            }
        });
    }

    /**
     * Hộp thoại thêm / sửa danh mục.
     *
     * @param existing null = thêm mới.
     */
    private void showCategoryDialog(@Nullable Category existing) {
        DialogCategoryBinding b = DialogCategoryBinding.inflate(getLayoutInflater());
        final String[] type = {existing != null ? existing.getType()
                : (viewModel.getSelectedType().getValue() == null ? TransactionType.EXPENSE
                : viewModel.getSelectedType().getValue())};
        final int[] color = {existing != null ? existing.getColor() : IconUtils.PALETTE[0]};
        final String[] icon = {existing != null ? existing.getIcon() : "category"};

        if (existing != null) {
            b.etCategoryName.setText(existing.getName());
        }
        b.toggleCategoryType.check(TransactionType.INCOME.equals(type[0]) ? R.id.btnCatIncome : R.id.btnCatExpense);
        if (existing != null) {
            // Không cho đổi loại danh mục đã có để tránh sai lệch dữ liệu giao dịch:
            // chỉ khóa nút không được chọn để vẫn thấy rõ loại hiện tại
            if (TransactionType.INCOME.equals(type[0])) {
                b.btnCatExpense.setEnabled(false);
            } else {
                b.btnCatIncome.setEnabled(false);
            }
        }
        b.tilCategoryBudget.setVisibility(TransactionType.EXPENSE.equals(type[0]) ? View.VISIBLE : View.GONE);
        b.toggleCategoryType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                type[0] = checkedId == R.id.btnCatIncome ? TransactionType.INCOME : TransactionType.EXPENSE;
                b.tilCategoryBudget.setVisibility(TransactionType.EXPENSE.equals(type[0]) ? View.VISIBLE : View.GONE);
            }
        });

        IconOptionAdapter iconAdapter = new IconOptionAdapter(IconUtils.ICON_KEYS, icon[0], color[0],
                key -> icon[0] = key);
        b.rvIcons.setLayoutManager(new GridLayoutManager(this, 6));
        b.rvIcons.setAdapter(iconAdapter);

        ColorOptionAdapter colorAdapter = new ColorOptionAdapter(IconUtils.PALETTE, color[0], selected -> {
            color[0] = selected;
            iconAdapter.setTintColor(selected);
        });
        b.rvColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        b.rvColors.setAdapter(colorAdapter);

        b.tilCategoryBudget.setSuffixText(CurrencyUtils.suffix());
        b.etCategoryBudget.addTextChangedListener(new MoneyTextWatcher(b.etCategoryBudget));
        if (existing != null) {
            Map<Long, Long> budgets = viewModel.getBudgets().getValue();
            Long budget = budgets == null ? null : budgets.get(existing.getId());
            if (budget != null) {
                b.etCategoryBudget.setText(CurrencyUtils.formatNumber(budget));
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing != null ? R.string.category_edit_title : R.string.category_add_title)
                .setView(b.getRoot())
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = b.etCategoryName.getText() == null ? "" : b.etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                b.tilCategoryName.setError(getString(R.string.error_name_required));
                return;
            }
            b.tilCategoryName.setError(null);

            Long budget = null;
            String budgetText = b.etCategoryBudget.getText() == null ? "" : b.etCategoryBudget.getText().toString();
            if (TransactionType.EXPENSE.equals(type[0]) && !budgetText.trim().isEmpty()) {
                long parsed = CurrencyUtils.parse(budgetText);
                if (parsed < 0 || parsed > CurrencyUtils.MAX_AMOUNT) {
                    b.tilCategoryBudget.setError(getString(R.string.error_amount_too_large));
                    return;
                }
                budget = parsed;
            }
            b.tilCategoryBudget.setError(null);

            Category category = new Category();
            if (existing != null) {
                category.setId(existing.getId());
                category.setCreatedAt(existing.getCreatedAt());
                category.setBuiltIn(existing.isBuiltIn());
            }
            category.setName(name);
            category.setType(type[0]);
            category.setColor(color[0]);
            category.setIcon(icon[0]);

            viewModel.saveCategory(category, budget, result -> {
                if (result.success) {
                    dialog.dismiss();
                    Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                } else {
                    b.tilCategoryName.setError(result.error);
                }
            });
        }));
        dialog.show();
    }
}
