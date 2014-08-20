package it.moondroid.paintbrush;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import it.moondroid.paintbrush.fragments.BrushSelectFragment;

/**
 * Created by marco.granatiero on 19/08/2014.
 */
public class BrushSelectActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_brush_select);

        int brushId = getIntent().getIntExtra(PaintBrushApp.EXTRA_BRUSH_ID, 0);
        int brushType = getIntent().getIntExtra(PaintBrushApp.EXTRA_BRUSH_TYPE, 0);
        BrushSelectFragment fragment = BrushSelectFragment.newInstance(brushId, brushType);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.commit();

    }
}

