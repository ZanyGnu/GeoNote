package geonote.app.Fragments;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
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
import java.util.Locale;

import geonote.app.Activity.NoteViewActivity;
import geonote.app.Constants;
import geonote.app.Note.NoteInfo;
import geonote.app.NoteInfoWindowAdapter;
import geonote.app.Note.NotesManager;
import geonote.app.Note.NotesRepository;
import geonote.app.R;
import geonote.app.Services.LocationListenerService;
import geonote.app.Settings;

public class MapViewFragment
        extends BaseFacebookHandlerFragment {

    private OnFragmentInteractionListener mListener;

    protected GoogleMap mGoogleMap;
    protected NotesRepository mNotesRepository;
    protected NotesManager mNotesManager;
    protected Geocoder mGeocoder;
    protected FloatingActionButton newNoteButton;
    protected HashMap<LatLng, Marker> mMarkers = new HashMap<>();
    protected NotificationManager mNotificationManager = null;
    protected boolean mGeoIntentReceived;

    protected Bundle mSavedInstanceState;
    private View mCurrentView;
    private Fragment mFragment;
    private Settings mSettings;

    private ServiceConnection mConnection;
    private LocationListenerService mService;
    private boolean mBound = false;

    private boolean mapZoomedIn = false;

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

        setupServiceConnection();

        this.mSettings = new Settings(this.getActivity());

        this.mSavedInstanceState = savedInstanceState;

        this.mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        this.mGeocoder = new Geocoder(getActivity().getBaseContext(), Locale.getDefault());
    }

    private void setupServiceConnection() {
        /** Defines callbacks for service binding, passed to bindService() */
        this.mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocationListenerService.LocationListenerBinder binder = (LocationListenerService.LocationListenerBinder) service;
                binder.mOnLocationChangedListener = new LocationListenerService.OnLocationChangedListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (!mapZoomedIn)
                        {
                            if (mGoogleMap != null) {
                                Log.d("onLocationChanged", "Found last location as " + location.toString());
                                moveMapCameraToLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                                mapZoomedIn = true;
                            }
                        }
                    }
                };

                mService = binder.getService();
                mBound = true;
                if (!mapZoomedIn)
                {
                    Location lastLocation = mService.getLastLocation();

                    if (lastLocation != null && mGoogleMap != null) {
                        Log.d("SetupMap", "Found last location as " + lastLocation.toString());
                        moveMapCameraToLocation(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
                        mapZoomedIn = true;
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mCurrentView = inflater.inflate(R.layout.fragment_map_view, container, false);

        setupNewNoteButton();

        setUpNotesRepository();

        checkAndHandleLocationIntent();

        return mCurrentView;
    }

    @Override
    protected void onSessionStateChange(Session session, SessionState state, Exception exception){
        final TextView txtUserDetails = (TextView) mCurrentView.findViewById(R.id.mapViewLoggedInUser);
        if (session != null && session.isOpened()) {
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user,
                                        Response response) {
                    if (user != null) {
                        txtUserDetails.setText("Logged in as " + user.getName());
                        System.out.println("onSessionStateChange: LoadNotes: session is open. username:"+user.getName());
                    }
                }
            });
            Request.executeBatchAsync(request);
        } else if (session.isClosed()) {
            txtUserDetails.setText("");
            System.out.println("onSessionStateChange: LoadNotes: session was closed.");
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

        bindToLocationListener();

        setUpMapIfNeeded();
    }

    @Override
    public void onStop() {
        super.onStop();

        unBindFromLocationListener();

        commitNotes();
    }

    protected void bindToLocationListener() {
        Intent intent = new Intent(this.getActivity(), LocationListenerService.class);
        this.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void unBindFromLocationListener() {
        // Unbind from the service
        if (mBound) {
            this.getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    private void commitNotes() {
        mNotesManager.commitNotes(this.getActivity(), this.mNotesRepository, this.getLoggedInUsername());
    }

    protected void setUpNotesRepository() {
        mNotesRepository = NotesRepository.Instance;

        mNotesManager = new NotesManager();
        NotesManager.loadNotesFromLocalStore(getActivity(), mNotesRepository);

        mNotesRepository.mNotesModifiedListener = new NotesRepository.NotesModifiedListener() {
            @Override
            public void OnNotesModified() {
                // every time the note list is modified, re-add the markers.
                removeNoteMarkersFromMap();
                addMarkersFromNotes();
            }
        };
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

    protected void moveMapCameraToLocation(LatLng latLng) {
        Log.d("MoveMap", " Moving to " + latLng.latitude + ", " + latLng.longitude);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1000, null);
    }

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
            mNotesRepository.NotifyNotesModified();
        }

        note = mNotesRepository.Notes.get(latLng);

        LaunchNoteViewActivity(note, this.getActivity(), this);
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
                    this.mNotesRepository.NotifyNotesModified();

                    // add the note to the map
                    addNoteMarkerToMap(mMarkers, mGoogleMap, noteInfo);
                    break;

                case Constants.RESULT_DELETE_NOTE:
                    noteInfo = data.getParcelableExtra("result");
                    this.mNotesRepository.Notes.remove(noteInfo.getLatLng());
                    this.mNotesRepository.NotifyNotesModified();
                    removeNoteMarkerFromMap(noteInfo);
            }

            // lets remember the changes
            commitNotes();
        }
    }

    protected void addMarkersFromNotes() {
        for (NoteInfo note: mNotesRepository.Notes.values()) {
            addNoteMarkerToMap(mMarkers, mGoogleMap, note);
        }
    }

    protected void removeNoteMarkersFromMap() {
        for (Marker marker: mMarkers.values()) {
            marker.remove();
        }

        mMarkers.clear();
    }

    protected static void addNoteMarkerToMap(HashMap<LatLng, Marker> mMarkers, GoogleMap mGoogleMap, NoteInfo note) {
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
            mMarkers.remove(marker);
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
