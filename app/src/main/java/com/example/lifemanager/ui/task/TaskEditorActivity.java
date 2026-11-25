package com.example.lifemanager.ui.task;

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
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.databinding.ActivityTaskEditorBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.TaskEditorViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.LocalDateTime;

/**
 * Màn hình thêm / sửa công việc.
 */
public class TaskEditorActivity extends BaseActivity {

    private static final String EXTRA_TASK_ID = "extra_task_id";
    private static final String EXTRA_INITIAL_STATUS = "extra_initial_status";
    private static final String EXTRA_PREFILL_TITLE = "extra_prefill_title";

    private static final String[] STATUSES = {Task.STATUS_TODO, Task.STATUS_IN_PROGRESS, Task.STATUS_DONE};

    private ActivityTaskEditorBinding binding;
    private TaskEditorViewModel viewModel;
    private long taskId;
    private boolean bindingReminderSwitch;
    @Nullable
    private Long originalReminder;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @Nullable String initialStatus,
                                   @Nullable String prefillTitle) {
        Intent intent = new Intent(context, TaskEditorActivity.class);
        if (initialStatus != null) {
            intent.putExtra(EXTRA_INITIAL_STATUS, initialStatus);
        }
        if (prefillTitle != null) {
            intent.putExtra(EXTRA_PREFILL_TITLE, prefillTitle);
        }
        return intent;
    }

    @NonNull
    public static Intent editIntent(@NonNull Context context, long taskId) {
        return new Intent(context, TaskEditorActivity.class).putExtra(EXTRA_TASK_ID, taskId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(TaskEditorViewModel.class);
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);

        setupToolbar();
        setupForm();

        if (!viewModel.isInitialized()) {
            if (taskId > 0) {
                viewModel.load(taskId, found -> {
                    if (!found) {
                        Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Task task = viewModel.current();
                    if (task != null) {
                        originalReminder = task.getReminderAt();
                        binding.etTitle.setText(task.getTitle());
                        binding.etDescription.setText(task.getDescription());
                        binding.etNote.setText(task.getNote());
                    }
                });
            } else {
                viewModel.initNew(getIntent().getStringExtra(EXTRA_INITIAL_STATUS));
                String prefill = getIntent().getStringExtra(EXTRA_PREFILL_TITLE);
                if (!TextUtils.isEmpty(prefill)) {
                    binding.etTitle.setText(prefill);
                }
            }
        } else {
            Task task = viewModel.current();
            originalReminder = task == null ? null : task.getReminderAt();
        }

        viewModel.getDraft().observe(this, this::renderDraft);
    }

    private void setupToolbar() {
        boolean editing = taskId > 0;
        binding.toolbar.setTitle(editing ? R.string.task_edit_title : R.string.task_add_title);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (editing) {
            binding.toolbar.inflateMenu(R.menu.menu_editor);
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    DialogUtils.confirm(this, R.string.task_delete_title,
                            getString(R.string.task_delete_message, textOf(binding.etTitle.getText())),
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
        String[] statusLabels = {
                getString(R.string.status_todo), getString(R.string.status_in_progress), getString(R.string.status_done)
        };
        binding.actStatus.setSimpleItems(statusLabels);
        binding.actStatus.setOnItemClickListener((parent, view, position, id) -> {
            Task task = viewModel.current();
            if (task != null && position >= 0 && position < STATUSES.length) {
                task.setStatus(STATUSES[position]);
            }
        });

        binding.togglePriority.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            Task task = viewModel.current();
            if (!isChecked || task == null) {
                return;
            }
            if (checkedId == R.id.btnPriorityHigh) {
                task.setPriority(Task.PRIORITY_HIGH);
            } else if (checkedId == R.id.btnPriorityLow) {
                task.setPriority(Task.PRIORITY_LOW);
            } else {
                task.setPriority(Task.PRIORITY_MEDIUM);
            }
        });

        binding.etStartDate.setOnClickListener(v -> pickDate(true));
        binding.etDueDate.setOnClickListener(v -> pickDate(false));
        binding.tilStartDate.setEndIconOnClickListener(v -> {
            Task task = viewModel.current();
            if (task != null) {
                task.setStartDate(null);
                viewModel.notifyDraftChanged();
            }
        });
        binding.tilDueDate.setEndIconOnClickListener(v -> {
            Task task = viewModel.current();
            if (task != null) {
                task.setDueDate(null);
                viewModel.notifyDraftChanged();
            }
        });

        binding.switchReminder.setOnCheckedChangeListener((button, checked) -> {
            if (bindingReminderSwitch) {
                return;
            }
            Task task = viewModel.current();
            if (task == null) {
                return;
            }
            if (checked) {
                pickReminder();
            } else {
                task.setReminderAt(null);
                viewModel.notifyDraftChanged();
            }
        });
        binding.etReminder.setOnClickListener(v -> pickReminder());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void renderDraft(@Nullable Task task) {
        if (task == null) {
            return;
        }
        binding.etStartDate.setText(task.getStartDate() == null ? "" : DateUtils.formatDate(task.getStartDate()));
        binding.etDueDate.setText(task.getDueDate() == null ? "" : DateUtils.formatDate(task.getDueDate()));

        int priorityButton;
        if (task.getPriority() == Task.PRIORITY_HIGH) {
            priorityButton = R.id.btnPriorityHigh;
        } else if (task.getPriority() == Task.PRIORITY_LOW) {
            priorityButton = R.id.btnPriorityLow;
        } else {
            priorityButton = R.id.btnPriorityMedium;
        }
        if (binding.togglePriority.getCheckedButtonId() != priorityButton) {
            binding.togglePriority.check(priorityButton);
        }

        int statusIndex = 0;
        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i].equals(task.getStatus())) {
                statusIndex = i;
            }
        }
        binding.actStatus.setText(binding.actStatus.getAdapter() != null
                ? binding.actStatus.getAdapter().getItem(statusIndex).toString()
                : getString(R.string.status_todo), false);

        bindingReminderSwitch = true;
        boolean hasReminder = task.getReminderAt() != null;
        binding.switchReminder.setChecked(hasReminder);
        bindingReminderSwitch = false;
        binding.tilReminder.setVisibility(hasReminder ? View.VISIBLE : View.GONE);
        binding.etReminder.setText(hasReminder ? DateUtils.formatDateTime(task.getReminderAt()) : "");
    }

    private void pickDate(boolean start) {
        Task task = viewModel.current();
        if (task == null) {
            return;
        }
        Long current = start ? task.getStartDate() : task.getDueDate();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(start ? R.string.task_start_date_hint : R.string.task_due_date_hint)
                .setSelection(DateUtils.epochDayToUtcMillis(current == null ? DateUtils.todayEpochDay() : current))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            long day = DateUtils.utcMillisToEpochDay(selection);
            if (start) {
                task.setStartDate(day);
            } else {
                task.setDueDate(day);
                binding.tilDueDate.setError(null);
            }
            viewModel.notifyDraftChanged();
        });
        picker.show(getSupportFragmentManager(), start ? "start_date_picker" : "due_date_picker");
    }

    /** Chọn ngày rồi chọn giờ nhắc nhở. */
    private void pickReminder() {
        Task task = viewModel.current();
        if (task == null) {
            return;
        }
        LocalDateTime initial;
        if (task.getReminderAt() != null) {
            initial = DateUtils.toLocalDateTime(task.getReminderAt());
        } else if (task.getDueDate() != null && task.getDueDate() >= DateUtils.todayEpochDay()) {
            initial = DateUtils.fromEpochDay(task.getDueDate()).atTime(8, 0);
        } else {
            initial = LocalDateTime.now().plusHours(1).withMinute(0);
        }
        final LocalDateTime start = initial;
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.task_reminder_hint)
                .setSelection(DateUtils.epochDayToUtcMillis(start.toLocalDate().toEpochDay()))
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            long day = DateUtils.utcMillisToEpochDay(selection);
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(start.getHour())
                    .setMinute(start.getMinute())
                    .setTitleText(R.string.task_reminder_time_title)
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                task.setReminderAt(DateUtils.toEpochMillis(day, timePicker.getHour(), timePicker.getMinute()));
                binding.tilReminder.setError(null);
                viewModel.notifyDraftChanged();
            });
            timePicker.addOnNegativeButtonClickListener(v -> viewModel.notifyDraftChanged());
            timePicker.addOnCancelListener(d -> viewModel.notifyDraftChanged());
            timePicker.show(getSupportFragmentManager(), "reminder_time_picker");
        });
        datePicker.addOnNegativeButtonClickListener(v -> viewModel.notifyDraftChanged());
        datePicker.addOnCancelListener(d -> viewModel.notifyDraftChanged());
        datePicker.show(getSupportFragmentManager(), "reminder_date_picker");
    }

    private void save() {
        ViewUtils.hideKeyboard(binding.getRoot());
        Task task = viewModel.current();
        if (task == null) {
            return;
        }
        String title = textOf(binding.etTitle.getText()).trim();
        if (title.isEmpty()) {
            binding.tilTitle.setError(getString(R.string.error_title_required));
            return;
        }
        binding.tilTitle.setError(null);
        if (task.getStartDate() != null && task.getDueDate() != null && task.getDueDate() < task.getStartDate()) {
            binding.tilDueDate.setError(getString(R.string.error_due_before_start));
            return;
        }
        binding.tilDueDate.setError(null);
        Long reminder = task.getReminderAt();
        if (reminder != null && reminder <= System.currentTimeMillis()
                && !reminder.equals(originalReminder) && !task.isDone()) {
            binding.tilReminder.setError(getString(R.string.error_reminder_past));
            return;
        }
        binding.tilReminder.setError(null);

        task.setTitle(title);
        String description = textOf(binding.etDescription.getText()).trim();
        task.setDescription(description.isEmpty() ? null : description);
        String note = textOf(binding.etNote.getText()).trim();
        task.setNote(note.isEmpty() ? null : note);

        binding.btnSave.setEnabled(false);
        ResultCallback<com.example.lifemanager.utils.OperationResult> callback = result -> {
            binding.btnSave.setEnabled(true);
            if (result.success) {
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
            }
        };
        viewModel.save(callback);
    }

    @NonNull
    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
