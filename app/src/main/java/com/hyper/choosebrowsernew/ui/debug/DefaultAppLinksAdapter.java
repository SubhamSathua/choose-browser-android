package com.hyper.choosebrowsernew.ui.debug;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hyper.choosebrowsernew.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DefaultAppLinksAdapter extends RecyclerView.Adapter<DefaultAppLinksAdapter.ViewHolder> {

    private final Context context;
    private final List<AppLinksItem> allItems = new ArrayList<>();
    private final List<AppLinksItem> displayList = new ArrayList<>();
    private String currentQuery = "";

    public DefaultAppLinksAdapter(Context context) {
        this.context = context;
    }

    public synchronized void addApp(AppLinksItem item) {
        allItems.add(item);
        if (matchesQuery(item, currentQuery)) {
            displayList.add(item);
            notifyItemInserted(displayList.size() - 1);
        }
    }

    public synchronized void filter(String query) {
        this.currentQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        displayList.clear();
        for (AppLinksItem item : allItems) {
            if (matchesQuery(item, currentQuery)) {
                displayList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesQuery(AppLinksItem item, String query) {
        if (query.isEmpty()) return true;
        if (item.getAppName().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (item.getPackageName().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (String link : item.getSupportedLinks()) {
            if (link.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    public int getLoadedCount() {
        return allItems.size();
    }

    public int getVisibleCount() {
        return displayList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_link, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppLinksItem item = displayList.get(position);

        if (item.getIcon() != null) {
            holder.ivIcon.setImageDrawable(item.getIcon());
        } else {
            holder.ivIcon.setImageResource(R.mipmap.ic_launcher);
        }

        holder.tvAppName.setText(item.getAppName());
        holder.tvPackageName.setText(item.getPackageName());
        holder.tvCountBadge.setText(item.getSupportedLinks().size() + " links");

        boolean hasMany = item.getSupportedLinks().size() > 4;
        holder.tvSupportedLinks.setText(item.getFormattedLinks(item.isExpanded()));

        if (hasMany) {
            holder.tvToggleExpand.setVisibility(View.VISIBLE);
            if (item.isExpanded()) {
                holder.tvToggleExpand.setText("Show less");
            } else {
                int remaining = item.getSupportedLinks().size() - 4;
                holder.tvToggleExpand.setText("Show " + remaining + " more links…");
            }
            holder.tvToggleExpand.setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                notifyItemChanged(holder.getAdapterPosition());
            });
        } else {
            holder.tvToggleExpand.setVisibility(View.GONE);
        }

        // Long press to copy all links
        holder.itemView.setOnLongClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            sb.append(item.getAppName()).append(" (").append(item.getPackageName()).append(")\n");
            for (String link : item.getSupportedLinks()) {
                sb.append("- ").append(link).append("\n");
            }
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Supported Links", sb.toString().trim()));
                Toast.makeText(context, "Copied links for " + item.getAppName(), Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvAppName;
        TextView tvPackageName;
        TextView tvCountBadge;
        TextView tvSupportedLinks;
        TextView tvToggleExpand;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            tvCountBadge = itemView.findViewById(R.id.tvLinksCountBadge);
            tvSupportedLinks = itemView.findViewById(R.id.tvSupportedLinks);
            tvToggleExpand = itemView.findViewById(R.id.tvToggleExpand);
        }
    }
}
