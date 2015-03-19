package geonote.app.Fragments;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.TokenCachingStrategy;
import com.facebook.model.GraphUser;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.shamanland.fab.FloatingActionButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import geonote.app.Activity.NoteViewActivity;
import geonote.app.Constants;
import geonote.app.GeoFenceWatcherService;
import geonote.app.NoteInfo;
import geonote.app.NoteInfoWindowAdapter;
import geonote.app.NotesRepository;
import geonote.app.R;
import geonote.app.Settings;

public class MapViewFragment
        extends Fragment
        implements  GoogleApiClient.ConnectionCallbacks,
                    GoogleApiClient.OnConnectionFailedListener,
                    LocationListener {

    private OnFragmentInteractionListener mListener;

    protected GoogleMap mGoogleMap;
    protected NotesRepository mNotesRepository;
    protected Geocoder mGeocoder;
    protected GoogleApiClient mGoogleApiClient;
    protected FloatingActionButton newNoteButton;
    protected Location mLastLocation = null;
    protected LocationRequest mLocationRequest;
    protected HashSet<NoteInfo> mSentNotifications = new HashSet<>();
    protected HashMap<LatLng, Marker> mMarkers = new HashMap<>();
    protected NotificationManager mNotificationManager = null;
    protected NoteInfo mCurrentShownNotificationNote = null;
    protected GeoFenceWatcherService mBoundService;
    protected boolean mIsBound;
    protected boolean mGeoIntentReceived;

    protected Bundle mSavedInstanceState;
    private View mCurrentView;
    private Fragment mFragment;
    private Settings mSettings;

    /**
     * Factory method to create a new instance of this fragment.
     * @return A new instance of fragment MapViewFragment.
     */
    public static MapViewFragment newInstance() {
        MapViewFragment fragment = new MapViewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MapViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.mSettings = new Settings(this.getActivity());

        this.mSavedInstanceState = savedInstanceState;

        this.mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        this.mGeocoder = new Geocoder(getActivity().getBaseContext(), Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mCurrentView = inflater.inflate(R.layout.fragment_map_view, container, false);

        setupNewNoteButton();

        buildGoogleApiClient();

        createLocationRequest();

        setUpNotesRepository();

        checkAndHandleLocationIntent();

        this.setupFacebookOverlay(savedInstanceState);

        return mCurrentView;
    }

    private void onSessionStateChange(Session session) {
        final TextView txtUserDetails = (TextView) mCurrentView.findViewById(R.id.mapViewLoggedInUser);
        if (session != null && session.isOpened()) {
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user,
                                        Response response) {
                    if (user != null) {
                        String user_ID = user.getId();//user id
                        String profileName = user.getName();//user's profile name
                        txtUserDetails.setText("Logged in as " + user.getName());
                    }
                }
            });
            Request.executeBatchAsync(request);
        } else if (session.isClosed()) {
            txtUserDetails.setText("");
        }
    }

    private void setupFacebookOverlay(Bundle savedInstanceState) {

        Session.StatusCallback statusCallback = new Session.StatusCallback() {
            @Override
            public void call(final Session session, final SessionState state, final Exception exception) {
                onSessionStateChange(session);
            }
        };

        Session session = Session.getActiveSession();

        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this.getActivity(), null, statusCallback, savedInstanceState);
            }

            if (session == null) {
                session = new Session(this.getActivity());
            }

            if (session!=null) {
                Session.setActiveSession(session);
            }
        }

        if (session!=null) {

            if(!session.isOpened()) {
                session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
            }

            onSessionStateChange(session);
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpMapIfNeeded();
    }

    @Override
    public void onStop() {
        super.onStop();

        commitNotes();
    }

    private void commitNotes() {
        commitNotes(this.getActivity(), this.mNotesRepository);
    }

    public static void commitNotes(Activity activity, NotesRepository notesRepository) {
        SharedPreferences settings = activity.getSharedPreferences(Constants.PREFS_NOTES, 0);

        String notesJson = notesRepository.serializeToJson();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Constants.PREFS_NOTES_VALUES_JSON, notesJson);

        // Commit the edits!
        editor.commit();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    // region Overrides for GoogleApiClient.ConnectionCallbacks

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        startLocationUpdates();

        if (mGeoIntentReceived != true && mLastLocation != null && mGoogleMap != null) {
            moveMapCameraToLocation(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this);
    }

    protected void moveMapCameraToLocation(LatLng latLng) {
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1000, null);
    }


    // endregion

    // region Overrides for GoogleApiClient.OnConnectionFailedListener

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity().getBaseContext(),
                "Connection failed",
                Toast.LENGTH_LONG).show();
    }

    // endregion

    // region Overrides for LocationListener

    @Override
    public void onLocationChanged(Location location) {
        this.mLastLocation = location;

        if (!mSettings.isNotificationsEnabled()) {
            return ;
        }

        // check if there is a note in the nearby location.
        float closestMatch = Integer.MAX_VALUE;
        NoteInfo noteInfoToNotifyOn = null;

        for(NoteInfo noteInfo: this.mNotesRepository.Notes.values())
        {
            // if we have a note within about GEO_FENCE_RADIUS meters from where we are,
            // and the note requested for an alert, send a notification.
            float distanceFromNote = noteInfo.getDistanceFrom(mLastLocation);
            if (distanceFromNote < mSettings.getGeoFenceRadius()) {
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
        if (mCurrentShownNotificationNote!=null && mCurrentShownNotificationNote.getDistanceFrom(mLastLocation) >= mSettings.getGeoFenceRadius()) {
            mNotificationManager.cancel(Constants.CURRENT_NOTIFICATION_ID);
        }
    }

    protected void sendNotification(String notificationContents, NoteInfo noteInfo) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.drawable.notespin)
                        .setContentTitle("Note available at nearby location")
                        .setAutoCancel(true)
                        .setContentText(notificationContents);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(getActivity(), NoteViewActivity.class);
        resultIntent.putExtra("noteInfoExtra", noteInfo);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());

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

        mNotificationManager.notify(Constants.CURRENT_NOTIFICATION_ID, notification);
        mCurrentShownNotificationNote = noteInfo;
    }

    // endregion


    protected void setupNewNoteButton() {
        newNoteButton = (FloatingActionButton) mCurrentView.findViewById(R.id.fabButton);
        newNoteButton.setSize(FloatingActionButton.SIZE_MINI);
        newNoteButton.setColor(Color.RED);
        // NOTE invoke this method after setting new values!
        newNoteButton.initBackground();
        final Activity currentActivity = getActivity();

        newNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewNote(mGoogleMap.getCameraPosition().target, currentActivity);
            }
        });
    }

    protected void addNewNote(LatLng latLng, Activity currentActivity) {
        NoteInfo note = null;
        if (!mNotesRepository.Notes.containsKey(latLng)) {
            Address address = NotesRepository.getAddressFromLatLng(mGeocoder, latLng);
            note = new NoteInfo()
                    .LatLng(latLng)
                    .Address(address);
            mNotesRepository.Notes.put(latLng, note);
        }

        note = mNotesRepository.Notes.get(latLng);

        LaunchNoteViewActivity(note, this.getActivity(), this);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void setUpNotesRepository() {
        SharedPreferences settings = getActivity().getSharedPreferences(Constants.PREFS_NOTES, 0);
        String settingJson = settings.getString(Constants.PREFS_NOTES_VALUES_JSON, "");

        mNotesRepository = new NotesRepository(this.mGeocoder);
        mNotesRepository.deserializeFromJson(settingJson);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mFragment = (SupportMapFragment) fm.findFragmentById(R.id.mapHolder);
        if (mFragment == null) {
            mFragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.mapHolder, mFragment).commit();
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mGoogleMap} is not null.
     */
    protected void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mGoogleMap == null) {
            mGoogleMap = ((SupportMapFragment)mFragment).getMap();
            // Check if we were successful in obtaining the map.
            if (mGoogleMap != null) {
                setUpMap();
            }
        }
    }

    protected void setUpMap() {
        final Activity currentActivity = getActivity();
        final Fragment currentFragment = this;

        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);

        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setBuildingsEnabled(true);
        final int defaultMapType = mGoogleMap.getMapType();

        this.addMarkersFromNotes();

        mGoogleMap.setOnInfoWindowClickListener(
                new GoogleMap.OnInfoWindowClickListener() {
                    public void onInfoWindowClick(Marker marker) {
                        LatLng position = marker.getPosition();
                        NoteInfo noteInfo = mNotesRepository.Notes.get(position);

                        LaunchNoteViewActivity(noteInfo, currentActivity, currentFragment);
                    }
                }
        );

        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            private float currentZoom = 25;

            @Override
            public void onCameraChange(CameraPosition pos) {
                if (pos.zoom < 17)  {
                    if (mGoogleMap.getMapType()!= defaultMapType)
                        mGoogleMap.setMapType(defaultMapType);
                } else {
                    if (!mSettings.shouldShowDetailViewWhenZoomedIn()) {
                        return;
                    }
                    if (mGoogleMap.getMapType()!= GoogleMap.MAP_TYPE_HYBRID)
                        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
            }
        });

        LayoutInflater layoutInflater = getLayoutInflater(mSavedInstanceState);

        mGoogleMap.setInfoWindowAdapter(new NoteInfoWindowAdapter(layoutInflater, this.mNotesRepository));

        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                addNewNote(latLng, currentActivity);
            }
        });
    }

    public static void LaunchNoteViewActivity(NoteInfo noteInfo, Activity currentActivity, Fragment currentFragment) {
        Intent myIntent = new Intent(currentActivity, NoteViewActivity.class);
        myIntent.putExtra("noteInfoExtra", noteInfo); //Optional parameters
        if (currentFragment != null) {
            currentFragment.startActivityForResult(myIntent, Constants.ACTIVITY_NOTE_VIEW);
        } else {
            currentActivity.startActivityForResult(myIntent, Constants.ACTIVITY_NOTE_VIEW);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the request went well (OK) and the request was ACTIVITY_NOTE_VIEW
        if (requestCode == Constants.ACTIVITY_NOTE_VIEW) {
            NoteInfo noteInfo = null;

            switch (resultCode) {
                case Constants.RESULT_SAVE_NOTE:
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

            // lets remember the changes
            commitNotes();
        }
    }

    protected void addMarkersFromNotes() {
        for (NoteInfo note: mNotesRepository.Notes.values()) {
            addNoteMarkerToMap(note);
        }
    }

    protected void addNoteMarkerToMap(NoteInfo note) {
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

    protected void removeNoteMarkerFromMap(NoteInfo noteInfo) {
        Marker marker = mMarkers.get(noteInfo.getLatLng());
        if (marker != null) {
            // the marker might not exist if we are trying to delete a note that
            // has not yet been created and saved
            marker.remove();
        }
    }

    protected void checkAndHandleLocationIntent() {
        Intent intent = getActivity().getIntent();
        Uri geoData = intent.getData();
        if (geoData!= null)
        {
            this.mGeoIntentReceived = true;
            String placeQueryPrefix = "geo:0,0?q=";
            if (geoData.toString().startsWith(placeQueryPrefix)) {
                //geo:0,0?q=street+address
                String locationName = geoData.toString().substring(placeQueryPrefix.length());
                Address address = NotesRepository.getAddressFromLocationName(this.mGeocoder, locationName);
                moveMapCameraToLocation(new LatLng(address.getLatitude(), address.getLongitude()));
            }
        }
    }
}
