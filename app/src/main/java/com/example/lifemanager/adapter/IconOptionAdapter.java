package com.example.lifemanager.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.databinding.ItemIconOptionBinding;
import com.example.lifemanager.utils.IconUtils;
import com.example.lifemanager.utils.ViewUtils;

/**
 * Bảng chọn biểu tượng cho danh mục.
 */
public class IconOptionAdapter extends RecyclerView.Adapter<IconOptionAdapter.ViewHolder> {

    public interface Listener {
        void onIconSelected(@NonNull String key);
    }

    private final String[] keys;
    private final Listener listener;
    private String selectedKey;
    private int tintColor;

    public IconOptionAdapter(@NonNull String[] keys, @NonNull String selectedKey, int tintColor,
                             @NonNull Listener listener) {
        this.keys = keys;
        this.selectedKey = selectedKey;
        this.tintColor = tintColor;
        this.listener = listener;
    }

    public void setTintColor(int color) {
        tintColor = color;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemIconOptionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = keys[position];
        boolean selected = key.equals(selectedKey);
        holder.binding.ivIcon.setImageResource(IconUtils.getIconRes(key));
        holder.binding.ivIcon.setContentDescription(key);
        int gray = ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.lm_text_secondary);
        if (selected) {
            ImageViewCompat.setImageTintList(holder.binding.ivIcon, ColorStateList.valueOf(0xFFFFFFFF));
            ViewUtils.setBackgroundTint(holder.binding.ivIcon, tintColor);
        } else {
            ImageViewCompat.setImageTintList(holder.binding.ivIcon, ColorStateList.valueOf(gray));
            ViewUtils.setBackgroundTint(holder.binding.ivIcon,
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.lm_card));
        }
        holder.binding.getRoot().setOnClickListener(v -> {
            if (!key.equals(selectedKey)) {
                selectedKey = key;
                notifyItemRangeChanged(0, getItemCount());
                listener.onIconSelected(key);
            }
        });
    }

    @Override
    public int getItemCount() {
        return keys.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemIconOptionBinding binding;

        ViewHolder(@NonNull ItemIconOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
