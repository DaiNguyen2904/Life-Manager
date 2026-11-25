package com.example.lifemanager.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.databinding.ItemSpendingShareBinding;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.ViewUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * Tỷ lệ phần trăm và số tiền đã chi của từng danh mục (Báo cáo - Tỷ trọng chi tiêu).
 */
public class SpendingShareAdapter extends ListAdapter<CategorySpending, SpendingShareAdapter.ViewHolder> {

    private long total;

    public SpendingShareAdapter() {
        super(DIFF);
    }

    public void submit(@NonNull java.util.List<CategorySpending> items, long totalExpense) {
        boolean totalChanged = total != totalExpense;
        total = totalExpense;
        submitList(items);
        if (totalChanged) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSpendingShareBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategorySpending item = getItem(position);
        ItemSpendingShareBinding b = holder.binding;
        double percent = total <= 0 ? 0 : item.spent * 100.0 / total;
        ViewUtils.setBackgroundTint(b.colorDot, item.color);
        b.tvName.setText(item.name);
        b.tvPercent.setText(b.getRoot().getContext().getString(R.string.share_percent_format,
                String.format(Locale.US, "%.1f", percent), CurrencyUtils.format(item.spent)));
        b.progress.setIndicatorColor(item.color);
        b.progress.setProgressCompat((int) Math.round(percent * 10), false);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSpendingShareBinding binding;

        ViewHolder(@NonNull ItemSpendingShareBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<CategorySpending> DIFF = new DiffUtil.ItemCallback<CategorySpending>() {
        @Override
        public boolean areItemsTheSame(@NonNull CategorySpending oldItem, @NonNull CategorySpending newItem) {
            return oldItem.categoryId == newItem.categoryId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CategorySpending oldItem, @NonNull CategorySpending newItem) {
            return oldItem.spent == newItem.spent && oldItem.color == newItem.color
                    && Objects.equals(oldItem.name, newItem.name);
        }
    };
}
