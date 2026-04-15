package com.hyper.choosebrowsernew;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import java.util.List;

public class BrowserGridAdapter extends BaseAdapter {

    Context context;
    List<AppInfo> apps;
    String url;

    public BrowserGridAdapter(Context context, List<AppInfo> apps, String url, BrowserChooserBottomSheet fragment) {
        this.context = context;
        this.apps = apps;
        this.url = url;
    }

    @Override
    public int getCount() {
        Log.d("BrowserGridAdapter", "Count = " + apps.size());
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
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.browser_grid_item, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.browserIcon);
        TextView name = convertView.findViewById(R.id.appName);

        AppInfo app = apps.get(i);
        icon.setImageDrawable(app.icon);
        name.setText(app.name);

        convertView.setOnClickListener(v -> {
            try {
                String launchUrl = url;
                if (!TextUtils.isEmpty(launchUrl)
                        && !launchUrl.startsWith("http://")
                        && !launchUrl.startsWith("https://")) {
                    launchUrl = "https://" + launchUrl;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchUrl));
                intent.setPackage(app.packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Failed to open browser", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }
}
