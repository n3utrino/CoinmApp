package ch.wootbarrel.coinmapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;

/**
 * Created by n3utrino on 19.12.13.
 * <p/>
 * map fragment with initialization code
 */
public class CoinmapFragment extends MapFragment {

    private MapEntryManager mapEntryManager;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);


        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mapEntryManager = new MapEntryManager(inflater.getContext());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                setUpMap();
            }
        }.execute();


        return view;
    }

    private void setUpMap() {

        if (getMap() != null) {
            UiSettings settings = getMap().getUiSettings();
            settings.setMyLocationButtonEnabled(true);
            getMap().setMyLocationEnabled(true);
            mapEntryManager.updateMap(getMap());
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapEntryManager != null)
            mapEntryManager.dispose();
    }
}