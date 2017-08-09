package util;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.smartregister.Context;
import org.smartregister.DristhiConfiguration;
import org.smartregister.domain.Response;
import org.smartregister.event.Listener;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.fragment.AdvancedSearchFragment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by keyman on 26/01/2017.
 */
public class GlobalSearchUtils {

    public static void backgroundSearch(final Map<String, String> map, final Listener<JSONArray> listener, final ProgressDialog progressDialog) {

        org.smartregister.util.Utils.startAsyncTask(new AsyncTask<Void, Void, JSONArray>() {
            @Override
            protected JSONArray doInBackground(Void... params) {
                publishProgress();
                Response<String> response = globalSearch(map);
                if (response.isFailure()) {
                    return null;
                } else {
                    try {
                        return new JSONArray(response.payload());
                    } catch (Exception e) {
                        Log.e(getClass().getName(), "", e);
                        return null;
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                progressDialog.show();
            }

            @Override
            protected void onPostExecute(JSONArray result) {
                listener.onEvent(result);
                progressDialog.dismiss();
            }
        }, null);
    }

    private static Response<String> globalSearch(Map<String, String> map) {
        Context context = VaccinatorApplication.getInstance().context();
        DristhiConfiguration configuration = context.configuration();
        String baseUrl = configuration.dristhiBaseURL();
        String paramString = "";
        if (!map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.contains(AdvancedSearchFragment.ACTIVE) && !key.contains(AdvancedSearchFragment.INACTIVE)) {
                    key = AdvancedSearchFragment.INACTIVE;
                    boolean v = !Boolean.valueOf(value);
                    value = Boolean.toString(v);
                }

                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    value = urlEncode(value);
                    String param = key.trim() + "=" + value.trim();
                    if (StringUtils.isBlank(paramString)) {
                        paramString = "?" + param;
                    } else {
                        paramString += "&" + param;
                    }
                }

            }

        }
        String uri = baseUrl + "/rest/search/path" + paramString;

        return context.getHttpAgent().fetch(uri);
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
