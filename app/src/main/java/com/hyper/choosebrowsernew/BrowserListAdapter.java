package com.hyper.choosebrowsernew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
public class BrowserListAdapter extends BaseAdapter {

    Context context;
    List<AppInfo> apps;

    public BrowserListAdapter(Context context, List<AppInfo> apps) {
        this.context = context;
        this.apps = apps;
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
    public View getView(int i, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.browser_list_item, parent, false);
        }

        ImageView icon = view.findViewById(R.id.browserIcon);
        TextView name = view.findViewById(R.id.appName);
        TextView packageName = view.findViewById(R.id.packageName);

        AppInfo app = apps.get(i);
        icon.setImageDrawable(app.icon);
        name.setText(app.name);
        packageName.setText(app.packageName);

        return view;
    }
}
