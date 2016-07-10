package com.nasctech.ucun.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import java.util.Map;

public class HTTPUtils {
    private static final String EMAIL_HEADER_KEY = "X-VIDEO-API-EMAIL";
    private static final String TOKEN_HEADER_KEY = "X-VIDEO-API-TOKEN";
    private static AsyncHttpClient client = new AsyncHttpClient();
    private static SyncHttpClient syncClient = new SyncHttpClient();

    public static void get(String url, Map<String, String> params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, createParams(params), responseHandler);
    }

    public static void getSyncWithHeaders(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String email = prefs.getString(Const.LOGIN_KEY, "");
        final String token = prefs.getString(Const.TOKEN_KEY, "");

        syncClient.addHeader(EMAIL_HEADER_KEY, email);
        syncClient.addHeader(TOKEN_HEADER_KEY, token);
        syncClient.get(context, url, params, responseHandler);
    }

    public static void getWithHeaders(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String email = prefs.getString(Const.LOGIN_KEY, "");
        final String token = prefs.getString(Const.TOKEN_KEY, "");

        client.addHeader(EMAIL_HEADER_KEY, email);
        client.addHeader(TOKEN_HEADER_KEY, token);
        client.get(context, url, params, responseHandler);
    }

    public static void getSync(String url, Map<String, String> params, AsyncHttpResponseHandler responseHandler) {
        syncClient.get(url, createParams(params), responseHandler);
    }

    public static void postSyncWithHeaders(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String email = prefs.getString(Const.LOGIN_KEY, "");
        final String token = prefs.getString(Const.TOKEN_KEY, "");

        syncClient.addHeader(EMAIL_HEADER_KEY, email);
        syncClient.addHeader(TOKEN_HEADER_KEY, token);
        syncClient.post(context, url, params, responseHandler);
    }

    public static void post(String url, Map<String, String> params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, createParams(params), responseHandler);
    }

    private static RequestParams createParams(Map<String, String> mapParams) {
        return new RequestParams(mapParams);
    }
}

