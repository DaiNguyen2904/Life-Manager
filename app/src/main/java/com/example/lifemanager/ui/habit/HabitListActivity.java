package com.example.lifemanager.ui.habit;

import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lifemanager.R;
import com.example.lifemanager.adapter.HabitStatsAdapter;
import com.example.lifemanager.data.model.HabitSnapshot;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.databinding.ActivityHabitListBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.DialogUtils;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.HabitViewModel;

/**
 * Màn hình Quản lý thói quen: danh sách, Streak, kỷ lục, tỷ lệ tuần / tháng, thêm / sửa / xóa.
 */
public class HabitListActivity extends BaseActivity {

    private ActivityHabitListBinding binding;
    private HabitViewModel viewModel;
    private HabitStatsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHabitListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(HabitViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new HabitStatsAdapter(false, new HabitStatsAdapter.Listener() {
            @Override
            public void onToggle(@NonNull HabitWithStats item) {
                viewModel.toggleToday(item);
            }

            @Override
            public void onClick(@NonNull HabitWithStats item) {
                startActivity(HabitEditorActivity.editIntent(HabitListActivity.this, item.habit.getId()));
            }

            @Override
            public void onMore(@NonNull HabitWithStats item, @NonNull View anchor) {
                showMenu(item, anchor);
            }
        });
        binding.rvHabits.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHabits.setAdapter(adapter);

        binding.fabAddHabit.setOnClickListener(v ->
                startActivity(HabitEditorActivity.newIntent(this, null)));

        viewModel.getSnapshot().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void render(@Nullable HabitSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        adapter.submitList(snapshot.stats);
        binding.tvEmpty.setVisibility(snapshot.stats.isEmpty() ? View.VISIBLE : View.GONE);

        binding.tvTodayRate.setText(getString(R.string.fraction_format,
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
        binding.tvWeekRate.setText(getString(R.string.percent_format,
                weekScheduled == 0 ? 0 : Math.round(weekDone * 100f / weekScheduled)));
        binding.tvMonthRate.setText(getString(R.string.percent_format,
                monthScheduled == 0 ? 0 : Math.round(monthDone * 100f / monthScheduled)));
    }

    private void showMenu(@NonNull HabitWithStats item, @NonNull View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_habit_item, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.action_edit) {
                startActivity(HabitEditorActivity.editIntent(this, item.habit.getId()));
            } else if (menuItem.getItemId() == R.id.action_delete) {
                DialogUtils.confirm(this, R.string.habit_delete_title,
                        getString(R.string.habit_delete_message, item.habit.getName()),
                        R.string.action_delete, () -> {
                            viewModel.deleteHabit(item.habit);
                            Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                        });
            }
            return true;
        });
        popup.show();
    }
}
