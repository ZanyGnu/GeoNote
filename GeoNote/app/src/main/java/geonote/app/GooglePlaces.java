package geonote.app;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import geonote.app.Model.Place;
import geonote.app.Model.PlaceDetails;
import geonote.app.Model.PlacesList;

public class GooglePlaces {

    // Create our transport.
    private static final HttpTransport transport = new ApacheHttpTransport();

    private static final JacksonFactory jacksonFactory = new JacksonFactory();

    private String apiKey = "";

    // The different Places API endpoints.
    private static final String PLACES_SEARCH_URL =  "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";

    private static final boolean PRINT_AS_STRING = false;

    public GooglePlaces(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public PlacesList searchForPlaces(LatLng location, double radius) throws Exception {
        try {
            System.out.println("Perform Search ....");
            HttpRequestFactory httpRequestFactory = createRequestFactory(transport);
            HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_SEARCH_URL));

            request.getUrl().put("key", this.apiKey);
            request.getUrl().put("location", location.latitude + "," + location.longitude);
            request.getUrl().put("radius", radius);
            request.getUrl().put("sensor", "false");

            if (PRINT_AS_STRING) {
                System.out.println(request.execute().parseAsString());
            } else {
                PlacesList places = request.execute().parseAs(PlacesList.class);
                System.out.println("STATUS = " + places.status);

                for (Place place : places.results) {
                    System.out.println(place);
                }

                return places;
            }
        } catch (HttpResponseException e) {
            System.err.println(e.getStatusMessage());
            throw e;
        }

        return null;
    }

    public PlaceDetails getPlaceDetails(Place place) throws Exception {
        try {
            System.out.println("Perform Place Detail....");
            HttpRequestFactory httpRequestFactory = createRequestFactory(transport);
            HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_DETAILS_URL));

            request.getUrl().put("key", this.apiKey);
            request.getUrl().put("reference", place.reference);
            request.getUrl().put("sensor", "false");

            if (PRINT_AS_STRING) {
                System.out.println(request.execute().parseAsString());
            } else {
                PlaceDetails placeDetails = request.execute().parseAs(PlaceDetails.class);
                System.out.println(placeDetails);
                return placeDetails;
            }

        } catch (HttpResponseException e) {
            System.err.println(e.getStatusMessage());
            throw e;
        }

        return null;
    }

    public static HttpRequestFactory createRequestFactory(final HttpTransport transport) {

        return transport.createRequestFactory(new HttpRequestInitializer() {
            public void initialize(HttpRequest request) {
                JsonObjectParser parser = new JsonObjectParser(jacksonFactory);
                request.setParser(parser);
            }
        });
    }
}
