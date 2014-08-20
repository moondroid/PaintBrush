package it.moondroid.paintbrush.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.List;

import it.moondroid.paintbrush.PaintBrushApp;
import it.moondroid.paintbrush.R;
import it.moondroid.paintbrush.drawing.Brush;
import it.moondroid.paintbrush.drawing.Brushes;

/**
 * Created by marco.granatiero on 19/08/2014.
 */

public class BrushSelectFragment extends Fragment implements AdapterView.OnItemClickListener {

    public static BrushSelectFragment newInstance(int brushId, int brushType){
        BrushSelectFragment f = new BrushSelectFragment();
        Bundle args = new Bundle();
        args.putInt(PaintBrushApp.EXTRA_BRUSH_ID, brushId);
        args.putInt(PaintBrushApp.EXTRA_BRUSH_TYPE, brushType);
        f.setArguments(args);
        return f;
    }

    private static class BrushAdapter extends ArrayAdapter<Brush> {
        private final LayoutInflater mInflater;

        public BrushAdapter(Context context, List<Brush> brushes) {
            super(context, 0, 0, brushes);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public long getItemId(int position) {
            return (long) ((Brush) getItem(position)).id;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.brush_gridview_item, parent, false);
            }
            TextView tv = (TextView) convertView;
            Brush brush = (Brush) getItem(position);
            tv.setText(brush.name);
            tv.setCompoundDrawablesWithIntrinsicBounds(0, brush.previewId, 0, 0);
            tv.setEnabled(isEnabled(position));
            return tv;
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_brush_select, null);
        GridView brushGridView = (GridView) v.findViewById(R.id.brushGridView);
        brushGridView.setOnItemClickListener(this);
        int brushType = getArguments().getInt(PaintBrushApp.EXTRA_BRUSH_TYPE, 0);
        int brushId = getArguments().getInt(PaintBrushApp.EXTRA_BRUSH_ID, -1);

        List<Brush> brushes = Brushes.get(getActivity(), brushType);
        brushGridView.setAdapter(new BrushAdapter(getActivity(), brushes));
        if (savedInstanceState == null) {
            int i = 0;
            while (i < brushes.size()) {
                if (((Brush) brushes.get(i)).id == brushId) {
                    brushGridView.setSelection(i);
                }
                i++;
            }
        }
        return v;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getActivity().setResult(-1, new Intent().putExtra(PaintBrushApp.EXTRA_BRUSH_ID, (int) id));
        getActivity().finish();
    }
}
