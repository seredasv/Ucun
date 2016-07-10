package com.nasctech.ucun.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nasctech.ucun.R;
import com.nasctech.ucun.utils.Const;
import com.nasctech.ucun.utils.DBHelper;
import com.nasctech.ucun.utils.HTTPUtils;
import com.nasctech.ucun.utils.Utils;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private Dialog dialog;
    private Spinner spinner;
    private Loader loader;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Utils.setPaths(this);
        dialog = new Dialog(this);
        DBHelper.init(getApplicationContext());
        deleteVideoOnFirstLaunch();
    }

    private void getLoginAndPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        String login = prefs.getString(Const.LOGIN_KEY, "");
        String password = prefs.getString(Const.PASS_KEY, "");

        if (!TextUtils.isEmpty(login)) {
            EditText loginEdit = (EditText) findViewById(R.id.editLogin);
            EditText passEdit = (EditText) findViewById(R.id.editPassword);
            loginEdit.setText(login);
            passEdit.setText(password);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        DBHelper.init(getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        getLoginAndPassword();
    }

    private void deleteVideoOnFirstLaunch() {
        if (DBHelper.getAllVideos().isEmpty()) {
            File dir = new File(Utils.PATH);
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++)
                if (files[i].delete())
                    Log.d("file", "deleted");
                else
                    Log.d("file", "not deleted");
        }
    }

    private ArrayAdapter<String> initSpinner() {
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return adapter;
    }

    public void login(View v) {
        EditText login = (EditText) findViewById(R.id.editLogin);
        EditText pass = (EditText) findViewById(R.id.editPassword);
        final String loginText = login.getText().toString();
        final String passText = pass.getText().toString();
        String url = Utils.SERVER_URL + "/api/get_client_token.json";

        HTTPUtils.post(url, createParams(loginText, passText), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("code", statusCode + "");
                if (statusCode == 200) {
                    saveToken(response);
                    saveLoginAndPassword(loginText, passText);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                    int id = prefs.getInt("id", 1);
                    //if no elements in spinner (haven't received places yet)
                    if (spinner == null || spinner.getCount() == 0) {
                        Utils.getPlaces(getApplicationContext(), id, initSpinner());
                        Toast.makeText(LoginActivity.this, "Please choose the place", Toast.LENGTH_SHORT).show();
                    } else {
                        int placeId = Utils.spinnerIDs[(int) spinner.getSelectedItemId()];
                        putPlacePreference(placeId);
                        loader = new Loader();
                        loader.execute(placeId);
                    }
                } else {
                    Log.d("login error", response.toString());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(LoginActivity.this, "Connection problems", Toast.LENGTH_SHORT).show();
                Log.d("login", responseString);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject object) {
                super.onFailure(statusCode, headers, throwable, object);
                Toast.makeText(LoginActivity.this, "Connection problems", Toast.LENGTH_SHORT).show();
                throwable.printStackTrace();
            }
        });
    }

    private Map<String, String> createParams(String login, String pass) {
        Map<String, String> map = new HashMap<>();
        map.put("email", login);
        map.put("password", pass);
        return map;
    }

    //{"client":{"token":"RqA-a4JxTV_6sQY9bGxm3Tw4ydpFK2vTRw","client_id":1}}
    private void saveToken(JSONObject json) {
        try {
            JSONObject client = json.getJSONObject("client");
            String token = client.getString("token");
            int id = client.getInt("client_id");
            putTokenPreferences(token, id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveLoginAndPassword(String login, String password) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Const.LOGIN_KEY, login);
        editor.putString(Const.PASS_KEY, password);
        editor.commit();
    }

    private void putTokenPreferences(String token, int id) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Const.TOKEN_KEY, token);
        editor.putInt(Const.ID_KEY, id);
        editor.commit();
    }

    private void putPlacePreference(int id) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Const.PLACE_ID_KEY, id);
        editor.commit();
    }

    private class Loader extends AsyncTask<Integer, Integer, String> {

        @Override
        protected String doInBackground(Integer... params) {
            download(params);
            return null;
        }

        private void download(Integer... params) {
            long bytes = 1024 * 1024 * 30; //if < 30mb space left, delete unused videos
            if (!Utils.isEnoughSpace(bytes)) {
                Utils.deleteUnusedVideos();
            }
            Utils.getPeriods(getApplicationContext(), params[0]);

            Cursor c = DBHelper.getDownloadLinks();

            while (c.moveToNext()) {
                String link = c.getString(c.getColumnIndex(DBHelper.LINK));
                Log.d("LINK", link);
                String path = c.getString(c.getColumnIndex(DBHelper.PATH));
                final File f = new File(path);
                if (!f.exists()) {
                    if (!Utils.isEnoughSpace(1024 * 1024 * 20)) {
                        //TODO do something if no space
                    } else
                        HTTPUtils.getSync(link, null, new FileAsyncHttpResponseHandler(f) {
                            @Override
                            public void onSuccess(int i, Header[] headers, File response) {
                            }

                            @Override
                            public void onFailure(int i, Header[] headers, Throwable throwable, File response) {
                                cancel(true);
                                response.delete();
                            }
                        });
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            dialog.setCancelable(true);
            dialog.dismiss();
            finish();
            startActivity(new Intent(LoginActivity.this, PlayerActivity.class));
            loader = null; //test
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            dialog.setCancelable(true);
            dialog.dismiss();
            Toast.makeText(LoginActivity.this, "Connection problems, please try again", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setCancelable(false);
            dialog.show();
            dialog.setContentView(R.layout.dialog);
            dialog.setTitle("Synchronizing with server");
        }
    }
}
