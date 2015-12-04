package org.tpmkranz.smsforward;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by tpm on 12/1/15.
 */
public class JavaMailPropertyDialog extends DialogFragment {

    public static String DIALOG_TAG = "property_dialog";
    public static String ARG_MODE = "mode", ARG_KEY = "key", ARG_VALUE = "value", ARG_IX = "index";
    public static int MODE_EDIT = 0, MODE_NEW = 1;

    private int dialogMode = MODE_NEW, propertyIndex = -1;
    private String propertyKey = "", propertyValue = "";
    private EditText keyInput, valueInput;
    private LinearLayout dialogView;
    private JavaMailPropertyDialogInterface dialogInterface;
    private AlertDialog thisDialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstance){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        int title_id = dialogMode == MODE_EDIT ? R.string.title_dialog_edit_javamail_property : R.string.title_dialog_add_javamail_property;
        dialogView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.dialog_javamail_properties, null);

        builder.setTitle(title_id)
                .setView(dialogView)
                .setPositiveButton(R.string.positive_dialog_add_javamail_property, null)
                .setNegativeButton(R.string.neutral_dialog_add_javamail_property, null);
        if (dialogMode == MODE_EDIT)
            builder.setNeutralButton(R.string.negative_dialog_add_javamail_property, null);
        thisDialog = builder.create();
        return thisDialog;
    }

    @Override
    public void onStart(){
        super.onStart();
        thisDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new OnFinishPropertyListener(thisDialog, DialogInterface.BUTTON_POSITIVE));
        if (dialogMode == MODE_EDIT)
            thisDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new OnFinishPropertyListener(thisDialog, DialogInterface.BUTTON_NEUTRAL));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        keyInput = (EditText) dialogView.findViewById(R.id.dialog_key_input);
        keyInput.setText(propertyKey);
        keyInput.addTextChangedListener(new SetupActivity.ClearErrorOnInputListener(keyInput));
        valueInput = (EditText) dialogView.findViewById(R.id.dialog_value_input);
        valueInput.setText(propertyValue);
        valueInput.addTextChangedListener(new SetupActivity.ClearErrorOnInputListener(valueInput));
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
            dialogInterface = (JavaMailPropertyDialogInterface) activity;
        } catch (ClassCastException e){
            dialogInterface = null;
        }
    }

    private class OnFinishPropertyListener implements View.OnClickListener{
        private int button;
        private AlertDialog dialog;

        public OnFinishPropertyListener(AlertDialog dialog, int button){
            this.button = button;
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            if (dialogInterface != null){
                if (button == DialogInterface.BUTTON_POSITIVE) {
                    if (!keyInput.getText().toString().contains(".") || keyInput.getText().toString().endsWith(".")){
                        ((TextInputLayout)keyInput.getParent()).setError(
                                v.getContext().getResources().getString(R.string.invalid_javamail_key));
                    }else if (valueInput.getText().toString().isEmpty()){
                        ((TextInputLayout)valueInput.getParent()).setError(
                                v.getContext().getResources().getString(R.string.invalid_javamail_value));
                    }else {
                        Bundle arguments = new Bundle();
                        arguments.putString(ARG_KEY, keyInput.getText().toString());
                        arguments.putString(ARG_VALUE, valueInput.getText().toString());
                        arguments.putInt(ARG_IX, propertyIndex);
                        dialogInterface.doneEditing(arguments);
                        dialog.dismiss();
                    }
                }else if (button == DialogInterface.BUTTON_NEUTRAL) {
                    dialogInterface.deleteProperty(propertyIndex);
                    dialog.dismiss();
                }
            }
        }
    }

    public interface JavaMailPropertyDialogInterface {
        void doneEditing(Bundle args);
        void deleteProperty(int which);
    }
}
