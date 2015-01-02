package geonote.app;

/**
 * Created by Ajay on 12/31/2014.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.OverlayItem;

import java.util.ArrayList;
import java.util.Map;

/**
 * An overlay that lets the User select any point on the map.
 *
 */
public class LocationSelectionOverlay extends ItemizedOverlay<OverlayItem> {

    private ArrayList<MapOverlay> mOverlays;

    private Map mMap;

    public LocationSelectionOverlay(Drawable defaultMarker, Map map) {
        super(boundCenterBottom(defaultMarker));
        mOverlays = new ArrayList<MapOverlay>();
        mMap = map;
        populate();
    }

    public void addOverlay(MapOverlay overlay, Drawable marker) {
        //overlay.setMarker(boundCenterBottom(marker));
        mOverlays.add(overlay);
        populate();
    }

    @Override
    protected OverlayItem createItem(int i) {
        //return mOverlays.get(i);
        return null;
    }

    @Override
    public boolean onTap(GeoPoint gp, MapView mv) {
        //mMap.returnCoordinates(gp);
        return true;
    }

    @Override
    public int size() {
        return mOverlays.size();
    }


    public class MapOverlay extends Overlay
    {
        @Override
        public boolean draw(Canvas canvas, MapView mapView,
                            boolean shadow, long when)
        {
            super.draw(canvas, mapView, shadow);

            //---translate the GeoPoint to screen pixels---
            Point screenPts = new Point();
            //mapView.getProjection().toPixels(p, screenPts);

            //---add the marker---
            //Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.notespin);
            //canvas.drawBitmap(bmp, screenPts.x, screenPts.y-50, null);
            return true;
        }
    }
}
