package com.example.lifemanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.databinding.ItemHabitStatsBinding;
import com.example.lifemanager.utils.HabitUiUtils;

/**
 * Danh sách thói quen kèm Streak, kỷ lục, tỷ lệ tuần / tháng.
 * Chế độ rút gọn (compact) dùng trong màn hình Báo cáo: chỉ đọc, không có nút thao tác.
 */
public class HabitStatsAdapter extends ListAdapter<HabitWithStats, HabitStatsAdapter.ViewHolder> {

    public interface Listener {
        void onToggle(@NonNull HabitWithStats item);

        void onClick(@NonNull HabitWithStats item);

        void onMore(@NonNull HabitWithStats item, @NonNull View anchor);
    }

    private final boolean compact;
    @Nullable
    private final Listener listener;

    public HabitStatsAdapter(boolean compact, @Nullable Listener listener) {
        super(DIFF);
        this.compact = compact;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHabitStatsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HabitWithStats item = getItem(position);
        ItemHabitStatsBinding b = holder.binding;
        Context context = b.getRoot().getContext();

        b.tvName.setText(item.habit.getName());
        if (!item.scheduledToday) {
            b.tvStatus.setText(R.string.habit_status_not_scheduled);
            b.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        } else if (item.doneToday) {
            b.tvStatus.setText(R.string.habit_status_done);
            b.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.lm_success));
        } else {
            b.tvStatus.setText(R.string.habit_status_pending);
            b.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        }
        b.tvStreak.setText(context.getString(R.string.streak_long, item.currentStreak));
        b.tvDetail.setText(HabitUiUtils.describeDetail(context, item.habit));
        b.tvLongest.setText(context.getString(R.string.habit_longest, item.longestStreak));
        b.tvWeek.setText(context.getString(R.string.habit_week_rate, item.getWeekRate()));
        b.tvMonth.setText(context.getString(R.string.habit_month_rate, item.getMonthRate()));
        b.progressMonth.setProgressCompat(item.getMonthRate(), false);

        if (compact) {
            b.ivCheck.setVisibility(View.GONE);
            b.btnMore.setVisibility(View.GONE);
            b.tvDetail.setVisibility(View.GONE);
            b.progressMonth.setVisibility(View.GONE);
            b.getRoot().setClickable(false);
        } else {
            b.ivCheck.setVisibility(item.scheduledToday ? View.VISIBLE : View.INVISIBLE);
            b.ivCheck.setImageResource(item.doneToday ? R.drawable.ic_circle_checked : R.drawable.ic_circle_unchecked);
            if (listener != null) {
                b.ivCheck.setOnClickListener(v -> listener.onToggle(item));
                b.btnMore.setOnClickListener(v -> listener.onMore(item, v));
                b.getRoot().setOnClickListener(v -> listener.onClick(item));
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHabitStatsBinding binding;

        ViewHolder(@NonNull ItemHabitStatsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<HabitWithStats> DIFF = new DiffUtil.ItemCallback<HabitWithStats>() {
        @Override
        public boolean areItemsTheSame(@NonNull HabitWithStats oldItem, @NonNull HabitWithStats newItem) {
            return oldItem.habit.getId() == newItem.habit.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull HabitWithStats oldItem, @NonNull HabitWithStats newItem) {
            return oldItem.doneToday == newItem.doneToday
                    && oldItem.scheduledToday == newItem.scheduledToday
                    && oldItem.currentStreak == newItem.currentStreak
                    && oldItem.longestStreak == newItem.longestStreak
                    && oldItem.getWeekRate() == newItem.getWeekRate()
                    && oldItem.getMonthRate() == newItem.getMonthRate()
                    && HabitUiUtilsEquals.sameHabit(oldItem, newItem);
        }
    };

    /** So sánh các trường hiển thị của thói quen. */
    private static final class HabitUiUtilsEquals {
        static boolean sameHabit(HabitWithStats a, HabitWithStats b) {
            return a.habit.getName().equals(b.habit.getName())
                    && a.habit.getDailyTarget() == b.habit.getDailyTarget()
                    && a.habit.getFrequencyMask() == b.habit.getFrequencyMask()
                    && java.util.Objects.equals(a.habit.getTargetUnit(), b.habit.getTargetUnit())
                    && java.util.Objects.equals(a.habit.getReminderMinutes(), b.habit.getReminderMinutes());
        }
    }
}
