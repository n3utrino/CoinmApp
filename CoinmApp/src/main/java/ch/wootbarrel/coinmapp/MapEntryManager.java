package ch.wootbarrel.coinmapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

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
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.wootbarrel.coinmapp.db.CoinmapDataSource;

/**
 * Created by n3utrino on 19.12.13.
 * <p/>
 * Holds and manages the map entries and the MapMarkers
 */
public class MapEntryManager implements GoogleMap.OnCameraChangeListener, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener {


    public static final int MAX_MAP_ITEMS = 500;
    private List<MapEntry> entries;
    private List<Marker> markers = new ArrayList<Marker>();
    private List<Circle> circles = new ArrayList<Circle>();
    private GoogleMap map;
    private Map<Integer, Map<LatLngBounds, List<MapEntry>>> bucketForZoom = new HashMap<Integer, Map<LatLngBounds, List<MapEntry>>>();
    private float currentZoom;
    private CoinmapDataSource dataSource;
    private VisibleRegion lastVisibleRegion;


    public MapEntryManager(Context ctx, ProgressDialog progressDialog) {

        this.dataSource = new CoinmapDataSource(ctx, progressDialog);
        dataSource.open();
        entries = dataSource.getAllEntries();

    }

    public void updateMap(GoogleMap map) {
        this.map = map;
        map.setOnMarkerClickListener(this);
        dataSource.open();
        map.setOnCameraChangeListener(this);
        map.setOnMapLoadedCallback(this);
        onCameraChange(map.getCameraPosition());
    }

    public void dispose() {
        dataSource.close();
        removeAllMapItems();
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
    public void onCameraChange(final CameraPosition cameraPosition) {

        double bucketSizePixel = 40;
        double worldPixels = 265 * Math.pow(2, cameraPosition.zoom);
        final double bucketSizeDistance = bucketSizePixel * 40000 / worldPixels;

        final VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();

        new AsyncTask<Void, Void, Void>() {

            Map<LatLngBounds, List<MapEntry>> buckets;

            List<MarkerOptions> markerOptionsList = new ArrayList<MarkerOptions>();
            List<CircleOptions> circleOptionsList = new ArrayList<CircleOptions>();

            @Override
            protected Void doInBackground(Void... voids) {

                currentZoom = cameraPosition.zoom;

                if (buckets == null) {
                    buckets = new HashMap<LatLngBounds, List<MapEntry>>();
                    bucketForZoom.put((int) currentZoom, buckets);

                    for (MapEntry entry : entries) {
                        LatLng entryLatLng = new LatLng(entry.lat, entry.lon);
                        if (!visibleRegion.latLngBounds.contains(entryLatLng)) {
                            continue;
                        }

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

                }

                for (List<MapEntry> mapEntries : buckets.values()) {
                    if (mapEntries.size() == 1) {
                        //TODO: better marker image
                        MapEntry mapEntry = mapEntries.get(0);
                        LatLng markerCenter = new LatLng(mapEntry.lat, mapEntry.lon);
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.food_pub);
                        markerOptionsList.add(new MarkerOptions().visible(visibleRegion.latLngBounds.contains(markerCenter))
                                .anchor(0.5f, 0.5f).icon(icon).position(markerCenter).title(mapEntry.title));

                    } else if (mapEntries.size() > 1) {

                        MapEntry mapEntry = mapEntries.get(0);
                        LatLng markerCenter = new LatLng(mapEntry.lat, mapEntry.lon);
                        //Todo make custom bitmap with circle and number of venues in it.
                        circleOptionsList.add(new CircleOptions()
                                .center(markerCenter)
                                .radius(bucketSizeDistance * 250)
                                .visible(visibleRegion.latLngBounds.contains(markerCenter))
                                .strokeWidth(4)
                                .fillColor(0xAAFFD700)
                                .strokeColor(Color.YELLOW));

                    }
                }


                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                removeAllMapItems();


                for (MarkerOptions markerOptions : markerOptionsList) {
                    Marker marker = map.addMarker(markerOptions);
                    markers.add(marker);

                }

                for (CircleOptions circleOptions : circleOptionsList) {
                    circles.add(map.addCircle(circleOptions));
                }


//                int markerIndex = 0;
//                int circleIndex = 0;
//
//                for (List<MapEntry> mapEntries : buckets.values()) {
//
//                    if (mapEntries.size() == 1) {
//
//                        if (markers.size() > markerIndex) {
//                            Marker marker = markers.get(markerIndex);
//                            MapEntry entry = mapEntries.get(0);
//                            LatLng position = new LatLng(entry.lat, entry.lon);
//                            marker.setPosition(position);
//                            marker.setVisible(true);
//                            marker.setTitle(entry.title);
//                            markerIndex++;
//                        }
//                    } else if (mapEntries.size() > 1) {
//                        if (circles.size()>circleIndex) {
//                            Circle circle = circles.get(circleIndex);
//                            MapEntry entry = mapEntries.get(0);
//                            LatLng position = new LatLng(entry.lat, entry.lon);
//                            circle.setRadius(bucketSizeDistance * 250);
//                            circle.setCenter(position);
//                            circle.setVisible(true);
//                            circleIndex++;
//                        }
//                    }
//                }


            }
        }.execute();


        lastVisibleRegion = visibleRegion;

    }

    private void removeAllMapItems() {
        for (Marker marker : markers) {
            marker.remove();
        }

        for (Circle circle : circles) {
            circle.remove();

        }

        markers.clear();
        circles.clear();

    }

    @Override
    public void onMapLoaded() {


    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    public List<MapEntry> getEntries() {
        return entries;
    }

}
