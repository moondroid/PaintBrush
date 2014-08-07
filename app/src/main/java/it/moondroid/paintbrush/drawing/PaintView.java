package it.moondroid.paintbrush.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by marco.granatiero on 07/08/2014.
 */
public class PaintView extends View {

    private static final float STROKE_WIDTH = 20.0f;

    private Brush mBrush;
    private int mColor;
    private int mLineColor;
    private int mBackgroundLayerColor;

    private float mLastDrawDistance;
    private float mSpacing;

    private Bitmap mDrawingLayer;
    private Canvas mDrawingLayerCanvas;
    private Rect mOnDrawCanvasRect;
    private Bitmap mPathLayer;
    private Canvas mPathLayerCanvas;
    private float mPathWidth;
    private float mPathWidthHalf;

    private Paint mNormalPaint;


    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mNormalPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
//        mNormalPaint.setAntiAlias(true);
//        mNormalPaint.setColor(Color.BLACK);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeJoin(Paint.Join.ROUND);
//        paint.setStrokeWidth(STROKE_WIDTH);

        mDrawingLayerCanvas = new Canvas();
        mPathLayerCanvas = new Canvas();

        mOnDrawCanvasRect = new Rect();

    }

    public void setBrush(Brush brush) {
        mBrush = brush;

        mPathWidth = brush.size;
        mPathWidthHalf = brush.size / 2.0f;

        mSpacing = brush.spacing * brush.size;

        mPathLayer = Bitmap.createBitmap((int) mPathWidth, (int) mPathWidth, Bitmap.Config.ARGB_8888);
        mPathLayerCanvas.setBitmap(this.mPathLayer);
    }

    public void setDrawingColor(int color) {
        mColor = color;
    }

    public void setDrawingBgColor(int color) {
        this.mBackgroundLayerColor = color;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mDrawingLayerCanvas.setBitmap(mDrawingLayer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastDrawDistance = 0.0f;
                moveToThread(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (this.mLastDrawDistance > 0.0f) {
                    addSpot(x, y, 0.0f, 0.0f);
                }
                float tipSpeedScale = 1.0f;
                this.mLastDrawDistance += mSpacing * tipSpeedScale;
                return true;

            case MotionEvent.ACTION_UP:

                return true;

            default:
                return false;
        }

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(this.mOnDrawCanvasRect);
        drawToCanvas(canvas, mOnDrawCanvasRect);
    }

    private void drawToCanvas(Canvas canvas, Rect rect) {
        canvas.drawColor(mBackgroundLayerColor, PorterDuff.Mode.SRC);

        if (rect == null) {
            canvas.saveLayer(null, null, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        } else {
            canvas.saveLayer((float) (rect.left - 1), (float) (this.mOnDrawCanvasRect.top - 1), (float) (this.mOnDrawCanvasRect.right + 1), (float) (this.mOnDrawCanvasRect.bottom + 1), null, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        }

        canvas.drawBitmap(mDrawingLayer, 0.0f, 0.0f, mNormalPaint);
        canvas.restore();

    }

    private void moveToThread(float x, float y) {
        float level = 1.0f;
        moveToAction(x, y, level);
    }

    private void moveToAction(float x, float y, float level) {
        beforeLine(x, y);
    }


    private void beforeLine(float x, float y) {
        Brush brush = mBrush;
        mLineColor = mColor;
    }

    private void addSpot(float x, float y, float tipScale, float tipAlpha) {
        float drawX = x;
        float drawY = y;
        Brush brush = mBrush;

        fillBrushWithColor(brush, drawX, drawY, tipAlpha);
        drawBrushWithScale(drawX, drawY, tipScale);
    }

    private void fillBrushWithColor(Brush brush, float x, float y, float tipAlpha) {
        int color = mLineColor;

        this.mPathLayerCanvas.drawColor(color, PorterDuff.Mode.SRC);
    }

    private void drawBrushWithScale(float x, float y, float tipScale) {
        Log.d("PaintView","drawBrushWithScale x:"+x+" y:"+y);
        this.mNormalPaint.setAlpha(255);
        this.mDrawingLayerCanvas.drawBitmap(mPathLayer, x - this.mPathWidthHalf, y - this.mPathWidthHalf, mNormalPaint);
    }

 }
