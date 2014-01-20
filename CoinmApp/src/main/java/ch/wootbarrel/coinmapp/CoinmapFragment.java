package ch.wootbarrel.coinmapp;

import android.app.ProgressDialog;
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
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);


        progressDialog = new ProgressDialog(inflater.getContext());
        progressDialog.setMessage("Initializing");


        new AsyncTask<Void, String, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {

                mapEntryManager = new MapEntryManager(inflater.getContext(), progressDialog);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                setUpMap();
                progressDialog.hide();

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