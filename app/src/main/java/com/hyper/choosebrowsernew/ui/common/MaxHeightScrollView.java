package com.hyper.choosebrowsernew.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * High-performance ScrollView with a dynamic maximum height constraint.
 *
 * Supports capping height based on:
 * 1. An exact pixel value via {@link #setMaxHeight(int)} or android:maxHeight XML attribute.
 * 2. A dynamic number of lines via {@link #setMaxLines(int, TextView)}, which computes the
 *    ceiling from the reference TextView's rendered line height, adapting in real time to
 *    system font scaling (Settings -> Display -> Font size), font metrics, line spacing,
 *    and view padding.
 */
public class MaxHeightScrollView extends ScrollView {

    private int maxHeight = -1;
    private int maxLines = -1;
    private TextView referenceTextView;

    public MaxHeightScrollView(Context context) {
        super(context);
        init(context, null);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = new int[]{android.R.attr.maxHeight};
            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            try {
                maxHeight = ta.getDimensionPixelSize(0, -1);
            } finally {
                ta.recycle();
            }
        }
    }

    /**
     * Set a fixed maximum height in pixels.
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
        invalidate();
    }

    /**
     * Dynamically constrain the maximum height to a specific number of lines
     * computed from the reference TextView's rendered line height.
     *
     * @param lines Number of lines to cap height to (e.g. 7 lines)
     * @param ref   Reference TextView whose typography determines line height
     */
    public void setMaxLines(int lines, TextView ref) {
        this.maxLines = lines;
        this.referenceTextView = ref;
        updateMaxHeightFromLines();
    }

    private void updateMaxHeightFromLines() {
        if (maxLines > 0 && referenceTextView != null) {
            int lh = referenceTextView.getLineHeight();
            if (lh <= 0) {
                float ts = referenceTextView.getTextSize();
                lh = Math.round(ts * 1.35f);
            }
            int padding = referenceTextView.getPaddingTop() + referenceTextView.getPaddingBottom();
            this.maxHeight = (lh * maxLines) + padding;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxLines > 0 && referenceTextView != null) {
            int lh = referenceTextView.getLineHeight();
            if (lh > 0) {
                int padding = referenceTextView.getPaddingTop() + referenceTextView.getPaddingBottom();
                maxHeight = (lh * maxLines) + padding;
            }
        }

        if (maxHeight > 0) {
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (heightSize > maxHeight || heightMode == MeasureSpec.UNSPECIFIED) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
