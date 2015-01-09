package geonote.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public class MapsActivity
        extends     ActionBarActivity
        implements  GoogleApiClient.ConnectionCallbacks,
                    GoogleApiClient.OnConnectionFailedListener,
                    LocationListener {

    static final int NOTE_VIEW_ACTIVITY = 1;
    static final String PREFS_NOTES = "GeoNote.Preferences.V1";
    static final String PREFS_NOTES_VALUES_JSON = "GeoNote.Preferences.V1.Notes";

    private GoogleMap mGoogleMap;
    private NotesRepository mNotesRepostiory;
    private Geocoder mGeocoder;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest;
    private HashSet<NoteInfo> mSentNotifications = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        buildGoogleApiClient();

        createLocationRequest();

        this.mGeocoder = new Geocoder(this.getBaseContext(), Locale.getDefault());

        setUpNotesRepository();

        setUpMapIfNeeded();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setUpNotesRepository() {
        SharedPreferences settings = getSharedPreferences(PREFS_NOTES, 0);
        String settingJson = settings.getString(PREFS_NOTES_VALUES_JSON, "");

        mNotesRepostiory = new NotesRepository(this.mGeocoder);
        mNotesRepostiory.deserializeFromJson(settingJson);
    }

    @Override
    protected void onStop(){
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NOTES, 0);

        String notesJson = this.mNotesRepostiory.serializeToJson();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_NOTES_VALUES_JSON, notesJson);

        // Commit the edits!
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mGoogleMap} is not null.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mGoogleMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mGoogleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mGoogleMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mGoogleMap} is not null.
     */
    private void setUpMap() {
        final Activity currentActivity = this;

        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        mGoogleMap.setMyLocationEnabled(true);

        this.addMarkersFromNotes();

        mGoogleMap.setOnInfoWindowClickListener(
                new GoogleMap.OnInfoWindowClickListener() {
                    public void onInfoWindowClick(Marker marker) {
                        LatLng position = marker.getPosition();
                        NoteInfo noteInfo = mNotesRepostiory.Notes.get(position);

                        Intent myIntent = new Intent(currentActivity, NoteViewActivity.class);
                        myIntent.putExtra("noteInfoExtra", noteInfo); //Optional parameters
                        currentActivity.startActivityForResult(myIntent, NOTE_VIEW_ACTIVITY);
                    }
                }
        );

        LayoutInflater layoutInflater = getLayoutInflater();

        mGoogleMap.setInfoWindowAdapter(new NoteInfoWindowAdapter(layoutInflater, this.mNotesRepostiory));

        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                NoteInfo note = null;
                if (!mNotesRepostiory.Notes.containsKey(latLng)) {
                    note = new NoteInfo()
                            .LatLng(latLng)
                            .Address(NotesRepository.getAddressFromLatLng(mGeocoder, latLng));
                    mNotesRepostiory.Notes.put(latLng, note);
                }

                note = mNotesRepostiory.Notes.get(latLng);

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
            this.mNotesRepostiory.Notes.put(noteInfo.getLatLng(), noteInfo);

            // add the note to the map
            addNoteMarkerToMap(noteInfo);
        }
    }

    private void addMarkersFromNotes() {
        for (NoteInfo note: mNotesRepostiory.Notes.values()) {
            addNoteMarkerToMap(note);
        }
    }

    private void addNoteMarkerToMap(NoteInfo note) {
        mGoogleMap.addMarker(new MarkerOptions()
                .position(note.getLatLng())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(true)
                .flat(true)
                .title("Location")
                .snippet(note.toString()));
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        startLocationUpdates();

        if (mLastLocation != null) {
            // Showing the current location in Google Map

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1000, null);
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getBaseContext(),
                "Connection failed",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng currentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        // as of now we always move the map to where the current location is.
        //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
        //mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1000, null);

        // check if there is a note in the nearby location.
        float results[] = new float[1];
        float closestMatch = Integer.MAX_VALUE;
        NoteInfo noteInfoToNotifyOn = null;

        for(NoteInfo noteInfo: this.mNotesRepostiory.Notes.values())
        {
            Location.distanceBetween(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude(),
                    noteInfo.getLatLng().latitude,
                    noteInfo.getLatLng().longitude,
                    results
            );

            // if we have a note within about 100 meters from where we are, send a notification.
            if (results[0] < 100) {
                if (closestMatch > results[0])
                {
                    closestMatch = results[0];
                    noteInfoToNotifyOn = noteInfo;
                }
            }
        }


        // send the notification from the closest note only if we havent already sent it.
        // TODO - do we need to remember this for a time period too?
        if(!mSentNotifications.contains(noteInfoToNotifyOn)) {
            sendNotification(noteInfoToNotifyOn.toString(), noteInfoToNotifyOn);
            mSentNotifications.add(noteInfoToNotifyOn);
        }
    }

    protected void sendNotification(String notificationContents, NoteInfo noteInfo)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notespin)
                        .setContentTitle("Note available at nearby location")
                        .setContentText(notificationContents);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, NoteViewActivity.class);
        resultIntent.putExtra("noteInfoExtra", noteInfo);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(NoteViewActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }
}
