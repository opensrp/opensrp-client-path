package util;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.Context;
import org.smartregister.DristhiConfiguration;
import org.smartregister.domain.Response;
import org.smartregister.domain.ResponseStatus;
import org.smartregister.domain.db.Event;
import org.smartregister.domain.db.Obs;
import org.smartregister.event.Listener;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathClientProcessor;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 26/01/2017.
 */
public class MoveToMyCatchmentUtils {
    public static final String MOVE_TO_CATCHMENT_EVENT = "Move To Catchment";

    public static void moveToMyCatchment(final List<String> ids, final Listener<JSONObject> listener, final ProgressDialog progressDialog) {

        org.smartregister.util.Utils.startAsyncTask(new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... params) {
                publishProgress();
                Response<String> response = move(ids);
                if (response.isFailure()) {
                    return null;
                } else {
                    try {
                        return new JSONObject(response.payload());
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
            protected void onPostExecute(JSONObject result) {
                listener.onEvent(result);
                progressDialog.dismiss();
            }
        }, null);
    }

    private static Response<String> move(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new Response<>(ResponseStatus.failure, "entityId doesn't exist");
        }

        Context context = VaccinatorApplication.getInstance().context();
        DristhiConfiguration configuration = context.configuration();

        String baseUrl = configuration.dristhiBaseURL();
        String idString = StringUtils.join(ids, ",");

        String paramString = "?baseEntityId=" + urlEncode(idString.trim()) + "&limit=1000";
        String uri = baseUrl + ECSyncUpdater.SEARCH_URL + paramString;

        return context.getHttpAgent().fetch(uri);
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    public static boolean processMoveToCatchment(android.content.Context context, AllSharedPreferences allSharedPreferences, JSONObject jsonObject) {

        try {
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
            int eventsCount = jsonObject.has("no_of_events") ? jsonObject.getInt("no_of_events") : 0;
            if (eventsCount == 0) {
                return false;
            }

            JSONArray events = jsonObject.has("events") ? jsonObject.getJSONArray("events") : new JSONArray();
            JSONArray clients = jsonObject.has("clients") ? jsonObject.getJSONArray("clients") : new JSONArray();

            ecUpdater.batchSave(events, clients);

            final String BIRTH_REGISTRATION_EVENT = "Birth Registration";
            final String NEW_WOMAN_REGISTRATION_EVENT = "New Woman Registration";
            final String HOME_FACILITY = "Home_Facility";

            String toProviderId = allSharedPreferences.fetchRegisteredANM();

            String toLocationId = allSharedPreferences
                    .fetchDefaultLocalityId(toProviderId);

            for (int i = 0; i < events.length(); i++) {
                JSONObject jsonEvent = events.getJSONObject(i);
                Event event = ecUpdater.convert(jsonEvent, Event.class);
                if (event == null) {
                    continue;
                }

                String fromLocationId = null;
                if (event.getEventType().equals(BIRTH_REGISTRATION_EVENT)) {
                    // Update home facility
                    for (Obs obs : event.getObs()) {
                        if (obs.getFormSubmissionField().equals(HOME_FACILITY)) {
                            fromLocationId = obs.getValue().toString();
                            List<Object> values = new ArrayList<>();
                            values.add(toLocationId);
                            obs.setValues(values);
                        }
                    }
                }

                if (event.getEventType().equals(BIRTH_REGISTRATION_EVENT) || event.getEventType().equals(NEW_WOMAN_REGISTRATION_EVENT)) {
                    //Create move to catchment event;
                    org.smartregister.clientandeventmodel.Event moveToCatchmentEvent = JsonFormUtils.createMoveToCatchmentEvent(context, event, fromLocationId, toProviderId, toLocationId);
                    JSONObject moveToCatchmentJsonEvent = ecUpdater.convertToJson(moveToCatchmentEvent);
                    if (moveToCatchmentEvent != null) {
                        ecUpdater.addEvent(moveToCatchmentEvent.getBaseEntityId(), moveToCatchmentJsonEvent);
                    }
                }

                // Update providerId, locationId and Save unsynced event
                event.setProviderId(toProviderId);
                event.setLocationId(toLocationId);
                JSONObject updatedJsonEvent = ecUpdater.convertToJson(event);
                jsonEvent = JsonFormUtils.merge(jsonEvent, updatedJsonEvent);

                ecUpdater.addEvent(event.getBaseEntityId(), jsonEvent);
            }

            long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PathClientProcessor.getInstance(context).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
            allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());

            return true;
        } catch (Exception e) {
            Log.e(MoveToMyCatchmentUtils.class.getName(), "Exception", e);
        }

        return false;
    }
}
