package com.hyper.choosebrowsernew.ui.viewer;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.webkit.WebView;

import androidx.annotation.NonNull;

/**
 * Modular controller managing pinch-to-zoom gestures on a WebView with strict limits.
 */
public class PinchZoomController {

    public static final int MIN_ZOOM = 50;     // 50%
    public static final int MAX_ZOOM = 250;    // 250%
    public static final int DEFAULT_ZOOM = 100; // 100%

    public interface OnZoomChangeListener {
        void onZoomChanged(int zoomPercent);
    }

    private final WebView webView;
    private final ScaleGestureDetector scaleDetector;
    private OnZoomChangeListener zoomChangeListener;
    private int currentZoom = DEFAULT_ZOOM;
    private float accumulatedFactor = 1.0f;

    public PinchZoomController(@NonNull Context context, @NonNull WebView webView) {
        this.webView = webView;
        this.scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                accumulatedFactor = 1.0f;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                accumulatedFactor *= factor;

                // Only step in increments to keep rendering crisp and steady
                if (Math.abs(accumulatedFactor - 1.0f) >= 0.05f) {
                    int targetZoom = Math.round(currentZoom * accumulatedFactor);
                    targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetZoom));
                    accumulatedFactor = 1.0f;

                    if (targetZoom != currentZoom) {
                        currentZoom = targetZoom;
                        applyZoom(currentZoom);
                        if (zoomChangeListener != null) {
                            zoomChangeListener.onZoomChanged(currentZoom);
                        }
                    }
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                accumulatedFactor = 1.0f;
            }
        });

        // Initialize webView settings
        webView.getSettings().setTextZoom(currentZoom);
    }

    public void setOnZoomChangeListener(OnZoomChangeListener listener) {
        this.zoomChangeListener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (event.getPointerCount() > 1 || scaleDetector.isInProgress()) {
            if (webView.getParent() != null) {
                webView.getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        return false;
    }

    public void resetZoom() {
        currentZoom = DEFAULT_ZOOM;
        applyZoom(currentZoom);
        if (zoomChangeListener != null) {
            zoomChangeListener.onZoomChanged(currentZoom);
        }
    }

    public int getCurrentZoom() {
        return currentZoom;
    }

    private void applyZoom(int zoomPercent) {
        webView.getSettings().setTextZoom(zoomPercent);
    }
}
