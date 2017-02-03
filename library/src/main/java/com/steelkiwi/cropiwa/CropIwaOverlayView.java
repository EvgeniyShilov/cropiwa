package com.steelkiwi.cropiwa;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author Yaroslav Polyakov https://github.com/polyak01
 * 03.02.2017.
 */
class CropIwaOverlayView extends View {

    private static final float CLICK_AREA_CORNER_POINT = 0f;
    private static final int MIN_HEIGHT_CROP_AREA = 0;
    private static final int MIN_WIDTH_CROP_AREA = 0;

    private static final int TOP_LEFT = 0;
    private static final int TOP_RIGHT = 1;
    private static final int BOTTOM_LEFT = 2;
    private static final int BOTTOM_RIGHT = 3;

    private Paint clearPaint;
    private Paint generalPaint;

    private CornerPoint[] cornerPoints;
    private SparseArray<CornerPoint> fingerToCornerMapping;

    private PointF cropDragStartPoint;
    private RectF cropRectBeforeDrag;

    private RectF cropRegion;

    public CropIwaOverlayView(Context context) {
        super(context);
    }

    public CropIwaOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CropIwaOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CropIwaOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        cropDragStartPoint = new PointF();
        cropRectBeforeDrag = new RectF();

        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        generalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onStartGesture(ev);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                onPointerMove(ev);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onPointerUp(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onEndGesture();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    private void onStartGesture(MotionEvent ev) {
        //Does user want to resize the crop area?
        if (tryAssociateWithCorner(ev)) {
            return;
        }
        //Does user want to drag the crop area?
        int index = ev.getActionIndex();
        if (cropRegion.contains(ev.getX(index), ev.getY(index))) {
            cropDragStartPoint = new PointF(ev.getX(index), ev.getY(index));
            cropRectBeforeDrag = new RectF(cropRegion);
        }
    }

    private void onPointerDown(MotionEvent ev) {
        tryAssociateWithCorner(ev);
    }

    private void onPointerUp(MotionEvent ev) {

    }

    private void onPointerMove(MotionEvent ev) {
        if (isResizing()) {
            for (int i = 0; i < ev.getPointerCount(); i++) {
                int id = ev.getPointerId(i);
                CornerPoint point = fingerToCornerMapping.get(id);
                if (point != null) {
                    point.processDrag(ev.getX(i), ev.getY(i));
                }
            }
            updateCropAreaCoordinates();
        } else if (isDraggingCropArea()) {
            float deltaX = ev.getX() - cropDragStartPoint.x;
            float deltaY = ev.getY() - cropDragStartPoint.y;
            cropRegion = Utils.moveRect(cropRectBeforeDrag, deltaX, deltaY, cropRegion);
            updateCornerPointsCoordinates();
        }
    }

    private void onEndGesture() {
        fingerToCornerMapping.clear();
        cropDragStartPoint = null;
        cropRectBeforeDrag = null;
    }

    private void updateCornerPointsCoordinates() {

    }

    private void updateCropAreaCoordinates() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        configurePaintToDrawOverlay(generalPaint);
        canvas.drawRect(0, 0, getWidth(), getHeight(), generalPaint);

        canvas.drawRect(cropRegion, clearPaint);

        configurePaintToDrawBorder(generalPaint);
        canvas.drawRect(cropRegion, generalPaint);

        for (CornerPoint point : cornerPoints) {

        }
    }

    private void configurePaintToDrawOverlay(Paint paint) {

    }

    private void configurePaintToDrawBorder(Paint paint) {

    }

    private boolean isResizing() {
        return fingerToCornerMapping.size() != 0;
    }

    private boolean isDraggingCropArea() {
        return cropDragStartPoint != null;
    }

    /**
     * @return {@literal true} if ev.x && ev.y are in area of some corner point
     */
    private boolean tryAssociateWithCorner(MotionEvent ev) {
        int index = ev.getActionIndex();
        return tryAssociateWithCorner(
                ev.getPointerId(index),
                ev.getX(index), ev.getY(index));
    }

    private boolean tryAssociateWithCorner(int id, float x, float y) {
        for (CornerPoint cornerPoint : cornerPoints) {
            if (cornerPoint.isClicked(x, y)) {
                fingerToCornerMapping.put(id, cornerPoint);
                return true;
            }
        }
        return false;
    }

    private static class CornerPoint {

        private RectF clickableArea;

        private PointF thisPoint;
        private PointF horizontalNeighbourPoint;
        private PointF verticalNeighbourPoint;

        public CornerPoint(
                PointF thisPoint, PointF horizontalNeighbourPoint,
                PointF verticalNeighbourPoint) {
            this.thisPoint = thisPoint;
            this.horizontalNeighbourPoint = horizontalNeighbourPoint;
            this.verticalNeighbourPoint = verticalNeighbourPoint;
            this.clickableArea = new RectF();
        }

        public void moveTo(float x, float y) {
            processDrag(x, y);
        }

        public void processDrag(float x, float y) {
            float newX = computeCoordinate(
                    thisPoint.x, x, horizontalNeighbourPoint.x,
                    MIN_WIDTH_CROP_AREA);
            thisPoint.x = newX;
            verticalNeighbourPoint.x = newX;

            float newY = computeCoordinate(
                    thisPoint.y, y, verticalNeighbourPoint.y,
                    MIN_HEIGHT_CROP_AREA);
            thisPoint.y = newY;
            horizontalNeighbourPoint.y = newY;
        }

        private float computeCoordinate(float old, float candidate, float opposite, int min) {
            float minAllowedPosition;
            boolean isCandidateAllowed = Math.abs(candidate - opposite) > min;
            boolean isDraggingFromLeftOrTop = opposite > old;
            if (isDraggingFromLeftOrTop) {
                minAllowedPosition = opposite - min;
                isCandidateAllowed &= candidate < opposite;
            } else {
                minAllowedPosition = opposite + min;
                isCandidateAllowed &= candidate > opposite;
            }
            return isCandidateAllowed ? candidate : minAllowedPosition;
        }

        public boolean isClicked(float x, float y) {
            clickableArea.set(thisPoint.x, thisPoint.y, thisPoint.x, thisPoint.y);
            Utils.enlargeRectBy(CLICK_AREA_CORNER_POINT, clickableArea);
            return clickableArea.contains(x, y);
        }
    }
}