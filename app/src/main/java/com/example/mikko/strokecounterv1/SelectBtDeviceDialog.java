package com.example.mikko.strokecounterv1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import java.util.ArrayList;

/**
 * Created by Mikko on 29-Sep-15.
 */
public class SelectBtDeviceDialog extends DialogFragment {

    // Define newInstance method and pass the ArrayList to the fragment
    // AlertDialog.builder can't take ArrayList, so convert it to CharSequence[]
    private CharSequence[] mPairedDeviceNames;
    public static SelectBtDeviceDialog newInstance(ArrayList<String> pairedDeviceNames){
        SelectBtDeviceDialog dialog = new SelectBtDeviceDialog();
        dialog.mPairedDeviceNames = pairedDeviceNames.toArray(new CharSequence[pairedDeviceNames.size()]);
        return dialog;
    }

    // Define interface to pass selection back to MainActivity
    public interface SelectBtDeviceDialogListener{
        void onSelectBtDevice(int which);
    }
    SelectBtDeviceDialogListener mSelectBtDeviceDialogListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSelectBtDeviceDialogListener = (SelectBtDeviceDialogListener) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // setRetainInstance(true);  Not sure if this works right.
        // Without it, on screen rotation, mPairedDeviceNames becomes empty. With it, the whole dialog disappears.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_bt_device);
        builder.setItems(mPairedDeviceNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        mSelectBtDeviceDialogListener.onSelectBtDevice(which);
                    }
                }
        );
        return builder.create();
    }

}
