package geonote.app.GooglePlaces.Model;

import java.io.Serializable;

import com.google.api.client.util.Key;

public class PlaceDetails implements Serializable {

    @Key
    public String status;

    @Key
    public Place result;

    @Override
    public String toString() {
        if (result!=null) {
            return result.toString();
        }
        return super.toString();
    }
}