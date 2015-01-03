package geonote.app;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Ajay on 1/1/2015.
 */
public class NotesRepository {

    static final LatLng MELBOURNE = new LatLng(-37.813, 144.962);
    static final LatLng VICTORS = new LatLng(47.673988, -122.121512);
    static final LatLng ADDRESS_14714 = new LatLng(47.735090, -122.159111);
    static final LatLng ADDRESS_14711 = new LatLng(47.734796, -122.159598);

    public HashMap<LatLng, NoteInfo> Notes = new HashMap<LatLng, NoteInfo>();

    private Geocoder geocoder;

    public NotesRepository(Geocoder geocoder)
    {
        this.geocoder = geocoder;

        this.Notes.put(VICTORS, new NoteInfo()
                .LatLng(VICTORS)
                .Address(getAddressFromLatLng(VICTORS))
                .AddNote("Remember to ask for extra hot")
                .AddNote("Ask for cups explicitly"));

        this.Notes.put(ADDRESS_14711, new NoteInfo()
                .LatLng(ADDRESS_14711)
                .Address(getAddressFromLatLng(ADDRESS_14711))
                .AddNote("Remember you are at home")
                .AddNote("Need to do something."));

        this.Notes.put(ADDRESS_14714, new NoteInfo()
                .LatLng(ADDRESS_14714)
                .Address(getAddressFromLatLng(ADDRESS_14714))
                .AddNote("Ask them to come home for a party?"));

    }

    private Address getAddressFromLatLng(LatLng latLng)
    {
        if (this.geocoder == null)
        {
            return null;
        }

        List<Address> addresses;

        try {
            addresses = this.geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses.size() > 0) {
                return addresses.get(0);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
