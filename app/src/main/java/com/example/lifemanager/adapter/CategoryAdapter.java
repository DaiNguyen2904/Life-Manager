package com.example.lifemanager.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Category;
import com.example.lifemanager.databinding.ItemCategoryBinding;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.IconUtils;
import com.example.lifemanager.utils.ViewUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Danh sách danh mục trong màn hình Quản lý danh mục.
 */
public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(@NonNull Category category);

        void onDelete(@NonNull Category category);
    }

    private final Listener listener;
    private Map<Long, Long> budgets = Collections.emptyMap();

    public CategoryAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setBudgets(@NonNull Map<Long, Long> budgets) {
        this.budgets = budgets;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = getItem(position);
        ItemCategoryBinding b = holder.binding;
        Context context = b.getRoot().getContext();

        b.ivIcon.setImageResource(IconUtils.getIconRes(category.getIcon()));
        ImageViewCompat.setImageTintList(b.ivIcon, ColorStateList.valueOf(category.getColor()));
        ViewUtils.setBackgroundTint(b.ivIcon, ViewUtils.softColor(category.getColor(), 0.8f));
        b.tvName.setText(category.getName());

        String subtitle;
        if (category.isExpense()) {
            Long budget = budgets.get(category.getId());
            subtitle = budget != null
                    ? context.getString(R.string.category_budget_format, CurrencyUtils.format(budget))
                    : context.getString(R.string.category_no_budget);
        } else {
            subtitle = context.getString(R.string.category_income_label);
        }
        if (category.isBuiltIn()) {
            subtitle += context.getString(R.string.category_default_suffix);
        }
        b.tvSubtitle.setText(subtitle);

        b.getRoot().setOnClickListener(v -> listener.onEdit(category));
        b.btnEdit.setOnClickListener(v -> listener.onEdit(category));
        b.btnDelete.setOnClickListener(v -> listener.onDelete(category));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCategoryBinding binding;

        ViewHolder(@NonNull ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Category> DIFF = new DiffUtil.ItemCallback<Category>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getColor() == newItem.getColor()
                    && oldItem.getIcon().equals(newItem.getIcon());
        }
    };
}
