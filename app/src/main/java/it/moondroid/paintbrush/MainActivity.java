package it.moondroid.paintbrush;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.IconButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import it.moondroid.paintbrush.dialogs.HSVColorPickerDialog;
import it.moondroid.paintbrush.drawing.Brush;
import it.moondroid.paintbrush.drawing.Brushes;
import it.moondroid.paintbrush.drawing.PaintView;
import it.moondroid.paintbrush.util.ImageUtils;
import it.moondroid.paintbrush.widget.OpacityPopupWindow;
import it.moondroid.paintbrush.widget.PopupSeekBar;
import it.moondroid.paintbrush.widget.SizePopupWindow;


public class MainActivity extends Activity implements View.OnClickListener {

    private PaintView mPaintView;
    private PopupSeekBar mSizeSeekBar;
    private PopupSeekBar mOpacitySeekBar;

    private ImageView mFirstBrushButton;

    private int mDrawingColor = 0xFF4488CC; //default color
    private int mCurrentBrushId = 0; //default brush

    private static final int REQUEST_BRUSH_SELECT = 3;

    IconButton mUndoButton, mRedoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
        mFirstBrushButton = (ImageView)findViewById(R.id.firstBrushButton);

        Brush brush = Brushes.get(getApplicationContext())[mCurrentBrushId];
        mFirstBrushButton.setImageResource(brush.iconId);

        mPaintView.setDrawingCacheEnabled(true);
        mPaintView.setBrush(brush);
        setColor(brush.defaultColor);
        mPaintView.setDrawingBgColor(Color.WHITE);

        this.mSizeSeekBar = (PopupSeekBar) findViewById(R.id.sizePopupSeekbar);
        this.mSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
               setScaledSize(seekBar.getProgress()/100.f);
            }
        });
        this.mOpacitySeekBar = (PopupSeekBar) findViewById(R.id.opacityPopupSeekbar);
        this.mOpacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setOpacity(progress/100.f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSizeSeekBar.setValuePopupWindow(new MySizePopupWindow(this));
        mOpacitySeekBar.setValuePopupWindow(new OpacityPopupWindow(this));

        setScaledSize(mPaintView.getDrawingScaledSize());
        setOpacity(0.5f);

        findViewById(R.id.currentColorButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HSVColorPickerDialog cpd = new HSVColorPickerDialog( MainActivity.this, mDrawingColor, new HSVColorPickerDialog.OnColorSelectedListener() {
                    @Override
                    public void colorSelected(Integer color) {
                        if(color!=null){
                            setColor(color);
                        }
                    }
                });
                cpd.setTitle(getResources().getString(R.string.dialog_color_title));
                cpd.show();
            }
        });

        findViewById(R.id.firstBrushButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent brushSelect = new Intent(MainActivity.this, BrushSelectActivity.class);
                brushSelect.putExtra(PaintBrushApp.EXTRA_BRUSH_ID, mCurrentBrushId);
                brushSelect.putExtra(PaintBrushApp.EXTRA_BRUSH_TYPE, 0);
                startActivityForResult(brushSelect, REQUEST_BRUSH_SELECT);
            }
        });

        mUndoButton = (IconButton) findViewById(R.id.undoButton);
        mUndoButton.setOnClickListener(this);
        mRedoButton = (IconButton) findViewById(R.id.redoButton);
        mRedoButton.setOnClickListener(this);

        mPaintView.setOnStateChangedListener(new PaintView.OnStateChangedListener() {

            @Override
            public void onResetCompleted() {
            }

            @Override
            public void onUndoRedoChanged(int undoSize, int redoSize) {
                Log.d("MainActivity", "onUndoRedoChanged undoSize:"+undoSize+" redoSize"+redoSize);
                MainActivity.this.updateUndoRedo(undoSize, redoSize);
            }
        });
        updateUndoRedo(0, 0);
    }

    private void updateUndoRedo(int undoSize, int redoSize) {
        if (undoSize != -1) {
            mUndoButton.setEnabled(undoSize > 0);
        }
        if (redoSize != -1) {
            mRedoButton.setEnabled(redoSize > 0);
        }
    }

    private void setScaledSize(float scaledSize) {
        mSizeSeekBar.setProgress((int) (scaledSize*100.0f));
        mPaintView.setDrawingScaledSize(scaledSize);
    }

    private void setOpacity(float opacity) {
        this.mOpacitySeekBar.setProgress((int) (opacity*100.0f));
        //this.mColorButton.setColor(getColorWithAlpha(this.mPaintView.getDrawingColor(), opacity));
        this.mPaintView.setDrawingAlpha(opacity);
    }

    private void setColor(int color) {
        //this.mColorButton.setColor(getColorWithAlpha(color, this.mPaintView.getDrawingAlpha()));
        this.mPaintView.setDrawingColor(color);
        mDrawingColor = color;
    }

    private void setBrush(int brushId){
        Brush[] brushes = Brushes.get(getApplicationContext());
        if(brushId>=0 && brushId<brushes.length){
            if(mPaintView!=null){
                mPaintView.setBrush(brushes[brushId]);
                mFirstBrushButton.setImageResource(brushes[brushId].iconId);
                setColor(brushes[brushId].defaultColor);
                mCurrentBrushId = brushId;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_delete).setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_trash_o)
                        .color(Color.WHITE)
                        .actionBarSize());

        menu.findItem(R.id.action_save).setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_save)
                        .color(Color.WHITE)
                        .actionBarSize());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()){
            case R.id.action_delete:
                if (!this.mPaintView.isClear()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.delete_confirm_message)
                            .setPositiveButton(R.string.delete_confirm_positive, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mPaintView.clear();
                                }
                            })
                            .setNegativeButton(R.string.delete_confirm_negative, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                            });
                    builder.create().show();
                }
                return true;

            case R.id.action_save:
                ImageUtils.saveImageFromBitmap(this, mPaintView.getDrawingCache(), new ImageUtils.SaveImageListener() {

                    @Override
                    public void onCompleted(Uri uri) {
                        Toast.makeText(MainActivity.this, getString(R.string.toast_image_saved), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(int i) {
                        Toast.makeText(MainActivity.this, getString(R.string.toast_image_save_error), Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != 0) {
            switch (requestCode) {
                case REQUEST_BRUSH_SELECT:
                    int brushId = data.getIntExtra(PaintBrushApp.EXTRA_BRUSH_ID, 0);
                    setBrush(brushId);
                    break;

                default:
                    break;
            }

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.undoButton:
                mPaintView.undo();
                break;
            case R.id.redoButton:
                mPaintView.redo();
                break;
        }
    }

    private class MySizePopupWindow extends SizePopupWindow {

        MySizePopupWindow(Context context) {
            super(context);
        }

        public float convertValue(float progress) {
            return mPaintView.getBrush().getSizeFromScaledSize(progress/100.0f);
        }
    }
}
