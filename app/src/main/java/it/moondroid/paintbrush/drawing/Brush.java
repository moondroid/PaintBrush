package it.moondroid.paintbrush.drawing;

import android.content.Context;
import android.content.res.TypedArray;

import it.moondroid.paintbrush.R;

/**
 * Created by marco.granatiero on 07/08/2014.
 */
public class Brush {

    private static int[] BRUSH_MASK_IMAGE_ARRAY_STYLEABLE = null;
    public static final int BRUSH_TYPE_PHOTO = 1;
    public static final int BRUSH_TYPE_STYLISH = 0;
    public static final int COLORING_TYPE_NORMAL = 0;
    public static final int COLORING_TYPE_REF_IMAGE_ALL_TIME = 1;
    public static final int COLORING_TYPE_REF_IMAGE_INIT_TIME = 2;
    private static final String TAG = "Brush";
    public float angle;
    public float angleJitter;
    public int autoStrokeCount;
    public float autoStrokeDistribution;
    public float autoStrokeJointPitch;
    public int autoStrokeLength;
    public float autoStrokeStraight;
    public float colorPatchAlpha;
    public int coloringType;
    public int defaultColor;
    public String icon;
    public int iconId;
    public final int id;
    public boolean isEraser;
    public float jitterBrightness;
    public float jitterHue;
    public float jitterSaturation;
    public float lineEndAlphaScale;
    public int lineEndFadeLength;
    public float lineEndSizeScale;
    public float lineEndSpeedLength;
    public int lineTaperFadeLength;
    public int lineTaperStartLength;
    public String[] maskImageArray;
    public int[] maskImageIdArray;
    public float maxSize;
    public float minSize;
    public String name;
    public String preview;
    public int previewId;
    public float size;
    public float smudgingPatchAlpha;
    public float spacing;
    public float spread;
    public float textureDepth;
    public boolean traceMode;
    public boolean useDeviceAngle;
    public boolean useFirstJitter;
    public boolean useFlowingAngle;
    public boolean useSmudging;

    static {
        BRUSH_MASK_IMAGE_ARRAY_STYLEABLE = new int[]{R.styleable.Brush_maskImageArray0,
                R.styleable.Brush_maskImageArray1, R.styleable.Brush_maskImageArray2,
                R.styleable.Brush_maskImageArray3, R.styleable.Brush_maskImageArray4,
                R.styleable.Brush_maskImageArray5, R.styleable.Brush_maskImageArray6,
                R.styleable.Brush_maskImageArray7, R.styleable.Brush_maskImageArray8,
                R.styleable.Brush_maskImageArray9};
    }

    public Brush(int id) {
        this.id = id;
    }

    private void loadFromTypedArray(TypedArray a) {
        this.angle = a.getFloat(R.styleable.Brush_angle, 0.0f);
        this.angleJitter = a.getFloat(R.styleable.Brush_angleJitter, 0.0f);
        this.autoStrokeCount = a.getInt(R.styleable.Brush_autoStrokeCount, 0);
        this.autoStrokeDistribution = a.getFloat(R.styleable.Brush_autoStrokeDistribution, 0.0f);
        this.autoStrokeJointPitch = a.getFloat(R.styleable.Brush_autoStrokeJointPitch, 0.0f);
        this.autoStrokeLength = a.getInt(R.styleable.Brush_autoStrokeLength, 1);
        this.autoStrokeStraight = a.getFloat(R.styleable.Brush_autoStrokeStraight, 0.0f);
        this.colorPatchAlpha = a.getFloat(R.styleable.Brush_colorPatchAlpha, 0.0f);
        this.coloringType = a.getInt(R.styleable.Brush_coloringType, COLORING_TYPE_NORMAL);
        this.iconId = a.getResourceId(R.styleable.Brush_icon, 0);
        this.isEraser = a.getBoolean(R.styleable.Brush_isEraser, false);
        this.jitterBrightness = a.getFloat(R.styleable.Brush_jitterBrightness, 0.0f);
        this.jitterHue = a.getFloat(R.styleable.Brush_jitterHue, 0.0f);
        this.jitterSaturation = a.getFloat(R.styleable.Brush_jitterSaturation, 0.0f);
        this.lineEndAlphaScale = a.getFloat(R.styleable.Brush_lineEndAlphaScale, 0.0f);
        this.lineEndFadeLength = a.getInt(R.styleable.Brush_lineEndFadeLength, 0);
        this.lineEndSizeScale = a.getFloat(R.styleable.Brush_lineEndSizeScale, 0.0f);
        this.lineEndSpeedLength = a.getFloat(R.styleable.Brush_lineEndSpeedLength, 0.0f);
        this.lineTaperFadeLength = a.getInt(R.styleable.Brush_lineTaperFadeLength, 0);
        this.lineTaperStartLength = a.getInt(R.styleable.Brush_lineTaperStartLength, 0);
        int i = COLORING_TYPE_NORMAL;
        while (i < BRUSH_MASK_IMAGE_ARRAY_STYLEABLE.length && a.getResourceId(BRUSH_MASK_IMAGE_ARRAY_STYLEABLE[i], 0) != 0) {
            i++;
        }
        this.maskImageIdArray = new int[i];
        i = 0;
        while (i < this.maskImageIdArray.length) {
            this.maskImageIdArray[i] = a.getResourceId(BRUSH_MASK_IMAGE_ARRAY_STYLEABLE[i], 0);
            i++;
        }
        this.maxSize = a.getDimension(R.styleable.Brush_maxSize, 0.0f);
        this.minSize = a.getDimension(R.styleable.Brush_minSize, 0.0f);
        this.name = a.getString(R.styleable.Brush_name);
        this.previewId = a.getResourceId(R.styleable.Brush_preview, 0);
        this.size = a.getDimension(R.styleable.Brush_size, 0.0f);
        this.smudgingPatchAlpha = a.getFloat(R.styleable.Brush_smudgingPatchAlpha, 0.0f);
        this.spacing = a.getFloat(R.styleable.Brush_spacing, 0.0f);
        this.spread = a.getFloat(R.styleable.Brush_spread, 0.0f);
        this.textureDepth = a.getFloat(R.styleable.Brush_textureDepth, 0.0f);
        this.traceMode = a.getBoolean(R.styleable.Brush_traceMode, false);
        this.useDeviceAngle = a.getBoolean(R.styleable.Brush_useDeviceAngle, false);
        this.useFirstJitter = a.getBoolean(R.styleable.Brush_useFirstJitter, false);
        this.useFlowingAngle = a.getBoolean(R.styleable.Brush_useFlowingAngle, false);
        this.useSmudging = a.getBoolean(R.styleable.Brush_useSmudging, false);
        this.defaultColor = a.getColor(R.styleable.Brush_defaultColor, 0);
    }

    public static Brush[] parseStyleData(Context context, int[] styleArray) {
        Brush[] brushes = new Brush[styleArray.length];
        int i = 0;
        while (i < brushes.length) {
            Brush brush = new Brush(i);
            TypedArray a = context.obtainStyledAttributes(styleArray[i], R.styleable.Brush);
            brush.loadFromTypedArray(a);
            a.recycle();
            brushes[i] = brush;
            i++;
        }
        return brushes;
    }

    public int getBrushType() {
        return this.coloringType == 0 ? COLORING_TYPE_NORMAL : COLORING_TYPE_REF_IMAGE_ALL_TIME;
    }

    public float getScaledSize() {
        return (this.size - this.minSize) / (this.maxSize - this.minSize);
    }

    public float getSizeFromScaledSize(float scaledSize) {
        return this.minSize + (this.maxSize - this.minSize) * scaledSize;
    }

    public boolean setScaledSize(float scaledSize) {
        if (scaledSize < 0.0f) {
            scaledSize = 0.0f;
        } else if (scaledSize > 1.0f) {
            scaledSize = 1.0f;
        }
        float newSize = getSizeFromScaledSize(scaledSize);
        if (this.size == newSize) {
            return false;
        }
        this.size = newSize;
        return true;
    }

    public String toString() {
        return "Brush " + this.angle + ", " + this.angleJitter + ", " + this.autoStrokeCount + ", " + this.name + ", " + this.isEraser + ", " + this.maskImageIdArray.length + ", " + (this.maskImageIdArray.length == 1 ? Integer.valueOf(this.maskImageIdArray[0]) : this.maskImageIdArray[0] + "/" + this.maskImageIdArray[1]);
    }
}
