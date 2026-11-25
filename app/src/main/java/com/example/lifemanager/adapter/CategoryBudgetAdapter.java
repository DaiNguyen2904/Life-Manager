package com.example.lifemanager.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.model.CategorySpending;
import com.example.lifemanager.databinding.ItemCategoryBudgetBinding;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.IconUtils;

import java.util.Objects;

/**
 * Danh sách ngân sách theo danh mục: đã chi / ngân sách, thanh tiến độ, cảnh báo vượt ngân sách.
 */
public class CategoryBudgetAdapter extends ListAdapter<CategorySpending, CategoryBudgetAdapter.ViewHolder> {

    public interface Listener {
        void onClick(@NonNull CategorySpending item);
    }

    private final Listener listener;

    public CategoryBudgetAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCategoryBudgetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategorySpending item = getItem(position);
        ItemCategoryBudgetBinding b = holder.binding;
        Context context = b.getRoot().getContext();

        b.ivIcon.setImageResource(IconUtils.getIconRes(item.icon));
        ImageViewCompat.setImageTintList(b.ivIcon, ColorStateList.valueOf(item.color));
        b.tvName.setText(item.name);
        long budget = item.budget == null ? 0 : item.budget;
        b.tvAmount.setText(context.getString(R.string.budget_amount_format,
                CurrencyUtils.format(item.spent), CurrencyUtils.format(budget)));

        int percent = item.getUsagePercent();
        b.progress.setProgressCompat(Math.min(percent, 100), false);
        int indicator;
        if (item.isOverBudget()) {
            indicator = ContextCompat.getColor(context, R.color.lm_danger);
            b.tvStatus.setText(context.getString(R.string.budget_over,
                    CurrencyUtils.format(item.spent - budget)));
            b.tvStatus.setTextColor(indicator);
        } else {
            indicator = ContextCompat.getColor(context, percent >= 80 ? R.color.lm_warning : R.color.lm_primary);
            b.tvStatus.setText(context.getString(R.string.budget_remaining,
                    CurrencyUtils.format(item.getRemaining()), percent));
            b.tvStatus.setTextColor(ContextCompat.getColor(context,
                    percent >= 80 ? R.color.lm_warning : R.color.lm_text_secondary));
        }
        b.progress.setIndicatorColor(indicator);
        b.getRoot().setOnClickListener(v -> listener.onClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCategoryBudgetBinding binding;

        ViewHolder(@NonNull ItemCategoryBudgetBinding binding) {
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
            return oldItem.spent == newItem.spent
                    && Objects.equals(oldItem.budget, newItem.budget)
                    && oldItem.color == newItem.color
                    && Objects.equals(oldItem.name, newItem.name)
                    && Objects.equals(oldItem.icon, newItem.icon);
        }
    };
}
