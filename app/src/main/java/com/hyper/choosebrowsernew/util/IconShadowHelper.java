package com.hyper.choosebrowsernew.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

/**
 * Generates an adaptive drop-shadow based on the icon's exact alpha silhouette.
 * Works universally across any Android version and any OEM adaptive icon shape
 * (circle, squircle, rounded-rect, teardrop, or freeform).
 */
public class IconShadowHelper {

    private static final LruCache<String, Drawable> sCache = new LruCache<>(80);

    public static Drawable getShadowedIcon(Context context, Drawable original, String cacheKey) {
        if (original == null) return null;
        if (context == null) return original;

        if (cacheKey != null && !cacheKey.isEmpty()) {
            Drawable cached = sCache.get(cacheKey);
            if (cached != null) return cached;
        }

        try {
            float density = context.getResources().getDisplayMetrics().density;
            int targetSize = (int) (42 * density);
            int pad = (int) (3 * density);
            int iconSize = targetSize - (pad * 2);

            if (iconSize <= 0) return original;

            // 1. Draw original icon into a bitmap of size iconSize x iconSize
            Bitmap iconBmp = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBmp);
            original.setBounds(0, 0, iconSize, iconSize);
            original.draw(iconCanvas);

            // 2. Extract the exact alpha silhouette of whatever shape the OEM/app uses
            Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            shadowPaint.setColor(0x38000000); // ~22% black ambient drop shadow
            shadowPaint.setMaskFilter(new BlurMaskFilter(pad * 0.8f, BlurMaskFilter.Blur.NORMAL));

            int[] offsetXY = new int[2];
            Bitmap alphaMask = iconBmp.extractAlpha(shadowPaint, offsetXY);

            // 3. Compose final output bitmap: shadow underneath + original icon on top
            Bitmap output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            float dy = 1.2f * density; // subtle downward light source
            if (alphaMask != null) {
                canvas.drawBitmap(alphaMask, pad + offsetXY[0], pad + offsetXY[1] + dy, shadowPaint);
                alphaMask.recycle();
            }

            canvas.drawBitmap(iconBmp, pad, pad, null);
            iconBmp.recycle();

            Drawable result = new BitmapDrawable(context.getResources(), output);
            if (cacheKey != null && !cacheKey.isEmpty()) {
                sCache.put(cacheKey, result);
            }
            return result;
        } catch (Throwable t) {
            // Fallback gracefully to original icon if memory/canvas fails
            return original;
        }
    }

    public static void clearCache() {
        sCache.evictAll();
    }
}
