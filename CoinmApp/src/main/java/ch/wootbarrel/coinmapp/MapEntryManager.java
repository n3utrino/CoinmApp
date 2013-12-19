package ch.wootbarrel.coinmapp;

import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by n3utrino on 19.12.13.
 * <p/>
 * Holds and manages the map entries and the MapMarkers
 */
public class MapEntryManager implements GoogleMap.OnCameraChangeListener, GoogleMap.OnMapLoadedCallback {


    private List<MapEntry> entries;
    private List<Marker> markers = new ArrayList<Marker>();
    private List<Circle> circles = new ArrayList<Circle>();
    private Gson gson = new Gson();
    private Context context;
    private GoogleMap map;
    private Map<LatLngBounds, List<MapEntry>> buckets;
    private float currentZoom;


    public MapEntryManager(Context ctx, GoogleMap map) {


        this.context = ctx;
        this.map = map;

        map.setOnCameraChangeListener(this);
        map.setOnMapLoadedCallback(this);

        StringBuilder builder = new StringBuilder();

        BufferedReader is = null;
        try {
            is = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.data), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String line;
        try {
            while (null != (line = is.readLine())) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Type collectionType = new TypeToken<List<MapEntry>>() {
        }.getType();
        entries = gson.fromJson(builder.toString(), collectionType);


    }

    private LatLng latLngFromDistanceAndBearing(LatLng start, double distance, double bearing) {
        double R = 6371;

        double lat1 = Math.toRadians(start.latitude);
        double lon1 = Math.toRadians(start.longitude);


        bearing = Math.toRadians(bearing);


        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / R) +
                Math.cos(lat1) * Math.sin(distance / R) * Math.cos(bearing));
        double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(distance / R) * Math.cos(lat1),
                Math.cos(distance / R) - Math.sin(lat1) * Math.sin(lat2));

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        if (currentZoom == cameraPosition.zoom) {

            for (Marker marker : markers) {
                marker.setVisible(map.getProjection().getVisibleRegion().latLngBounds.contains(marker.getPosition()));
            }

            for (Circle circle : circles) {
                circle.setVisible(map.getProjection().getVisibleRegion().latLngBounds.contains(circle.getCenter()));
            }

            return;
        }


        currentZoom = cameraPosition.zoom;

        double bucketSizePixel = 40;
        double worldPixels = 265 * Math.pow(2, cameraPosition.zoom);
        double bucketSizeDistance = bucketSizePixel * 40000 / worldPixels;

        buckets = new HashMap<LatLngBounds, List<MapEntry>>();

        for (Marker marker : markers) {
            marker.remove();
        }

        for (Circle circle : circles) {
            circle.remove();
        }

        markers.clear();
        circles.clear();

        for (MapEntry entry : entries) {
            LatLng entryLatLng = new LatLng(entry.lat, entry.lon);
            boolean isAdded = false;
            for (LatLngBounds bounds : buckets.keySet()) {
                if (bounds.contains(entryLatLng)) {
                    buckets.get(bounds).add(entry);
                    isAdded = true;
                }
            }

            if (!isAdded) {
                LatLng southWest = latLngFromDistanceAndBearing(entryLatLng, bucketSizeDistance / 2, 360 - 135);
                LatLng northEast = latLngFromDistanceAndBearing(entryLatLng, bucketSizeDistance / 2, 45);
                List<MapEntry> mapEntries = new ArrayList<MapEntry>();
                buckets.put(new LatLngBounds(southWest, northEast), mapEntries);
                mapEntries.add(entry);
            }


        }

        //TODO: better marker image
        for (List<MapEntry> mapEntries : buckets.values()) {
            if (mapEntries.size() == 1) {
                MapEntry mapEntry = mapEntries.get(0);
                LatLng markerCenter = new LatLng(mapEntry.lat, mapEntry.lon);


                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.food_pub);
                markers.add(map.addMarker(new MarkerOptions().visible(map.getProjection().getVisibleRegion().latLngBounds.contains(markerCenter)).anchor(0.5f, 0.5f).icon(icon).position(markerCenter).title(mapEntry.title)));
            } else if (mapEntries.size() > 1) {

                MapEntry mapEntry = mapEntries.get(0);
                LatLng markerCenter = new LatLng(mapEntry.lat, mapEntry.lon);

                //Todo make custom bitmap with circle and number of venues in it.

                circles.add(map.addCircle(
                        new CircleOptions()
                                .center(markerCenter)
                                .radius(bucketSizeDistance * 250)
                                .visible(map.getProjection().getVisibleRegion().latLngBounds.contains(markerCenter))
                                .strokeWidth(4)
                                .fillColor(0xAAFFD700)
                                .strokeColor(Color.YELLOW)));
            }
        }

    }

    @Override
    public void onMapLoaded() {


    }

    public List<MapEntry> getEntries() {
        return entries;
    }

}
