package it.moondroid.paintbrush.drawing;

/**
 * Created by marco.granatiero on 03/09/2014.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

public interface BitmapHistoryManager {

    public static final int COMMAND_RESET = 0;
    public static final int COMMAND_WRITE = 1;
    public static final int COMMAND_READ = 2;
    public static final int COMMAND_QUIT = 3;
    public static final int COMMAND_INFO = 4;
    public static final int COMMAND_TOTAL_COUNT = 5;
    public static final int RESET_CLEAR = 0;
    public static final int RESET_REBASE = 1;
    public static final int RESET_RESTORE = 2;

    public static interface HistoryChangedListener {
        void onHistoryChanged(int i, int i2, int i3, int i4);
    }

    void addDirtyArea(float left, float top, float right, float bottom);

    void clearDirtyArea();

    void init(Context context, Bitmap bitmap, Canvas canvas);

    boolean isEmpty();

    void moveHistoryBy(int diff);

    void release();

    void reset(int mode);

    void saveDirtyAreaToHistory();
}
