package geonote.app;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.LocationListener;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.location.LocationManager;
import android.location.Location;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MapsActivity extends FragmentActivity {

    private GoogleMap googleMap; // Might be null if Google Play services APK is not available.
    private NotesRepository notesRepostiory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpNotesRepository();
        setUpMapIfNeeded();
    }

    private void setUpNotesRepository() {
        notesRepostiory = new NotesRepository();
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
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
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
        //addLocations(locationManager, provider);
        addMarkersFromNotes(locationManager, provider);


        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1000, null);

        LocationListener locationListener = new CustomLocationListener(getApplicationContext());

        locationManager.requestLocationUpdates(
                provider, 5000, 10, locationListener);

        //LocationSelectionOverlay.MapOverlay mapOverlay = new LocationSelectionOverlay.MapOverlay();
        //List<Overlay> listOfOverlays = mapView.getOverlays();
        //listOfOverlays.clear();
        //listOfOverlays.add(mapOverlay);

        //mapView.invalidate();
    }

    private void addLocations(LocationManager locationManager, String provider) {
        // Getting Current Location
        List<LatLng> locations = this.getLocations(locationManager, provider);

        int locationNum = 0;

        for (LatLng latLng : locations)
        {
            Address address = getAddressFromLatLng(this.getBaseContext(), latLng);

            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .draggable(true)
                    .flat(true)
                    .title("Location #" + ++locationNum)
                    .snippet(address.toString()));

            // Showing the current location in Google Map
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(LKG_CURRENT_LOCATION));
        }
    }

    static final LatLng MELBOURNE = new LatLng(-37.813, 144.962);
    static final LatLng VICTORS = new LatLng(47.673988, -122.121512);
    static final LatLng ADDRESS_14714 = new LatLng(47.735090, -122.159111);

    static LatLng LKG_CURRENT_LOCATION = new LatLng(47.734796, -122.159598);

    private void addMarkersFromNotes(LocationManager locationManager, String provider) {

        int locationNum = 0;
        for (NoteInfo note: notesRepostiory.Notes) {

            String notesText = "";
            for(String noteText: note.getNotes())
            {
                notesText += noteText + "\n";
            }

            googleMap.addMarker(new MarkerOptions()
                    .position(note.getLatLng())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .draggable(true)
                    .flat(true)
                    .title("Location #" + ++locationNum)
                    .snippet(notesText));

            // Showing the current location in Google Map
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(LKG_CURRENT_LOCATION));
        }
    }

    private ArrayList<LatLng> getLocations(LocationManager locationManager, String provider) {
        // Getting Current Location
        ArrayList<LatLng> locations = new ArrayList<LatLng>();

        Location currentLocation = locationManager.getLastKnownLocation(provider);
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        LKG_CURRENT_LOCATION = latLng;

        locations.add(latLng);

        // Add other locations
        locations.add(MELBOURNE);
        locations.add(VICTORS);
        locations.add(ADDRESS_14714);

        return locations;
    }

    private Address getAddressFromLatLng(Context baseContext, LatLng latLng)
    {
        Geocoder gcd = new Geocoder(baseContext, Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = gcd.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses.size() > 0) {
                return addresses.get(0);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public class NoteInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
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
