package com.example.lifemanager.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.databinding.ItemHomeTaskBinding;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.TaskUiUtils;

import java.time.LocalDate;

/**
 * Danh sách công việc cần làm hôm nay (Trang chủ).
 */
public class HomeTaskAdapter extends ListAdapter<Task, HomeTaskAdapter.ViewHolder> {

    public interface Listener {
        void onToggle(@NonNull Task task);

        void onOpen(@NonNull Task task);
    }

    private final Listener listener;
    private long todayEpochDay = LocalDate.now().toEpochDay();

    public HomeTaskAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setToday(long epochDay) {
        if (todayEpochDay != epochDay) {
            todayEpochDay = epochDay;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHomeTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = getItem(position);
        ItemHomeTaskBinding b = holder.binding;
        Context context = b.getRoot().getContext();

        b.tvTitle.setText(task.getTitle());
        boolean done = task.isDone();
        if (done) {
            b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            b.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        } else {
            b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            b.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.lm_text_primary));
        }
        b.ivCheck.setImageResource(done ? R.drawable.ic_circle_checked : R.drawable.ic_circle_unchecked);

        if (done && task.getCompletedAt() != null) {
            b.tvDue.setText(context.getString(R.string.task_completed_at_format,
                    DateUtils.formatDateTime(task.getCompletedAt())));
            b.tvDue.setTextColor(ContextCompat.getColor(context, R.color.lm_success));
        } else if (task.getDueDate() != null) {
            b.tvDue.setText(context.getString(R.string.task_due_format,
                    DateUtils.formatDate(task.getDueDate()),
                    DateUtils.describeDue(task.getDueDate(), todayEpochDay)));
            b.tvDue.setTextColor(ContextCompat.getColor(context,
                    task.isOverdue(todayEpochDay) ? R.color.lm_danger : R.color.lm_text_secondary));
        } else {
            b.tvDue.setText(R.string.task_no_due);
            b.tvDue.setTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        }
        TaskUiUtils.stylePriority(b.tvPriority, task.getPriority());

        b.ivCheck.setOnClickListener(v -> listener.onToggle(task));
        b.getRoot().setOnClickListener(v -> listener.onOpen(task));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHomeTaskBinding binding;

        ViewHolder(@NonNull ItemHomeTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Task> DIFF = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getUpdatedAt() == newItem.getUpdatedAt()
                    && oldItem.getStatus().equals(newItem.getStatus());
        }
    };
}
