package geonote.app.Note;

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

    public NotesModifiedListener mNotesModifiedListener;
    public static interface NotesModifiedListener {
        void OnNotesModified();
    }

    static final LatLng VICTORS = new LatLng(47.673988, -122.121512);

    public static NotesRepository Instance;
    public HashMap<LatLng, NoteInfo> Notes = new HashMap<LatLng, NoteInfo>();
    public Integer NotesVersion = 0;
    private Geocoder geocoder;

    public NotesRepository(Geocoder geocoder)
    {
        this.geocoder = geocoder;
    }

    public static void SetInstance(NotesRepository instance) throws Exception {
        if (Instance == null) {
            Instance = instance;
        } else {
            throw new Exception("NotesRepository has already been initialized.");
        }
    }

    public void populateDefaultValues() {
        this.Notes.put(VICTORS, new NoteInfo()
                .LatLng(VICTORS)
                .Address(getAddressFromLatLng(VICTORS))
                .AddNote("Remember to ask for extra hot"));
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

    public void deserializeFromJson(String jsonString, Integer version)
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

        this.NotesVersion = version;
    }

    public ArrayList<NoteInfo> getNotes()
    {
        return new ArrayList<>(this.Notes.values());
    }

    public void NotifyNotesModified()
    {
        if(mNotesModifiedListener != null) {
            mNotesModifiedListener.OnNotesModified();
        }
    }
}
