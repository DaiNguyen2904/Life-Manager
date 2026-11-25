package com.example.lifemanager.ui.finance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.data.entity.TransactionType;
import com.example.lifemanager.databinding.ActivityTransactionEditorBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.MoneyTextWatcher;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.TransactionEditorViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình thêm / sửa giao dịch thu chi.
 */
public class TransactionEditorActivity extends BaseActivity {

    private static final String EXTRA_TRANSACTION_ID = "extra_transaction_id";

    private ActivityTransactionEditorBinding binding;
    private TransactionEditorViewModel viewModel;
    private long transactionId;
    private List<Category> categories = new ArrayList<>();

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, TransactionEditorActivity.class);
    }

    @NonNull
    public static Intent editIntent(@NonNull Context context, long transactionId) {
        return new Intent(context, TransactionEditorActivity.class)
                .putExtra(EXTRA_TRANSACTION_ID, transactionId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(TransactionEditorViewModel.class);
        transactionId = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, 0);

        setupToolbar();
        setupForm();
        observeViewModel();

        if (transactionId > 0 && !viewModel.isLoaded()) {
            viewModel.load(transactionId, entity -> {
                if (entity == null) {
                    Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                binding.etTitle.setText(entity.getTitle());
                binding.etAmount.setText(CurrencyUtils.formatNumber(entity.getAmount()));
                binding.etNote.setText(entity.getNote());
            });
        }
    }

    private void setupToolbar() {
        boolean editing = transactionId > 0;
        binding.toolbar.setTitle(editing ? R.string.transaction_edit_title : R.string.transaction_add_title);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (editing) {
            binding.toolbar.inflateMenu(R.menu.menu_editor);
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    confirmDelete();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupForm() {
        binding.tilAmount.setSuffixText(CurrencyUtils.suffix());
        binding.etAmount.addTextChangedListener(new MoneyTextWatcher(binding.etAmount));

        binding.toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setType(checkedId == R.id.btnTypeIncome ? TransactionType.INCOME : TransactionType.EXPENSE);
            }
        });

        binding.actCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < categories.size()) {
                viewModel.setSelectedCategoryId(categories.get(position).getId());
                binding.tilCategory.setError(null);
            }
        });

        binding.etDate.setOnClickListener(v -> pickDate());
        binding.tilDate.setStartIconOnClickListener(v -> pickDate());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void observeViewModel() {
        viewModel.getType().observe(this, type -> {
            int buttonId = TransactionType.INCOME.equals(type) ? R.id.btnTypeIncome : R.id.btnTypeExpense;
            if (binding.toggleType.getCheckedButtonId() != buttonId) {
                binding.toggleType.check(buttonId);
            }
        });

        viewModel.getCategories().observe(this, list -> {
            categories = list == null ? new ArrayList<>() : new ArrayList<>(list);
            ArrayAdapter<Category> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, categories);
            binding.actCategory.setAdapter(adapter);
            renderSelectedCategory();
        });

        viewModel.getDate().observe(this, epochDay -> {
            if (epochDay != null) {
                binding.etDate.setText(DateUtils.formatDate(epochDay));
            }
        });
    }

    private void renderSelectedCategory() {
        Long selected = viewModel.getSelectedCategoryId();
        String text = "";
        if (selected != null) {
            for (Category c : categories) {
                if (c.getId() == selected) {
                    text = c.getName();
                    break;
                }
            }
            if (text.isEmpty()) {
                // Danh mục đã bị xóa hoặc không thuộc loại hiện tại
                viewModel.setSelectedCategoryId(null);
            }
        }
        binding.actCategory.setText(text, false);
    }

    private void pickDate() {
        Long current = viewModel.getDate().getValue();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.transaction_date_hint)
                .setSelection(DateUtils.epochDayToUtcMillis(current == null ? DateUtils.todayEpochDay() : current))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                viewModel.setDate(DateUtils.utcMillisToEpochDay(selection));
            }
        });
        picker.show(getSupportFragmentManager(), "transaction_date_picker");
    }

    private void save() {
        ViewUtils.hideKeyboard(binding.getRoot());
        String title = binding.etTitle.getText() == null ? "" : binding.etTitle.getText().toString().trim();
        long amount = CurrencyUtils.parse(binding.etAmount.getText() == null ? null
                : binding.etAmount.getText().toString());
        String note = binding.etNote.getText() == null ? null : binding.etNote.getText().toString();

        boolean valid = true;
        if (title.isEmpty()) {
            binding.tilTitle.setError(getString(R.string.error_title_required));
            valid = false;
        } else {
            binding.tilTitle.setError(null);
        }
        if (amount <= 0) {
            binding.tilAmount.setError(getString(R.string.error_amount_invalid));
            valid = false;
        } else if (amount > CurrencyUtils.MAX_AMOUNT) {
            binding.tilAmount.setError(getString(R.string.error_amount_too_large));
            valid = false;
        } else {
            binding.tilAmount.setError(null);
        }
        if (viewModel.getSelectedCategoryId() == null) {
            binding.tilCategory.setError(getString(R.string.error_category_required));
            valid = false;
        } else {
            binding.tilCategory.setError(null);
        }
        if (!valid) {
            return;
        }

        binding.btnSave.setEnabled(false);
        viewModel.save(title, amount, note, result -> {
            binding.btnSave.setEnabled(true);
            if (!result.success) {
                Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                return;
            }
            if (result.warning != null) {
                // Cảnh báo vượt ngân sách
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.budget_warning_title)
                        .setMessage(result.warning)
                        .setPositiveButton(R.string.action_ok, (d, w) -> finish())
                        .setOnCancelListener(d -> finish())
                        .show();
            } else {
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void confirmDelete() {
        DialogUtils.confirm(this, R.string.transaction_delete_title,
                getString(R.string.transaction_delete_message,
                        binding.etTitle.getText() == null ? "" : binding.etTitle.getText().toString()),
                R.string.action_delete, () -> viewModel.delete(() -> {
                    Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                }));
    }
}
