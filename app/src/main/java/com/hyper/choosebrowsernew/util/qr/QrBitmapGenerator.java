package com.hyper.choosebrowsernew.util.qr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Generates high-resolution, scannable QR Code bitmaps from URLs or text
 * using Project Nayuki's pure Java QR engine with zero external dependencies.
 */
public class QrBitmapGenerator {

    private static final int DEFAULT_SIZE = 600;
    private static final int QUIET_ZONE_MODULES = 3; // Quiet zone border

    public static Bitmap generate(String content) {
        return generate(content, DEFAULT_SIZE);
    }

    public static Bitmap generate(String content, int targetSize) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            QrCode qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
            int qrSize = qr.size;
            int totalModules = qrSize + (QUIET_ZONE_MODULES * 2);

            int scale = Math.max(2, targetSize / totalModules);
            int imageSize = totalModules * scale;

            Bitmap bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Solid high-contrast white background for universal camera readability
            canvas.drawColor(Color.WHITE);

            Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            darkPaint.setColor(Color.parseColor("#121214")); // Deep near-black for crisp scanning
            darkPaint.setStyle(Paint.Style.FILL);

            for (int y = 0; y < qrSize; y++) {
                for (int x = 0; x < qrSize; x++) {
                    if (qr.getModule(x, y)) {
                        float left = (x + QUIET_ZONE_MODULES) * scale;
                        float top = (y + QUIET_ZONE_MODULES) * scale;
                        float right = left + scale;
                        float bottom = top + scale;
                        canvas.drawRect(left, top, right, bottom, darkPaint);
                    }
                }
            }

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
