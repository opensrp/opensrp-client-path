package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.domain.Response;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.receiver.VaccinatorAlarmReceiver;
import org.smartregister.path.repository.UniqueIdRepository;
import org.smartregister.service.HTTPAgent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import util.PathConstants;


/**
 * Created by onamacuser on 18/03/2016.
 */
public class PullUniqueIdsIntentService extends IntentService {
    public static final String ID_URL = "/uniqueids/get";
    public static final String IDENTIFIERS = "identifiers";
    private static final String TAG = PullUniqueIdsIntentService.class.getCanonicalName();
    private UniqueIdRepository uniqueIdRepo;


    public PullUniqueIdsIntentService() {
        super("PullUniqueOpenMRSUniqueIdsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            int numberToGenerate;
            if (uniqueIdRepo.countUnUsedIds() == 0) { // first time pull no ids at all
                numberToGenerate = PathConstants.OPENMRS_UNIQUE_ID_INITIAL_BATCH_SIZE;
            } else if (uniqueIdRepo.countUnUsedIds() <= 250) { //maintain a minimum of 250 else skip this pull
                numberToGenerate = PathConstants.OPENMRS_UNIQUE_ID_BATCH_SIZE;
            } else {
                return;
            }
            JSONObject ids = fetchOpenMRSIds(PathConstants.OPENMRS_UNIQUE_ID_SOURCE, numberToGenerate);
            if (ids != null && ids.has(IDENTIFIERS)) {
                parseResponse(ids);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }finally {
            VaccinatorAlarmReceiver.completeWakefulIntent(intent);
        }
    }

    private JSONObject fetchOpenMRSIds(int source, int numberToGenerate) throws Exception {
        HTTPAgent httpAgent = VaccinatorApplication.getInstance().context().getHttpAgent();
        String baseUrl = VaccinatorApplication.getInstance().context().
                configuration().dristhiBaseURL();
        String endString = "/";
        if (baseUrl.endsWith(endString)) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(endString));
        }

        String url = baseUrl + ID_URL + "?source=" + source + "&numberToGenerate=" + numberToGenerate;
        Log.i(PullUniqueIdsIntentService.class.getName(), "URL: " + url);

        if (httpAgent == null) {
            throw new Exception(ID_URL + " http agent is null");
        }

        Response resp = httpAgent.fetch(url);
        if (resp.isFailure()) {
            throw new Exception(ID_URL + " not returned data");
        }

        return new JSONObject((String) resp.payload());
    }

    /**
     * @param connection object; note: before calling this function,
     *                   ensure that the connection is already be open, and any writes to
     *                   the connection's output stream should have already been completed.
     * @return String containing the body of the connection response or null if the input stream could not be read correctly
     */
    private String readInputStreamToString(HttpURLConnection connection) {
        String result = null;
        StringBuilder sb = new StringBuilder();
        InputStream is = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        } catch (Exception e) {
            Log.i(TAG, "Error reading InputStream");
            result = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.i(TAG, "Error closing InputStream");
                }
            }
        }

        return result;
    }

    private void parseResponse(JSONObject idsFromOMRS) throws Exception {
        JSONArray jsonArray = idsFromOMRS.getJSONArray(IDENTIFIERS);
        if (jsonArray != null && jsonArray.length() > 0) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                ids.add(jsonArray.getString(i));
            }
            uniqueIdRepo.bulkInserOpenmrsIds(ids);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uniqueIdRepo = VaccinatorApplication.getInstance().uniqueIdRepository();
        return super.onStartCommand(intent, flags, startId);
    }
}
