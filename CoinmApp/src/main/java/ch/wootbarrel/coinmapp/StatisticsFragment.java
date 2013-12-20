package ch.wootbarrel.coinmapp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.wootbarrel.coinmapp.db.CoinmapDataSource;

/**
 * Created by n3utrino on 20.12.13.
 * <p/>
 * Show stats about the data
 */
public class StatisticsFragment extends Fragment {


    private CoinmapDataSource dataSource;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dataSource = new CoinmapDataSource(this.getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.statistics, container, false);
    }
}