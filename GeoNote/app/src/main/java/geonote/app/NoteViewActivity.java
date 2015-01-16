package geonote.app;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import geonote.app.Model.PlaceDetails;
import geonote.app.Model.PlacesList;

public class NoteViewActivity extends ActionBarActivity {

    NoteInfo noteInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.noteInfo = intent.getParcelableExtra("noteInfoExtra");

        setContentView(R.layout.activity_note_view);

        final TextView addressDetailsTextView = (TextView) findViewById(R.id.txtNoteViewPlaceDetails);

        PlaceDetails placeDetails = tryGetPlaceDetails(noteInfo);
        if (placeDetails != null
                && noteInfo.getAddressDetails()!= null
                && noteInfo.getAddressDetails() != ""){
            addressDetailsTextView.setText(placeDetails.result.name);
        } else {
            addressDetailsTextView.setText(noteInfo.getAddressDetails());
        }

        TextView textView = (TextView) findViewById(R.id.txtNoteViewAddress);
        textView.setText(noteInfo.getAddressString());

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

    private PlaceDetails tryGetPlaceDetails(NoteInfo noteInfo) {
        try {
            GooglePlaces googlePlaces = new GooglePlaces(getString(R.string.google_maps_key));

            // Moscone Center, Howard Street, San Francisco, CA, United States
            PlacesList placesList = googlePlaces.searchForPlaces(new LatLng(37.784147, -122.402115), 20);

            // pick the first place
            PlaceDetails placeDetails = googlePlaces.getPlaceDetails(placesList.results.get(0));
            return placeDetails;
        }
        catch (Exception e) {
            // best effort
        }

        return null;
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
}
