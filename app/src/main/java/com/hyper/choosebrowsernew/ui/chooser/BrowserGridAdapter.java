package com.hyper.choosebrowsernew.ui.chooser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.data.model.AppInfo;

import java.util.List;

public class BrowserGridAdapter extends BaseAdapter {

    private final Context context;
    private final List<AppInfo> apps;
    private final String url;
    private final Runnable onDismiss;

    public BrowserGridAdapter(Context context, List<AppInfo> apps, String url, Runnable onDismiss) {
        this.context = context;
        this.apps = apps;
        this.url = url;
        this.onDismiss = onDismiss;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int i) {
        return apps.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.browser_grid_item, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.browserIcon);
            holder.name = convertView.findViewById(R.id.appName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AppInfo app = apps.get(i);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.name);

        convertView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage(app.packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                if (onDismiss != null) onDismiss.run();
            } catch (Exception e) {
                Toast.makeText(context, "Failed to open browser", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}
