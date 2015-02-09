package geonote.app;

import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    }

    public void populateDefaultValues() {
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
        return NotesRepository.getAddressFromLatLng(this.geocoder, latLng);
    }

    public static Address getAddressFromLatLng(Geocoder geocoder, LatLng latLng) {

        if (geocoder == null)
        {
            return null;
        }

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses.size() > 0) {
                return addresses.get(0);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Address getAddressFromLocationName(Geocoder geocoder, String address) {

        if (geocoder == null)
        {
            return null;
        }

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocationName(address, 1);
            if (addresses.size() > 0) {
                return addresses.get(0);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String serializeToJson()
    {
        GsonBuilder builder = new GsonBuilder();

        Gson gson = builder.enableComplexMapKeySerialization().setPrettyPrinting().create();
        Type type = new TypeToken<HashMap<LatLng, NoteInfo>>(){}.getType();
        String json = gson.toJson(this.Notes, type);
        System.out.println(json);

        return json;
    }

    public void deserializeFromJson(String jsonString)
    {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.enableComplexMapKeySerialization().setPrettyPrinting().create();
        Type type = new TypeToken<HashMap<LatLng, NoteInfo>>(){}.getType();

        HashMap<LatLng, NoteInfo> notes = gson.fromJson(jsonString, type);

        if (notes != null)
        {
            this.Notes = notes;
        }
        else
        {
            this.populateDefaultValues();
        }
    }

    public ArrayList<NoteInfo> getNotes()
    {
        return new ArrayList<>(this.Notes.values());
    }
}
