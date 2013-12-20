package ch.wootbarrel.coinmapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by n3utrino on 20.12.13.
 * <p/>
 * Database helper manages the db tables
 */
public class CoinmapDbHelper extends SQLiteOpenHelper {

    public static final String TABLE_MAP_ENTRIES = "mapentries";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CITY = "city";
    public static final String COLUMN_ADDR = "addr";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_WEB = "web";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_OSM_ID = "osm_id";


    public static final String[] fields = {COLUMN_CITY, COLUMN_ADDR, COLUMN_PHONE, COLUMN_TITLE, COLUMN_LON, COLUMN_LAT, COLUMN_WEB, COLUMN_ICON, COLUMN_OSM_ID};


    public static final String DB_NAME = "mapentries.db";
    public static final int VERSION = 1;

    public CoinmapDbHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        StringBuilder sqlBuilder = new StringBuilder("create table ");
        sqlBuilder.append(TABLE_MAP_ENTRIES).append("(" + COLUMN_ID + " integer primary key autoincrement");
        for (String field : fields) {
            sqlBuilder.append(", ").append(field).append(" text");
        }
        sqlBuilder.append(");");

        sqLiteDatabase.execSQL(sqlBuilder.toString());

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(CoinmapDbHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MAP_ENTRIES);
        onCreate(db);
    }
}
