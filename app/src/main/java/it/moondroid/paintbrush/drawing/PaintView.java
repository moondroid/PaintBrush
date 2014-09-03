package it.moondroid.paintbrush.drawing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import it.moondroid.paintbrush.R;

/**
 * Created by marco.granatiero on 07/08/2014.
 */
public class PaintView extends View {

    private static final float STROKE_WIDTH = 20.0f;

    private Brush mBrush;
    private int mColor;
    private int mLineColor;
    private float mDrawingAlpha;
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
    private Bitmap mMergedLayer;
    private Canvas mMergedLayerCanvas;
    private Bitmap mTextureLayer;
    private Canvas mTextureLayerCanvas;
    private BitmapDrawable mTextureDrawable;
    private Bitmap mTempPathLayer;
    private Canvas mTempPathLayerCanvas;

    private RectF mLineDirtyRect;
    private RectF mDirtyRect;

    private Paint mNormalPaint;
    private Paint mSrcPaint;
    private Paint mDstInPaint;
    private Paint mDstOutPaint;

    private OnTouchHandler mCurveDrawingHandler;
    private TouchResampler mTouchResampler;
    private float mMaxVelocityScale;
    private static float VELOCITY_MAX_SCALE = 130.0f;
    private static final Bitmap EMPTY_BITMAP = Build.VERSION.SDK_INT < 14 ? Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) : null;

    private Bitmap[] mMaskBitmap;
    private int mMaskPadding;

    private Random mRandom;
    private Matrix mMatrix;
    private float mDeviceAngle;
    private PointF mOldPt;
    private boolean mIsJitterColor;

    private boolean mDrawingLayerNeedDrawn;
    private boolean mIsBatchDraw;

    private BitmapHistoryManager mBitmapHistoryManager;
    private BitmapHistoryManager.HistoryChangedListener mUndoRedoListener;
    private OnStateChangedListener mOnStateChangedListener;

    public static interface OnStateChangedListener {
        void onResetCompleted();
        void onUndoRedoChanged(int undoSize, int redoSize);
    }

    private static interface OnTouchHandler {
        boolean onTouchEvent(int i, MotionEvent motionEvent);
    }


    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.mTextureDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.texture01));
        this.mTextureDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        mNormalPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mSrcPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mDstInPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mDstOutPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

//        mNormalPaint.setAntiAlias(true);
//        mNormalPaint.setColor(Color.BLACK);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeJoin(Paint.Join.ROUND);
//        paint.setStrokeWidth(STROKE_WIDTH);

        mDrawingLayerCanvas = new Canvas();
        mPathLayerCanvas = new Canvas();
        this.mMergedLayerCanvas = new Canvas();
        this.mTextureLayerCanvas = new Canvas();
        this.mTempPathLayerCanvas = new Canvas();

        mOnDrawCanvasRect = new Rect();
        mLineDirtyRect = new RectF();
        mDirtyRect = new RectF();

        mDrawingAlpha = 1.0f;

        mSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mDstInPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mDstOutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        this.mCurveDrawingHandler = new OnTouchHandler() {
            public boolean onTouchEvent(int action, MotionEvent event) {
                if (!PaintView.this.mBrush.traceMode) {
                    mTouchResampler.onTouchEvent(event);
                    return true;
                }
                return false;
            }
        };

        this.mTouchResampler = new MyTouchDistanceResampler();

        mRandom = new Random();
        mMatrix = new Matrix();
        mOldPt = new PointF();

        mUndoRedoListener = new BitmapHistoryManager.HistoryChangedListener() {

            @Override
            public void onHistoryChanged(int prevLength, int nextLength, int commandType, int mode) {
                if (PaintView.this.mOnStateChangedListener != null) {
                    PaintView.this.mOnStateChangedListener.onUndoRedoChanged(prevLength, nextLength);
                    if (commandType == BitmapHistoryManager.COMMAND_RESET) {
                        PaintView.this.mOnStateChangedListener.onResetCompleted();
                    }
                }
                if ((commandType == BitmapHistoryManager.COMMAND_RESET && mode == BitmapHistoryManager.RESET_RESTORE) || commandType == BitmapHistoryManager.COMMAND_READ) {
                    PaintView.this.invalidate();
                }
            }
        };
        mBitmapHistoryManager = new SimpleBitmapHistoryManager(context, mUndoRedoListener);

    }

    public void setBrush(Brush brush) {
        mBrush = brush;

        mPathWidth = brush.size;
        mPathWidthHalf = brush.size / 2.0f;

        mSpacing = brush.spacing * brush.size;

        releaseBrushSizeBitmaps();

        mPathLayer = Bitmap.createBitmap((int) mPathWidth, (int) mPathWidth, Bitmap.Config.ARGB_8888);
        mPathLayerCanvas.setBitmap(this.mPathLayer);

        mMaxVelocityScale = (brush.size * brush.lineEndSpeedLength) / VELOCITY_MAX_SCALE;

        this.mMaskBitmap = new Bitmap[brush.maskImageIdArray.length];
        this.mMaskPadding = (int) (this.mPathWidth / 3.5f);
        int i = 0;
        while (i < this.mMaskBitmap.length) {
            this.mMaskBitmap[i] = decodeScaledExpandResource(getResources(), brush.maskImageIdArray[i], (int) this.mPathWidth, (int) this.mPathWidth, this.mMaskPadding);
            i++;
        }

        if (((double) brush.jitterHue) == 0.0d && ((double) brush.jitterSaturation) == 0.0d && ((double) brush.jitterBrightness) == 0.0d) {
            mIsJitterColor = false;
        } else {
            mIsJitterColor = true;
        }

        this.mTempPathLayer = Bitmap.createBitmap((int) this.mPathWidth, (int) this.mPathWidth, Bitmap.Config.ARGB_8888);
        this.mTempPathLayerCanvas.setBitmap(this.mTempPathLayer);

    }

    private static Bitmap decodeScaledExpandResource(Resources res, int id, int width, int height, int padding) {
        Bitmap src = BitmapFactory.decodeResource(res, id);
        if (src == null) {
            return null;
        }
        Bitmap dst = Bitmap.createBitmap(padding * 2 + width, padding * 2 + height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dst);
        c.drawBitmap(src, new Rect(0, 0, src.getWidth(), src.getHeight()), new Rect(padding, padding, padding + width, padding + height), null);
        c.setBitmap(EMPTY_BITMAP);
        if (src == dst) {
            return dst;
        }
        src.recycle();
        return dst;
    }



    public Brush getBrush() {
        return mBrush;
    }


    public void setDrawingColor(int color) {
        mColor = color;
    }

    public void setDrawingBgColor(int color) {
        this.mBackgroundLayerColor = color;
        invalidate();
    }

    public float getDrawingScaledSize() {
        return mBrush.getScaledSize();
    }

    public void setDrawingScaledSize(float scaledSize) {
        if (mBrush.setScaledSize(scaledSize)) {
            setBrush(mBrush);
        }
    }

    public void setDrawingAlpha(float alpha) {
        this.mDrawingAlpha = alpha;
    }

    public float getDrawingAlpha() {
        return this.mDrawingAlpha;
    }


    public boolean isClear() {
        return (this.mBitmapHistoryManager.isEmpty()) ? true : false;
    }

    public void clear() {
        setDrawingForegroundBitmap(null);
        //setDrawingBackgroundBitmap(null);
    }

    public void undo() {
        mBitmapHistoryManager.moveHistoryBy(-1);
    }

    public void redo() {
        mBitmapHistoryManager.moveHistoryBy(1);
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public void setDrawingForegroundBitmap(Bitmap bitmap) {
        this.mDrawingLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
        this.mMergedLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
        if (bitmap == null) {
            this.mBitmapHistoryManager.reset(BitmapHistoryManager.RESET_CLEAR);
        } else {
            this.mMergedLayerCanvas.drawBitmap(bitmap, 0.0f, 0.0f, this.mSrcPaint);
            this.mBitmapHistoryManager.reset(BitmapHistoryManager.RESET_REBASE);
        }

        invalidate();
    }

    public Bitmap getForegroundBitmap() {
        return this.mMergedLayer;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        releaseViewSizeBitmaps();

        mDrawingLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mDrawingLayerCanvas.setBitmap(mDrawingLayer);
        this.mMergedLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        this.mMergedLayerCanvas.setBitmap(this.mMergedLayer);
        this.mTextureLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
        this.mTextureLayerCanvas.setBitmap(this.mTextureLayer);
        this.mTextureDrawable.setBounds(0, 0, w, h);
        this.mTextureDrawable.draw(this.mTextureLayerCanvas);

        this.mBitmapHistoryManager.init(getContext(), this.mMergedLayer, this.mMergedLayerCanvas);
        this.mBitmapHistoryManager.reset(BitmapHistoryManager.RESET_RESTORE);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mIsBatchDraw = false;

        if (this.mBrush == null) {
            return super.onTouchEvent(event);
        }
        int action = Build.VERSION.SDK_INT >= 8 ? event.getActionMasked() : event.getAction() & 255;

        if (this.mCurveDrawingHandler != null) {
            return this.mCurveDrawingHandler.onTouchEvent(action, event);
        }

        return false;

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.getClipBounds(this.mOnDrawCanvasRect);
        drawToCanvas(canvas, mOnDrawCanvasRect);
        canvas.restore();

        invalidate(mOnDrawCanvasRect);
    }

    private void drawToCanvas(Canvas canvas, Rect rect) {
        canvas.drawColor(mBackgroundLayerColor, PorterDuff.Mode.SRC);

        if (rect == null) {
            canvas.saveLayer(null, null, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        } else {
            canvas.saveLayer((float) (rect.left - 1), (float) (this.mOnDrawCanvasRect.top - 1),
                    (float) (this.mOnDrawCanvasRect.right + 1), (float) (this.mOnDrawCanvasRect.bottom + 1),
                    null, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        }

        if(mBrush.useSingleLayerStroke){
            canvas.drawBitmap(this.mMergedLayer, 0.0f, 0.0f, this.mSrcPaint);
            if ((!this.mDrawingLayerNeedDrawn)) {
                canvas.restore();
            }else {
                Paint p;
                p = !(this.mBrush.isEraser) ? this.mNormalPaint : this.mDstOutPaint;
                p.setAlpha((int) (this.mDrawingAlpha * 255.0f));
                canvas.drawBitmap(mDrawingLayer, 0.0f, 0.0f, p);
                canvas.restore();
            }
        }else{

            canvas.drawBitmap(this.mMergedLayer, 0.0f, 0.0f, this.mSrcPaint);
            if ((this.mDrawingLayerNeedDrawn)) {
                Paint p;
                p = !(this.mBrush.isEraser) ? this.mNormalPaint : this.mDstOutPaint;
                canvas.drawBitmap(mDrawingLayer, 0.0f, 0.0f, p);
            }
            canvas.restore();
        }

    }

    private void moveToThread(float x, float y) {
        float level = 1.0f;
        resetDrawingDirtyRect();
        moveToAction(x, y, level);
    }

    private void moveToAction(float x, float y, float level) {

        mOldPt.set(x, y);

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

        if (brush.spread > 0.0f) {
            float spreadAngle = this.mRandom.nextFloat() * 6.2831855f;
            drawX += (FloatMath.cos(spreadAngle) * brush.spread) * brush.size;
            drawY += (FloatMath.sin(spreadAngle) * brush.spread) * brush.size;
        }

        fillBrushWithColor(brush, drawX, drawY, tipAlpha);
        if (brush.useSmudging) {
            smudgingBrush(brush, this.mOldPt.x - this.mPathWidthHalf, this.mOldPt.y - this.mPathWidthHalf, tipAlpha);
        }
        maskBrushWithAngle(brush, getBrushSpotAngle(brush, this.mOldPt.x, this.mOldPt.y, x, y), tipAlpha);
        if (brush.textureDepth > 0.0f) {
            textureBrush(brush, drawX - this.mPathWidthHalf, drawY - this.mPathWidthHalf);
        }
        drawBrushWithScale(drawX, drawY, tipScale);

        mOldPt.set(x, y);
        mDirtyRect.union(drawX - this.mPathWidthHalf, drawY - this.mPathWidthHalf,
                this.mPathWidthHalf + drawX, this.mPathWidthHalf + drawY);

        mBitmapHistoryManager.addDirtyArea(drawX - this.mPathWidthHalf, drawY - this.mPathWidthHalf, this.mPathWidthHalf + drawX, this.mPathWidthHalf + drawY);

    }

    private void addUndo() {
        mBitmapHistoryManager.saveDirtyAreaToHistory();
    }


    private void fillBrushWithColor(Brush brush, float x, float y, float tipAlpha) {
        //int color = mLineColor;
        int color;
        float drawingAlpha;

        if(mBrush.useSingleLayerStroke){
            drawingAlpha = 1.0f;
        }else {
            drawingAlpha = mDrawingAlpha;
        }

        if ((!this.mIsJitterColor) || brush.useFirstJitter) {
            color = Color.argb((int) ((drawingAlpha * brush.colorPatchAlpha * tipAlpha) * 255.0f), Color.red(mLineColor), Color.green(mLineColor), Color.blue(mLineColor));
        } else {
            int jitterColor = jitterColor(this.mLineColor);
            color = Color.argb((int) (drawingAlpha * tipAlpha * 255.0f), Color.red(jitterColor), Color.green(jitterColor), Color.blue(jitterColor));
        }

        mPathLayerCanvas.drawColor(color, PorterDuff.Mode.SRC);

    }

    private void maskBrushWithAngle(Brush brush, float angle, float tipAlpha) {

        mDstInPaint.setAlpha((int) ((tipAlpha * tipAlpha) * 255.0f));

        Bitmap maskLayer = this.mMaskBitmap.length == 1 ? this.mMaskBitmap[0] : this.mMaskBitmap[this.mRandom.nextInt(this.mMaskBitmap.length)];

        if (angle != 0.0f) {
            this.mMatrix.setTranslate((float) (-mMaskPadding), (float) (-mMaskPadding));
            this.mMatrix.postRotate((float) Math.toDegrees((double) angle), this.mPathWidthHalf, this.mPathWidthHalf);
            mPathLayerCanvas.drawBitmap(maskLayer, this.mMatrix, mDstInPaint);
        }else{
            mPathLayerCanvas.drawBitmap(maskLayer, (float) (-mMaskPadding), (float) (-mMaskPadding), mDstInPaint);
        }


    }

    private void destLineThread() {
        if (this.mBrush.isEraser) {
            mergeWithAlpha(this.mDrawingAlpha, this.mDstOutPaint, this.mLineDirtyRect);
        } else {
            mergeWithAlpha(this.mDrawingAlpha, this.mNormalPaint, this.mLineDirtyRect);
        }
        if (!(this.mIsBatchDraw)) {
            addUndo();
        }
    }

    private void mergeWithAlpha(float alpha, Paint paint, RectF rectF) {
        if(mBrush.useSingleLayerStroke){
            paint.setAlpha((int) (255.0f * alpha));
        }else{
            paint.setAlpha(255);
        }
        this.mMergedLayerCanvas.save();
        this.mMergedLayerCanvas.clipRect(rectF);
        this.mMergedLayerCanvas.drawBitmap(this.mDrawingLayer, 0.0f, 0.0f, paint);
        this.mMergedLayerCanvas.restore();
        clearDrawingLayer(rectF);
        if (!(this.mIsBatchDraw)) {
            Rect rect = new Rect();
            rectF.round(rect);
            invalidate(rect);
        }

    }

    private void clearDrawingLayer(RectF rectF) {
        this.mDrawingLayerCanvas.save();
        this.mDrawingLayerCanvas.clipRect(rectF);
        this.mDrawingLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
        this.mDrawingLayerCanvas.restore();
        this.mDrawingLayerNeedDrawn = false;
    }

    private void openLine() {
        this.mDirtyRect.set(this.mOldPt.x - this.mPathWidthHalf,
                this.mOldPt.y - this.mPathWidthHalf, this.mOldPt.x + this.mPathWidthHalf,
                this.mOldPt.y + this.mPathWidthHalf);
    }

    private void closeLine() {
        this.mLineDirtyRect.union(this.mDirtyRect);
        if (!(this.mIsBatchDraw)) {
            Rect rect = new Rect();
            mDirtyRect.round(rect);
            invalidate(rect);
        }
    }

    private void resetDrawingDirtyRect() {
        this.mLineDirtyRect.setEmpty();
        this.mDrawingLayerNeedDrawn = true;
        this.mBitmapHistoryManager.clearDirtyArea();
    }

    private void drawBrushWithScale(float x, float y, float tipScale) {

        this.mNormalPaint.setAlpha(255);

        if (tipScale == 1.0f) {
            mDrawingLayerCanvas.drawBitmap(mPathLayer, x - mPathWidthHalf, y - mPathWidthHalf, mNormalPaint);
        }else{
            mDrawingLayerCanvas.save();
            mDrawingLayerCanvas.translate(x, y);
            mDrawingLayerCanvas.scale(tipScale, tipScale);
            mDrawingLayerCanvas.drawBitmap(mPathLayer, -mPathWidthHalf, -mPathWidthHalf, mNormalPaint);
            mDrawingLayerCanvas.restore();
        }


    }

    private float getBrushSpotAngle(Brush brush, float oldX, float oldY, float curX, float curY) {
        float angle = brush.angle * 6.2831855f;
        if (brush.useDeviceAngle) {
            angle += this.mDeviceAngle;
        }
        if (brush.useFlowingAngle) {
            angle += ((float) Math.atan2((double) (curY - oldY), (double) (curX - oldX))) - 1.5707964f;
        }
        return brush.angleJitter > 0.0f ? angle + ((this.mRandom.nextFloat() - 0.5f) * 6.2831855f) * brush.angleJitter : angle;
    }

    private int jitterColor(int color) {
        if (!mIsJitterColor) {
            return color;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        float hue = hsv[0];
        float saturation = hsv[1];
        hsv[0] = ((hue + (((mRandom.nextFloat() - 0.5f) * 360.0f) * mBrush.jitterHue)) + 360.0f) % 360.0f;
        hsv[1] = saturation + (mRandom.nextFloat() - 0.5f) * mBrush.jitterSaturation;
        hsv[2] = hsv[2] + (mRandom.nextFloat() - 0.5f) * mBrush.jitterBrightness;
        return Color.HSVToColor(hsv);
    }

    private void textureBrush(Brush brush, float x, float y) {
        this.mDstOutPaint.setAlpha((int) (brush.textureDepth * 255.0f));
        this.mPathLayerCanvas.drawBitmap(this.mTextureLayer, -x, -y, this.mDstOutPaint);
    }

    private void smudgingBrush(Brush brush, float x, float y, float tipAlpha) {
        x = -x;
        y = -y;
        this.mTempPathLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
        this.mTempPathLayerCanvas.drawBitmap(this.mMergedLayer, x, y, null);
        this.mNormalPaint.setAlpha((int) (this.mDrawingAlpha * 255.0f));
        this.mTempPathLayerCanvas.drawBitmap(this.mDrawingLayer, x, y, this.mNormalPaint);
        this.mNormalPaint.setAlpha((int) ((brush.smudgingPatchAlpha * tipAlpha) * 255.0f));
        this.mPathLayerCanvas.drawBitmap(this.mTempPathLayer, 0.0f, 0.0f, this.mNormalPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    public void release() {
        releaseViewSizeBitmaps();
        releaseBrushSizeBitmaps();
        mBitmapHistoryManager.release();
    }

    private void releaseBrushSizeBitmaps() {
        this.mPathLayerCanvas.setBitmap(EMPTY_BITMAP);
        if (this.mPathLayer != null) {
            this.mPathLayer.recycle();
            this.mPathLayer = null;
        }
        this.mTempPathLayerCanvas.setBitmap(EMPTY_BITMAP);
        if (this.mTempPathLayer != null) {
            this.mTempPathLayer.recycle();
            this.mTempPathLayer = null;
        }
        if (this.mMaskBitmap != null) {
            int i = 0;
            while (i < this.mMaskBitmap.length) {
                if (this.mMaskBitmap[i] != null) {
                    this.mMaskBitmap[i].recycle();
                    this.mMaskBitmap[i] = null;
                }
                i++;
            }
            this.mMaskBitmap = null;
        }
    }

    private void releaseViewSizeBitmaps() {
        this.mMergedLayerCanvas.setBitmap(EMPTY_BITMAP);
        if (this.mMergedLayer != null) {
            this.mMergedLayer.recycle();
            this.mMergedLayer = null;
        }
        this.mDrawingLayerCanvas.setBitmap(EMPTY_BITMAP);
        if (this.mDrawingLayer != null) {
            this.mDrawingLayer.recycle();
            this.mDrawingLayer = null;
        }
        this.mTextureLayerCanvas.setBitmap(EMPTY_BITMAP);
        if (this.mTextureLayer != null) {
            this.mTextureLayer.recycle();
            this.mTextureLayer = null;
        }
    }

    private class MyTouchDistanceResampler extends TouchDistanceResampler {
        private float mLastDrawDistance;
        private float[] mTempXYV = new float[3];

        @Override
        protected void onTouchDown(float x, float y) {
            Log.d("PaintView", "onTouchDown");
            this.mLastDrawDistance = 0.0f;
            PaintView.this.moveToThread(x, y);
        }

        @Override
        protected void onTouchMove(float x, float y, float t) {
            Log.d("PaintView", "onTouchMove");
            Brush brush = PaintView.this.mBrush;

            openLine();
            while (getXYVAtDistance(this.mLastDrawDistance, this.mTempXYV)) {
                float tipSpeedScale;
                float tipSpeedAlpha;
                float px = this.mTempXYV[0];
                float py = this.mTempXYV[1];
                float pv = this.mTempXYV[2];
                if (brush.lineEndSpeedLength > 0.0f) {
                    float velocityLevel;
                    velocityLevel = pv > PaintView.this.mMaxVelocityScale ? 1.0f : pv / PaintView.this.mMaxVelocityScale;
                    tipSpeedScale = brush.lineEndSizeScale + (1.0f - velocityLevel) * (1.0f - brush.lineEndSizeScale);
                    tipSpeedAlpha = brush.lineEndAlphaScale + (1.0f - velocityLevel) * (1.0f - brush.lineEndAlphaScale);
                } else {
                    tipSpeedScale = 1.0f;
                    tipSpeedAlpha = 1.0f;
                }
                if (this.mLastDrawDistance > 0.0f) {

                    Log.d("PaintView", "onTouchMove "+px+", "+py);
                    PaintView.this.addSpot(px, py, tipSpeedScale, tipSpeedAlpha);
                }
                this.mLastDrawDistance += PaintView.this.mSpacing * tipSpeedScale;
            }
            closeLine();
        }

        @Override
        protected void onTouchUp() {
            Log.d("PaintView", "onTouchUp");
            PaintView.this.destLineThread();
        }
    }


}
