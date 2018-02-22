package utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by bunny on 03/10/17.
 */

public class SqlDatabaseHelper extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "NewsManager";

    // Contacts table name
    private static final String TABLE_READ_FEEDS = "readfeeds";
    private static final String TABLE_SAVED_FEED = "savedfeeds";


    // Contacts Table Columns names
    private static final String KEY_NEWSID = "newsID";
    private static final String KEY_HEADING = "heading";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_IMAGE_URL = "imageurl";
    private static final String KEY_LINK = "link";
    private static final String KEY_TIME_IN_MILLIS = "time";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_TOPIC = "topic";


    public SqlDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATEREAD_FEED = "CREATE TABLE " + TABLE_READ_FEEDS + "("
                + KEY_NEWSID + " TEXT PRIMARY KEY,"
                + KEY_LINK + " TEXT,"
                + KEY_HEADING + " TEXT" +
                ")";


        String CREATESAVED_FEED = "CREATE TABLE " + TABLE_SAVED_FEED + "("
                + KEY_NEWSID + " TEXT PRIMARY KEY,"
                + KEY_HEADING + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_IMAGE_URL + " TEXT,"
                + KEY_LINK + " TEXT,"
                + KEY_TIME_IN_MILLIS + " INTEGER,"
                + KEY_SOURCE + " TEXT,"
                + KEY_TOPIC + " TEXT" +
                ")";


        db.execSQL(CREATEREAD_FEED);
        db.execSQL(CREATESAVED_FEED);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READ_FEEDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVED_FEED);

        // Create tables again
        onCreate(db);
    }

    // Adding new Feed
    public void addReadNews(News news) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_LINK, news.getNewsSourceLink());
        values.put(KEY_HEADING, news.getNewsTitle());
        values.put(KEY_NEWSID, news.getNewsID());

        try {
            // Inserting Row
            db.insert(TABLE_READ_FEEDS, null, values);
            db.close(); // Closing database connection

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    public boolean getNewsReadStatus(News news) {

        try {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_READ_FEEDS, new String[]{KEY_NEWSID,
                            KEY_LINK}, KEY_NEWSID + "=?",
                    new String[]{news.getNewsID()}, null, null, null, null);

            if (cursor == null) {

                return false;
            } else {

                if (cursor.moveToFirst()) {
                    cursor.close();
                    return true;
                } else {
                    cursor.close();
                    return false;
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean getNewsBookMarkStatus(News news) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();


            Cursor cursor = db.query(TABLE_SAVED_FEED, new String[]{KEY_NEWSID,
                            KEY_LINK}, KEY_NEWSID + "=?",
                    new String[]{news.getNewsID()}, null, null, null, null);

            if (cursor == null) {

                return false;
            } else {

                if (cursor.moveToFirst()) {
                    cursor.close();
                    return true;
                } else {
                    cursor.close();
                    return false;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    public void addSavedNews(News news) {

        if (getNewsBookMarkStatus(news)) {

            deleteSavedNote(news);
            return;
        }

        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues values = new ContentValues();
        values.put(KEY_LINK, news.getNewsSourceLink());
        values.put(KEY_HEADING, news.getNewsTitle());

        try {
            values.put(KEY_NEWSID, news.getNewsID());

            values.put(KEY_DESCRIPTION, news.getNewsDescription());
            values.put(KEY_IMAGE_URL, news.getNewsImageURL());

            values.put(KEY_SOURCE, news.getNewsSource());
            values.put(KEY_TIME_IN_MILLIS, news.getTimeInMillis());
            values.put(KEY_TOPIC, news.getNewsTopic());


            // Inserting Row
            long i = db.insert(TABLE_SAVED_FEED, null, values);
            Log.d("TAG", "addSavedNews: " + i);
            db.close(); // Closing database connection

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void deleteSavedNote(News news) {
        SQLiteDatabase db = this.getWritableDatabase();


        db.delete(TABLE_SAVED_FEED, KEY_NEWSID + " =?", new String[]{news.getNewsID()});
        db.close();


    }

    public ArrayList<News> getAllSavedNotes() {

        ArrayList<News> newsArrayList = new ArrayList<News>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_SAVED_FEED;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                News news = new News();
                news.setNewsID(cursor.getString(0));
                news.setNewsTitle(cursor.getString(1));
                news.setNewsDescription(cursor.getString(2));
                news.setNewsImageURL(cursor.getString(3));
                news.setNewsSourceLink(cursor.getString(4));
                news.setTimeInMillis(cursor.getLong(5));
                news.setNewsSource(cursor.getString(6));
                news.setNewsTopic(cursor.getString(7));


                // Adding contact to list
                newsArrayList.add(news);
            } while (cursor.moveToNext());


        }

        return newsArrayList;
    }


}
