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
import com.example.lifemanager.data.model.TransactionWithCategory;
import com.example.lifemanager.databinding.ItemTransactionBinding;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.IconUtils;
import com.example.lifemanager.utils.ViewUtils;

import java.util.Objects;

/**
 * Danh sách lịch sử giao dịch thu / chi.
 */
public class TransactionAdapter extends ListAdapter<TransactionWithCategory, TransactionAdapter.ViewHolder> {

    public interface Listener {
        void onClick(@NonNull TransactionWithCategory item);
    }

    private final Listener listener;

    public TransactionAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionWithCategory item = getItem(position);
        ItemTransactionBinding b = holder.binding;
        Context context = b.getRoot().getContext();
        bindIcon(b.ivIcon, item);

        b.tvTitle.setText(item.transaction.getTitle());
        String category = item.categoryName != null ? item.categoryName : context.getString(R.string.no_category);
        b.tvSubtitle.setText(context.getString(R.string.transaction_subtitle_format, category,
                DateUtils.formatDate(item.transaction.getDate())));

        boolean income = item.transaction.isIncome();
        b.tvAmount.setText(CurrencyUtils.formatSigned(item.transaction.getAmount(), income));
        b.tvAmount.setTextColor(ContextCompat.getColor(context, income ? R.color.lm_income : R.color.lm_expense));
        b.getRoot().setOnClickListener(v -> listener.onClick(item));
    }

    /** Vẽ icon danh mục trong vòng tròn màu nhạt. */
    public static void bindIcon(@NonNull android.widget.ImageView imageView, @NonNull TransactionWithCategory item) {
        Context context = imageView.getContext();
        int color = item.categoryColor != null ? item.categoryColor
                : ContextCompat.getColor(context, R.color.lm_text_secondary);
        imageView.setImageResource(IconUtils.getIconRes(item.categoryIcon));
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color));
        ViewUtils.setBackgroundTint(imageView, ViewUtils.softColor(color, 0.8f));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemTransactionBinding binding;

        ViewHolder(@NonNull ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<TransactionWithCategory> DIFF =
            new DiffUtil.ItemCallback<TransactionWithCategory>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransactionWithCategory oldItem,
                                               @NonNull TransactionWithCategory newItem) {
                    return oldItem.transaction.getId() == newItem.transaction.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransactionWithCategory oldItem,
                                                  @NonNull TransactionWithCategory newItem) {
                    return oldItem.transaction.getUpdatedAt() == newItem.transaction.getUpdatedAt()
                            && Objects.equals(oldItem.categoryName, newItem.categoryName)
                            && Objects.equals(oldItem.categoryColor, newItem.categoryColor)
                            && Objects.equals(oldItem.categoryIcon, newItem.categoryIcon);
                }
            };
}
