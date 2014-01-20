package ch.wootbarrel.coinmapp.db;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import ch.wootbarrel.coinmapp.MapEntry;

/**
 * Created by n3utrino on 20.12.13.
 * <p/>
 * datasource to the map entities
 */
public class CoinmapDataSource {


    public static final String LAST_UPDATE = "last_update";
    public static final String DB_PREFS = "dbPrefs";
    private static String[] allColumns = {CoinmapDbHelper.COLUMN_ID, CoinmapDbHelper.COLUMN_CITY, CoinmapDbHelper.COLUMN_ADDR, CoinmapDbHelper.COLUMN_PHONE,
            CoinmapDbHelper.COLUMN_TITLE, CoinmapDbHelper.COLUMN_LON, CoinmapDbHelper.COLUMN_LAT,
            CoinmapDbHelper.COLUMN_WEB, CoinmapDbHelper.COLUMN_ICON, CoinmapDbHelper.COLUMN_OSM_ID};
    private final SharedPreferences prefs;
    private final ProgressDialog progressDialog;
    private SQLiteDatabase database;
    private CoinmapDbHelper helper;
    private Gson gson = new Gson();


    public CoinmapDataSource(Context ctx, ProgressDialog progressDialog) {

        this.progressDialog = progressDialog;
        helper = new CoinmapDbHelper(ctx);

        prefs = ctx.getSharedPreferences(DB_PREFS, Context.MODE_PRIVATE);

        open();
        if (isUpdateDue()) {
            initializeDb(ctx);
        }
        close();
    }

    private boolean isUpdateDue() {

        long last = prefs.getLong(LAST_UPDATE, 0);
        return System.currentTimeMillis() - last > 1000 * 60 * 60 * 24; // Update every 24H
    }

    private void initializeDb(final Context ctx) {
        final Handler toastHandler = new Handler(Looper.getMainLooper());

        //Initial Import Show Progress bar
        if (getAllEntries().size() == 0) {
            toastHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });

            fetchAndParseData(ctx, toastHandler);

        } else {

            new Thread() {
                @Override
                public void run() {
                    fetchAndParseData(ctx, toastHandler);
                }
            }.start();
        }


    }

    private void updateProgressDialog(final String message) {
        final Handler toastHandler = new Handler(Looper.getMainLooper());

        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(message);
            }
        });
    }

    private void setProgressMax(final int progressMax) {
        final Handler toastHandler = new Handler(Looper.getMainLooper());

        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(progressMax);
            }
        });
    }


    private void setProgress(final int progress) {
        final Handler toastHandler = new Handler(Looper.getMainLooper());

        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgress(progress);
            }
        });
    }

    private void fetchAndParseData(final Context ctx, Handler toastHandler) {
        StringBuilder builder = new StringBuilder();

        BufferedReader is;
        try {

            updateProgressDialog("Fetching Data");
            URL dataUrl = new URL("http://coinmap.org/data/data-overpass-bitcoin.json");
            URLConnection connection = dataUrl.openConnection();
            connection.addRequestProperty("User-Agent", "CoinmApp;1.0;Android");
            is = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            final int lastSize = getAllEntries().size();

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
            updateProgressDialog("Parsing JSON");
            List<MapEntry> mapEntries = gson.fromJson(builder.toString(), collectionType);

            this.clearData();
            updateProgressDialog("Inserting Entries");
            setProgressMax(mapEntries.size());
            int i = 1;
            this.database.beginTransaction();
            for (MapEntry entry : mapEntries) {
                this.insertMapEntry(entry);
                setProgress(i);
                i++;
            }
            this.database.setTransactionSuccessful();
            final int newSize = (getAllEntries().size() - lastSize);

            prefs.edit().putLong(LAST_UPDATE, System.currentTimeMillis()).commit();

            toastHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, "Updating Data. Success - " + newSize + " new shops", Toast.LENGTH_LONG).show();

                }
            });

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            toastHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, "Updating Data. Failed - maybe no connection", Toast.LENGTH_LONG).show();

                }
            });

            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
    }

    private void clearData() {
        database.delete(CoinmapDbHelper.TABLE_MAP_ENTRIES, null, null);
    }

    public void open() throws SQLException {
        database = helper.getWritableDatabase();
    }

    public void close() {
        helper.close();
    }

    public void deleteMapEntry(MapEntry mapEntry) {
        long id = mapEntry.id;
        System.out.println("Comment deleted with id: " + id);
        database.delete(CoinmapDbHelper.TABLE_MAP_ENTRIES, CoinmapDbHelper.COLUMN_ID
                + " = " + id, null);
    }

    public long insertMapEntry(MapEntry mapEntry) {
        ContentValues values = new ContentValues();
        values.put(CoinmapDbHelper.COLUMN_ADDR, mapEntry.addr);
        values.put(CoinmapDbHelper.COLUMN_CITY, mapEntry.city);
        values.put(CoinmapDbHelper.COLUMN_ICON, mapEntry.icon);
        values.put(CoinmapDbHelper.COLUMN_PHONE, mapEntry.phone);
        values.put(CoinmapDbHelper.COLUMN_TITLE, mapEntry.title);
        values.put(CoinmapDbHelper.COLUMN_LAT, mapEntry.lat);
        values.put(CoinmapDbHelper.COLUMN_LON, mapEntry.lon);
        return database.insert(CoinmapDbHelper.TABLE_MAP_ENTRIES, null,
                values);

    }

    public String topCategory() {
        Cursor cursor = database.rawQuery("select icon, count(*) as cnt from " + CoinmapDbHelper.TABLE_MAP_ENTRIES + " where icon <> 'bitcoin' group by icon order by cnt desc", null);
        if (cursor.moveToFirst()) {

            String icon = cursor.getString(0);
            int count = cursor.getInt(1);

            return count + " " + icon;
        }

        return "";

    }

    public List<MapEntry> getAllEntries() {
        List<MapEntry> entries = new ArrayList<MapEntry>();

        Cursor cursor = database.query(CoinmapDbHelper.TABLE_MAP_ENTRIES,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            MapEntry comment = cursorToEntry(cursor);
            entries.add(comment);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return entries;
    }

    private MapEntry cursorToEntry(Cursor cursor) {
        MapEntry entry = new MapEntry();
        entry.id = cursor.getLong(0);
        entry.city = cursor.getString(1);
        entry.addr = cursor.getString(2);
        entry.phone = cursor.getString(3);
        entry.title = cursor.getString(4);
        entry.lon = Double.parseDouble(cursor.getString(5));
        entry.lat = Double.parseDouble(cursor.getString(6));
        entry.web = cursor.getString(7);
        entry.icon = cursor.getString(8);

        return entry;
    }

}
