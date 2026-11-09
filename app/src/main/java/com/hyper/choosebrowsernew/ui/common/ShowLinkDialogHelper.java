package com.hyper.choosebrowsernew.ui.common;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.util.LinkCleaner;
import com.hyper.choosebrowsernew.util.MotionUiHelper;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.util.Locale;

/**
 * Mature, professional "Full Link" inspector and editor dialog.
 * Features:
 *   - Bold domain header with character count
 *   - Vector icon badges (No emojis: HTTPS lock, HTTP warning, Cleaned checkmark)
 *   - Clean button: strips tracking parameters from link
 *   - Circular Copy button
 *   - Set button: confirms link and applies it to chooser
 *   - Edit button: in-place URL editing (Enter key disabled) with [Cancel, Set] controls
 *   - Fully reactive to app's 6 custom theme overlays
 */
public class ShowLinkDialogHelper {

    public interface OnUrlSetListener {
        void onUrlSet(String newUrl);
    }

    private static class DialogState {
        String url;
        boolean isEditMode = false;
        boolean wasCleaned = false;

        DialogState(String initialUrl) {
            this.url = initialUrl != null ? initialUrl : "";
        }
    }

    public static void show(Context context, String url) {
        show(context, url, null);
    }

    public static void show(Context context, String url, OnUrlSetListener listener) {
        if (context == null) return;

        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(context);
        Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(themedContext).inflate(R.layout.chooser_dialog_show_link, null);
        dialog.setContentView(view);

        applyPopupTheme(themedContext, view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setDimAmount(0.55f);
        }

        DialogState state = new DialogState(url);

        // Header views
        TextView titleText = view.findViewById(R.id.showLinkTitle);
        TextView subtitleText = view.findViewById(R.id.showLinkSubtitle);
        ImageView editBtn = view.findViewById(R.id.showLinkEditBtn);
        ImageView closeBtn = view.findViewById(R.id.showLinkCloseBtn);

        // Content views
        View securityBadge = view.findViewById(R.id.showLinkSecurityBadge);
        ImageView securityIcon = view.findViewById(R.id.showLinkSecurityIcon);
        TextView securityText = view.findViewById(R.id.showLinkSecurityText);

        View statusBadge = view.findViewById(R.id.showLinkStatusBadge);
        ImageView statusIcon = view.findViewById(R.id.showLinkStatusIcon);
        TextView statusText = view.findViewById(R.id.showLinkStatusText);

        MaxHeightScrollView scrollArea = view.findViewById(R.id.showLinkScrollArea);
        TextView urlTextView = view.findViewById(R.id.showLinkUrlText);
        MaxHeightScrollView editArea = view.findViewById(R.id.showLinkEditArea);
        EditText urlEditText = view.findViewById(R.id.showLinkUrlEdit);

        // Cap height dynamically to exactly 7 lines based on user's active system font
        scrollArea.setMaxLines(7, urlTextView);
        editArea.setMaxLines(7, urlEditText);

        // Disallow Enter key in edit mode:
        // 1. InputFilter to strip \n and \r
        InputFilter filterNoEnter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == '\n' || c == '\r') {
                    return "";
                }
            }
            return null;
        };
        urlEditText.setFilters(new InputFilter[]{filterNoEnter});

        // 2. Consume Enter key on keyboard
        urlEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard(urlEditText);
                return true;
            }
            return false;
        });
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard(urlEditText);
                return true;
            }
            return false;
        });

        // Action rows
        View viewActions = view.findViewById(R.id.showLinkViewActions);
        View editActions = view.findViewById(R.id.showLinkEditActions);
        View cleanBtn = view.findViewById(R.id.showLinkCleanBtn);
        ImageView copyBtn = view.findViewById(R.id.showLinkCopyBtn);
        View setBtn = view.findViewById(R.id.showLinkSetBtn);
        View cancelEditBtn = view.findViewById(R.id.showLinkCancelEditBtn);
        View setEditBtn = view.findViewById(R.id.showLinkSetEditBtn);

        // Apply motion / micro-interactions
        MotionUiHelper.applyTapScale(editBtn);
        MotionUiHelper.applyTapScale(closeBtn);
        MotionUiHelper.applyTapScale(cleanBtn);
        MotionUiHelper.applyTapScale(copyBtn);
        MotionUiHelper.applyTapScale(setBtn);
        MotionUiHelper.applyTapScale(cancelEditBtn);
        MotionUiHelper.applyTapScale(setEditBtn);

        Runnable refreshDisplay = () -> {
            String current = state.url;
            urlTextView.setText(current);
            urlEditText.setText(current);

            // Subtitle: bold site.domain • xx chars
            String domain = getDomain(current);
            int chars = current.length();
            String charText = chars + (chars == 1 ? " char" : " chars");

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(domain, new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append("  •  ");
            ssb.append(charText);
            subtitleText.setText(ssb);

            // Security badge (Lock / Warning icon + text)
            applySecurityBadge(current, securityIcon, securityText);

            // Tracker status badge
            String cleanedCandidate = LinkCleaner.clean(themedContext, current);
            boolean hasTrackers = !cleanedCandidate.equals(current);

            if (state.wasCleaned) {
                statusBadge.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_check_circle_16);
                statusText.setText("Cleaned");
            } else if (hasTrackers) {
                statusBadge.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_warning_16);
                statusText.setText("Trackers");
            } else {
                statusBadge.setVisibility(View.GONE);
            }
        };

        refreshDisplay.run();

        // Mode switcher
        Runnable updateMode = () -> {
            if (state.isEditMode) {
                titleText.setText("Edit Link");
                editBtn.setImageResource(R.drawable.ic_check);
                scrollArea.setVisibility(View.GONE);
                editArea.setVisibility(View.VISIBLE);
                viewActions.setVisibility(View.GONE);
                editActions.setVisibility(View.VISIBLE);
                urlEditText.requestFocus();
                urlEditText.setSelection(urlEditText.getText().length());
                showKeyboard(urlEditText);
            } else {
                titleText.setText("Full Link");
                editBtn.setImageResource(R.drawable.ic_edit);
                scrollArea.setVisibility(View.VISIBLE);
                editArea.setVisibility(View.GONE);
                viewActions.setVisibility(View.VISIBLE);
                editActions.setVisibility(View.GONE);
                hideKeyboard(urlEditText);
            }
        };

        // Header Edit button
        editBtn.setOnClickListener(v -> {
            if (!state.isEditMode) {
                state.isEditMode = true;
                updateMode.run();
            } else {
                // If clicked while editing, confirm edit
                String edited = urlEditText.getText() != null ? urlEditText.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(edited)) {
                    state.url = normalizeUrl(edited);
                    state.wasCleaned = false;
                    refreshDisplay.run();
                }
                state.isEditMode = false;
                updateMode.run();
            }
        });

        // Header Close button
        closeBtn.setOnClickListener(v -> {
            hideKeyboard(urlEditText);
            dialog.dismiss();
        });

        // Clean button
        cleanBtn.setOnClickListener(v -> {
            String cleaned = LinkCleaner.clean(context, state.url);
            if (!cleaned.equals(state.url)) {
                state.url = cleaned;
                state.wasCleaned = true;
                refreshDisplay.run();
                Toast.makeText(context, "Tracking parameters removed", Toast.LENGTH_SHORT).show();
            } else {
                state.wasCleaned = true;
                refreshDisplay.run();
                Toast.makeText(context, "Link is already clean", Toast.LENGTH_SHORT).show();
            }
        });

        // Circular Copy button
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", state.url));
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // Set button (View Mode)
        setBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUrlSet(state.url);
            }
            Toast.makeText(context, "Link set", Toast.LENGTH_SHORT).show();
            hideKeyboard(urlEditText);
            dialog.dismiss();
        });

        // Cancel Edit button
        cancelEditBtn.setOnClickListener(v -> {
            urlEditText.setText(state.url);
            state.isEditMode = false;
            updateMode.run();
        });

        // Set button (Edit Mode)
        setEditBtn.setOnClickListener(v -> {
            String edited = urlEditText.getText() != null ? urlEditText.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(edited)) {
                state.url = normalizeUrl(edited);
                state.wasCleaned = false;
                refreshDisplay.run();
            }
            if (listener != null) {
                listener.onUrlSet(state.url);
            }
            Toast.makeText(context, "Link updated", Toast.LENGTH_SHORT).show();
            hideKeyboard(urlEditText);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> hideKeyboard(urlEditText));

        dialog.show();
    }

    private static String normalizeUrl(String input) {
        if (TextUtils.isEmpty(input)) return input;
        String trimmed = input.trim();
        if (!trimmed.contains("://") && !trimmed.startsWith("about:") && !trimmed.startsWith("javascript:")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private static String getDomain(String urlStr) {
        if (TextUtils.isEmpty(urlStr)) return "Link";
        try {
            Uri uri = Uri.parse(urlStr);
            String host = uri.getHost();
            if (!TextUtils.isEmpty(host)) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return "Link";
    }

    private static void applySecurityBadge(String urlStr, ImageView icon, TextView text) {
        if (icon == null || text == null) return;
        if (TextUtils.isEmpty(urlStr)) {
            icon.setImageResource(R.drawable.link);
            text.setText("URL");
            return;
        }
        String lower = urlStr.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) {
            icon.setImageResource(R.drawable.ic_lock_16);
            text.setText("HTTPS");
        } else if (lower.startsWith("http://")) {
            icon.setImageResource(R.drawable.ic_warning_16);
            text.setText("HTTP");
        } else {
            icon.setImageResource(R.drawable.link);
            int schemeEnd = urlStr.indexOf("://");
            text.setText(schemeEnd > 0 ? urlStr.substring(0, schemeEnd).toUpperCase(Locale.ROOT) : "URL");
        }
    }

    private static void showKeyboard(View view) {
        if (view == null) return;
        view.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 120);
    }

    private static void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static void tintBackground(View view, int color) {
        if (view != null && view.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(view.getBackground().mutate());
            DrawableCompat.setTint(bg, color);
            view.setBackground(bg);
        }
    }

    private static void applyPopupTheme(Context context, View root) {
        int surface = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupSurface, R.color.PopUpCardBg);
        int dock = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDock, R.color.PopUpCardDockBg);
        int dockBtn = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDockButton, R.color.PopUpCardDockBtn);
        int dockBtnText = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDockButtonText, R.color.PopUpCardDockText);
        int action = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupAction, R.color.PopUpCard_ActionBtnBg);
        int text = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupText, R.color.text);

        // Root card background
        tintBackground(root, surface);

        // Header views
        tintBackground(root.findViewById(R.id.showLinkIconContainer), action);
        tintBackground(root.findViewById(R.id.showLinkEditBtn), action);
        tintBackground(root.findViewById(R.id.showLinkCloseBtn), action);

        // Middle URL container
        tintBackground(root.findViewById(R.id.showLinkUrlBox), dock);
        tintBackground(root.findViewById(R.id.showLinkSecurityBadge), action);
        tintBackground(root.findViewById(R.id.showLinkStatusBadge), action);

        // Action buttons
        tintBackground(root.findViewById(R.id.showLinkCleanBtn), action);
        tintBackground(root.findViewById(R.id.showLinkCopyBtn), action);
        tintBackground(root.findViewById(R.id.showLinkCancelEditBtn), action);

        // Primary Set buttons
        tintBackground(root.findViewById(R.id.showLinkSetBtn), dockBtn);
        tintBackground(root.findViewById(R.id.showLinkSetEditBtn), dockBtn);

        // Text & Icon tints
        TextView title = root.findViewById(R.id.showLinkTitle);
        if (title != null) title.setTextColor(text);

        TextView subtitle = root.findViewById(R.id.showLinkSubtitle);
        if (subtitle != null) {
            int subtextColor = Color.argb(190, Color.red(text), Color.green(text), Color.blue(text));
            subtitle.setTextColor(subtextColor);
        }

        TextView urlText = root.findViewById(R.id.showLinkUrlText);
        if (urlText != null) urlText.setTextColor(text);

        EditText urlEdit = root.findViewById(R.id.showLinkUrlEdit);
        if (urlEdit != null) {
            urlEdit.setTextColor(text);
            urlEdit.setHintTextColor(Color.argb(120, Color.red(text), Color.green(text), Color.blue(text)));
        }

        TextView securityText = root.findViewById(R.id.showLinkSecurityText);
        if (securityText != null) securityText.setTextColor(text);

        TextView statusText = root.findViewById(R.id.showLinkStatusText);
        if (statusText != null) statusText.setTextColor(text);

        TextView cleanText = root.findViewById(R.id.showLinkCleanText);
        if (cleanText != null) cleanText.setTextColor(text);

        TextView cancelEditText = root.findViewById(R.id.showLinkCancelEditText);
        if (cancelEditText != null) cancelEditText.setTextColor(text);

        TextView setText = root.findViewById(R.id.showLinkSetText);
        if (setText != null) setText.setTextColor(dockBtnText);

        TextView setEditText = root.findViewById(R.id.showLinkSetEditText);
        if (setEditText != null) setEditText.setTextColor(dockBtnText);

        // Icons tint
        ImageView icon = root.findViewById(R.id.showLinkIcon);
        if (icon != null) icon.setColorFilter(text);

        ImageView editIcon = root.findViewById(R.id.showLinkEditBtn);
        if (editIcon != null) editIcon.setColorFilter(text);

        ImageView closeIcon = root.findViewById(R.id.showLinkCloseBtn);
        if (closeIcon != null) closeIcon.setColorFilter(text);

        ImageView securityIconView = root.findViewById(R.id.showLinkSecurityIcon);
        if (securityIconView != null) securityIconView.setColorFilter(text);

        ImageView statusIconView = root.findViewById(R.id.showLinkStatusIcon);
        if (statusIconView != null) statusIconView.setColorFilter(text);

        ImageView cleanIcon = root.findViewById(R.id.showLinkCleanIcon);
        if (cleanIcon != null) cleanIcon.setColorFilter(text);

        ImageView copyIcon = root.findViewById(R.id.showLinkCopyBtn);
        if (copyIcon != null) copyIcon.setColorFilter(text);
    }
}
