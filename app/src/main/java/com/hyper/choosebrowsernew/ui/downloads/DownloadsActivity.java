package com.hyper.choosebrowsernew.ui.downloads;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hyper.choosebrowsernew.R;

import java.util.List;

public class DownloadsActivity extends AppCompatActivity implements DownloadsAdapter.OnDownloadActionListener {

    private DownloadsAdapter adapter;
    private RecyclerView rvDownloads;
    private LinearLayout layoutEmpty;
    private LinearLayout barSearch;
    private EditText etSearchQuery;
    private HorizontalScrollView scrollChips;
    private LinearLayout barSelectionMode;
    private TextView tvSelectedCount;
    private TextView btnSelectAll;

    private TextView[] chipViews;
    private final String[] chipCategories = {"All", "Images", "PDF", "Documents", "Others"};
    private int currentSortMode = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status and navigation bar matching dark preview theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.previewPage_primary));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.previewPage_primary));
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
        }

        setContentView(R.layout.activity_downloads);

        rvDownloads = findViewById(R.id.rvDownloads);
        layoutEmpty = findViewById(R.id.layoutEmptyDownloads);
        barSearch = findViewById(R.id.barSearch);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        scrollChips = findViewById(R.id.scrollChips);
        barSelectionMode = findViewById(R.id.barSelectionMode);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnSelectAll = findViewById(R.id.btnSelectAll);

        adapter = new DownloadsAdapter(this, this);
        rvDownloads.setLayoutManager(new LinearLayoutManager(this));
        rvDownloads.setAdapter(adapter);

        // Sticky Header elevation on scroll
        View stickyHeaderContainer = findViewById(R.id.stickyHeaderContainer);
        rvDownloads.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean canScrollUp = recyclerView.canScrollVertically(-1);
                if (stickyHeaderContainer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    stickyHeaderContainer.setElevation(canScrollUp ? 8f : 0f);
                }
            }
        });

        // Sticky Header: Back
        findViewById(R.id.btnDownloadsBack).setOnClickListener(v -> onBackPressed());

        // Search Toggle
        findViewById(R.id.btnDownloadsSearch).setOnClickListener(v -> {
            if (barSearch.getVisibility() == View.VISIBLE) {
                barSearch.setVisibility(View.GONE);
                etSearchQuery.setText("");
                adapter.setQuery("");
            } else {
                barSearch.setVisibility(View.VISIBLE);
                etSearchQuery.requestFocus();
            }
        });

        // Clear Search
        findViewById(R.id.btnClearSearch).setOnClickListener(v -> {
            etSearchQuery.setText("");
            adapter.setQuery("");
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setQuery(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Sort Button
        ImageButton btnSort = findViewById(R.id.btnDownloadsSort);
        btnSort.setOnClickListener(v -> showSortMenu());

        // More Button (Clear All)
        ImageButton btnMore = findViewById(R.id.btnDownloadsMore);
        btnMore.setOnClickListener(v -> showMoreMenu());

        // Chips setup
        setupChips();

        // Selection mode buttons
        findViewById(R.id.btnCancelSelection).setOnClickListener(v -> adapter.setSelectionMode(false));

        btnSelectAll.setOnClickListener(v -> {
            if (adapter.getSelectedCount() == adapter.getItemCount()) {
                adapter.selectAll(false);
                btnSelectAll.setText("Select All");
            } else {
                adapter.selectAll(true);
                btnSelectAll.setText("Deselect All");
            }
        });

        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelectedFiles());

        loadDownloads();
    }

    private void setupChips() {
        chipViews = new TextView[]{
                findViewById(R.id.chipAll),
                findViewById(R.id.chipImages),
                findViewById(R.id.chipPdf),
                findViewById(R.id.chipDocs),
                findViewById(R.id.chipOthers)
        };

        for (int i = 0; i < chipViews.length; i++) {
            final int index = i;
            chipViews[i].setOnClickListener(v -> {
                for (int j = 0; j < chipViews.length; j++) {
                    boolean active = (j == index);
                    chipViews[j].setTextColor(active ? Color.WHITE : Color.parseColor("#99FFFFFF"));
                    chipViews[j].setTextScaleX(active ? 1.05f : 1.0f);
                }
                adapter.setCategory(chipCategories[index]);
            });
        }
    }

    private void showSortMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_download_sort, null);
        dialog.setContentView(view);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.7f);
        }

        view.findViewById(R.id.btnSortClose).setOnClickListener(v -> dialog.dismiss());

        ImageView checkNewest = view.findViewById(R.id.checkSortDateNewest);
        ImageView checkOldest = view.findViewById(R.id.checkSortDateOldest);
        ImageView checkName = view.findViewById(R.id.checkSortNameAZ);
        ImageView checkSize = view.findViewById(R.id.checkSortSizeLargest);

        checkNewest.setVisibility(currentSortMode == 0 ? View.VISIBLE : View.GONE);
        checkOldest.setVisibility(currentSortMode == 1 ? View.VISIBLE : View.GONE);
        checkName.setVisibility(currentSortMode == 2 ? View.VISIBLE : View.GONE);
        checkSize.setVisibility(currentSortMode == 3 ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.sortDateNewest).setOnClickListener(v -> {
            currentSortMode = 0;
            adapter.setSortMode(0);
            dialog.dismiss();
        });
        view.findViewById(R.id.sortDateOldest).setOnClickListener(v -> {
            currentSortMode = 1;
            adapter.setSortMode(1);
            dialog.dismiss();
        });
        view.findViewById(R.id.sortNameAZ).setOnClickListener(v -> {
            currentSortMode = 2;
            adapter.setSortMode(2);
            dialog.dismiss();
        });
        view.findViewById(R.id.sortSizeLargest).setOnClickListener(v -> {
            currentSortMode = 3;
            adapter.setSortMode(3);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showMoreMenu() {
        showConfirmDialog(
                "Clear All Downloads",
                "ChooseBrowser / Downloads",
                "Do you want to permanently delete all downloaded files in ChooseBrowser?",
                "Delete All",
                () -> {
                    List<DownloadItem> all = DownloadsHelper.loadDownloadFiles();
                    for (DownloadItem di : all) {
                        DownloadsHelper.deleteFile(di);
                    }
                    loadDownloads();
                    Toast.makeText(this, "All downloads deleted", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void deleteSelectedFiles() {
        List<DownloadItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }

        showConfirmDialog(
                "Delete Selected Files",
                selected.size() + " file(s) selected",
                "Permanently delete " + selected.size() + " selected file(s) from your storage?",
                "Delete",
                () -> {
                    for (DownloadItem item : selected) {
                        DownloadsHelper.deleteFile(item);
                    }
                    adapter.setSelectionMode(false);
                    loadDownloads();
                    Toast.makeText(this, "Deleted " + selected.size() + " file(s)", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showConfirmDialog(String title, String subtitle, String message, String positiveText, Runnable onConfirm) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_download_confirm, null);
        dialog.setContentView(view);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.7f);
        }

        TextView tvTitle = view.findViewById(R.id.tvConfirmTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvConfirmSubtitle);
        TextView tvDetail = view.findViewById(R.id.tvConfirmDetail);
        TextView tvMessage = view.findViewById(R.id.tvConfirmMessage);
        Button btnCancel = view.findViewById(R.id.btnConfirmCancel);
        Button btnPositive = view.findViewById(R.id.btnConfirmPositive);
        ImageButton btnClose = view.findViewById(R.id.btnConfirmClose);

        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        tvDetail.setText("Downloads / ChooseBrowser");
        tvMessage.setText(message);
        btnPositive.setText(positiveText);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        dialog.show();
    }

    private void loadDownloads() {
        List<DownloadItem> items = DownloadsHelper.loadDownloadFiles();
        adapter.setData(items);
    }

    @Override
    public void onFileClick(DownloadItem item) {
        DownloadsHelper.openFile(this, item);
    }

    @Override
    public void onDeleteItem(DownloadItem item) {
        showConfirmDialog(
                "Delete File",
                item.getFileName(),
                "Permanently delete this file from your device storage?",
                "Delete",
                () -> {
                    DownloadsHelper.deleteFile(item);
                    loadDownloads();
                    Toast.makeText(this, "Deleted " + item.getFileName(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    public void onFileMoreClick(DownloadItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_download_file_actions, null);
        dialog.setContentView(view);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.7f);
        }

        view.findViewById(R.id.btnActionClose).setOnClickListener(v -> dialog.dismiss());

        TextView tvName = view.findViewById(R.id.tvActionFileName);
        TextView tvDetail = view.findViewById(R.id.tvActionFileDetail);
        tvName.setText(item.getFileName());
        tvDetail.setText(DownloadsHelper.formatFileSize(item.getFileSize()) + " • " + DownloadsHelper.formatDate(item.getDateModified()));

        view.findViewById(R.id.actionOpen).setOnClickListener(v -> {
            dialog.dismiss();
            DownloadsHelper.openFile(this, item);
        });
        view.findViewById(R.id.actionShare).setOnClickListener(v -> {
            dialog.dismiss();
            DownloadsHelper.shareFile(this, item);
        });
        view.findViewById(R.id.actionDelete).setOnClickListener(v -> {
            dialog.dismiss();
            onDeleteItem(item);
        });

        dialog.show();
    }

    @Override
    public void onSelectionModeChanged(boolean active, int count) {
        if (active) {
            scrollChips.setVisibility(View.GONE);
            barSelectionMode.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(count + " selected");
            btnSelectAll.setText(count == adapter.getItemCount() ? "Deselect All" : "Select All");
        } else {
            barSelectionMode.setVisibility(View.GONE);
            scrollChips.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onListChanged(boolean isEmpty) {
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvDownloads.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
            return;
        }
        if (barSearch.getVisibility() == View.VISIBLE) {
            barSearch.setVisibility(View.GONE);
            etSearchQuery.setText("");
            adapter.setQuery("");
            return;
        }
        super.onBackPressed();
    }
}
