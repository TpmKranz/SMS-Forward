package org.tpmkranz.smsforward;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.wnafee.vector.compat.ResourcesCompat;

import java.util.ArrayList;

public class JavaMailProperties extends AppCompatActivity implements JavaMailPropertyDialog.JavaxMailPropertyDialogInterface {

    private RecyclerView propertyRecycler;
    private JavaxMailPropertyAdapter propertyAdapter;
    private RecyclerView.LayoutManager propertyLayout;
    private Resources resources;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_javamail_properties);
        Toolbar toolbar = (Toolbar) findViewById(R.id.javax_mail_properties_toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new AddPairOnClickListener());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        resources = getResources();
        prefs = getSharedPreferences(SetupActivity.SHAREDPREFSNAME, MODE_PRIVATE);
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        populateDefaultPropPreferences(prefs, resources);
        populatePropsLists(keys, values, prefs);
        propertyRecycler = (RecyclerView) findViewById(R.id.javax_mail_properties_recyclerview);
        propertyLayout = new LinearLayoutManager(this);
        propertyRecycler.setLayoutManager(propertyLayout);
        propertyAdapter = new JavaxMailPropertyAdapter(keys, values);
        propertyRecycler.setAdapter(propertyAdapter);
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                propertyAdapter.deletePair(viewHolder.getAdapterPosition());
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(propertyRecycler);
        propertyRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                DialogFragment propertyDialog = new JavaMailPropertyDialog();
                Bundle arguments = new Bundle();
                arguments.putInt(JavaMailPropertyDialog.ARG_MODE, JavaMailPropertyDialog.MODE_EDIT);
                arguments.putInt(JavaMailPropertyDialog.ARG_IX, position);
                arguments.putString(JavaMailPropertyDialog.ARG_KEY, propertyAdapter.propsKeys.get(position));
                arguments.putString(JavaMailPropertyDialog.ARG_VALUE, propertyAdapter.propsValues.get(position));
                propertyDialog.setArguments(arguments);
                propertyDialog.show(getSupportFragmentManager(), JavaMailPropertyDialog.DIALOG_TAG);
            }
        }));
    }

    @Override
    public void doneEditing(Bundle args) {
        int index = args.getInt(JavaMailPropertyDialog.ARG_IX, -1);
        String newKey = args.getString(JavaMailPropertyDialog.ARG_KEY, "");
        String newValue = args.getString(JavaMailPropertyDialog.ARG_VALUE, "");
        if (index == -1){
            propertyAdapter.addPair(newKey, newValue);
            propertyRecycler.smoothScrollToPosition(propertyAdapter.getItemCount()-1);
        }else{
            propertyAdapter.editPair(index, newKey, newValue);
            propertyRecycler.smoothScrollToPosition(index);
        }
    }

    @Override
    public void deleteProperty(int index) {
        propertyAdapter.deletePair(index);
    }

    private void updatePrefsWithKeysAndValues(SharedPreferences settings, ArrayList<String> keys, ArrayList<String> values) {
        SharedPreferences.Editor editor = settings.edit();
        int count = keys.size();
        for (int i = 0; i < count; i++) {
            editor.putString(SetupActivity.SHAREDPREFSJAVAMAILKEY, keys.get(i));
            editor.putString(SetupActivity.SHAREDPREFSJAVAMAILVALUE, values.get(i));
        }
        editor.putInt(SetupActivity.SHAREDPREFSJAVAMAILCOUNT, count);
        editor.apply();
    }


    private class JavaxMailPropertyAdapter extends RecyclerView.Adapter {

        public ArrayList<String> propsKeys, propsValues;

        public JavaxMailPropertyAdapter(ArrayList<String> keys, ArrayList<String> values){
            propsKeys = keys;
            propsValues = values;
        }

        public class PropsViewHolder extends RecyclerView.ViewHolder{
            public TextInputLayout keyLayout, valueLayout;
            public EditText keyInput, valueInput;
            public LinearLayout holderLayout;
            public PropsViewHolder(LinearLayout item){
                super(item);
                keyLayout = (TextInputLayout) item.getChildAt(0);
                keyInput = (EditText) keyLayout.getChildAt(0);
                valueLayout = (TextInputLayout) item.getChildAt(1);
                valueInput = (EditText) valueLayout.getChildAt(0);
                holderLayout = item;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_javamail_properties, parent, false);
            return new PropsViewHolder(item);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            PropsViewHolder propsViewHolder = (PropsViewHolder) holder;
            propsViewHolder.keyInput.setText(propsKeys.get(position));
            propsViewHolder.valueInput.setText(propsValues.get(position));
        }

        @Override
        public int getItemCount() {
            return propsKeys.size();
        }

        public void setKeysAndValues(ArrayList<String> keys, ArrayList<String> values){
            ArrayList<String> oldKeys = new ArrayList<>(propsKeys);
            ArrayList<String> oldValues = new ArrayList<>(propsValues);
            Snackbar.make(findViewById(R.id.javax_mail_properties_layout), R.string.snackbar_property_reset, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_property_revert, new RevertResettingPropsListener(oldKeys, oldValues))
                    .show();
            propsKeys = keys;
            propsValues = values;
        }

        public void addPair(String key, String value){
            String text = resources.getString(R.string.snackbar_property_added, key);
            Snackbar.make(findViewById(R.id.javax_mail_properties_layout), text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_property_revert, new RevertAddingPropListener(propsKeys.size()))
                    .show();
            propsKeys.add(key);
            propsValues.add(value);
            notifyItemInserted(getItemCount() - 1);
            updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
        }

        public void editPair(int index, String key, String value){
            String text = resources.getString(R.string.snackbar_property_edited, key);
            Snackbar.make(findViewById(R.id.javax_mail_properties_layout), text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_property_revert,
                            new RevertEditingPropListener(propsKeys.get(index), propsValues.get(index), index))
                    .show();
            propsKeys.set(index, key);
            propsValues.set(index, value);
            notifyItemChanged(index);
            updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
        }

        public void deletePair(int index) {
            String text = resources.getString(R.string.snackbar_property_removed, propsKeys.get(index));
            Snackbar.make(findViewById(R.id.javax_mail_properties_layout), text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_property_revert,
                            new RevertDeletingPropListener(propsKeys.get(index), propsValues.get(index), index))
                    .show();
            propsKeys.remove(index);
            propsValues.remove(index);
            notifyItemRemoved(index);
            updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
        }

        private class RevertAddingPropListener implements View.OnClickListener {
            int index;

            public RevertAddingPropListener(int index){
                this.index = index;
            }

            @Override
            public void onClick(View v) {
                propsKeys.remove(index);
                propsValues.remove(index);
                notifyItemRemoved(index);
                updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
            }
        }

        private class RevertEditingPropListener implements View.OnClickListener {

            private String key;
            private String value;
            private int index;

            public RevertEditingPropListener(String key, String value, int index) {
                this.key = key;
                this.value = value;
                this.index = index;
            }

            @Override
            public void onClick(View v) {
                propsKeys.set(index, key);
                propsValues.set(index, value);
                notifyItemChanged(index);
                updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
                propertyRecycler.smoothScrollToPosition(index);
            }
        }

        private class RevertDeletingPropListener implements View.OnClickListener {
            private String key;
            private String value;
            private int index;

            public RevertDeletingPropListener(String key, String value, int index) {
                this.key = key;
                this.value = value;
                this.index = index;
            }

            @Override
            public void onClick(View v) {
                propsKeys.add(index, key);
                propsValues.add(index, value);
                notifyItemInserted(index);
                updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
                propertyRecycler.smoothScrollToPosition(index);
            }
        }

        private class RevertResettingPropsListener implements View.OnClickListener {
            private ArrayList<String> oldKeys;
            private ArrayList<String> oldValues;

            public RevertResettingPropsListener(ArrayList<String> oldKeys, ArrayList<String> oldValues) {
                this.oldKeys = oldKeys;
                this.oldValues = oldValues;
            }

            @Override
            public void onClick(View v) {
                propsKeys = oldKeys;
                propsValues = oldValues;
                notifyDataSetChanged();
                updatePrefsWithKeysAndValues(prefs, propsKeys, propsValues);
                propertyRecycler.smoothScrollToPosition(0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_javamail_properties, menu);
        MenuItem resetItem = menu.findItem(R.id.action_reset_props);
        Drawable icon = ResourcesCompat.getDrawable(this, R.drawable.revert);
        resetItem.setIcon(icon);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reset_props) {
            propertyRecycler.smoothScrollToPosition(0);
            populateDefaultPropPreferences(prefs, resources, true);
            ArrayList<String> keys = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            populatePropsLists(keys, values, prefs);
            propertyAdapter.setKeysAndValues(keys, values);
            propertyAdapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populatePropsLists(ArrayList<String> keys, ArrayList<String> values, SharedPreferences preferences){
        for (int i = 0; i < preferences.getInt(SetupActivity.SHAREDPREFSJAVAMAILCOUNT, 0); i++){
            keys.add(preferences.getString(SetupActivity.SHAREDPREFSJAVAMAILKEY +String.valueOf(i), ""));
            values.add(preferences.getString(SetupActivity.SHAREDPREFSJAVAMAILVALUE +String.valueOf(i), ""));
        }
    }

    private class AddPairOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            DialogFragment propertyDialog = new JavaMailPropertyDialog();
            Bundle arguments = new Bundle();
            arguments.putInt(JavaMailPropertyDialog.ARG_MODE, JavaMailPropertyDialog.MODE_NEW);
            propertyDialog.setArguments(arguments);
            propertyDialog.show(getSupportFragmentManager(), JavaMailPropertyDialog.DIALOG_TAG);
        }
    }

    static public void populateDefaultPropPreferences(SharedPreferences prefs, Resources res){
        JavaMailProperties.populateDefaultPropPreferences(prefs, res, false);
    }

    static public void populateDefaultPropPreferences(SharedPreferences prefs, Resources res, boolean force){
        if (force || prefs.getInt(SetupActivity.SHAREDPREFSJAVAMAILCOUNT, 0) == 0) {
            SharedPreferences.Editor editor = prefs.edit();
            String[] defaultPropsKeys = res.getStringArray(R.array.javamail_default_property_keys);
            String[] defaultPropsValues = res.getStringArray(R.array.javamail_default_property_values);
            for (int i = 0; i < defaultPropsKeys.length; i++){
                editor.putString(SetupActivity.SHAREDPREFSJAVAMAILKEY +String.valueOf(i), defaultPropsKeys[i]);
                editor.putString(SetupActivity.SHAREDPREFSJAVAMAILVALUE + String.valueOf(i), defaultPropsValues[i]);
            }
            editor.putInt(SetupActivity.SHAREDPREFSJAVAMAILCOUNT, defaultPropsKeys.length);
            editor.apply();
        }
    }
}
