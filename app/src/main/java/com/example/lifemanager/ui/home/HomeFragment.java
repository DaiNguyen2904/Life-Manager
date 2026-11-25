package com.example.lifemanager.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lifemanager.MainActivity;
import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Note;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.databinding.DialogQuickAddBinding;
import com.example.lifemanager.databinding.FragmentHomeBinding;
import com.example.lifemanager.adapter.HomeHabitAdapter;
import com.example.lifemanager.adapter.HomeTaskAdapter;
import com.example.lifemanager.adapter.NoteAdapter;
import com.example.lifemanager.ui.habit.HabitEditorActivity;
import com.example.lifemanager.ui.habit.HabitListActivity;
import com.example.lifemanager.ui.task.TaskEditorActivity;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.OperationResult;
import com.example.lifemanager.utils.ResultCallback;
import com.example.lifemanager.viewmodel.HomeViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Màn hình Trang chủ: ngày tháng, việc hôm nay & thói quen, ghi chú nhanh, thống kê nhanh.
 */
public class HomeFragment extends Fragment {

    private static final String KEY_TAB = "home_selected_tab";

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private HomeHabitAdapter habitAdapter;
    private HomeTaskAdapter taskAdapter;
    private NoteAdapter noteAdapter;
    private boolean updatingNoteText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupTabs(savedInstanceState == null ? 0 : savedInstanceState.getInt(KEY_TAB, 0));
        setupLists();
        setupNoteEditor();

        binding.btnQuickAdd.setOnClickListener(v -> showQuickAddDialog());
        binding.btnManageHabits.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HabitListActivity.class)));
        binding.btnViewAllTasks.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_tasks);
            }
        });

        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshDay();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.flushNote();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putInt(KEY_TAB, binding.tabLayout.getSelectedTabPosition());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ------------------------------------------------------------------ Thiết lập giao diện

    private void setupTabs(int selected) {
        TabLayout tabs = binding.tabLayout;
        tabs.addTab(tabs.newTab().setText(R.string.home_tab_today));
        tabs.addTab(tabs.newTab().setText(R.string.home_tab_stats));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPage(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        TabLayout.Tab tab = tabs.getTabAt(selected);
        if (tab != null) {
            tab.select();
        }
        showPage(selected);
    }

    private void showPage(int position) {
        binding.scrollToday.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.scrollStats.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
    }

    private void setupLists() {
        habitAdapter = new HomeHabitAdapter(item -> viewModel.toggleHabit(item));
        binding.rvHabits.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHabits.setAdapter(habitAdapter);

        taskAdapter = new HomeTaskAdapter(new HomeTaskAdapter.Listener() {
            @Override
            public void onToggle(@NonNull Task task) {
                viewModel.toggleTask(task);
            }

            @Override
            public void onOpen(@NonNull Task task) {
                startActivity(TaskEditorActivity.editIntent(requireContext(), task.getId()));
            }
        });
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(taskAdapter);

        noteAdapter = new NoteAdapter(new NoteAdapter.Listener() {
            @Override
            public void onOpen(@NonNull Note note) {
                viewModel.openNote(note);
                binding.etNote.requestFocus();
            }

            @Override
            public void onDelete(@NonNull Note note) {
                DialogUtils.confirm(requireContext(), R.string.note_delete_title,
                        getString(R.string.note_delete_message), R.string.action_delete,
                        () -> viewModel.deleteNote(note.getId()));
            }
        });
        binding.rvNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotes.setAdapter(noteAdapter);
    }

    private void setupNoteEditor() {
        binding.etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!updatingNoteText) {
                    viewModel.onNoteTextChanged(s.toString());
                }
            }
        });
        binding.btnNewNote.setOnClickListener(v -> {
            viewModel.newNote();
            binding.etNote.requestFocus();
        });
        binding.btnDeleteNote.setOnClickListener(v -> {
            String text = binding.etNote.getText() == null ? "" : binding.etNote.getText().toString();
            Long id = viewModel.getCurrentNoteId().getValue();
            if (text.trim().isEmpty() && (id == null || id == 0)) {
                return;
            }
            DialogUtils.confirm(requireContext(), R.string.note_delete_title,
                    getString(R.string.note_delete_message), R.string.action_delete,
                    () -> viewModel.deleteCurrentNote());
        });
    }

    // ------------------------------------------------------------------ Quan sát dữ liệu

    private void observeViewModel() {
        viewModel.getToday().observe(getViewLifecycleOwner(), this::renderDate);

        viewModel.getTodayHabits().observe(getViewLifecycleOwner(), this::renderHabits);
        viewModel.getTodayTasks().observe(getViewLifecycleOwner(), this::renderTasks);

        viewModel.getEditorText().observe(getViewLifecycleOwner(), text -> {
            String current = binding.etNote.getText() == null ? "" : binding.etNote.getText().toString();
            if (text != null && !text.equals(current)) {
                updatingNoteText = true;
                binding.etNote.setText(text);
                binding.etNote.setSelection(text.length());
                updatingNoteText = false;
            }
        });
        viewModel.getNoteStatus().observe(getViewLifecycleOwner(), status ->
                binding.tvNoteStatus.setText(status == null || status.isEmpty()
                        ? getString(R.string.note_status_auto) : status));
        viewModel.getCurrentNoteId().observe(getViewLifecycleOwner(), id ->
                noteAdapter.setSelectedId(id == null ? 0 : id));
        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            noteAdapter.submitList(notes);
            int count = notes == null ? 0 : notes.size();
            binding.tvSavedNotesHeader.setText(getString(R.string.home_saved_notes_header, count));
            binding.tvSavedNotesHeader.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });

        // Trang 2: thống kê nhanh
        viewModel.getMonthIncome().observe(getViewLifecycleOwner(), value ->
                binding.tvStatIncome.setText(CurrencyUtils.format(value == null ? 0 : value)));
        viewModel.getMonthExpense().observe(getViewLifecycleOwner(), value ->
                binding.tvStatExpense.setText(CurrencyUtils.format(value == null ? 0 : value)));
        viewModel.getBalance().observe(getViewLifecycleOwner(), value ->
                binding.tvStatBalance.setText(CurrencyUtils.format(value == null ? 0 : value)));
        viewModel.getTaskStats().observe(getViewLifecycleOwner(), this::renderTaskStats);
        viewModel.getHabitSnapshot().observe(getViewLifecycleOwner(), this::renderHabitStats);
    }

    private void renderDate(@Nullable LocalDate date) {
        if (date == null) {
            return;
        }
        binding.tvFullDate.setText(DateUtils.formatFullDate(date));
        int dayOfYear = DateUtils.dayOfYear(date);
        int total = DateUtils.daysInYear(date);
        binding.tvDayOfYear.setText(getString(R.string.home_day_of_year, dayOfYear, total));
        binding.progressYear.setMax(total);
        binding.progressYear.setProgressCompat(dayOfYear, false);
        binding.tvStatsTitle.setText(getString(R.string.home_stats_title,
                DateUtils.formatMonth(YearMonth.from(date))));
        taskAdapter.setToday(date.toEpochDay());
    }

    private void renderHabits(@Nullable List<HabitWithStats> habits) {
        habitAdapter.submitList(habits);
        int total = habits == null ? 0 : habits.size();
        int done = 0;
        if (habits != null) {
            for (HabitWithStats h : habits) {
                if (h.doneToday) {
                    done++;
                }
            }
        }
        binding.tvHabitHeader.setText(getString(R.string.home_habits_header, done, total));
        binding.tvHabitsEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void renderTasks(@Nullable List<Task> tasks) {
        taskAdapter.submitList(tasks);
        int total = tasks == null ? 0 : tasks.size();
        int done = 0;
        if (tasks != null) {
            for (Task t : tasks) {
                if (t.isDone()) {
                    done++;
                }
            }
        }
        binding.tvTaskHeader.setText(getString(R.string.home_tasks_header, done, total));
        binding.tvTasksEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void renderTaskStats(@Nullable TaskStats stats) {
        if (stats == null) {
            return;
        }
        binding.tvStatTasksDone.setText(getString(R.string.fraction_format, stats.done, stats.total));
        binding.tvStatTasksRate.setText(getString(R.string.stat_tasks_rate, stats.getCompletionRate()));
        binding.tvStatOverdue.setText(String.valueOf(stats.overdue));
        binding.tvStatInProgress.setText(getString(R.string.stat_tasks_in_progress, stats.inProgress, stats.todo));
    }

    private void renderHabitStats(@Nullable HabitSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        int rate = snapshot.getTodayRate();
        binding.tvStatHabitRate.setText(getString(R.string.percent_format, rate));
        binding.progressStatHabit.setProgressCompat(rate, true);
        binding.tvStatHabitDetail.setText(getString(R.string.stat_habit_detail,
                snapshot.getDoneTodayCount(), snapshot.getScheduledTodayCount()));
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
        binding.tvStatHabitWeek.setText(getString(R.string.percent_format,
                weekScheduled == 0 ? 0 : Math.round(weekDone * 100f / weekScheduled)));
        binding.tvStatHabitMonth.setText(getString(R.string.percent_format,
                monthScheduled == 0 ? 0 : Math.round(monthDone * 100f / monthScheduled)));
    }

    // ------------------------------------------------------------------ Thêm nhanh

    private void showQuickAddDialog() {
        DialogQuickAddBinding dialogBinding = DialogQuickAddBinding.inflate(getLayoutInflater());
        dialogBinding.toggleQuickType.check(R.id.btnQuickTask);
        dialogBinding.toggleQuickType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            boolean isTask = checkedId == R.id.btnQuickTask;
            dialogBinding.tilQuickName.setHint(isTask ? R.string.quick_add_task_hint : R.string.quick_add_habit_hint);
            dialogBinding.tvQuickHint.setText(isTask ? R.string.quick_add_task_desc : R.string.quick_add_habit_desc);
            dialogBinding.tilQuickName.setError(null);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.quick_add_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_add, null)
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(R.string.quick_add_details, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = dialogBinding.etQuickName.getText() == null ? ""
                        : dialogBinding.etQuickName.getText().toString().trim();
                if (name.isEmpty()) {
                    dialogBinding.tilQuickName.setError(getString(R.string.error_name_required));
                    return;
                }
                boolean isTask = dialogBinding.toggleQuickType.getCheckedButtonId() == R.id.btnQuickTask;
                ResultCallback<OperationResult> callback = result -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (result.success) {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), isTask ? R.string.msg_task_added
                                : R.string.msg_habit_added, Toast.LENGTH_SHORT).show();
                    } else {
                        dialogBinding.tilQuickName.setError(result.error);
                    }
                };
                if (isTask) {
                    viewModel.quickAddTask(name, callback);
                } else {
                    viewModel.quickAddHabit(name, callback);
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String name = dialogBinding.etQuickName.getText() == null ? ""
                        : dialogBinding.etQuickName.getText().toString().trim();
                boolean isTask = dialogBinding.toggleQuickType.getCheckedButtonId() == R.id.btnQuickTask;
                dialog.dismiss();
                if (isTask) {
                    startActivity(TaskEditorActivity.newIntent(requireContext(), null, name));
                } else {
                    startActivity(HabitEditorActivity.newIntent(requireContext(), name));
                }
            });
        });
        dialog.show();
    }
}
