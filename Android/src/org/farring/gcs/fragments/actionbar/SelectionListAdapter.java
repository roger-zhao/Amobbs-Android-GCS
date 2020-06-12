package org.farring.gcs.fragments.actionbar;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * Created by Fredia Huya-Kouadio on 9/25/15.
 */
public abstract class SelectionListAdapter<T> extends ArrayAdapter<T> {

    protected SelectionListener listener;

    public SelectionListAdapter(Context context) {
        super(context, 0);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    public abstract int getSelection();

    public interface SelectionListener {
        void onSelection();
    }
}
