package org.tpmkranz.smsforward;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by tpm on 12/1/15.
 */
public class JavaxMailPropertyDialog extends DialogFragment {

    public static String DIALOG_TAG = "property_dialog";
    public static String ARG_MODE = "mode", ARG_KEY = "key", ARG_VALUE = "value", ARG_IX = "index";
    public static int MODE_EDIT = 0, MODE_NEW = 1;

    private int dialogMode = MODE_NEW, propertyIndex = -1;
    private String propertyKey = "", propertyValue = "";
    private EditText keyInput, valueInput;
    private LinearLayout dialogView;
    private JavaxMailPropertyDialogInterface dialogInterface;

    @Override
    public Dialog onCreateDialog(Bundle savedInstance){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        int title_id = dialogMode == MODE_EDIT ? R.string.title_dialog_edit_javax_mail_property : R.string.title_dialog_add_javax_mail_property;
        dialogView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.dialog_javax_mail_properties, null);
        OnFinishPropertyListener finishPropertyListener = new OnFinishPropertyListener();
        builder.setTitle(title_id)
                .setView(dialogView)
                .setPositiveButton(R.string.positive_dialog_add_javax_mail_property, finishPropertyListener)
                .setNegativeButton(R.string.neutral_dialog_add_javax_mail_property, null);
        if (dialogMode == MODE_EDIT)
            builder.setNeutralButton(R.string.negative_dialog_add_javax_mail_property, finishPropertyListener);
        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        keyInput = (EditText) dialogView.findViewById(R.id.dialog_key_input);
        keyInput.setText(propertyKey);
        valueInput = (EditText) dialogView.findViewById(R.id.dialog_value_input);
        valueInput.setText(propertyValue);
    }

    @Override
    public void setArguments(Bundle args){
        dialogMode = args.getInt(ARG_MODE, MODE_NEW);
        propertyKey = args.getString(ARG_KEY, "");
        propertyValue = args.getString(ARG_VALUE, "");
        propertyIndex = args.getInt(ARG_IX, -1);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        try {
            dialogInterface = (JavaxMailPropertyDialogInterface) activity;
        } catch (ClassCastException e){
            dialogInterface = null;
        }
    }

    private class OnFinishPropertyListener implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (dialogInterface != null){
                if (which == DialogInterface.BUTTON_POSITIVE){
                    Bundle arguments = new Bundle();
                    arguments.putString(ARG_KEY, keyInput.getText().toString());
                    arguments.putString(ARG_VALUE, valueInput.getText().toString());
                    arguments.putInt(ARG_IX, propertyIndex);
                    dialogInterface.doneEditing(arguments);
                }else if (which == DialogInterface.BUTTON_NEUTRAL){
                    dialogInterface.deleteProperty(propertyIndex);
                }
            }
        }
    }

    public interface JavaxMailPropertyDialogInterface {
        void doneEditing(Bundle args);
        void deleteProperty(int which);
    }
}
