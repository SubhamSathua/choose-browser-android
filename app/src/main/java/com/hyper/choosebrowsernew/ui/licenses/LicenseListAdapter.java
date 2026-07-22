package com.hyper.choosebrowsernew.ui.licenses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hyper.choosebrowsernew.R;

import java.util.ArrayList;

public class LicenseListAdapter extends BaseAdapter {

    private final ArrayList<LicenseListActivity.LicenseItem> items;
    private final LayoutInflater inflater;

    public LicenseListAdapter(LicenseListActivity activity, ArrayList<LicenseListActivity.LicenseItem> items) {
        this.items = items;
        this.inflater = LayoutInflater.from(activity);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.license_list_item, parent, false);
        }
        LicenseListActivity.LicenseItem item = items.get(position);
        ((TextView) convertView.findViewById(R.id.licenseItemName)).setText(item.name);
        ((TextView) convertView.findViewById(R.id.licenseItemAuthor)).setText(item.author);
        ((TextView) convertView.findViewById(R.id.licenseItemType)).setText(item.type);
        return convertView;
    }
}
