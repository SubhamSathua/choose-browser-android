package com.hyper.choosebrowsernew;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Shows a "Full Link" popup dialog with:
 *   - scrollable full URL display
 *   - Share button
 *   - Copy Clean Link button (strips trackers)
 *   - Close button
 *
 * Supports both light and dark mode via color resources.
 */
public class ShowLinkDialogHelper {

    /**
     * Show the Full Link dialog.
     *
     * @param context  Activity or Fragment context
     * @param url      The original full URL to display
     */
    public static void show(Context context, String url) {
        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(context);
        Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(themedContext).inflate(R.layout.dialog_show_link, null);
        dialog.setContentView(view);

        applyPopupTheme(themedContext, view);

        // Transparent background so our rounded-corner drawable shows correctly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            // Dim behind
            dialog.getWindow().setDimAmount(0.5f);
        }

        // --- URL text ---
        TextView urlText = view.findViewById(R.id.showLinkUrlText);
        urlText.setText(url);

        // --- Close button ---
        ImageView closeBtn = view.findViewById(R.id.showLinkCloseBtn);
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        // --- Share button ---
        LinearLayout shareBtn = view.findViewById(R.id.showLinkShareBtn);
        shareBtn.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, url);
            context.startActivity(Intent.createChooser(share, "Share URL"));
        });

        // --- Copy Clean Link button ---
        LinearLayout copyCleanBtn = view.findViewById(R.id.showLinkCopyCleanBtn);
        copyCleanBtn.setOnClickListener(v -> {
            String cleanedUrl = LinkCleaner.clean(url);
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Clean URL", cleanedUrl));
                Toast.makeText(context, "Clean link copied!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private static void applyPopupTheme(Context context, View root) {
        int surface = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupSurface, R.color.PopUpCardBg);
        int dock = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDock, R.color.PopUpCardDockBg);
        int dockBtn = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDockButton, R.color.PopUpCardDockBtn);
        int dockBtnText = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDockButtonText, R.color.PopUpCardDockText);
        int action = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupAction, R.color.PopUpCard_ActionBtnBg);
        int text = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupText, R.color.text);

        if (root.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(root.getBackground().mutate());
            DrawableCompat.setTint(bg, surface);
            root.setBackground(bg);
        }

        View closeBtn = root.findViewById(R.id.showLinkCloseBtn);
        if (closeBtn != null && closeBtn.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(closeBtn.getBackground().mutate());
            DrawableCompat.setTint(bg, action);
            closeBtn.setBackground(bg);
        }

        View shareBtn = root.findViewById(R.id.showLinkShareBtn);
        if (shareBtn != null && shareBtn.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(shareBtn.getBackground().mutate());
            DrawableCompat.setTint(bg, action);
            shareBtn.setBackground(bg);
        }

        View copyBtn = root.findViewById(R.id.showLinkCopyCleanBtn);
        if (copyBtn != null && copyBtn.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(copyBtn.getBackground().mutate());
            DrawableCompat.setTint(bg, dockBtn);
            copyBtn.setBackground(bg);
        }

        View scroll = root.findViewById(R.id.showLinkScrollArea);
        if (scroll != null && scroll.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(scroll.getBackground().mutate());
            DrawableCompat.setTint(bg, dock);
            scroll.setBackground(bg);
        }

        TextView title = root.findViewById(R.id.showLinkTitle);
        if (title != null) title.setTextColor(text);

        TextView urlText = root.findViewById(R.id.showLinkUrlText);
        if (urlText != null) urlText.setTextColor(text);

        TextView shareText = root.findViewById(R.id.showLinkShareText);
        if (shareText != null) shareText.setTextColor(text);

        TextView copyText = root.findViewById(R.id.showLinkCopyText);
        if (copyText != null) copyText.setTextColor(dockBtnText);
    }
}
