package geonote.app;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.os.Bundle;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MapsActivity extends ActionBarActivity {

    static LatLng LKG_CURRENT_LOCATION = new LatLng(47.734796, -122.159598);
    static final int NOTE_VIEW_ACTIVITY = 1;

    private GoogleMap googleMap;
    private NotesRepository notesRepostiory;
    Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.geocoder = new Geocoder(this.getBaseContext(), Locale.getDefault());

        setUpNotesRepository();
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setUpNotesRepository() {
        notesRepostiory = new NotesRepository(this.geocoder);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #googleMap} is not null.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (googleMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (googleMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #googleMap} is not null.
     */
    private void setUpMap() {
        final Activity currentActivity = this;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        this.addMarkersFromNotes();

        // Showing the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(LKG_CURRENT_LOCATION));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1000, null);

        locationManager.requestLocationUpdates(
            provider,
            5000,
            10,
            new CustomLocationListener(getApplicationContext()));

        googleMap.setOnInfoWindowClickListener(
            new GoogleMap.OnInfoWindowClickListener() {
                public void onInfoWindowClick(Marker marker) {
                    LatLng position = marker.getPosition();
                    NoteInfo noteInfo = notesRepostiory.Notes.get(position);

                    Intent myIntent = new Intent(currentActivity, NoteViewActivity.class);
                    myIntent.putExtra("noteInfoExtra", noteInfo); //Optional parameters
                    currentActivity.startActivityForResult(myIntent, NOTE_VIEW_ACTIVITY);
                }
            }
        );

        LayoutInflater layoutInflater = getLayoutInflater();

        googleMap.setInfoWindowAdapter(new NoteInfoWindowAdapter(layoutInflater, this.notesRepostiory));

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                NoteInfo note = null;
                if(!notesRepostiory.Notes.containsKey(latLng))
                {
                    note = new NoteInfo()
                            .LatLng(latLng)
                            .Address(NotesRepository.getAddressFromLatLng(geocoder, latLng));
                    notesRepostiory.Notes.put(latLng, note);
                }

                note = notesRepostiory.Notes.get(latLng);

                Intent myIntent = new Intent(currentActivity, NoteViewActivity.class);
                myIntent.putExtra("noteInfoExtra", note); //Optional parameters
                currentActivity.startActivityForResult(myIntent, NOTE_VIEW_ACTIVITY);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the request went well (OK) and the request was NOTE_VIEW_ACTIVITY
        if (resultCode == Activity.RESULT_OK && requestCode == NOTE_VIEW_ACTIVITY) {
            NoteInfo noteInfo = data.getParcelableExtra("result");

            // replace existing note with new note.
            this.notesRepostiory.Notes.put(noteInfo.getLatLng(), noteInfo);

            // add the note to the map
            addNoteMarkerToMap(noteInfo);
        }
    }

    private void addMarkersFromNotes() {
        for (NoteInfo note: notesRepostiory.Notes.values()) {
            addNoteMarkerToMap(note);
        }
    }

    private void addNoteMarkerToMap(NoteInfo note) {
        googleMap.addMarker(new MarkerOptions()
                .position(note.getLatLng())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(true)
                .flat(true)
                .title("Location")
                .snippet(note.toString()));
    }

    public String ConvertPointToLocation(double pointlat, double pointlog) {

        String address = "";
        Geocoder geoCoder = new Geocoder(getApplicationContext(),
                Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(pointlat,pointlog, 1);
            if (addresses.size() > 0) {
                for (int index = 0; index < addresses.get(0)
                        .getMaxAddressLineIndex(); index++)
                    address += addresses.get(0).getAddressLine(index) + " ";
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }
}
