package it.moondroid.paintbrush.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;

/**
 * Created by marco.granatiero on 03/09/2014.
 */
public class SimpleBitmapHistoryManager implements BitmapHistoryManager {

    private static final int BITMAP_CHUNK_SIZE = 40;
    private static final Bitmap EMPTY_BITMAP;
    private static final int HISTORY_MAX_SIZE = 20;
    private static final String TAG = "SimpleBitmapHistoryManager";
    private static final String IMAGE_CACHE_DIR = "paintbrush_hystory";
    private static final String IMAGE_CACHE_FILE = "snapshot";

    private Bitmap mChunkBitmap;
    private Canvas mChunkCanvas;

    private static Paint mSrcPaint;
    private static Bitmap mTotalBitmap;
    private static Canvas mTotalCanvas;

    private final HistoryChangedListener mHistoryChangedListener;
    private boolean mIsHistoryEmpty;

    private int mHistoryListCurrent;
    private int mHistoryListFirst;
    private int mHistoryListLast;
    private int mHistoryListMoveBy;

    private final CommandManagerHandler mCommandManager;
    private static HistoryHandlerThread sHistoryHandlerThread;
    private Handler mMainHandler;

    private static ImageCache mImageCache;

    static {
        EMPTY_BITMAP = Build.VERSION.SDK_INT < 14 ? Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) : null;
    }

    public SimpleBitmapHistoryManager(Context context, HistoryChangedListener listener) {

        this.mChunkBitmap = Bitmap.createBitmap(BITMAP_CHUNK_SIZE, BITMAP_CHUNK_SIZE, Bitmap.Config.ARGB_8888);
        this.mChunkCanvas = new Canvas(this.mChunkBitmap);
        this.mSrcPaint = new Paint();
        this.mSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.mIsHistoryEmpty = true;
        this.mHistoryChangedListener = listener;

        synchronized (SimpleBitmapHistoryManager.class) {
            if (sHistoryHandlerThread == null) {
                sHistoryHandlerThread = new HistoryHandlerThread(context);
                sHistoryHandlerThread.start();
            }
        }

        mMainHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                HandlerData handlerData = (HandlerData) msg.obj;
                switch (msg.what) {
                    case COMMAND_RESET:
                        break;

                    case COMMAND_WRITE:
                        break;

                    case COMMAND_READ:
                        //SimpleBitmapHistoryManager.this.drawHandlerDataToBitmap(handlerData);
                        SimpleBitmapHistoryManager.this.mHistoryListFirst = handlerData.first;
                        SimpleBitmapHistoryManager.this.mHistoryListCurrent = handlerData.current;
                        SimpleBitmapHistoryManager.this.mHistoryListLast = handlerData.last;
                        SimpleBitmapHistoryManager.this.mHistoryListMoveBy = 0;
                        SimpleBitmapHistoryManager.this.notifyHistoryChanged(handlerData.current - handlerData.first, handlerData.last - handlerData.current, msg.what, 0);
                        break;

                    default:
                        break;
                }
            }
        };

        mCommandManager = new CommandManagerHandler(mMainHandler, sHistoryHandlerThread);


    }

    @Override
    public void addDirtyArea(float left, float top, float right, float bottom) {
        //TODO dirty area
    }

    @Override
    public void clearDirtyArea() {
        //TODO dirty area
    }

    @Override
    public void saveDirtyAreaToHistory() {
        HandlerData handlerData = new HandlerData();

        this.mHistoryListCurrent++;
        this.mHistoryListLast = this.mHistoryListCurrent;
        int moveBy = this.mHistoryListLast - this.mHistoryListFirst - HISTORY_MAX_SIZE;
        if (moveBy > 0) {
            this.mHistoryListFirst += moveBy;
        }
        handlerData.first = this.mHistoryListFirst;
        handlerData.current = this.mHistoryListCurrent;
        handlerData.last = this.mHistoryListLast;
        handlerData.moveBy = moveBy;
        this.mHistoryListMoveBy = 0;
        notifyHistoryChanged(this.mHistoryListCurrent - this.mHistoryListFirst, 0, COMMAND_WRITE, 0);
        this.mIsHistoryEmpty = false;

        mCommandManager.sendToWorkerHandler(COMMAND_WRITE, handlerData, 0);
    }

    @Override
    public void moveHistoryBy(int diff) {
        int newMoveBy = mHistoryListMoveBy + diff;
        int newCurrent = mHistoryListCurrent + newMoveBy;
        if (newCurrent >= mHistoryListFirst && newCurrent <= mHistoryListLast) {
            HandlerData handlerData = new HandlerData();
            handlerData.first = mHistoryListFirst;
            handlerData.current = mHistoryListCurrent;
            handlerData.last = mHistoryListLast;
            handlerData.moveBy = newMoveBy;
            mHistoryListMoveBy = newMoveBy;

            mCommandManager.sendToWorkerHandler(COMMAND_READ, handlerData, diff);
        }
    }

    @Override
    public void init(Context context, Bitmap bitmap, Canvas canvas) {
        this.mTotalBitmap = bitmap;
        this.mTotalCanvas = canvas;

        mCommandManager.sendToWorkerHandler(COMMAND_INFO, null, 0);

    }

    @Override
    public boolean isEmpty() {
        return mIsHistoryEmpty;
    }


    @Override
    public void release() {
        mCommandManager.sendToWorkerHandler(COMMAND_QUIT);

        this.mChunkCanvas.setBitmap(EMPTY_BITMAP);
        this.mChunkBitmap.recycle();
        this.mChunkBitmap = null;
        this.mChunkCanvas = null;
    }

    @Override
    public void reset(int mode) {
        this.mIsHistoryEmpty = true;
        this.mHistoryListCurrent = 0;
        this.mHistoryListFirst = 0;
        this.mHistoryListLast = 0;
        this.mHistoryListMoveBy = 0;

        HandlerData handlerData = new HandlerData();
        handlerData.first = 0;
        handlerData.current = 0;
        handlerData.last = 0;

        notifyHistoryChanged(0, 0, 0, mode);

        mCommandManager.sendToWorkerHandler(COMMAND_RESET, handlerData, mode);
    }

    private void notifyHistoryChanged(int prevLength, int nextLength, int commandType, int mode) {
        if (this.mHistoryChangedListener != null) {
            this.mHistoryChangedListener.onHistoryChanged(prevLength, nextLength, commandType, mode);
        }
    }


    private static class CommandManagerHandler extends Handler {
        private static int mLastCommandId = 0;
        private static final int[] mLastCommandIds = new int[COMMAND_TOTAL_COUNT];

        private final HistoryHandlerThread mHistoryHandlerThread;
        private final Handler mMainHandler;

        private CommandManagerHandler(Handler mainHandler, HistoryHandlerThread thread) {
            super(thread.getLooper());
            this.mMainHandler = mainHandler;
            this.mHistoryHandlerThread = thread;
        }

        private static int getNewCommandId(int commandType) {
            mLastCommandId++;
            mLastCommandIds[commandType] = mLastCommandId;
            return mLastCommandId;
        }

        private void sendToWorkerHandler(int commandType) {
            sendToWorkerHandler(commandType, null, 0);
        }

        private void sendToWorkerHandler(int commandType, Object obj, int arg2) {
            int commandId = getNewCommandId(commandType);
            Message.obtain(this, commandType, commandId, arg2, obj).sendToTarget();
        }

        private void replyToMainHandler(Message msg) {
            Message.obtain(mMainHandler, msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mHistoryHandlerThread.handleMessage(msg, this);

        }
    }

    private static class HistoryHandlerThread extends HandlerThread {

        private final Context mContext;
        private final LinkedList<long[]> mHistoryList;

        private HistoryHandlerThread(Context context) {
            super("HistoryManagerThread");
            this.mHistoryList = new LinkedList();
            this.mContext = context;
        }

        private void handleMessage(Message msg, CommandManagerHandler commandManager) {
            switch (msg.what) {

                case COMMAND_RESET:
                    Log.d(TAG, "COMMAND_RESET");
                    if(resetHistoryList(msg)){
                        commandManager.replyToMainHandler(msg);
                    }
                    break;

                case COMMAND_WRITE:
                    Log.d(TAG, "COMMAND_WRITE");
                    if (writeHistory(msg)) {
                        commandManager.replyToMainHandler(msg);
                    }
                    break;

                case COMMAND_READ:
                    Log.d(TAG, "COMMAND_READ");
                    if (readHistory(msg)) {
                        commandManager.replyToMainHandler(msg);
                    }
                    break;

                case COMMAND_QUIT:
                    Log.d(TAG, "COMMAND_QUIT");
                    if(deleteHistory(msg)){
                        commandManager.replyToMainHandler(msg);
                    }
                    break;

                case COMMAND_INFO:
                    Log.d(TAG, "COMMAND_INFO");
                    if(initHistory(msg)){
                        commandManager.replyToMainHandler(msg);
                    }
                    break;

                default:
                    break;
            }

        }

        private boolean initHistory(Message msg){
            //init cache
            ImageCache.ImageCacheParams cacheParams =
                    new ImageCache.ImageCacheParams(mContext, IMAGE_CACHE_DIR);
            //cacheParams.setMemCacheSizePercent(0.5f); // Set memory cache to 50% of app memory
            cacheParams.memoryCacheEnabled = false;
            cacheParams.diskCacheEnabled = true;
            mImageCache = new ImageCache(cacheParams);

            return true;
        }

        private boolean resetHistoryList(Message msg) {
            mHistoryList.clear();
            mImageCache.clearCache();
            return writeHistory(msg);
        }

        private boolean deleteHistory(Message msg){
            //delete cache
            mImageCache.clearCache();
            mImageCache.close();
            return true;
        }

        private boolean writeHistory(Message msg) {
            HandlerData handlerData = (HandlerData) msg.obj;
            if (handlerData == null) {
                this.mHistoryList.addLast(null);
                return true;
            }

            //write bitmap to cache
            mImageCache.addBitmapToCache(IMAGE_CACHE_FILE+handlerData.current, mTotalBitmap);
            Log.d(TAG, "writeHistory handlerData.current:"+handlerData.current);
            long[] chunkIdArray = new long[1];
            chunkIdArray[0] = getLongFromCurrentPosition(handlerData.current);
            this.mHistoryList.addLast(chunkIdArray);
            return true;
        }

        private boolean readHistory(Message msg) {
            HandlerData handlerData = (HandlerData) msg.obj;
            int newCurrent = handlerData.current + handlerData.moveBy;
//            long[] src = (long[]) this.mHistoryList.get(handlerData.current - handlerData.first);
//            long[] dst = (long[]) this.mHistoryList.get(newCurrent - handlerData.first);

            //read bitmap from cache
            Bitmap readBitmap = mImageCache.getBitmapFromDiskCache(IMAGE_CACHE_FILE+newCurrent);
            if (readBitmap!=null){
                mTotalCanvas.drawBitmap(readBitmap, 0, 0, mSrcPaint);
                handlerData.current = newCurrent;
                Log.d(TAG, "readHistory handlerData.current:"+handlerData.current);
                return true;
            }

            Log.d(TAG, "readHistory handlerData.current:"+handlerData.current);
            return false;
        }

        private static long getLongFromCurrentPosition(int i) {
            return (long)i;
        }

    }


    private static class HandlerData {
        int current;
        int first;
        int last;
        int moveBy;

        private HandlerData() {
        }

        public String toString() {
            return "f=" + this.first + ", c=" + this.current + ", l=" + this.last + ", m=" + this.moveBy;
        }
    }

}
