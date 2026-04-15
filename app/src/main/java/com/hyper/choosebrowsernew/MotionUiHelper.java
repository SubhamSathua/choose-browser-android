package com.hyper.choosebrowsernew;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public final class MotionUiHelper {

    private static final long TAP_SCALE_DOWN_MS = 90L;
    private static final long TAP_SCALE_UP_MS = 120L;
    private static final float TAP_SCALE_FACTOR = 0.97f;

    private MotionUiHelper() {
        // Utility class
    }

    public static void applyTapScale(View target) {
        if (target == null) return;
        target.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(TAP_SCALE_FACTOR)
                            .scaleY(TAP_SCALE_FACTOR)
                            .setDuration(TAP_SCALE_DOWN_MS)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(TAP_SCALE_UP_MS)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    break;
                default:
                    break;
            }
            // Keep existing click handlers and ripple behavior.
            return false;
        });
    }
}