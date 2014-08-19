package it.moondroid.paintbrush;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import it.moondroid.paintbrush.dialogs.HSVColorPickerDialog;
import it.moondroid.paintbrush.drawing.Brush;
import it.moondroid.paintbrush.drawing.Brushes;
import it.moondroid.paintbrush.drawing.PaintView;
import it.moondroid.paintbrush.widget.OpacityPopupWindow;
import it.moondroid.paintbrush.widget.PopupSeekBar;
import it.moondroid.paintbrush.widget.SizePopupWindow;


public class MainActivity extends Activity {

    private PaintView mPaintView;
    private PopupSeekBar mSizeSeekBar;
    private PopupSeekBar mOpacitySeekBar;

    private ImageView mFirstBrushButton;

    private int mDrawingColor = 0xFF4488CC; //default color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
        mFirstBrushButton = (ImageView)findViewById(R.id.firstBrushButton);

        Brush brush = Brushes.get(getApplicationContext())[0];
        mFirstBrushButton.setImageResource(brush.iconId);

        mPaintView.setBrush(brush);
        setColor(mDrawingColor);
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
                            mDrawingColor = color;
                        }
                    }
                });
                cpd.setTitle(getResources().getString(R.string.dialog_color_title));
                cpd.show();
            }
        });
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);

        int i = 0;
        for (Brush brush : Brushes.get(getApplicationContext())){
            menu.add(Menu.NONE, i, Menu.NONE, brush.name);
            i++;
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
        Brush[] brushes = Brushes.get(getApplicationContext());
        if(id>=0 && id<brushes.length){
            if(mPaintView!=null){
                mPaintView.setBrush(brushes[id]);
                mFirstBrushButton.setImageResource(brushes[id].iconId);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
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
