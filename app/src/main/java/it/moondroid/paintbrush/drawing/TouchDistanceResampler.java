package it.moondroid.paintbrush.drawing;

/**
 * Created by marco.granatiero on 08/08/2014.
 */

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.FloatMath;

public abstract class TouchDistanceResampler extends TouchResampler {
    private float mDistance0;
    private float mDistance1;
    private float mDistance2;
    private float mDistance3;
    private float mLastPathTime;
    private final Path mPath;
    private PointF mPathBezier1;
    private final PathMeasure mPathMeasure;
    private float[] mTempPos;
    private float mTime0;
    private float mTime1;
    private float mTime2;
    private float mTime3;

    public TouchDistanceResampler() {
        this.mPathBezier1 = new PointF();
        this.mTempPos = new float[2];
        this.mPath = new Path();
        this.mPathMeasure = new PathMeasure();
    }

    private static float calcInstantVelocity(float da, float db, float dc, float ta, float tb, float tc, float s) {
        return ((((da - (2.0f * db)) + dc) * s) - (da - db)) / ((((ta - (2.0f * tb)) + tc) * s) - (ta - tb));
    }

    private static float calcRootOfQuadraticBezier(float a, float b, float c, float distance) {
        float A = a - 2.0f * b + c;
        float B = a - b;
        float C = a - distance;
        return A == 0.0f ? (-C) / B : (B + FloatMath.sqrt((B * B) - (A * C))) / A;
    }

    protected void addToPath(float x, float y, long t, boolean isMove) {
        this.mPath.quadTo(this.mPathBezier1.x, this.mPathBezier1.y, (this.mPathBezier1.x + x) / 2.0f, (this.mPathBezier1.y + y) / 2.0f);
        this.mPathBezier1.set(x, y);
        this.mPathMeasure.setPath(this.mPath, false);
        float totalDistance = this.mPathMeasure.getLength();
        float totalTime = (this.mLastPathTime + ((float) t)) / 2.0f;
        if (isMove) {
            if (this.mDistance2 == 0.0f) {
                this.mDistance0 = 0.0f;
                this.mDistance1 = totalDistance / 4.0f;
                this.mDistance2 = totalDistance / 2.0f;
                this.mTime0 = 0.0f;
                this.mTime1 = totalTime / 4.0f;
                this.mTime2 = totalTime / 2.0f;
            } else {
                this.mDistance0 = this.mDistance2;
                this.mDistance1 = this.mDistance3;
                this.mDistance2 = (this.mDistance1 + totalDistance) / 2.0f;
                this.mTime0 = this.mTime2;
                this.mTime1 = this.mTime3;
                this.mTime2 = (this.mTime1 + totalTime) / 2.0f;
            }
            this.mDistance3 = totalDistance;
            this.mTime3 = totalTime;
            super.addToPath(x, y, t, isMove);
        } else {
            this.mDistance0 = this.mDistance2;
            this.mDistance1 = this.mDistance3;
            this.mDistance2 = totalDistance;
            this.mTime0 = this.mTime2;
            this.mTime1 = this.mTime3;
            this.mTime2 = this.mLastPathTime;
            super.addToPath(x, y, (long) this.mLastPathTime, isMove);
        }
        this.mLastPathTime = (float) t;
    }

    public boolean getXYVAtDistance(float distance, float[] xyv) {
        if (this.mDistance2 == 0.0f || distance > this.mDistance2) {
            return false;
        }
        this.mPathMeasure.getPosTan(distance, this.mTempPos, null);
        xyv[0] = this.mTempPos[0];
        xyv[1] = this.mTempPos[1];
        xyv[2] = calcInstantVelocity(this.mDistance0, this.mDistance1, this.mDistance2, this.mTime0, this.mTime1, this.mTime2, calcRootOfQuadraticBezier(this.mDistance0, this.mDistance1, this.mDistance2, distance));
        return true;
    }

    protected void startPath(float x, float y, long t) {
        this.mPath.reset();
        this.mPath.moveTo(x, y);
        this.mPathBezier1.set(x, y);
        this.mDistance0 = 0.0f;
        this.mDistance1 = 0.0f;
        this.mDistance2 = 0.0f;
        this.mDistance3 = 0.0f;
        this.mTime0 = 0.0f;
        this.mTime1 = 0.0f;
        this.mTime2 = 0.0f;
        this.mTime3 = 0.0f;
        this.mLastPathTime = 0.0f;
        super.startPath(x, y, t);
    }
}