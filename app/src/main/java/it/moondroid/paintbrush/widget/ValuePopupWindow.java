package it.moondroid.paintbrush.widget;

/**
 * Created by marco.granatiero on 08/08/2014.
 */


import android.content.Context;
import android.widget.PopupWindow;

import it.moondroid.paintbrush.R;


public abstract class ValuePopupWindow extends PopupWindow {

    public ValuePopupWindow(Context context) {
        super(context);
        prepare(context);
    }

    protected float convertValue(float value) {
        return value;
    }

    protected void prepare(Context context) {
        int width = context.getResources().getDimensionPixelSize(R.dimen.popup_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.popup_height);
        setWidth(width);
        setHeight(height);
        setBackgroundDrawable(context.getResources().getDrawable(R.drawable.brush_opacity_size_bg));
    }

    protected abstract void setConvertedValue(float f);

    public final void setValue(float value) {
        setConvertedValue(convertValue(value));
    }
}