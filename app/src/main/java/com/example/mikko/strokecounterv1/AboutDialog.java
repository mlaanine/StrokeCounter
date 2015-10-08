package com.example.mikko.strokecounterv1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;

/**
 * Created by Mikko on 08-Oct-15.
 */
public class AboutDialog extends DialogFragment{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.about_dialog, null));
        builder.setCancelable(false);
        builder.setPositiveButton("OK", null);
        return builder.create();
    }

}
