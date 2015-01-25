package geonote.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
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
import com.shamanland.fab.FloatingActionButton;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class MapsActivity
        extends     ActionBarActivity
        implements  GoogleApiClient.ConnectionCallbacks,
                    GoogleApiClient.OnConnectionFailedListener,
                    LocationListener {

    private static final int NOTE_VIEW_ACTIVITY = 1;
    private static final String PREFS_NOTES = "GeoNote.Preferences.V1";
    private static final String PREFS_NOTES_VALUES_JSON = "GeoNote.Preferences.V1.Notes";
    private static final String APP_ID = "e3ec817cadded7a87ea28a89852d8011";
    private static final int GEO_FENCE_RADIUS = 100;
    private static final int CURRNET_NOTIFICATION_ID=0;

    private GoogleMap mGoogleMap;
    private NotesRepository mNotesRepository;
    private Geocoder mGeocoder;
    private GoogleApiClient mGoogleApiClient;
    private FloatingActionButton newNoteButton;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest;
    private HashSet<NoteInfo> mSentNotifications = new HashSet<>();
    private HashMap<LatLng, Marker> mMarkers = new HashMap<>();
    NotificationManager mNotificationManager = null;
    private NoteInfo mCurrentShownNotificationNote = null;
    private GeoFenceWatcherService mBoundService;
    private boolean mIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        this.mGeocoder = new Geocoder(this.getBaseContext(), Locale.getDefault());

        setContentView(R.layout.activity_main);

        setupNewNoteButton();

        buildGoogleApiClient();

        createLocationRequest();

        setUpNotesRepository();

        setUpMapIfNeeded();

        checkForUpdates();
    }

    private void setupNewNoteButton() {
        newNoteButton = (FloatingActionButton) findViewById(R.id.fabButton);
        newNoteButton.setSize(FloatingActionButton.SIZE_MINI);
        newNoteButton.setColor(Color.RED);
        // NOTE invoke this method after setting new values!
        newNoteButton.initBackground();
        final Activity currentActivity = this;

        newNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewNote(mGoogleMap.getCameraPosition().target, currentActivity);
            }
        });
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

        mNotesRepository = new NotesRepository(this.mGeocoder);
        mNotesRepository.deserializeFromJson(settingJson);
    }

    @Override
    protected void onStop(){
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NOTES, 0);

        String notesJson = this.mNotesRepository.serializeToJson();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_NOTES_VALUES_JSON, notesJson);

        // Commit the edits!
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        checkForCrashes();
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
                        NoteInfo noteInfo = mNotesRepository.Notes.get(position);

                        Intent myIntent = new Intent(currentActivity, NoteViewActivity.class);
                        myIntent.putExtra("noteInfoExtra", noteInfo); //Optional parameters
                        currentActivity.startActivityForResult(myIntent, NOTE_VIEW_ACTIVITY);
                    }
                }
        );

        LayoutInflater layoutInflater = getLayoutInflater();

        mGoogleMap.setInfoWindowAdapter(new NoteInfoWindowAdapter(layoutInflater, this.mNotesRepository));

        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                addNewNote(latLng, currentActivity);
            }
        });
    }

    private void addNewNote(LatLng latLng, Activity currentActivity) {
        NoteInfo note = null;
        if (!mNotesRepository.Notes.containsKey(latLng)) {
            note = new NoteInfo()
                    .LatLng(latLng)
                    .Address(NotesRepository.getAddressFromLatLng(mGeocoder, latLng));
            mNotesRepository.Notes.put(latLng, note);
        }

        note = mNotesRepository.Notes.get(latLng);

        Intent myIntent = new Intent(currentActivity, NoteViewActivity.class);
        myIntent.putExtra("noteInfoExtra", note); //Optional parameters
        currentActivity.startActivityForResult(myIntent, NOTE_VIEW_ACTIVITY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the request went well (OK) and the request was NOTE_VIEW_ACTIVITY
        if (requestCode == NOTE_VIEW_ACTIVITY) {
            NoteInfo noteInfo = null;

            switch (resultCode) {
                case Activity.RESULT_OK:
                    noteInfo = data.getParcelableExtra("result");

                    // replace existing note with new note.
                    this.mNotesRepository.Notes.put(noteInfo.getLatLng(), noteInfo);

                    // add the note to the map
                    addNoteMarkerToMap(noteInfo);
                    break;

                case Constants.RESULT_DELETE_NOTE:
                    noteInfo = data.getParcelableExtra("result");
                    this.mNotesRepository.Notes.remove(noteInfo.getLatLng());
                    removeNoteMarkerFromMap(noteInfo);
            }
        }
    }

    private void addMarkersFromNotes() {
        for (NoteInfo note: mNotesRepository.Notes.values()) {
            addNoteMarkerToMap(note);
        }
    }

    private void addNoteMarkerToMap(NoteInfo note) {
        if (mMarkers.containsKey(note.getLatLng()))
        {
            mMarkers.get(note.getLatLng()).remove();
        }

        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                .position(note.getLatLng())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(false)
                .flat(true)
                .title(note.getAddressDetails())
                .snippet(note.toString()));
        marker.setVisible(true);
        marker.showInfoWindow();
        mMarkers.put(note.getLatLng(), marker);
    }

    private void removeNoteMarkerFromMap(NoteInfo noteInfo)
    {
        Marker marker = mMarkers.get(noteInfo.getLatLng());
        marker.remove();
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
        this.mLastLocation = location;

        // check if there is a note in the nearby location.
        float closestMatch = Integer.MAX_VALUE;
        NoteInfo noteInfoToNotifyOn = null;

        for(NoteInfo noteInfo: this.mNotesRepository.Notes.values())
        {
            // if we have a note within about GEO_FENCE_RADIUS meters from where we are,
            // and the note requested for an alert, send a notification.
            float distanceFromNote = noteInfo.getDistanceFrom(mLastLocation);
            if (distanceFromNote < GEO_FENCE_RADIUS) {
                if (closestMatch > distanceFromNote && noteInfo.getEnableRaisingEvents())
                {
                    closestMatch = distanceFromNote;
                    noteInfoToNotifyOn = noteInfo;
                }
            }
        }

        if (noteInfoToNotifyOn != null) {
            // send the notification from the closest note only if we havent already sent it.
            // TODO - do we need to remember this for a time period too?
            if (!mSentNotifications.contains(noteInfoToNotifyOn)) {
                sendNotification(noteInfoToNotifyOn.toString(), noteInfoToNotifyOn);
                mSentNotifications.add(noteInfoToNotifyOn);
            }
        }

        // if the posted notification is outside of GEO_FENCE_RADIUS,
        // cancel the sent notification.
        if (mCurrentShownNotificationNote!=null && mCurrentShownNotificationNote.getDistanceFrom(mLastLocation) >= GEO_FENCE_RADIUS) {
            mNotificationManager.cancel(CURRNET_NOTIFICATION_ID);
        }
    }

    protected void sendNotification(String notificationContents, NoteInfo noteInfo)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notespin)
                        .setContentTitle("Note available at nearby location")
                        .setAutoCancel(true)
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

        Notification notification = mBuilder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        
        mNotificationManager.notify(CURRNET_NOTIFICATION_ID, notification);
        mCurrentShownNotificationNote = noteInfo;
    }

    @Override
    protected void onPause() {
        super.onPause();
        UpdateManager.unregister();
    }

    private void checkForCrashes() {
        CrashManager.register(this, APP_ID);
    }

    private void checkForUpdates() {
        // Remove this for store / production builds!
        UpdateManager.register(this, APP_ID);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((GeoFenceWatcherService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            //Toast.makeText(Binding.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            //Toast.makeText(this, "local_service_disconnected", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
     protected void onStart()
    {
        super.onStart();

        // TODO: uncomment until we can make this work
        //doBindService();
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this,
                GeoFenceWatcherService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

}
