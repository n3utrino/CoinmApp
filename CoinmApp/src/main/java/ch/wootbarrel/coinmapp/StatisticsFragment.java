package ch.wootbarrel.coinmapp;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        final View view = inflater.inflate(R.layout.statistics, container, false);

        new AsyncTask<Void, Void, Void>() {

            private int venues = 0;
            private String topCategory = "";

            @Override
            protected Void doInBackground(Void... voids) {
                dataSource = new CoinmapDataSource(StatisticsFragment.this.getActivity());

                dataSource.open();
                venues = dataSource.getAllEntries().size();

                topCategory = dataSource.topCategory();

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                TextView accepting = (TextView) view.findViewById(R.id.numberTxt);

                accepting.setText(Integer.toString(venues));

                TextView top = (TextView) view.findViewById(R.id.categoryTxt);
                top.setText(topCategory);


            }
        }.execute();


        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataSource != null)
            dataSource.open();
    }

    @Override
    public void onPause() {
        super.onPause();
        dataSource.close();
    }
}