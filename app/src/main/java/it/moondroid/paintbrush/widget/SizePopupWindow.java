package it.moondroid.paintbrush.widget;

/**
 * Created by marco.granatiero on 08/08/2014.
 */


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import it.moondroid.paintbrush.R;


public class SizePopupWindow extends ValuePopupWindow {
    private CircleView mCircleView;
    private TextView mTextView;

    public static class CircleView extends View {
        private Paint mPaint;
        private float mRadius;

        public CircleView(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mPaint = new Paint(1);
        }

        private void setCircleColor(int color) {
            this.mPaint.setColor(color);
            invalidate();
        }

        private void setCircleRadius(float radius) {
            this.mRadius = radius;
            invalidate();
        }

        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle((float) (canvas.getWidth() / 2), (float) (canvas.getHeight() / 2), this.mRadius, this.mPaint);
        }
    }

    public SizePopupWindow(Context context) {
        super(context);
        View v = LayoutInflater.from(context).inflate(R.layout.size_popup, null);
        this.mTextView = (TextView) v.findViewById(R.id.sizePopupText);
        this.mCircleView = (CircleView) v.findViewById(R.id.sizePopupCircle);
        this.mCircleView.setCircleColor(-1);
        setContentView(v);
    }

    protected void setConvertedValue(float value) {
        if (this.mTextView.getTextSize() > 0.5f * value) {
            this.mTextView.setVisibility(View.GONE);
        } else {
            this.mTextView.setVisibility(View.VISIBLE);
            this.mTextView.setText(Integer.toString((int) value));
        }
        this.mCircleView.setCircleRadius(value / 2.0f);
    }
}