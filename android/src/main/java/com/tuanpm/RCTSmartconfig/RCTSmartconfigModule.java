/**
 * Created by TuanPM (tuanpm@live.com) on 1/4/16.
 */

package com.tuanpm.RCTSmartconfig;

import android.os.AsyncTask;
import android.util.Log;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

public class RCTSmartconfigModule extends ReactContextBaseJavaModule {
    private static final String TAG = "RCTSmartconfigModule";
    private static final int TIMEOUT_MS = 15000+6000;

    public RCTSmartconfigModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "Smartconfig";
    }

    void onFinished(List<IEsptouchResult> result, final Promise promise) {
        final WritableArray ret = Arguments.createArray();

        boolean resolved = false;
        for (IEsptouchResult resultInList : result) {
            if (!resultInList.isCancelled() && resultInList.getBssid() != null) {
                final WritableMap map = Arguments.createMap();
                map.putString("bssid", resultInList.getBssid());
                map.putString("ipv4", resultInList.getInetAddress().getHostAddress());
                ret.pushMap(map);
                resolved = true;
                if (!resultInList.isSuc())
                    break;
            }
        }

        if (resolved) {
            Log.d(TAG, "Smartconfig success");
            promise.resolve(ret);
        } else {
            promise.reject(new Throwable("Smartconfig Error"));
        }
    }

    @ReactMethod
    public void start(final ReadableMap options, final Promise promise) {
        String ssid = options.getString("ssid");
        String pass = options.getString("password");
        Log.d(TAG, "ssid " + ssid + ":pass " + pass);

        new AsyncTask<String, Void, List<IEsptouchResult>>() {
            @Override
            protected List<IEsptouchResult> doInBackground(String... params) {
                Log.d(TAG, "doing task");
                final String apSsid = params[0];
                final String apBssid = params[1];
                final String apPassword = params[2];
                final String isSsidHiddenStr = params[3];
                final String taskResultCountStr = params[4];
                final boolean isSsidHidden = isSsidHiddenStr.equals("YES");
                final int taskResultCount = Integer.parseInt(taskResultCountStr);
                return new EsptouchTask(apSsid, apBssid, apPassword,
                        isSsidHidden, TIMEOUT_MS, getCurrentActivity()).executeForResults(taskResultCount);
            }

            @Override
            protected void onPostExecute(List<IEsptouchResult> result) {
                Log.d(TAG, "finished task");
                final IEsptouchResult firstResult = result.get(0);
                // check whether the task is cancelled and no results received
                if (!firstResult.isCancelled()) {
                    onFinished(result, promise);
                }
            }

        }.execute(ssid, new String(""), pass, "YES", "1"); // end AsyncTask

    } // start
}
