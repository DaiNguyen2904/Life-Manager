package com.example.lifemanager.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifemanager.R;
import com.example.lifemanager.data.entity.Note;
import com.example.lifemanager.databinding.ItemNoteBinding;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.ViewUtils;

/**
 * Danh sách ghi chú nhanh đã lưu.
 */
public class NoteAdapter extends ListAdapter<Note, NoteAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(@NonNull Note note);

        void onDelete(@NonNull Note note);
    }

    private final Listener listener;
    private long selectedId;

    public NoteAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setSelectedId(long id) {
        if (selectedId != id) {
            selectedId = id;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = getItem(position);
        ItemNoteBinding b = holder.binding;
        String content = note.getContent().trim();
        if (content.isEmpty()) {
            b.tvContent.setText(R.string.note_empty_content);
        } else {
            b.tvContent.setText(content);
        }
        b.tvDate.setText(b.getRoot().getContext().getString(R.string.note_updated_format,
                DateUtils.formatDateTime(note.getUpdatedAt())));
        b.cardNote.setStrokeWidth(note.getId() == selectedId ? ViewUtils.dp(b.getRoot().getContext(), 2) : 0);
        b.getRoot().setOnClickListener(v -> listener.onOpen(note));
        b.btnDelete.setOnClickListener(v -> listener.onDelete(note));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemNoteBinding binding;

        ViewHolder(@NonNull ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getUpdatedAt() == newItem.getUpdatedAt()
                    && oldItem.getContent().equals(newItem.getContent());
        }
    };
}
