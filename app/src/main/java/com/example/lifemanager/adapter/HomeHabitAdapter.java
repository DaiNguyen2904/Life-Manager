package com.example.lifemanager.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.model.HabitWithStats;
import com.example.lifemanager.databinding.ItemHomeHabitBinding;
import com.example.lifemanager.utils.HabitUiUtils;

/**
 * Danh sách thói quen cần thực hiện hôm nay (Trang chủ).
 */
public class HomeHabitAdapter extends ListAdapter<HabitWithStats, HomeHabitAdapter.ViewHolder> {

    public interface Listener {
        void onToggle(@NonNull HabitWithStats item);
    }

    private final Listener listener;

    public HomeHabitAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHomeHabitBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HabitWithStats item = getItem(position);
        ItemHomeHabitBinding b = holder.binding;
        b.tvName.setText(item.habit.getName());
        b.tvTarget.setText(HabitUiUtils.describeTarget(b.getRoot().getContext(), item.habit));
        b.tvStreak.setText(b.getRoot().getContext().getString(R.string.streak_short, item.currentStreak));
        b.ivCheck.setImageResource(item.doneToday ? R.drawable.ic_circle_checked : R.drawable.ic_circle_unchecked);
        b.tvName.setAlpha(item.doneToday ? 0.6f : 1f);
        b.getRoot().setOnClickListener(v -> listener.onToggle(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHomeHabitBinding binding;

        ViewHolder(@NonNull ItemHomeHabitBinding binding) {
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
                    && oldItem.currentStreak == newItem.currentStreak
                    && oldItem.habit.getDailyTarget() == newItem.habit.getDailyTarget()
                    && oldItem.habit.getName().equals(newItem.habit.getName())
                    && java.util.Objects.equals(oldItem.habit.getTargetUnit(), newItem.habit.getTargetUnit());
        }
    };
}
