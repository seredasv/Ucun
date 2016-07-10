package com.nasctech.ucun.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Video.db";
    public static final String VIDEO = "video";
    public static final String VIDEO_NAME = "name";
    public static final String VIEWS = "views";
    public static final String REPLAYS = "replays";
    public static final String REVIEWS = "reviews";
    public static final String _ID = "_id";
    public static final String LINK = "link";
    public static final String PATH = "path";
    public static final String PERIOD = "period";
    public static final String START_TIME = "start_time";
    public static final String END_TIME = "end_time";
    public static final String GROUP_ID = "group_id";
    public static final String GROUPS = "groups";
    public static final String VIDEO_ID = "video_id";
    public static final String VIDEO_GROUPS = "video_groups";
    private static final String SQL_CREATE_VIDEO =
            "CREATE TABLE " + VIDEO + " (" +
                    _ID + " INTEGER PRIMARY KEY not null," +
                    VIDEO_NAME + " text, " +
                    VIEWS + " integer, " +
                    REPLAYS + " integer, " +
                    REVIEWS + " text, " +
                    LINK + " text, " +
                    PATH + " text)";
    private static final String SQL_CREATE_GROUPS =
            "CREATE TABLE " + GROUPS + " (" +
                    _ID + " integer primary key not null)";
    private static final String SQL_CREATE_VIDEO_GROUPS =
            "CREATE TABLE " + VIDEO_GROUPS + " (" +
                    VIDEO_ID + " integer not null, " +
                    GROUP_ID + " integer not null, " +
                    "primary key (" + VIDEO_ID + "," + GROUP_ID + ")," +
                    "foreign key (" + VIDEO_ID + ") references " + VIDEO + " (" + _ID + ") ON DELETE CASCADE," +
                    "foreign key (" + GROUP_ID + ") references " + GROUPS + " (" + _ID + ") ON DELETE CASCADE)";
    private static final String SQL_CREATE_PERIOD =
            "CREATE TABLE " + PERIOD + " (" +
                    _ID + " integer primary key not null, " +
                    GROUP_ID + " integer, " +
                    START_TIME + " integer, " +
                    END_TIME + " integer, " +
                    "foreign key (" + GROUP_ID + ") references " + GROUPS + "(" + _ID + ") ON DELETE CASCADE)";
    private static SQLiteDatabase db;
    private static DBHelper instance = null;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static void init(Context context) {
        if (null == instance)
            instance = new DBHelper(context);
    }

    public static SQLiteDatabase getDb() {
        if (null == db)
            db = instance.getWritableDatabase();
        return db;
    }

    public static void deactivate() {
        if (null != db && db.isOpen()) {
            db.close();
            db = null;
        }
        instance = null;
    }

    public static void addVideo(String name, int id, String link, String path) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, id);
        cv.put(VIDEO_NAME, name);
        cv.put(PATH, path);
        cv.put(LINK, link);
        getDb().insertWithOnConflict(VIDEO, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void addPeriod(int periodId, int groupId, int start, int end) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, periodId);
        cv.put(GROUP_ID, groupId);
        cv.put(START_TIME, start);
        cv.put(END_TIME, end);
        getDb().insertWithOnConflict(PERIOD, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void addGroup(int groupId) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, groupId);
        getDb().insertWithOnConflict(GROUPS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void addVideoGroup(int videoId, int groupId) {
        ContentValues cv = new ContentValues();
        cv.put(VIDEO_ID, videoId);
        cv.put(GROUP_ID, groupId);
        getDb().insertWithOnConflict(VIDEO_GROUPS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    //TODO: add check if no videos
    public static void addReplay(int id) {
        Cursor c = getDb().rawQuery("select * from " + VIDEO + " where " + _ID + " = " + id, null);
        if (c.moveToFirst()) {
            int replays = c.getInt(c.getColumnIndex(REPLAYS));
            ContentValues cv = new ContentValues();
            cv.put(REPLAYS, 1 + replays);
            c.close();
            getDb().update(VIDEO, cv, _ID + " = " + id, null);
        }
    }

    public static void addView(int id) {
        Cursor c = getDb().rawQuery("select * from " + VIDEO + " where " + _ID + " = " + id, null);
        if (c.moveToFirst()) {
            int views = c.getInt(c.getColumnIndex(VIEWS));
            ContentValues cv = new ContentValues();
            cv.put(VIEWS, views + 1);
            c.close();
            getDb().update(VIDEO, cv, _ID + " = " + id, null);
        }
    }

    public static String getVideoNameById(int id) {
        Cursor c = getDb().rawQuery("select " + _ID + ", " + VIDEO_NAME + " from " + VIDEO + " where " + _ID + " = " + id, null);
        c.moveToFirst();
        String name = c.getString(c.getColumnIndex(VIDEO_NAME));
        return name;
    }

    public static Cursor getDownloadLinks() {
        return getDb().rawQuery("select " + PATH + "," + LINK + " from " + VIDEO, null);
    }

    public static List<Integer> getAllVideos() {
        Cursor c = getDb().rawQuery("select * from " + VIDEO, null);
        List<Integer> ids = new ArrayList<>();
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex(_ID));
            ids.add(id);
        }
        return ids;
    }

    public static Cursor getAllVideosCursor() {
        return getDb().rawQuery("" +
                "SELECT distinct V._id, G._id g_id, " + VIEWS + ", " + REPLAYS +
                " FROM " + VIDEO + " V, " + GROUPS + " G," + VIDEO_GROUPS + " VG, " + PERIOD + " P\n" +
                "WHERE V._ID = VG.VIDEO_ID\n" +
                "AND g_id = VG.GROUP_ID\n" +
                "AND P.GROUP_ID = g_id\n", null);
    }

    public static List<Integer> getVideoByDates() {
        List<Integer> ids = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000L;
        Cursor c = getDb().rawQuery("" +
                "SELECT distinct V._ID, V.name\n" +
                "FROM " + VIDEO + " V, " + GROUPS + " G," + VIDEO_GROUPS + " VG, " + PERIOD + " P\n" +
                "WHERE V._ID = VG.VIDEO_ID\n" +
                "AND G._ID = VG.GROUP_ID\n" +
                "AND P.GROUP_ID = G._ID\n" +
                "AND " + currentTime + " >= P.START_TIME\n" +
                "AND " + currentTime + " <= P.END_TIME;", null);
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex(_ID));
            ids.add(id);
        }
        c.close();
        return ids;
    }

    public static void deleteViews() {
        ContentValues cv = new ContentValues();
        cv.put(VIEWS, 0);
        cv.put(REPLAYS, 0);
        getDb().update(VIDEO, cv, null, null);
        Log.d("Statistics", "deleted");
    }

    public static void deleteViews(int id) {
        ContentValues cv = new ContentValues();
        cv.put(VIEWS, 0);
        cv.put(REPLAYS, 0);
        getDb().update(VIDEO, cv, _ID + " = " + id, null);
    }

    public static Cursor selectUnused() {
        long currentTime = System.currentTimeMillis() / 1000L;
        Cursor c = getDb().rawQuery("" +
                "SELECT distinct V._id, V.path \n" +
                "FROM " + VIDEO + " V, " + GROUPS + " G," + VIDEO_GROUPS + " VG, " + PERIOD + " P\n" +
                "WHERE V._ID = VG.VIDEO_ID\n" +
                "AND G._ID = VG.GROUP_ID\n" +
                "AND P.GROUP_ID = G._ID\n" +
                "AND (" + currentTime + " <= P.START_TIME\n" +
                "OR " + currentTime + " >= P.END_TIME);", null);
        return c;
    }

    /**
     * deletes all videos from db which are not in current period
     */
    public static void deleteUnused() {
        long currentTime = System.currentTimeMillis() / 1000L;
        getDb().execSQL("delete from " + VIDEO + " where " + _ID + " IN (SELECT distinct V._id \n" +
                "FROM " + VIDEO + " V, " + GROUPS + " G," + VIDEO_GROUPS + " VG, " + PERIOD + " P\n" +
                "WHERE V._ID = VG.VIDEO_ID\n" +
                "AND G._ID = VG.GROUP_ID\n" +
                "AND P.GROUP_ID = G._ID\n" +
                "AND (" + currentTime + " <= P.START_TIME\n" +
                "OR " + currentTime + " >= P.END_TIME));");
    }

    public static void deleteAllVideos() {
        getDb().execSQL("delete from " + VIDEO);
    }

    public static void deletePeriods() {
        getDb().execSQL("delete from " + PERIOD);
    }

    public static Cursor getAllVideosWithPath() {
        Cursor c = getDb().rawQuery("select " + _ID + ", " + PATH + " from " + VIDEO, null);
        return c;
    }

    public static void deleteById(int id) {
        getDb().execSQL("delete from " + VIDEO + " where " + _ID + " = " + id);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_VIDEO);
        db.execSQL(SQL_CREATE_GROUPS);
        db.execSQL(SQL_CREATE_VIDEO_GROUPS);
        db.execSQL(SQL_CREATE_PERIOD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
