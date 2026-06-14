package com.hyper.choosebrowsernew.ui.chooser;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.data.model.UpdateResult;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.main.MainActivity;
import com.hyper.choosebrowsernew.ui.webview.WebViewActivity;
import com.hyper.choosebrowsernew.ui.preview.PreviewPageActivity;
import com.hyper.choosebrowsernew.ui.common.ShowLinkDialogHelper;
import com.hyper.choosebrowsernew.data.local.BrowserExclusionManager;
import com.hyper.choosebrowsernew.data.model.AppInfo;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrowserChooserBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_URL = "arg_url";
    private String url;
    private GridView gridView;
    private TextView urlTextView;
    private Button copyBtn;
    private CardView shareBtn;
    private CardView previewLinkBtn;
    private CardView previewPageBtn;
    private BrowserChooserViewModel viewModel;

    public static BrowserChooserBottomSheet newInstance(String url) {
        BrowserChooserBottomSheet fragment = new BrowserChooserBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottomsheet_browser_chooser, container, false);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext())).get(BrowserChooserViewModel.class);

        // ── Critical update: block all app usage, redirect to MainActivity ──
        if (UpdateChecker.getCachedResult(requireContext()).priority == UpdateChecker.Priority.CRITICAL) {
            redirectToMainForUpdate();
            return view;
        }

        urlTextView = view.findViewById(R.id.urlTextView);
        copyBtn = view.findViewById(R.id.copyBtn);
        shareBtn = view.findViewById(R.id.shareBtn);
        gridView = view.findViewById(R.id.browserGridView);
        previewLinkBtn = view.findViewById(R.id.previewLinkBtn);
        previewPageBtn = view.findViewById(R.id.previewPage);
        ViewCompat.setNestedScrollingEnabled(gridView, true);

        applyPopupTheme(view);
        setupObservers();

        // Set URL (limit to 2 lines)
        urlTextView.setText(url);
        urlTextView.setMaxLines(2);
        urlTextView.setEllipsize(TextUtils.TruncateAt.END);

        // Copy button
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("URL", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Share button
        shareBtn.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(share, "Share URL"));
        });

        // Show Link button – opens the Full Link dialog
        previewLinkBtn.setOnClickListener(v -> {
            ShowLinkDialogHelper.show(requireContext(), url);
        });

        // Preview Page button – opens sandboxed incognito WebView
        previewPageBtn.setOnClickListener(v -> {
            Intent previewIntent = new Intent(requireContext(), PreviewPageActivity.class);
            previewIntent.putExtra("url", url);
            startActivity(previewIntent);
            Activity activity = getActivity();
            if (activity != null) {
                activity.overridePendingTransition(R.anim.slide_up, R.anim.stay);
            }
        });

        // Privacy Policy button
        view.findViewById(R.id.privacyBtn).setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity instanceof AppCompatActivity) {
                WebViewActivity.openPrivacyPolicy((AppCompatActivity) activity);
            }
        });

        // Update dot card – separate blank card next to privacy button, click → MainActivity
        View updateDotCard = view.findViewById(R.id.updateDotCard);
        View popupUpdateDot = view.findViewById(R.id.popupUpdateDot);
        updateDotCard.setOnClickListener(v -> redirectToMainForUpdate());
        UpdateChecker.check(requireContext(), result -> {
            if (result.priority == UpdateChecker.Priority.CRITICAL) {
                // Also enforce critical block if freshly detected
                redirectToMainForUpdate();
            } else if (result.priority == UpdateChecker.Priority.WARNING) {
                popupUpdateDot.setBackgroundResource(R.drawable.dot_warning);
                updateDotCard.setVisibility(View.VISIBLE);
            } else if (result.priority == UpdateChecker.Priority.LATEST) {
                popupUpdateDot.setBackgroundResource(R.drawable.dot_blue);
                updateDotCard.setVisibility(View.VISIBLE);
            }
        });

        viewModel.loadBrowsers();

        return view;
    }

    private void setupObservers() {
        viewModel.browsers.observe(getViewLifecycleOwner(), browsers -> {
            BrowserGridAdapter adapter = new BrowserGridAdapter(requireContext(), browsers, url, this);
            gridView.setAdapter(adapter);
        });
    }

    private void applyPopupTheme(View root) {
        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(requireContext());

        int surface = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupSurface, R.color.PopUpCardBg);
        int dock = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupDock, R.color.PopUpCardDockBg);
        int dockBtn = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupDockButton, R.color.PopUpCardDockBtn);
        int dockBtnText = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupDockButtonText, R.color.PopUpCardDockText);
        int action = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupAction, R.color.PopUpCard_ActionBtnBg);
        int text = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupText, R.color.text);

        View sheetRoot = root.findViewById(R.id.card);
        if (sheetRoot != null && sheetRoot.getBackground() != null) {
            Drawable bg = DrawableCompat.wrap(sheetRoot.getBackground().mutate());
            DrawableCompat.setTint(bg, surface);
            sheetRoot.setBackground(bg);
        }

        urlTextView.setTextColor(text);

        View privacyBtn = root.findViewById(R.id.privacyBtn);
        if (privacyBtn instanceof Button) {
            Button btn = (Button) privacyBtn;
            btn.setBackgroundTintList(ColorStateList.valueOf(dock));
            btn.setTextColor(text);
            btn.setCompoundDrawableTintList(ColorStateList.valueOf(text));
        }

        if (copyBtn != null) {
            copyBtn.setBackgroundTintList(ColorStateList.valueOf(dockBtn));
            copyBtn.setTextColor(dockBtnText);
            copyBtn.setCompoundDrawableTintList(ColorStateList.valueOf(dockBtnText));
        }

        tintCard(previewLinkBtn, action);
        tintCard(shareBtn, action);
        tintCard(previewPageBtn, action);

        tintNestedText(previewLinkBtn, text);
        tintNestedText(shareBtn, text);
        tintNestedText(previewPageBtn, text);
    }

    private void tintCard(CardView card, int color) {
        if (card != null) {
            card.setCardBackgroundColor(color);
        }
    }

    private void tintNestedText(View root, int color) {
        if (!(root instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            } else if (child instanceof ViewGroup) {
                tintNestedText(child, color);
            }
        }
    }

    /** Redirects to MainActivity (forces update flow). Safe to call even before fragment attaches. */
    private void redirectToMainForUpdate() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent i = new Intent(activity, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(i);
            activity.finish();
        }
        dismissAllowingStateLoss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() != null) {
            getActivity().finish();  // Close the transparent activity when popup closes
        }
    }


    // Called by adapter when browser clicked
    public void openUrlInBrowser(String packageName) {
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(url)) return;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            dismiss();
            requireActivity().finish(); // close your app after opening external browser
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to open browser", Toast.LENGTH_SHORT).show();
        }
    }
}
