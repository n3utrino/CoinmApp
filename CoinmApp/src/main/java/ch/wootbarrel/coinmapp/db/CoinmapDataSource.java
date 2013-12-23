package ch.wootbarrel.coinmapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ch.wootbarrel.coinmapp.MapEntry;
import ch.wootbarrel.coinmapp.R;

/**
 * Created by n3utrino on 20.12.13.
 * <p/>
 * datasource to the map entities
 */
public class CoinmapDataSource {


    private static String[] allColumns = {CoinmapDbHelper.COLUMN_ID, CoinmapDbHelper.COLUMN_CITY, CoinmapDbHelper.COLUMN_ADDR, CoinmapDbHelper.COLUMN_PHONE,
            CoinmapDbHelper.COLUMN_TITLE, CoinmapDbHelper.COLUMN_LON, CoinmapDbHelper.COLUMN_LAT,
            CoinmapDbHelper.COLUMN_WEB, CoinmapDbHelper.COLUMN_ICON, CoinmapDbHelper.COLUMN_OSM_ID};
    private SQLiteDatabase database;
    private CoinmapDbHelper helper;

    private Gson gson = new Gson();


    public CoinmapDataSource(Context ctx) {
        helper = new CoinmapDbHelper(ctx);

        open();
        if (getAllEntries().isEmpty()) {
            initializeDb(ctx);
        }
        close();
    }

    private void initializeDb(Context ctx) {
        //TODO: notify user
        StringBuilder builder = new StringBuilder();

        BufferedReader is = null;
        try {
            is = new BufferedReader(new InputStreamReader(ctx.getResources().openRawResource(R.raw.data), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String line;
        try {
            assert is != null;
            while (null != (line = is.readLine())) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Type collectionType = new TypeToken<List<MapEntry>>() {
        }.getType();
        List<MapEntry> mapEntries = gson.fromJson(builder.toString(), collectionType);
        for (MapEntry entry : mapEntries) {
            this.insertMapEntry(entry);
        }


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
        cursor.moveToFirst();

        String icon = cursor.getString(0);
        int count = cursor.getInt(1);

        return count + " " + icon;

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
