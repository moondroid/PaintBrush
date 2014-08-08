package it.moondroid.paintbrush.widget;

/**
 * Created by marco.granatiero on 08/08/2014.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.SeekBar;


public class PopupSeekBar extends SeekBar {

    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private OnSeekBarChangeListener mOnSeekBarChangeListenerForPopup;
    private ValuePopupWindow mPopup;

    public PopupSeekBar(Context context, AttributeSet attrs) {

        super(context, attrs);

        mOnSeekBarChangeListenerForPopup = new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
                }
                if (fromUser && mPopup != null) {
                    mPopup.setValue(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStartTrackingTouch(seekBar);
                }
                if (mPopup != null) {
                    mPopup.setValue(seekBar.getProgress());
                    mPopup.showAtLocation(PopupSeekBar.this, Gravity.CENTER, 0, 0);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStopTrackingTouch(seekBar);
                }
                if (mPopup != null) {
                    mPopup.dismiss();
                }
            }
        };
        setOnSeekBarChangeListener(null);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListenerForPopup);
        mOnSeekBarChangeListener = l;
    }

    public void setValuePopupWindow(ValuePopupWindow popup) {
        mPopup = popup;
    }
}