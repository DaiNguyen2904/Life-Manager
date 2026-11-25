package com.example.lifemanager.ui.task;

import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lifemanager.R;
import com.example.lifemanager.adapter.TaskAdapter;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.databinding.BottomSheetTaskDetailBinding;
import com.example.lifemanager.databinding.FragmentTaskBinding;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.TaskUiUtils;
import com.example.lifemanager.viewmodel.TaskViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

/**
 * Màn hình Công việc dạng Kanban: 4 cột (Cần làm, Đang làm, Hoàn thành, Quá hạn),
 * tìm kiếm, lọc theo ưu tiên, sắp xếp theo thời hạn.
 */
public class TaskFragment extends Fragment {

    private static final String[] COLUMNS = {
            Task.STATUS_TODO, Task.STATUS_IN_PROGRESS, Task.STATUS_DONE, Task.STATUS_OVERDUE
    };

    private FragmentTaskBinding binding;
    private TaskViewModel viewModel;
    private TaskAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        setupTabs();
        setupFilters();

        adapter = new TaskAdapter(new TaskAdapter.Listener() {
            @Override
            public void onClick(@NonNull Task task) {
                showTaskDetail(task);
            }

            @Override
            public void onToggleDone(@NonNull Task task) {
                viewModel.toggleDone(task);
                Toast.makeText(requireContext(), task.isDone() ? R.string.msg_task_reopened
                        : R.string.msg_task_completed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMore(@NonNull Task task, @NonNull View anchor) {
                showTaskMenu(task, anchor);
            }
        });
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);

        binding.fabAddTask.setOnClickListener(v -> {
            String column = viewModel.getColumn().getValue();
            startActivity(TaskEditorActivity.newIntent(requireContext(),
                    Task.STATUS_OVERDUE.equals(column) ? null : column, null));
        });

        viewModel.getBoard().observe(getViewLifecycleOwner(), this::renderBoard);
        viewModel.getSortAscending().observe(getViewLifecycleOwner(), asc ->
                binding.btnSort.setText(asc == null || asc ? R.string.task_sort_asc : R.string.task_sort_desc));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshDay();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupTabs() {
        TabLayout tabs = binding.tabStatus;
        for (int i = 0; i < COLUMNS.length; i++) {
            tabs.addTab(tabs.newTab().setText(tabLabel(COLUMNS[i], 0)));
        }
        String current = viewModel.getColumn().getValue();
        for (int i = 0; i < COLUMNS.length; i++) {
            if (COLUMNS[i].equals(current)) {
                TabLayout.Tab tab = tabs.getTabAt(i);
                if (tab != null) {
                    tab.select();
                }
            }
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setColumn(COLUMNS[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @NonNull
    private CharSequence tabLabel(@NonNull String column, int count) {
        switch (column) {
            case Task.STATUS_IN_PROGRESS:
                return getString(R.string.task_tab_in_progress, count);
            case Task.STATUS_DONE:
                return getString(R.string.task_tab_done, count);
            case Task.STATUS_OVERDUE:
                SpannableString text = new SpannableString(getString(R.string.task_tab_overdue, count));
                text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.lm_danger)),
                        0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return text;
            case Task.STATUS_TODO:
            default:
                return getString(R.string.task_tab_todo, count);
        }
    }

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
        binding.chipGroupPriority.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipPriorityHigh) {
                viewModel.setPriority(Task.PRIORITY_HIGH);
            } else if (id == R.id.chipPriorityMedium) {
                viewModel.setPriority(Task.PRIORITY_MEDIUM);
            } else if (id == R.id.chipPriorityLow) {
                viewModel.setPriority(Task.PRIORITY_LOW);
            } else {
                viewModel.setPriority(TaskViewModel.PRIORITY_ALL);
            }
        });
        binding.btnSort.setOnClickListener(v -> viewModel.toggleSort());
    }

    private void renderBoard(@Nullable TaskViewModel.TaskBoard board) {
        if (board == null) {
            return;
        }
        TaskStats stats = board.stats;
        int[] counts = {stats.todo, stats.inProgress, stats.done, stats.overdue};
        for (int i = 0; i < COLUMNS.length; i++) {
            TabLayout.Tab tab = binding.tabStatus.getTabAt(i);
            if (tab != null) {
                tab.setText(tabLabel(COLUMNS[i], counts[i]));
            }
        }
        adapter.setToday(board.todayEpochDay);
        adapter.submitList(board.visibleTasks);
        binding.tvEmpty.setVisibility(board.visibleTasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------ Menu & chi tiết

    private void showTaskMenu(@NonNull Task task, @NonNull View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_task_item, popup.getMenu());
        popup.getMenu().findItem(R.id.action_move_todo).setVisible(!Task.STATUS_TODO.equals(task.getStatus()));
        popup.getMenu().findItem(R.id.action_move_in_progress)
                .setVisible(!Task.STATUS_IN_PROGRESS.equals(task.getStatus()));
        popup.getMenu().findItem(R.id.action_move_done).setVisible(!task.isDone());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_move_todo) {
                viewModel.updateStatus(task, Task.STATUS_TODO);
            } else if (id == R.id.action_move_in_progress) {
                viewModel.updateStatus(task, Task.STATUS_IN_PROGRESS);
            } else if (id == R.id.action_move_done) {
                viewModel.updateStatus(task, Task.STATUS_DONE);
            } else if (id == R.id.action_edit) {
                startActivity(TaskEditorActivity.editIntent(requireContext(), task.getId()));
            } else if (id == R.id.action_delete) {
                confirmDelete(task);
            }
            return true;
        });
        popup.show();
    }

    private void confirmDelete(@NonNull Task task) {
        DialogUtils.confirm(requireContext(), R.string.task_delete_title,
                getString(R.string.task_delete_message, task.getTitle()), R.string.action_delete, () -> {
                    viewModel.deleteTask(task);
                    Toast.makeText(requireContext(), R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                });
    }

    private void showTaskDetail(@NonNull Task task) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        BottomSheetTaskDetailBinding b = BottomSheetTaskDetailBinding.inflate(getLayoutInflater());
        long today = DateUtils.todayEpochDay();

        b.tvTitle.setText(task.getTitle());
        TaskUiUtils.stylePriority(b.tvPriority, task.getPriority());
        TaskUiUtils.styleStatus(b.tvStatus, task.getEffectiveStatus(today));
        if (TextUtils.isEmpty(task.getDescription())) {
            b.tvDescription.setVisibility(View.GONE);
        } else {
            b.tvDescription.setText(task.getDescription());
        }
        b.tvStartDate.setText(task.getStartDate() == null ? getString(R.string.empty_value)
                : DateUtils.formatDate(task.getStartDate()));
        b.tvDueDate.setText(task.getDueDate() == null ? getString(R.string.task_no_due)
                : getString(R.string.task_due_detail_format, DateUtils.formatDate(task.getDueDate()),
                DateUtils.describeDue(task.getDueDate(), today)));
        b.tvReminder.setText(task.getReminderAt() == null ? getString(R.string.task_no_reminder)
                : DateUtils.formatDateTime(task.getReminderAt()));
        b.tvNote.setText(TextUtils.isEmpty(task.getNote()) ? getString(R.string.empty_value) : task.getNote());
        if (task.getCompletedAt() != null) {
            b.tvCompletedAt.setText(DateUtils.formatDateTime(task.getCompletedAt()));
        } else {
            b.rowCompleted.setVisibility(View.GONE);
        }

        int checkedId;
        if (task.isDone()) {
            checkedId = R.id.btnStatusDone;
        } else if (Task.STATUS_IN_PROGRESS.equals(task.getStatus())) {
            checkedId = R.id.btnStatusInProgress;
        } else {
            checkedId = R.id.btnStatusTodo;
        }
        b.toggleStatus.check(checkedId);
        b.toggleStatus.addOnButtonCheckedListener((group, id, isChecked) -> {
            if (!isChecked || id == checkedId) {
                return;
            }
            String status = id == R.id.btnStatusDone ? Task.STATUS_DONE
                    : id == R.id.btnStatusInProgress ? Task.STATUS_IN_PROGRESS : Task.STATUS_TODO;
            viewModel.updateStatus(task, status);
            Toast.makeText(requireContext(), getString(R.string.msg_status_changed,
                    TaskUiUtils.statusLabel(requireContext(), status)), Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        b.btnEdit.setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(TaskEditorActivity.editIntent(requireContext(), task.getId()));
        });
        b.btnDelete.setOnClickListener(v -> {
            sheet.dismiss();
            confirmDelete(task);
        });
        sheet.setContentView(b.getRoot());
        sheet.show();
    }
}
