package org.farring.gcs.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.amap.api.maps.model.LatLng;

import org.farring.gcs.R;

public class GuidedDialog extends DialogFragment {

    private GuidedDialogListener listener;
    private LatLng coord;

    public void setCoord(LatLng coord) {
        this.coord = coord;
    }

    public void setListener(GuidedDialogListener mListener) {
        this.listener = mListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(R.string.guided_mode_warning)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (coord != null) {
                            listener.onForcedGuidedPoint(coord);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public interface GuidedDialogListener {
        void onForcedGuidedPoint(LatLng coord);
    }
}