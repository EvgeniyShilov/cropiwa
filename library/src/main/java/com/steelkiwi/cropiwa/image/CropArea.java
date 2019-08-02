package com.steelkiwi.cropiwa.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * @author yarolegovich
 * 25.02.2017.
 */
public class CropArea {

    public static CropArea create(RectF coordinateSystem, RectF imageRect, RectF cropRect) {
        return new CropArea(
                moveRectToCoordinateSystem(coordinateSystem, imageRect),
                moveRectToCoordinateSystem(coordinateSystem, cropRect));
    }

    private static Rect moveRectToCoordinateSystem(RectF system, RectF rect) {
        float originX = system.left, originY = system.top;
        return new Rect(
                Math.round(rect.left - originX), Math.round(rect.top - originY),
                Math.round(rect.right - originX), Math.round(rect.bottom - originY));
    }

    private final Rect imageRect;
    private final Rect cropRect;

    public CropArea(Rect imageRect, Rect cropRect) {
        this.imageRect = imageRect;
        this.cropRect = cropRect;
    }

    public Bitmap applyCropTo(Bitmap bitmap) {
        int x = findRealCoordinate(bitmap.getWidth(), cropRect.left, imageRect.width());
        int y = findRealCoordinate(bitmap.getHeight(), cropRect.top, imageRect.height());
        int width = findRealCoordinate(bitmap.getWidth(), cropRect.width(), imageRect.width());
        int height = findRealCoordinate(bitmap.getHeight(), cropRect.height(), imageRect.height());
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > bitmap.getWidth()) width = bitmap.getWidth() - x;
        if (y + height > bitmap.getHeight()) height = bitmap.getHeight() - y;
        Bitmap immutableCropped = Bitmap.createBitmap(bitmap, x, y, width, height);
        return immutableCropped.copy(immutableCropped.getConfig(), true);
    }


    private int findRealCoordinate(int imageRealSize, int cropCoordinate, float cropImageSize) {
        return Math.round((imageRealSize * cropCoordinate) / cropImageSize);
    }

}
