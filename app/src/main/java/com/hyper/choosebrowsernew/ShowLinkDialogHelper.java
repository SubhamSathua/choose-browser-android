package com.hyper.choosebrowsernew;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_show_link, null);
        dialog.setContentView(view);

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
}
