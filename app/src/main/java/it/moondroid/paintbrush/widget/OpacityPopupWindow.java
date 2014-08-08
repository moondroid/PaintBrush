package it.moondroid.paintbrush.widget;

/**
 * Created by marco.granatiero on 08/08/2014.
 */

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import it.moondroid.paintbrush.R;

public class OpacityPopupWindow extends ValuePopupWindow {
    private TextView mTextView;

    public OpacityPopupWindow(Context context) {
        super(context);
        View v = LayoutInflater.from(context).inflate(R.layout.opacity_popup, null);
        this.mTextView = (TextView) v.findViewById(R.id.opacityPopupText);
        setContentView(v);
    }

    protected void setConvertedValue(float value) {
        this.mTextView.setText(Integer.toString((int) (value)));
    }
}