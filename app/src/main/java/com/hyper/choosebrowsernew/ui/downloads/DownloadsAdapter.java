package com.hyper.choosebrowsernew.ui.downloads;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hyper.choosebrowsernew.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

    public interface OnDownloadActionListener {
        void onFileClick(DownloadItem item);
        void onSelectionModeChanged(boolean active, int count);
        void onListChanged(boolean isEmpty);
        void onDeleteItem(DownloadItem item);
        void onFileMoreClick(DownloadItem item);
    }

    private final Context context;
    private final OnDownloadActionListener listener;

    private List<DownloadItem> originalList = new ArrayList<>();
    private List<DownloadItem> displayList = new ArrayList<>();

    private boolean isSelectionMode = false;
    private String currentCategory = "All";
    private String currentQuery = "";
    private int currentSortMode = 0; // 0 = Date Newest, 1 = Date Oldest, 2 = Name A-Z, 3 = Size Largest

    public DownloadsAdapter(Context context, OnDownloadActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<DownloadItem> items) {
        this.originalList = new ArrayList<>(items);
        applyFilterAndSort();
    }

    public void setCategory(String category) {
        this.currentCategory = category;
        applyFilterAndSort();
    }

    public void setQuery(String query) {
        this.currentQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        applyFilterAndSort();
    }

    public void setSortMode(int sortMode) {
        this.currentSortMode = sortMode;
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        displayList.clear();
        for (DownloadItem item : originalList) {
            boolean matchesCategory = "All".equalsIgnoreCase(currentCategory)
                    || DownloadsHelper.getCategory(item.getFileName()).equalsIgnoreCase(currentCategory);
            boolean matchesQuery = currentQuery.isEmpty()
                    || item.getFileName().toLowerCase(Locale.ROOT).contains(currentQuery);

            if (matchesCategory && matchesQuery) {
                displayList.add(item);
            }
        }

        // Sort
        switch (currentSortMode) {
            case 0: // Date newest
                Collections.sort(displayList, (a, b) -> Long.compare(b.getDateModified(), a.getDateModified()));
                break;
            case 1: // Date oldest
                Collections.sort(displayList, (a, b) -> Long.compare(a.getDateModified(), b.getDateModified()));
                break;
            case 2: // Name A-Z
                Collections.sort(displayList, (a, b) -> a.getFileName().compareToIgnoreCase(b.getFileName()));
                break;
            case 3: // Size largest
                Collections.sort(displayList, (a, b) -> Long.compare(b.getFileSize(), a.getFileSize()));
                break;
        }

        notifyDataSetChanged();
        if (listener != null) {
            listener.onListChanged(displayList.isEmpty());
        }
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean active) {
        this.isSelectionMode = active;
        if (!active) {
            for (DownloadItem item : originalList) {
                item.setSelected(false);
            }
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionModeChanged(isSelectionMode, getSelectedCount());
        }
    }

    public void selectAll(boolean selectAll) {
        for (DownloadItem item : displayList) {
            item.setSelected(selectAll);
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionModeChanged(isSelectionMode, getSelectedCount());
        }
    }

    public int getSelectedCount() {
        int count = 0;
        for (DownloadItem item : originalList) {
            if (item.isSelected()) count++;
        }
        return count;
    }

    public List<DownloadItem> getSelectedItems() {
        List<DownloadItem> selected = new ArrayList<>();
        for (DownloadItem item : originalList) {
            if (item.isSelected()) selected.add(item);
        }
        return selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.download_item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadItem item = displayList.get(position);

        holder.tvFileName.setText(item.getFileName());
        String details = DownloadsHelper.formatFileSize(item.getFileSize()) + " • " + DownloadsHelper.formatDate(item.getDateModified());
        holder.tvDetails.setText(details);

        // Icon badge tint / category icon
        String cat = DownloadsHelper.getCategory(item.getFileName());
        switch (cat) {
            case "Images":
                holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50")); // Green
                break;
            case "PDF":
                holder.ivIcon.setColorFilter(Color.parseColor("#FF5252")); // Red
                break;
            case "Documents":
                holder.ivIcon.setColorFilter(Color.parseColor("#2196F3")); // Blue
                break;
            default:
                holder.ivIcon.setColorFilter(Color.parseColor("#FFFFFF"));
                break;
        }

        if (isSelectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(item.isSelected());
            holder.btnMore.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(item.isSelected() ? Color.parseColor("#22FFFFFF") : Color.TRANSPARENT);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_preview_grid_item_hover));
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                item.setSelected(!item.isSelected());
                notifyItemChanged(holder.getAdapterPosition());
                if (listener != null) {
                    listener.onSelectionModeChanged(true, getSelectedCount());
                }
            } else {
                if (listener != null) {
                    listener.onFileClick(item);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
                item.setSelected(true);
                notifyItemChanged(holder.getAdapterPosition());
                if (listener != null) {
                    listener.onSelectionModeChanged(true, getSelectedCount());
                }
            }
            return true;
        });

        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileMoreClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvFileName;
        TextView tvDetails;
        CheckBox checkBox;
        ImageButton btnMore;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivItemCategory);
            tvFileName = itemView.findViewById(R.id.tvItemFileName);
            tvDetails = itemView.findViewById(R.id.tvItemFileDetails);
            checkBox = itemView.findViewById(R.id.cbItemSelected);
            btnMore = itemView.findViewById(R.id.btnItemMore);
        }
    }
}
