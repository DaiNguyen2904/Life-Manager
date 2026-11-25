package com.example.lifemanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.databinding.ItemColorOptionBinding;
import com.example.lifemanager.utils.ViewUtils;

/**
 * Bảng chọn màu cho danh mục.
 */
public class ColorOptionAdapter extends RecyclerView.Adapter<ColorOptionAdapter.ViewHolder> {

    public interface Listener {
        void onColorSelected(int color);
    }

    private final int[] colors;
    private final Listener listener;
    private int selectedColor;

    public ColorOptionAdapter(@NonNull int[] colors, int selectedColor, @NonNull Listener listener) {
        this.colors = colors;
        this.selectedColor = selectedColor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemColorOptionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int color = colors[position];
        boolean selected = color == selectedColor;
        ViewUtils.setBackgroundTint(holder.binding.viewColor, color);
        holder.binding.ivCheck.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        holder.binding.viewSelected.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (selectedColor != color) {
                selectedColor = color;
                notifyItemRangeChanged(0, getItemCount());
                listener.onColorSelected(color);
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemColorOptionBinding binding;

        ViewHolder(@NonNull ItemColorOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
