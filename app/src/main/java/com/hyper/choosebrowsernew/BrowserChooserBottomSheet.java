package com.hyper.choosebrowsernew;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class BrowserChooserBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_URL = "arg_url";
    private String url;
    private GridView gridView;
    private TextView urlTextView;
    private Button copyBtn;
    private CardView shareBtn;
    private CardView previewLinkBtn;
    private CardView previewPageBtn;

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
                activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
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

        loadBrowsers();

        return view;
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

    private void loadBrowsers() {
        PackageManager pm = requireContext().getPackageManager();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL);




        Log.d("BrowserChooser", "Found browsers count: " + resolveInfos.size());
        for (ResolveInfo info : resolveInfos) {
            Log.d("BrowserChooser", "Browser: " + info.loadLabel(pm) + " pkg: " + info.activityInfo.packageName);
        }


        List<AppInfo> browsers = new ArrayList<>();

        String myPackage = requireContext().getPackageName();
//        String chooseBrowserOld = "com.hyper.choosebrowser";

        for (ResolveInfo info : resolveInfos) {
            String pkgName = info.activityInfo.packageName;
            if (pkgName.equals(myPackage)) continue; // exclude self
            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            browsers.add(new AppInfo(label, pkgName, icon));
        }

        BrowserGridAdapter adapter = new BrowserGridAdapter(requireContext(), browsers, url, this);
        gridView.setNumColumns(4);
        gridView.setAdapter(adapter);
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
