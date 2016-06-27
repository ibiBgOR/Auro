package com.architjn.acjmusicplayer.utils.handlers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.architjn.acjmusicplayer.task.FetchAlbum;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by architjn on 15/12/15.
 */
public abstract class AlbumImgHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "AlbumDB";
    private static final String TABLE_ALBUM = "album";
    private static final String ALBUM_KEY_ID = "album_id";
    private static final String ALBUM_KEY_NAME = "album_name";
    private static final String ARTIST_KEY_NAME = "artist_name";
    private static final String ALBUM_KEY_URL = "album_img";
    private static final String IMAGE_QUALITY = "image_quality";
    private final Integer[] randomNumbers;
    private Context context;

    public AlbumImgHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        randomNumbers = randomNumbers(1000);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_PLAYBACK_SONG_TABLE = "CREATE TABLE " + TABLE_ALBUM + " (" +
                ALBUM_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                ALBUM_KEY_NAME + " TEXT," +
                ARTIST_KEY_NAME + " TEXT," +
                ALBUM_KEY_URL + " TEXT," +
                IMAGE_QUALITY + " TEXT)";
        sqLiteDatabase.execSQL(CREATE_PLAYBACK_SONG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUM);
    }

    public String getAlbumArtWork(final String albumName, String artistName, int pos, FetchAlbum.Quality quality) {
        String url = getAlbumImgFromDB(albumName, artistName, quality);
        if (url != null) {
            if ((new File(url)).exists()) {
                return url;
            }
            else {
                removeAlbumImgFromDB(albumName, artistName, quality);
            }
        } else {
            new FetchAlbum(context, albumName, artistName, randomNumbers[pos], quality, this);
        }
        return null;
    }


    public abstract void onDownloadComplete(String url);

    public void updateAlbumArtWorkInDB(String albumName, String artistName, String url, FetchAlbum.Quality quality) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.putNull(ALBUM_KEY_ID);
        values.put(ALBUM_KEY_NAME, albumName);
        values.put(ARTIST_KEY_NAME, artistName);
        values.put(ALBUM_KEY_URL, url);
        values.put(IMAGE_QUALITY, quality.name());
        db.insert(TABLE_ALBUM, null, values);
        db.close();
    }

    public String getAlbumImgFromDB(String albumName, String artistName, FetchAlbum.Quality quality) {
        Cursor cursor = null;
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            String query = "SELECT  * FROM " + TABLE_ALBUM + " WHERE "
                    + ALBUM_KEY_NAME + "='" + albumName.replace("'", "''") + "'" +
                    " AND " + ARTIST_KEY_NAME + "='" + artistName.replace("'", "''") + "'" +
                    " AND " + IMAGE_QUALITY + "='" + quality.name().replace("'", "''") + "'";
            cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                return cursor.getString(3);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public void removeAlbumImgFromDB(String albumName, String artistName, FetchAlbum.Quality quality) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE  FROM " + TABLE_ALBUM + " WHERE "
                + ALBUM_KEY_NAME + "='" + albumName.replace("'", "''") + "'" +
                " AND " + ARTIST_KEY_NAME + "='"  + artistName.replace("'", "''") + "'" +
                " AND " + IMAGE_QUALITY + "='" + quality.name().replace("'", "''") + "'";
        Cursor c = db.rawQuery(query, null);
        db.close();
    }

    public Integer[] randomNumbers(int range) {
        Integer[] arr = new Integer[range];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        Collections.shuffle(Arrays.asList(arr));
        return arr;
    }

}
