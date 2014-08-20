package it.moondroid.paintbrush;

import android.app.Application;

import it.moondroid.paintbrush.drawing.Brushes;

/**
 * Created by Marco on 09/08/2014.
 */
public class PaintBrushApp extends Application {

    public static final String TAG = "PaintBrush";

    public static final String EXTRA_BRUSH_ID = "brushId";
    public static final String EXTRA_BRUSH_TYPE = "brushType";

    public void onCreate() {
        super.onCreate();
        Brushes.loadBrushList(getApplicationContext());
    }
}
