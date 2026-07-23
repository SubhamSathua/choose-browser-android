package com.hyper.choosebrowsernew.ui.chooser;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.main.MainActivity;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.util.List;

public class BrowserChooserActivity extends AppCompatActivity {

    private BrowserChooserViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(BrowserChooserViewModel.class);
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        if (UpdateChecker.getCachedResult(this).priority == UpdateChecker.Priority.CRITICAL) {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                openChooserForUrl(data.toString());
                return;
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            List<String> foundLinks = viewModel.findUrls(intent.getStringExtra(Intent.EXTRA_TEXT));
            if (foundLinks.isEmpty()) {
                Toast.makeText(this, "Cant find a link", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (foundLinks.size() == 1) {
                openChooserForUrl(foundLinks.get(0));
            } else {
                showLinkPicker(foundLinks);
            }
            return;
        }

        Toast.makeText(this, "Cant find a link", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void openChooserForUrl(String candidate) {
        String url = normalizeForIntent(candidate);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Cant find a link", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showDialogChooser(url);
    }

    // ── BottomSheetDialogFragment chooser ─────────────────────────────

    private void showDialogChooser(String url) {
        BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(url);
        chooser.show(getSupportFragmentManager(), "browser_chooser");
    }

    // ── Link picker for multiple links from shared text ──────────────

    private void showLinkPicker(List<String> links) {
        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(this);
        final Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(themedContext).inflate(R.layout.dialog_link_picker, null);
        dialog.setContentView(view);

        int surface = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupSurface, R.color.backgroundSecondary);
        int dockBg = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupDock, R.color.PopUpCardDockBg);
        int textCol = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupText, R.color.text);

        View card = view.findViewById(R.id.linkPickerCard);
        if (card instanceof CardView) {
            ((CardView) card).setCardBackgroundColor(surface);
        }

        TextView title = view.findViewById(R.id.linkPickerTitle);
        if (title != null) title.setTextColor(textCol);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.55f);
        }

        ImageView closeBtn = view.findViewById(R.id.linkPickerClose);
        if (closeBtn != null) closeBtn.setColorFilter(textCol);

        TextView cancelBtn = view.findViewById(R.id.linkPickerCancel);
        if (cancelBtn != null) {
            cancelBtn.setTextColor(textCol);
            if (cancelBtn.getBackground() != null) {
                cancelBtn.getBackground().setTint(dockBg);
            }
        }

        ListView listView = view.findViewById(R.id.linkPickerList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                themedContext, R.layout.item_link_picker, android.R.id.text1, links) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof CardView) {
                    ((CardView) v).setCardBackgroundColor(dockBg);
                }
                TextView tv = v.findViewById(android.R.id.text1);
                if (tv != null) tv.setTextColor(textCol);
                return v;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            dialog.dismiss();
            openChooserForUrl(links.get(position));
        });

        View.OnClickListener dismissAndFinish = v -> {
            dialog.dismiss();
            finish();
        };

        if (closeBtn != null) closeBtn.setOnClickListener(dismissAndFinish);
        if (cancelBtn != null) cancelBtn.setOnClickListener(dismissAndFinish);
        dialog.setOnCancelListener(d -> finish());
        dialog.show();
    }

    private String normalizeForIntent(String candidate) {
        if (TextUtils.isEmpty(candidate)) return candidate;
        String trimmed = candidate.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }
}