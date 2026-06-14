package com.hyper.choosebrowsernew.ui.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.data.model.AppInfo;

import java.util.List;
import java.util.Set;

public class BrowserExclusionAdapter extends BaseAdapter {

    public interface ToggleListener {
        void onToggle(AppInfo app);
    }

    private final Context context;
    private final List<AppInfo> apps;
    private final Set<String> excludedPackages;
    private final ToggleListener toggleListener;

    public BrowserExclusionAdapter(
            Context context,
            List<AppInfo> apps,
            Set<String> excludedPackages,
            ToggleListener toggleListener
    ) {
        this.context = context;
        this.apps = apps;
        this.excludedPackages = excludedPackages;
        this.toggleListener = toggleListener;
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
            view = LayoutInflater.from(context).inflate(R.layout.browser_exclusion_item, parent, false);
        }

        ImageView icon = view.findViewById(R.id.browserIcon);
        TextView name = view.findViewById(R.id.appName);
        TextView packageName = view.findViewById(R.id.packageName);
        ImageView visibilityIcon = view.findViewById(R.id.visibilityIcon);

        AppInfo app = apps.get(i);
        boolean excluded = excludedPackages.contains(app.packageName);

        icon.setImageDrawable(app.icon);
        name.setText(app.name);
        packageName.setText(app.packageName);
        visibilityIcon.setImageResource(excluded ? R.drawable.visibility_off : R.drawable.visibility);

        view.setOnClickListener(v -> {
            if (toggleListener != null) toggleListener.onToggle(app);
        });
        visibilityIcon.setOnClickListener(v -> {
            if (toggleListener != null) toggleListener.onToggle(app);
        });

        return view;
    }
}