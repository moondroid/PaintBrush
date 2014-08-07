package it.moondroid.paintbrush.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by marco.granatiero on 07/08/2014.
 */

public class DrawingView extends View {

    public enum MotionMode {
        DRAW_POLY
    }

    private static final float STROKE_WIDTH = 20.0f;

    /** Need to track this so the dirty region can accommodate the stroke. **/
    private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;

    private Paint paint = new Paint();
    private Path path = new Path();

    /**
     * Optimizes painting by invalidating the smallest possible area.
     */
    private float lastTouchX;
    private float lastTouchY;
    private final RectF dirtyRect = new RectF();

//    final OnTouchListener selectionAndMoveListener = // not shown;
//
//    final OnTouchListener drawRectangleListener = // not shown;
//
//    final OnTouchListener drawOvalListener = // not shown;

    final OnTouchListener drawPolyLineListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d("jabagator", "onTouch: " + event);
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    // No end point yet, so don't waste cycles invalidating.
                    return true;

                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    // Start tracking the dirty region.
                    resetDirtyRect(eventX, eventY);

                    // When the hardware tracks events faster than
                    // they can be delivered to the app, the
                    // event will contain a history of those skipped points.
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }

                    // After replaying history, connect the line to the touch point.
                    path.lineTo(eventX, eventY);
                    break;

                default:
                    Log.d("jabagator", "Unknown touch event  " + event.toString());
                    return false;
            }

            // Include half the stroke width to avoid clipping.
            invalidate(
                    (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        /**
         * Called when replaying history to ensure the dirty region
         * includes all points.
         */
        private void expandDirtyRect(float historicalX, float historicalY) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }
            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        /**
         * Resets the dirty region when the motion event occurs.
         */
        private void resetDirtyRect(float eventX, float eventY) {

            // The lastTouchX and lastTouchY were set when the ACTION_DOWN
            // motion event occurred.
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }
    };

    /** DrawingView Constructor */
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);

        setMode(MotionMode.DRAW_POLY);
    }

    public void clear() {
        path.reset();

        // Repaints the entire view.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    /**
     * Sets the DrawingView into one of several modes, such
     * as "select" mode (e.g., for moving or resizeing objects),
     * or "Draw polyline" (smooth curve), "draw rectangle", etc.
     */
    private void setMode(MotionMode motionMode) {
        switch(motionMode) {
//            case SELECT_AND_MOVE:
//                setOnTouchListener(selectionAndMoveListener);
//                break;
            case DRAW_POLY:
                setOnTouchListener(drawPolyLineListener);
                break;
//            case DRAW_RECTANGLE:
//                setOnTouchListener(drawRectangleListener);
//                break;
//            case DRAW_OVAL:
//                setOnTouchListener(drawOvalListener);
//                break;
            default:
                throw new IllegalStateException("Unknown MotionMode " + motionMode);
        }
    }
}