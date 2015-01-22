package geonote.app;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import geonote.app.Model.Place;
import geonote.app.Model.PlaceDetails;
import geonote.app.Model.PlacesList;

public class NoteViewActivity extends ActionBarActivity {

    NoteInfo noteInfo;
    GooglePlaces googlePlaces = null;

    TextView addressDetailsTextView = null;
    TextView addressTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.noteInfo = intent.getParcelableExtra("noteInfoExtra");

        setContentView(R.layout.activity_note_view);

        this.addressDetailsTextView = (TextView) findViewById(R.id.txtNoteViewPlaceDetails);

        // execute a background task to populate the drop down choices for the place.
        new LoadPlaces().execute();

        addressDetailsTextView.setText(noteInfo.getAddressDetails());

        addressTextView = (TextView) findViewById(R.id.txtNoteViewAddress);
        addressTextView.setText(noteInfo.getAddressString());

        final EditText editText = (EditText) findViewById(R.id.editTextNoteView);
        editText.setText(noteInfo.toString());

        Button saveButton = (Button) findViewById(R.id.buttonNoteActivitySave);
        final CheckBox checkBoxEnableAlerts = (CheckBox) findViewById(R.id.checkboxEnableAlerts);

        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                noteInfo.getNotes().clear();
                for (String note : editText.getText().toString().split("\n")) {
                    noteInfo.AddNote(note);
                }
                noteInfo.AddressDetails(addressDetailsTextView.getText().toString())
                        .EnableRaisingEvents(checkBoxEnableAlerts.isChecked());

                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", noteInfo);
                setResult(RESULT_OK,returnIntent);

                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.buttonNoteActivityCancel);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                List<Place> places = googlePlaces.searchForPlaces(noteInfo.getLatLng(), 75).results;
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
                            if(parent.getItemAtPosition(position) != null) {
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
                        items[0] = null;
                        int pos = 1;
                        for (Place place : placesList) {
                            items[pos++] = place.name;
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_item, items);
                        dropdown.setAdapter(adapter);
                    }
                }
            });
        }
    }
}
