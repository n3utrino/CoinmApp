package ch.wootbarrel.coinmapp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Created by n3utrino on 19.12.13.
 *
 * displays the about screen in a webview
 */
public class AboutFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.about, container, false);

        WebView webView = (WebView) view.findViewById(R.id.webview);

        webView.loadUrl("file:///android_asset/about.html");

        return view;
    }
}