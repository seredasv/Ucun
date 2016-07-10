package com.nasctech.ucun.utils;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//TODO move to Retrofit
public class Utils {
    public static String PATH;
    public static int[] spinnerIDs;
    public static String[] spinnerNames;

    public static String SERVER_URL = "http://104.236.104.107";

    public static boolean isEnoughSpace(long size) {
        StatFs stat = new StatFs(PATH);
        long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
        return size < bytesAvailable;
    }

    public static void setPaths(Context c) {
        String secondary = System.getenv("SECONDARY_STORAGE");
        String external = System.getenv("EXTERNAL_STORAGE");
        File f = Environment.getExternalStorageDirectory();
        File f2 = c.getExternalFilesDir(null);
        if (f2 != null && !f2.getAbsolutePath().isEmpty()) {
            PATH = f2.getAbsolutePath();
        } else if (f != null && !f.getAbsolutePath().isEmpty()) {
            PATH = f.getAbsolutePath();
        } else if (external != null && !external.contains(":") && !external.isEmpty()) {
            PATH = external;
        } else if (secondary != null && !secondary.isEmpty()) {
            if (!secondary.contains(":")) {
                PATH = secondary;
            } else {
                String[] secondaryPaths = secondary.split(":");
                for (String path : secondaryPaths) {
                    File file = new File(path);
                    if (file.isDirectory() && file.canRead()) {
                        PATH = file.getAbsolutePath();
                        break;
                    }
                }
            }
        }
        PATH += "/VideoAds/";
        f = new File(PATH);
        if (!f.exists()) {
            f.mkdirs();
        }
    }


    private static void deleteRemovedVideos(JSONArray videos) {
        Cursor c = DBHelper.getAllVideosWithPath();
        List<Integer> ids = new ArrayList<>();
        try {
            for (int i = 0; i < videos.length(); i++) {
                JSONObject video = videos.getJSONObject(i);
                ids.add(video.getInt("id"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex(DBHelper._ID));
            if (!ids.contains(id)) {
                String pathToVideo = c.getString(c.getColumnIndex(DBHelper.PATH));
                File f = new File(pathToVideo);
                if (f.delete())
                    DBHelper.deleteById(id);
            }
        }
    }

    //{"videos":[{"id":1,"title":"Short video",
    // "path":"/system/videos/files/000/000/001/original/Motivational_short_video_-_How_to_succeed_-_cartoon.mp4?1418397964"}]}
    private static void getVideos(Context context, final int groupId) {
        String url = SERVER_URL + "/api/videos.json";
        RequestParams params = new RequestParams();
        params.put("group_id", groupId + "");
        Log.d("ucun", "gr id: " + groupId);
        HTTPUtils.getSyncWithHeaders(context, url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, JSONObject object) {
                try {
                    JSONArray videos = object.getJSONArray("videos");
                    deleteRemovedVideos(videos);
                    for (int k = 0; k < videos.length(); k++) {
                        JSONObject video = videos.getJSONObject(k);
                        int id = video.getInt("id");
                        String title = video.getString("title") + ".mp4";
                        String link = SERVER_URL + video.getString("path");
                        String path = PATH + title;
                        DBHelper.addVideo(title, id, link, path);
                        DBHelper.addVideoGroup(id, groupId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d("ucun", errorResponse.toString());
            }
        });
    }

    //{"periods":[{"id":2,"group_id":1,"start_time":1417392000,"end_time":1419984000},
    // {"id":3,"group_id":1,"start_time":1418774400,"end_time":1419465600}]}
    public static void getPeriods(final Context context, int placeId) {
        String url = SERVER_URL + "/api/periods.json";
        RequestParams params = new RequestParams();
        params.put("place_id", placeId + "");
        //DBHelper.deletePeriods(); //TODO: test
        HTTPUtils.getSyncWithHeaders(context, url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, JSONObject object) {
                try {
                    JSONArray periods = object.getJSONArray("periods");
                    for (int k = 0; k < periods.length(); k++) {
                        JSONObject o = periods.getJSONObject(k);
                        int id = o.getInt("id");
                        int groupId = o.getInt("group_id");
                        int start = o.getInt("start_time");
                        int end = o.getInt("end_time");
                        Log.d("ucun", object.toString());
                        DBHelper.addGroup(groupId);
                        getVideos(context, groupId);
                        DBHelper.addPeriod(id, groupId, start, end);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //{"palces":[{"id":1,"name":"test","city_id":1,"city_name":"Odessa","region_id":1,"region_name":"Odessa"}]}
    public static void getPlaces(Context context, int clientId, final ArrayAdapter<String> adapter) {
        String url = SERVER_URL + "/api/places.json";
        RequestParams params = new RequestParams();
        params.put("client_id", clientId + "");
        HTTPUtils.getWithHeaders(context, url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int responseCode, Header[] headers, JSONObject object) {
                try {
                    Log.d("ucun", responseCode + "");
                    Log.d("ucun", object.toString());
                    JSONArray places = object.getJSONArray("places");
                    Utils.spinnerIDs = new int[places.length()];
                    Utils.spinnerNames = new String[places.length()];
                    for (int k = 0; k < places.length(); k++) {
                        JSONObject o = places.getJSONObject(k);
                        Utils.spinnerIDs[k] = o.getInt("id");
                        Utils.spinnerNames[k] = o.getString("name");
                    }
                    adapter.addAll(Utils.spinnerNames);
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                throwable.printStackTrace();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                throwable.printStackTrace();
                Log.d("ucun", errorResponse.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                throwable.printStackTrace();
            }
        });
    }

    public static void sendStatistics(Context context, int placeId) {
        String url = SERVER_URL + "/api/statistics.json";
        Cursor c = DBHelper.getAllVideosCursor();

        while (c.moveToNext()) {
            int views = c.getInt(c.getColumnIndex(DBHelper.VIEWS));
            //send statistics only if views>0
            if (views > 0) {
                RequestParams params = fillStatistics(c, placeId, true); //for video

                HTTPUtils.postSyncWithHeaders(context, url, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int i, Header[] headers, byte[] bytes) {
                        Log.d("ucun", "statistics sent");
                    }

                    @Override
                    public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                        Log.d("ucun", "statistics failed");
                        if (bytes != null) {
                            Log.d("ucun", new String(bytes));
                        }
                    }
                });

                params = fillStatistics(c, placeId, false); //for groups

                HTTPUtils.postSyncWithHeaders(context, url, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int i, Header[] headers, byte[] bytes) {
                        Log.d("ucun", "statistics sent\n");
                    }

                    @Override
                    public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                        if (bytes != null) {
                            Log.d("ucun", new String(bytes));
                        }
                        Log.d("ucun", "statistics failed\n");
                    }
                });
            }
        }
    }

    private static RequestParams fillStatistics(Cursor c, int placeId, boolean forVideo) {
        RequestParams params = new RequestParams();
        long date = System.currentTimeMillis() / 1000L;
        int id = c.getInt(c.getColumnIndex(DBHelper._ID));
        int groupId = c.getInt(c.getColumnIndex("g_id"));
        Log.d("ucun", groupId + "");
        int views = c.getInt(c.getColumnIndex(DBHelper.VIEWS));
        int replays = c.getInt(c.getColumnIndex(DBHelper.REPLAYS));
        Log.d("ucun", views + "");
        Log.d("ucun", replays + "");

        params.put("date", date + "");
        if (forVideo)
            params.put("video_id", id + "");
        else
            params.put("group_id", groupId + "");
        params.put("views", views + "");
        params.put("replays", replays + "");
        params.put("reviews", "[0,0]");
        params.put("place_id", placeId + "");

        return params;
    }

    public static void deleteUnusedVideos() {
        Cursor c = DBHelper.selectUnused();
        while (c.moveToNext()) {
            String path = c.getString(c.getColumnIndex(DBHelper.PATH));
            File f = new File(path);
            f.delete();
        }
        c.close();
        DBHelper.deleteUnused();
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}

