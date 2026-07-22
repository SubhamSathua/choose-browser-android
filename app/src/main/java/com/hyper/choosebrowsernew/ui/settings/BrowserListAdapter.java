package com.hyper.choosebrowsernew.ui.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.data.model.AppInfo;

import java.util.List;

public class BrowserListAdapter extends BaseAdapter {

    private final Context context;
    private final List<AppInfo> apps;
    private final int iconTargetSize;

    public BrowserListAdapter(Context context, List<AppInfo> apps) {
        this.context = context;
        this.apps = apps;
        this.iconTargetSize = (int) (40 * context.getResources().getDisplayMetrics().density);
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
        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.browser_list_item, parent, false);
            holder = new ViewHolder();
            holder.icon = view.findViewById(R.id.browserIcon);
            holder.name = view.findViewById(R.id.appName);
            holder.packageName = view.findViewById(R.id.packageName);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        AppInfo app = apps.get(i);
        holder.icon.setImageDrawable(scaleIcon(app.icon));
        holder.name.setText(app.name);
        holder.packageName.setText(app.packageName);

        return view;
    }

    private Drawable scaleIcon(Drawable icon) {
        if (icon == null) return null;
        if (icon instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) icon;
            if (bd.getBitmap().getWidth() <= iconTargetSize) return icon;
        }
        Bitmap bitmap = Bitmap.createBitmap(iconTargetSize, iconTargetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView packageName;
    }
}
