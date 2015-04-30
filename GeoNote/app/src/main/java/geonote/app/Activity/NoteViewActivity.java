package geonote.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import geonote.app.Constants;
import geonote.app.Tasks.DownloadMapImageTask;
import geonote.app.GooglePlaces.GooglePlaces;
import geonote.app.GooglePlaces.Model.Place;
import geonote.app.NoteInfo;
import geonote.app.NotesRepository;
import geonote.app.R;

public class NoteViewActivity extends ActionBarActivity {

    NoteInfo noteInfo;
    GooglePlaces googlePlaces = null;
    Geocoder mGeocoder = null;

    TextView addressDetailsTextView = null;
    TextView addressTextView = null;
    EditText editText = null;
    CheckBox checkBoxEnableAlerts = null;
    ImageView mImageView = null;

    // region Overrides for ActionBarActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        this.mGeocoder = new Geocoder(this.getBaseContext(), Locale.getDefault());

        Intent intent = getIntent();
        this.noteInfo = intent.getParcelableExtra("noteInfoExtra");

        setContentView(R.layout.activity_note_view);

        this.addressDetailsTextView = (TextView) findViewById(R.id.txtNoteViewPlaceDetails);
        addressDetailsTextView.setText(noteInfo.getAddressDetails());

        // execute a background task to populate the drop down choices for the place.
        new LoadPlaces().execute();

        addressTextView = (TextView) findViewById(R.id.txtNoteViewAddress);
        addressTextView.setText(noteInfo.getAddress());

        editText = (EditText) findViewById(R.id.editTextNoteView);
        editText.setText(noteInfo.toString());

        checkBoxEnableAlerts = (CheckBox) findViewById(R.id.checkboxEnableAlerts);
        checkBoxEnableAlerts.setChecked(noteInfo.getEnableRaisingEvents());

        mImageView = (ImageView)findViewById(R.id.noteMapImageHolder);

        new DownloadMapImageTask(mImageView).execute(
                (double) 600,
                (double) 200,
                noteInfo.getLatLng().latitude,
                noteInfo.getLatLng().longitude);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id)
        {
            case R.id.action_settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                this.startActivityForResult(myIntent, Constants.ACTIVITY_SETTINGS);
                return true;

            case R.id.action_delete:
                new AlertDialog.Builder(this)
                        .setTitle("Delete note")
                        .setMessage("Are you sure you want to delete this Note?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                returnFromActivity(Constants.RESULT_DELETE_NOTE, noteInfo);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_delete)
                        .show();

                break;

            case R.id.action_save:
                returnFromActivity(Constants.RESULT_SAVE_NOTE, createNoteInfoFromUserData());
                break;

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        NoteInfo newNoteInfo = createNoteInfoFromUserData();
        if (newNoteInfo.equals(this.noteInfo)) {
            NoteViewActivity.super.onBackPressed();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("Unsaved changes")
                .setMessage("There are unsaved changes. Do you want to save changes?")
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        returnFromActivity(Constants.RESULT_SAVE_NOTE, createNoteInfoFromUserData());
                    }
                })
                .setNegativeButton("Don't save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing to stay back in the same view
                    }
                })
                .create()
                .show();
        }
    }

    // endregion

    private NoteInfo createNoteInfoFromUserData() {
        final NoteInfo newNoteInfo = new NoteInfo();

        String editTextValue = editText.getText().toString();
        if (editTextValue != null && !editTextValue.equals("")) {
            for (String note : editText.getText().toString().split("\n")) {
                newNoteInfo.AddNote(note);
            }
        }

        // save modifications to address
        newNoteInfo.AddressDetails(addressDetailsTextView.getText().toString())
                .EnableRaisingEvents(checkBoxEnableAlerts.isChecked())
                .Address(addressTextView.getText().toString())
                .LatLng(noteInfo.getLatLng());
        return newNoteInfo;
    }

    private void returnFromActivity(int resultCode, NoteInfo resultObject) {
        Intent returnIntent;
        returnIntent = new Intent();
        returnIntent.putExtra("result", resultObject);
        setResult(resultCode, returnIntent);

        finish();
    }

    class LoadPlaces extends AsyncTask<String, String, String> {

        ArrayList<Place> placesList = new ArrayList<Place>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            googlePlaces = new GooglePlaces(getString(R.string.google_maps_key));
        }

        protected String doInBackground(String... args) {
            try {
                LatLng latLng = noteInfo.getLatLng();
                if (noteInfo.getAddress() != null) {

                    Address address = NotesRepository.getAddressFromLocationName(mGeocoder, noteInfo.getAddress());
                    if (address != null) {
                        latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    }
                }

                List<Place> places = googlePlaces.searchForPlaces(latLng , 75).results;
                for(Place place:places) {
                    this.placesList.add(this.placesList.size(), googlePlaces.getPlaceDetails(place).result);
                }
            } catch (Exception e) {
                System.out.println("Unhandled exception trying to get google places details: " + e.toString());
            }
            return null;
        }

        /**
         * After completing background task show the data in UI
         * Use runOnUiThread(new Runnable()) to update UI from background
         * **/
        protected void onPostExecute(String file_url) {
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    Spinner dropdown = (Spinner)findViewById(R.id.spinnerPlaceDetails);
                    dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if(parent.getItemAtPosition(position) != "") {
                                String placeName = parent.getItemAtPosition(position).toString();
                                addressDetailsTextView.setText(placeName);

                                for(Place place:placesList)
                                {
                                    // find matching place, and change the address to that location
                                    if (place.name == placeName &&

                                            (place.formatted_address != null
                                            || place.formatted_address != ""))
                                    {
                                        addressTextView.setText(place.formatted_address);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    if (placesList!=null) {
                        String[] items = new String[placesList.size() + 1];
                        items[0] = "";
                        int pos = 1;
                        for (Place place : placesList) {
                            if (place != null) {
                                items[pos++] = place.name;
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                getBaseContext(),
                                android.R.layout.simple_spinner_item,
                                items);

                        dropdown.setAdapter(adapter);
                    }
                }
            });
        }
    }
}
