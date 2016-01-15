package org.tpmkranz.smsforward;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.wnafee.vector.compat.AnimatedVectorDrawable;
import com.wnafee.vector.compat.ResourcesCompat;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SetupActivity extends AppCompatActivity {
    protected static String LOGCATTAG = "SetupActivity";
    protected static String SHAREDPREFSNAME = "org.tpmkranz.smsforward.SETTINGS";
    protected static String SHAREDPREFSEMAIL = "email";
    protected static String SHAREDPREFSPASSWORD = "pass";
    protected static String SHAREDPREFSSERVER = "server";
    protected static String SHAREDPREFSPORT = "port";
    protected static String SHAREDPREFSTARGET = "target";
    protected static String SHAREDPREFSPUBKEY = "pubkey";
    protected static String SHAREDPREFSJAVAMAIL = "javamail_default_property-";
    protected static String SHAREDPREFSJAVAMAILVALUE = "javamail_default_property-_value";
    protected static String SHAREDPREFSJAVAMAILKEY = "javamail_default_property-_key";
    protected static String SHAREDPREFSJAVAMAILCOUNT = "javamail_default_property-_count";
    private ComponentName receiver;
    private PackageManager pm;
    private boolean enabled;
    private FloatingActionButton fab;
    private EditText inputEmail, inputPassword, inputServer, inputPort, inputTarget, inputPubkey;
    private SharedPreferences settings;
    private HashMap<String,String> currentSettings;
    private PGPPubkeyEncryptionUtil encrypter;
    private float scaledDensity = 1f;


    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.setup_toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.setup_fab);
        inputEmail = (EditText) findViewById(R.id.input_email);
        inputPassword = (EditText) findViewById(R.id.input_password);
        inputServer = (EditText) findViewById(R.id.input_server);
        inputPort = (EditText) findViewById(R.id.input_port);
        inputTarget = (EditText) findViewById(R.id.input_target);
        inputPubkey = (EditText) findViewById(R.id.input_pubkey);
        settings = this.getSharedPreferences(SHAREDPREFSNAME, Context.MODE_PRIVATE);
        currentSettings = new HashMap<>();
        readSettings(true);
        inputEmail.addTextChangedListener(new ClearErrorOnInputListener(inputEmail));
        inputPassword.addTextChangedListener(new ClearErrorOnInputListener(inputPassword));
        inputServer.addTextChangedListener(new ClearErrorOnInputListener(inputServer));
        inputPort.addTextChangedListener(new ClearErrorOnInputListener(inputPort));
        inputTarget.addTextChangedListener(new ClearErrorOnInputListener(inputTarget));
        inputPubkey.addTextChangedListener(new ClearErrorOnInputListener(inputPubkey));
        receiver = new ComponentName(this, SMSListener.class);
        pm = getPackageManager();
    }


    @Override
    protected void onResume(){
        super.onResume();
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        inputEmail.setText(currentSettings.get(SHAREDPREFSEMAIL));
        inputServer.setText(currentSettings.get(SHAREDPREFSSERVER));
        inputPort.setText(currentSettings.get(SHAREDPREFSPORT));
        inputTarget.setText(currentSettings.get(SHAREDPREFSTARGET));
        inputFitting(inputPubkey, currentSettings.get(SHAREDPREFSPUBKEY));
        enabled = isListenerListening(receiver);
        switchUIStates(enabled);
    }

    private void readSettings(boolean fromPrefs){
        currentSettings.put(SHAREDPREFSEMAIL, fromPrefs ? settings.getString(SHAREDPREFSEMAIL, "") : inputEmail.getText().toString());
        currentSettings.put(SHAREDPREFSPASSWORD, fromPrefs || inputPassword.getText().toString().isEmpty() ? settings.getString(SHAREDPREFSPASSWORD, "") : inputPassword.getText().toString());
        currentSettings.put(SHAREDPREFSSERVER, fromPrefs ? settings.getString(SHAREDPREFSSERVER, "") : inputServer.getText().toString());
        currentSettings.put(SHAREDPREFSPORT, fromPrefs ? settings.getString(SHAREDPREFSPORT, "") : inputPort.getText().toString());
        currentSettings.put(SHAREDPREFSTARGET, fromPrefs ? settings.getString(SHAREDPREFSTARGET, "") : inputTarget.getText().toString());
        currentSettings.put(SHAREDPREFSPUBKEY, fromPrefs ? settings.getString(SHAREDPREFSPUBKEY, "") : inputPubkey.getText().toString());
    }

    private boolean haveSettingsChanged(){
        return !getInputText(inputEmail).equals(currentSettings.get(SHAREDPREFSEMAIL))
                || (!getInputText(inputPassword).equals(currentSettings.get(SHAREDPREFSPASSWORD))
                && !getInputText(inputPassword).equals(""))
                || !getInputText(inputServer).equals(currentSettings.get(SHAREDPREFSSERVER))
                || !getInputText(inputPort).equals(currentSettings.get(SHAREDPREFSPORT))
                || !getInputText(inputTarget).equals(currentSettings.get(SHAREDPREFSTARGET))
                || !getInputText(inputPubkey).equals(currentSettings.get(SHAREDPREFSPUBKEY));
    }

    private List<EditText> invalidSettings(){
        ArrayList<EditText> invalidSettings = new ArrayList<EditText>();
        ((TextInputLayout)inputEmail.getParent()).setError(null);
        ((TextInputLayout)inputPassword.getParent()).setError(null);
        ((TextInputLayout)inputServer.getParent()).setError(null);
        ((TextInputLayout)inputPort.getParent()).setError(null);
        ((TextInputLayout)inputTarget.getParent()).setError(null);
        ((TextInputLayout)inputPubkey.getParent()).setError(null);
        if (!getInputText(inputEmail).contains("@"))
            invalidSettings.add(inputEmail);
        if (getInputText(inputPassword).isEmpty() && currentSettings.get(SHAREDPREFSPASSWORD).isEmpty())
            invalidSettings.add(inputPassword);
        if (!getInputText(inputServer).contains("."))
            invalidSettings.add(inputServer);
        if (getInputText(inputPort).isEmpty())
            invalidSettings.add(inputPort);
        if (!getInputText(inputTarget).contains("@"))
            invalidSettings.add(inputTarget);
        if (!getInputText(inputPubkey).isEmpty()){
            encrypter = new PGPPubkeyEncryptionUtil(getInputText(inputPubkey));
            if (!encrypter.hasKey()) {
                encrypter = null;
                invalidSettings.add(inputPubkey);
            }else{
                String testEncryption = null;
                try{
                    testEncryption = encrypter.encrypt("Test");
                }catch (Exception e){
                    Log.e(LOGCATTAG, e.toString());
                    encrypter = null;
                    invalidSettings.add(inputPubkey);
                }
                if (testEncryption == null)
                    invalidSettings.add(inputPubkey);
            }
        }
        return invalidSettings;
    }

    private boolean isListenerListening(ComponentName listener){
        return pm.getComponentEnabledSetting(listener) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void switchUIStates(boolean state){
        AnimatedVectorDrawable fabImage = AnimatedVectorDrawable.getDrawable(this, R.drawable.play_to_pause);
        if (!state)
            fabImage.reverse();
        fab.setImageDrawable(fabImage);
        inputEmail.setEnabled(!state);
        inputPassword.setEnabled(!state);
        inputServer.setEnabled(!state);
        inputPort.setEnabled(!state);
        inputTarget.setEnabled(!state);
        inputPubkey.setEnabled(!state);
        fabImage.start();
    }

    private String getInputText(EditText input){
        return input.getText().toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_props) {
            startActivity(new Intent(this, JavaMailProperties.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void enableBroadcastReceiver(View view) {
        List<EditText> invalidSettings = invalidSettings();
        if (invalidSettings.isEmpty()) {
            if (haveSettingsChanged()) {
                writeSettings();
                askUserToTestSettings();
            }else{
                pm.setComponentEnabledSetting(receiver,
                        (enabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED),
                        PackageManager.DONT_KILL_APP);
            }
            enabled = isListenerListening(receiver);
            switchUIStates(enabled);
        }else{
            for (EditText settingInput: invalidSettings){
                String errorMessage;
                if (settingInput.equals(inputEmail))
                    errorMessage = getResources().getString(R.string.invalid_email);
                else if (settingInput.equals(inputPassword))
                    errorMessage = getResources().getString(R.string.invalid_password);
                else if (settingInput.equals(inputServer))
                    errorMessage = getResources().getString(R.string.invalid_server);
                else if (settingInput.equals(inputPort))
                    errorMessage = getResources().getString(R.string.invalid_port);
                else if (settingInput.equals(inputTarget))
                    errorMessage = getResources().getString(R.string.invalid_email);
                else if (settingInput.equals(inputPubkey))
                    errorMessage = getResources().getString(R.string.invalid_pubkey);
                else
                    errorMessage = getResources().getString(R.string.invalid_something);
                ((TextInputLayout)settingInput.getParent()).setError(errorMessage);
            }
        }
    }

    private void writeSettings() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SHAREDPREFSEMAIL, getInputText(inputEmail));
        if (!getInputText(inputPassword).isEmpty())
            editor.putString(SHAREDPREFSPASSWORD, getInputText(inputPassword));
        editor.putString(SHAREDPREFSSERVER, getInputText(inputServer));
        editor.putString(SHAREDPREFSPORT, getInputText(inputPort));
        editor.putString(SHAREDPREFSTARGET, getInputText(inputTarget));
        editor.putString(SHAREDPREFSPUBKEY, getInputText(inputPubkey));
        editor.apply();
        readSettings(false);
    }

    private void askUserToTestSettings() {
        Snackbar
                .make(findViewById(R.id.setup_layout), R.string.snackbar_settings_changed, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_settings_confirmed, new SnackBarSettingsConfirmationListener())
                .show();
        Intent testMailIntent = new Intent(this, EmailSenderService.class);
        testMailIntent.putExtra(EmailSenderService.INTENTEXTRASUBJECT, getResources().getString(R.string.email_subject_test));
        testMailIntent.putExtra(EmailSenderService.INTENTEXTRABODY, getResources().getString(R.string.email_body_test));
        startService(testMailIntent);
    }

    public void expandIntro(View view) {
        TextView intro = (TextView) view;
        boolean isSingleLine = intro.getLineCount() == 1;
        String introHint = getResources().getString(isSingleLine ? R.string.hint_intro1 : R.string.hint_intro0);
        ((TextInputLayout) intro.getParent()).setHint(introHint);
        intro.setEllipsize(isSingleLine ? null : TextUtils.TruncateAt.END);
        intro.setSingleLine(!isSingleLine);
    }

    public void showPubkeyPasteDeleteDialog(View view){
        (new AlertDialog.Builder(this)).setItems(R.array.pubkey_dialog_list, new PasteDeleteDialogListener()).create().show();
    }

    private class PasteDeleteDialogListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == 0){
                ClipboardManager clippy = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clippy.hasPrimaryClip() &&
                        (clippy.getPrimaryClipDescription().getMimeType(0).equals(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                                clippy.getPrimaryClipDescription().getMimeType(0).equals(ClipDescription.MIMETYPE_TEXT_HTML))) {
                    inputFitting(inputPubkey, clippy.getPrimaryClip().getItemAt(0).getText().toString());
                }
            }else if (which == 1){
                inputPubkey.setText("");
                inputPubkey.setTextSize(TypedValue.COMPLEX_UNIT_PX, inputTarget.getTextSize());
            }
        }
    }

    public void inputFitting(EditText which, String input){
        String[] pastedInputLines = input.split("\n");
        int longestLine = 0, longestCount = 0;
        for (int i = 0; i < pastedInputLines.length; i++){
            if (pastedInputLines[i].length() > longestCount){
                longestCount = pastedInputLines[i].length();
                longestLine = i;
            }
        }
        if (pastedInputLines.length != 0 && !pastedInputLines[longestLine].isEmpty()){
            which.setText(pastedInputLines[longestLine]);
            which.setTextSize(which.getTextSize()/scaledDensity + 1f);
            which.post(new EnlargeTextRunnable(which, input));
        }
    }

    private class EnlargeTextRunnable implements Runnable{
        EditText theView;
        String theFinalInput;

        public EnlargeTextRunnable(EditText view, String finalInput){
            theView = view;
            theFinalInput = finalInput;
        }

        @Override
        public void run() {
            if (theView.getLineCount() == 1 && theView.getTextSize()/scaledDensity < 100f){
                theView.setTextSize(theView.getTextSize()/scaledDensity+1f);
                theView.post(new EnlargeTextRunnable(theView, theFinalInput));
            }else{
                theView.setTextSize(theView.getTextSize()/scaledDensity-1f);
                theView.post(new ShrinkTextRunnable(theView, theFinalInput));
            }
        }

        private class ShrinkTextRunnable implements Runnable{
            EditText theView;
            String theFinalInput;

            public ShrinkTextRunnable(EditText view, String finalInput){
                theView = view;
                theFinalInput = finalInput;
            }

            @Override
            public void run() {
                if (theView.getLineCount() > 1 && theView.getTextSize()/scaledDensity > 1f){
                    theView.setTextSize(theView.getTextSize()/scaledDensity-1f);
                    theView.post(new ShrinkTextRunnable(theView, theFinalInput));
                }else{
                    theView.setText(theFinalInput);
                }
            }
        }
    }

    public static class ClearErrorOnInputListener implements TextWatcher{

        TextInputLayout wrapper;

        public ClearErrorOnInputListener(EditText input){
            wrapper = (TextInputLayout) input.getParent();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            wrapper.setError(null);
        }
    }

    private class SnackBarSettingsConfirmationListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            enableBroadcastReceiver(v);
        }
    }
}
