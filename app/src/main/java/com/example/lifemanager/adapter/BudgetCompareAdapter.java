package com.example.lifemanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.databinding.ItemBudgetCompareBinding;
import com.example.lifemanager.utils.CurrencyUtils;

import java.util.Objects;

/**
 * So sánh ngân sách và số tiền đã chi của từng danh mục (Báo cáo - So sánh ngân sách).
 */
public class BudgetCompareAdapter extends ListAdapter<CategorySpending, BudgetCompareAdapter.ViewHolder> {

    public BudgetCompareAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBudgetCompareBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategorySpending item = getItem(position);
        ItemBudgetCompareBinding b = holder.binding;
        Context context = b.getRoot().getContext();
        long budget = item.budget == null ? 0 : item.budget;

        b.tvName.setText(item.name);
        b.tvSpent.setText(context.getString(R.string.compare_spent, CurrencyUtils.format(item.spent)));
        b.tvBudget.setText(context.getString(R.string.compare_budget, CurrencyUtils.format(budget)));
        int percent = item.getUsagePercent();
        b.progress.setProgressCompat(Math.min(percent, 100), false);

        if (item.isOverBudget()) {
            int danger = ContextCompat.getColor(context, R.color.lm_danger);
            b.tvStatus.setText(R.string.compare_over_budget);
            b.tvStatus.setTextColor(danger);
            b.progress.setIndicatorColor(danger);
            b.tvRemaining.setText(context.getString(R.string.compare_over_amount,
                    CurrencyUtils.format(item.spent - budget), percent));
            b.tvRemaining.setTextColor(danger);
        } else {
            int success = ContextCompat.getColor(context, R.color.lm_success);
            b.tvStatus.setText(R.string.compare_in_budget);
            b.tvStatus.setTextColor(success);
            b.progress.setIndicatorColor(ContextCompat.getColor(context,
                    percent >= 80 ? R.color.lm_warning : R.color.lm_primary));
            b.tvRemaining.setText(context.getString(R.string.compare_remaining,
                    CurrencyUtils.format(item.getRemaining()), percent));
            b.tvRemaining.setTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBudgetCompareBinding binding;

        ViewHolder(@NonNull ItemBudgetCompareBinding binding) {
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
            return oldItem.spent == newItem.spent && Objects.equals(oldItem.budget, newItem.budget)
                    && Objects.equals(oldItem.name, newItem.name);
        }
    };
}
