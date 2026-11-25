package com.example.lifemanager.ui.habit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Habit;
import com.example.lifemanager.databinding.ActivityHabitEditorBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.HabitEditorViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/**
 * Màn hình thêm / sửa thói quen: mục tiêu, đơn vị, tần suất, ngày bắt đầu, giờ nhắc nhở.
 */
public class HabitEditorActivity extends BaseActivity {

    private static final String EXTRA_HABIT_ID = "extra_habit_id";
    private static final String EXTRA_PREFILL_NAME = "extra_prefill_name";

    private ActivityHabitEditorBinding binding;
    private HabitEditorViewModel viewModel;
    private long habitId;
    private Chip[] dayChips;
    private boolean bindingState;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @Nullable String prefillName) {
        Intent intent = new Intent(context, HabitEditorActivity.class);
        if (prefillName != null) {
            intent.putExtra(EXTRA_PREFILL_NAME, prefillName);
        }
        return intent;
    }

    @NonNull
    public static Intent editIntent(@NonNull Context context, long habitId) {
        return new Intent(context, HabitEditorActivity.class).putExtra(EXTRA_HABIT_ID, habitId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHabitEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(HabitEditorViewModel.class);
        habitId = getIntent().getLongExtra(EXTRA_HABIT_ID, 0);
        // Thứ tự khớp với bit trong frequencyMask: bit 0 = Thứ Hai ... bit 6 = Chủ Nhật
        dayChips = new Chip[]{binding.chipMon, binding.chipTue, binding.chipWed, binding.chipThu,
                binding.chipFri, binding.chipSat, binding.chipSun};

        setupToolbar();
        setupForm();

        if (!viewModel.isInitialized()) {
            if (habitId > 0) {
                viewModel.load(habitId, found -> {
                    if (!found) {
                        Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Habit habit = viewModel.current();
                    if (habit != null) {
                        binding.etName.setText(habit.getName());
                        binding.etDescription.setText(habit.getDescription());
                        binding.etTarget.setText(String.valueOf(habit.getDailyTarget()));
                        binding.etUnit.setText(habit.getTargetUnit());
                    }
                });
            } else {
                viewModel.initNew();
                String prefill = getIntent().getStringExtra(EXTRA_PREFILL_NAME);
                if (!TextUtils.isEmpty(prefill)) {
                    binding.etName.setText(prefill);
                }
                binding.etTarget.setText("1");
                binding.etUnit.setText(R.string.habit_default_unit);
            }
        }
        viewModel.getDraft().observe(this, this::renderDraft);
    }

    private void setupToolbar() {
        boolean editing = habitId > 0;
        binding.toolbar.setTitle(editing ? R.string.habit_edit_title : R.string.habit_add_title);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (editing) {
            binding.toolbar.inflateMenu(R.menu.menu_editor);
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    Habit habit = viewModel.current();
                    DialogUtils.confirm(this, R.string.habit_delete_title,
                            getString(R.string.habit_delete_message, habit == null ? "" : habit.getName()),
                            R.string.action_delete, () -> viewModel.delete(() -> {
                                Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                                finish();
                            }));
                    return true;
                }
                return false;
            });
        }
    }

    private void setupForm() {
        // Chọn nhanh tần suất
        binding.chipGroupPreset.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingState || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            int mask;
            if (id == R.id.chipWeekdays) {
                mask = Habit.MASK_WEEKDAYS;
            } else if (id == R.id.chipWeekends) {
                mask = Habit.MASK_WEEKENDS;
            } else {
                mask = Habit.MASK_DAILY;
            }
            updateMask(mask);
        });
        // Chọn từng ngày trong tuần
        for (Chip chip : dayChips) {
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (!bindingState) {
                    updateMask(maskFromChips());
                }
            });
        }

        binding.etStartDate.setOnClickListener(v -> pickStartDate());
        binding.tilStartDate.setStartIconOnClickListener(v -> pickStartDate());

        binding.switchReminder.setOnCheckedChangeListener((button, checked) -> {
            if (bindingState) {
                return;
            }
            Habit habit = viewModel.current();
            if (habit == null) {
                return;
            }
            if (checked) {
                pickReminderTime();
            } else {
                habit.setReminderMinutes(null);
                viewModel.notifyDraftChanged();
            }
        });
        binding.etReminderTime.setOnClickListener(v -> pickReminderTime());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private int maskFromChips() {
        int mask = 0;
        for (int i = 0; i < dayChips.length; i++) {
            if (dayChips[i].isChecked()) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private void updateMask(int mask) {
        Habit habit = viewModel.current();
        if (habit != null) {
            habit.setFrequencyMask(mask);
            viewModel.notifyDraftChanged();
        }
    }

    private void renderDraft(@Nullable Habit habit) {
        if (habit == null) {
            return;
        }
        bindingState = true;
        int mask = habit.getFrequencyMask() & Habit.MASK_DAILY;
        for (int i = 0; i < dayChips.length; i++) {
            dayChips[i].setChecked((mask & (1 << i)) != 0);
        }
        if (mask == Habit.MASK_DAILY) {
            binding.chipGroupPreset.check(R.id.chipDaily);
        } else if (mask == Habit.MASK_WEEKDAYS) {
            binding.chipGroupPreset.check(R.id.chipWeekdays);
        } else if (mask == Habit.MASK_WEEKENDS) {
            binding.chipGroupPreset.check(R.id.chipWeekends);
        } else {
            binding.chipGroupPreset.clearCheck();
        }
        binding.tvFrequencyError.setVisibility(mask == 0 ? View.VISIBLE : View.GONE);

        binding.etStartDate.setText(DateUtils.formatDate(habit.getStartDate()));

        Integer minutes = habit.getReminderMinutes();
        binding.switchReminder.setChecked(minutes != null);
        binding.tilReminderTime.setVisibility(minutes != null ? View.VISIBLE : View.GONE);
        binding.etReminderTime.setText(minutes != null ? DateUtils.formatMinutesOfDay(minutes) : "");
        bindingState = false;
    }

    private void pickStartDate() {
        Habit habit = viewModel.current();
        if (habit == null) {
            return;
        }
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.habit_start_date_hint)
                .setSelection(DateUtils.epochDayToUtcMillis(habit.getStartDate()))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                habit.setStartDate(DateUtils.utcMillisToEpochDay(selection));
                viewModel.notifyDraftChanged();
            }
        });
        picker.show(getSupportFragmentManager(), "habit_start_date_picker");
    }

    private void pickReminderTime() {
        Habit habit = viewModel.current();
        if (habit == null) {
            return;
        }
        int minutes = habit.getReminderMinutes() != null ? habit.getReminderMinutes() : 7 * 60;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(minutes / 60)
                .setMinute(minutes % 60)
                .setTitleText(R.string.habit_reminder_time_hint)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            habit.setReminderMinutes(picker.getHour() * 60 + picker.getMinute());
            viewModel.notifyDraftChanged();
        });
        picker.addOnNegativeButtonClickListener(v -> viewModel.notifyDraftChanged());
        picker.addOnCancelListener(d -> viewModel.notifyDraftChanged());
        picker.show(getSupportFragmentManager(), "habit_reminder_picker");
    }

    private void save() {
        ViewUtils.hideKeyboard(binding.getRoot());
        Habit habit = viewModel.current();
        if (habit == null) {
            return;
        }
        String name = binding.etName.getText() == null ? "" : binding.etName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.tilName.setError(getString(R.string.error_name_required));
            return;
        }
        binding.tilName.setError(null);

        int target;
        try {
            target = Integer.parseInt(binding.etTarget.getText() == null ? ""
                    : binding.etTarget.getText().toString().trim());
        } catch (NumberFormatException e) {
            target = 0;
        }
        if (target <= 0) {
            binding.tilTarget.setError(getString(R.string.error_target_invalid));
            return;
        }
        binding.tilTarget.setError(null);

        if ((habit.getFrequencyMask() & Habit.MASK_DAILY) == 0) {
            binding.tvFrequencyError.setVisibility(View.VISIBLE);
            return;
        }

        habit.setName(name);
        String description = binding.etDescription.getText() == null ? ""
                : binding.etDescription.getText().toString().trim();
        habit.setDescription(description.isEmpty() ? null : description);
        habit.setDailyTarget(target);
        String unit = binding.etUnit.getText() == null ? "" : binding.etUnit.getText().toString().trim();
        habit.setTargetUnit(unit.isEmpty() ? null : unit);

        binding.btnSave.setEnabled(false);
        viewModel.save(result -> {
            binding.btnSave.setEnabled(true);
            if (result.success) {
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                binding.tilName.setError(result.error);
            }
        });
    }
}
