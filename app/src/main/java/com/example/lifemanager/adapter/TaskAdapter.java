package com.example.lifemanager.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.databinding.ItemTaskBinding;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.TaskUiUtils;

import java.time.LocalDate;

/**
 * Danh sách công việc trong một cột của bảng Kanban.
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.ViewHolder> {

    public interface Listener {
        void onClick(@NonNull Task task);

        void onToggleDone(@NonNull Task task);

        void onMore(@NonNull Task task, @NonNull View anchor);
    }

    private final Listener listener;
    private long todayEpochDay = LocalDate.now().toEpochDay();

    public TaskAdapter(@NonNull Listener listener) {
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
        return new ViewHolder(ItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = getItem(position);
        ItemTaskBinding b = holder.binding;
        Context context = b.getRoot().getContext();
        String effective = task.getEffectiveStatus(todayEpochDay);

        b.priorityStrip.setBackgroundColor(TaskUiUtils.priorityColor(context, task.getPriority()));
        b.tvTitle.setText(task.getTitle());
        if (task.isDone()) {
            b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            b.tvTitle.setPaintFlags(b.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        b.ivCheck.setImageResource(task.isDone() ? R.drawable.ic_circle_checked : R.drawable.ic_circle_unchecked);

        if (TextUtils.isEmpty(task.getDescription())) {
            b.tvDescription.setVisibility(View.GONE);
        } else {
            b.tvDescription.setVisibility(View.VISIBLE);
            b.tvDescription.setText(task.getDescription());
        }

        TaskUiUtils.stylePriority(b.tvPriority, task.getPriority());
        // Ở cột Quá hạn, hiển thị trạng thái gốc (Cần làm / Đang làm) để người dùng biết tiến độ
        String statusToShow = Task.STATUS_OVERDUE.equals(effective) ? task.getStatus() : effective;
        TaskUiUtils.styleStatus(b.tvStatus, statusToShow);

        if (task.getDueDate() != null) {
            String due = context.getString(R.string.task_due_short_format,
                    DateUtils.formatShortDay(task.getDueDate()),
                    task.isDone() ? context.getString(R.string.status_done)
                            : DateUtils.describeDue(task.getDueDate(), todayEpochDay));
            b.tvDue.setText(due);
            b.tvDue.setTextColor(ContextCompat.getColor(context,
                    task.isOverdue(todayEpochDay) ? R.color.lm_danger : R.color.lm_text_secondary));
        } else {
            b.tvDue.setText(R.string.task_no_due);
            b.tvDue.setTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        }

        b.getRoot().setOnClickListener(v -> listener.onClick(task));
        b.ivCheck.setOnClickListener(v -> listener.onToggleDone(task));
        b.btnMore.setOnClickListener(v -> listener.onMore(task, v));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemTaskBinding binding;

        ViewHolder(@NonNull ItemTaskBinding binding) {
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
