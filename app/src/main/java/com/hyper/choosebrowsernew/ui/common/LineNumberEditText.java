package com.hyper.choosebrowsernew.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * An EditText that draws line numbers in the left gutter — minimal code-editor feel.
 */
public class LineNumberEditText extends AppCompatEditText {

    private final Paint gutterPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gutterBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint  = new Paint();
    private final Rect  lineBounds    = new Rect();

    private int gutterWidth = 0;

    public LineNumberEditText(Context context) {
        super(context);
        init(context);
    }

    public LineNumberEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        setTypeface(android.graphics.Typeface.MONOSPACE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        setBackgroundColor(Color.TRANSPARENT);
        setTextColor(0xFFE0E0E0);
        setHorizontallyScrolling(false); // wrap lines for readability

        gutterBgPaint.setColor(0xFF1E1E2A);
        gutterBgPaint.setStyle(Paint.Style.FILL);

        gutterPaint.setColor(0xFF606080);
        gutterPaint.setTextSize(getTextSize());
        gutterPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        gutterPaint.setTextAlign(Paint.Align.RIGHT);

        dividerPaint.setColor(0xFF444466);
        dividerPaint.setStrokeWidth(1f);

        // Recompute gutter width when text changes
        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateGutterWidth(); invalidate(); }
        });

        updateGutterWidth();
    }

    private void updateGutterWidth() {
        int lines = Math.max(1, getLineCount());
        String maxLabel = String.valueOf(lines);
        float textWidth = gutterPaint.measureText(maxLabel);
        int dp8 = (int)(8 * getResources().getDisplayMetrics().density);
        gutterWidth = (int)(textWidth) + dp8 * 2;
        setPadding(gutterWidth + dp8, getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lineCount = getLineCount();
        int scrollY   = getScrollY();

        // Draw gutter background
        canvas.drawRect(0, scrollY, gutterWidth, scrollY + getHeight(), gutterBgPaint);
        // Draw divider line
        canvas.drawLine(gutterWidth, scrollY, gutterWidth, scrollY + getHeight(), dividerPaint);

        // Draw each line number
        for (int i = 0; i < lineCount; i++) {
            getLineBounds(i, lineBounds);
            // Only draw visible lines for performance
            if (lineBounds.bottom < scrollY) continue;
            if (lineBounds.top   > scrollY + getHeight()) break;

            int dp4 = (int)(4 * getResources().getDisplayMetrics().density);
            canvas.drawText(
                    String.valueOf(i + 1),
                    gutterWidth - dp4,
                    lineBounds.bottom - getLayout().getLineDescent(i),
                    gutterPaint
            );
        }

        super.onDraw(canvas);
    }
}
