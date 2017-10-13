package org.smartregister.path.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.domain.Response;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.HTTPAgent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by keyman on 11/10/2017.
 */
public class ValidateIntentService extends IntentService {

    private Context context;
    private HTTPAgent httpAgent;
    private static final int FETCH_LIMIT = 100;
    private static final String VALIDATE_SYNC_PATH = "rest/validate/sync";


    public ValidateIntentService() {
        super("ValidateIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getBaseContext();
        httpAgent = VaccinatorApplication.getInstance().context().getHttpAgent();
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {

             int fetchLimit = FETCH_LIMIT;
            EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();

            List<String> clientIds = db.getUnValidatedClientBaseEntityIds(fetchLimit);
            if (!clientIds.isEmpty()) {
                fetchLimit -= clientIds.size();
            }

            List<String> eventIds = new ArrayList<>();
            if (fetchLimit > 0) {
                eventIds = db.getUnValidatedEventFormSubmissionIds(fetchLimit);
            }

            JSONObject request = request(clientIds, eventIds);
            if (request == null) {
                return;
            }

            String baseUrl = VaccinatorApplication.getInstance().context().configuration().dristhiBaseURL();
            if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
                baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
            }

            String jsonPayload = request.toString();
            Response<String> response = httpAgent.post(
                    MessageFormat.format("{0}/{1}",
                            baseUrl,
                            VALIDATE_SYNC_PATH),
                    jsonPayload);
            if (response.isFailure()) {
                Log.e(getClass().getName(), "Events sync failed.");
                return;
            }

            JSONObject results = new JSONObject(response.payload());

            if (results.has(getString(R.string.clients_key))) {
                JSONArray inValidClients = results.getJSONArray(getString(R.string.clients_key));

                for (int i = 0; i < inValidClients.length(); i++) {
                    String inValidClientId = inValidClients.getString(i);
                    clientIds.remove(inValidClientId);
                    db.markEventValidationStatus(BaseRepository.TYPE_InValid, inValidClientId);
                }

                for (String clientId : clientIds) {
                    db.markClientValidationStatus(BaseRepository.TYPE_Valid, clientId);
                }
            }

            JSONArray inValidEvents = results.has("events") ? results.getJSONArray("events") : new JSONArray();
            if (inValidEvents.length() > 0) {
                for (int i = 0; i < inValidEvents.length(); i++) {
                    String inValidEventId = inValidEvents.getString(i);
                    eventIds.remove(inValidEventId);
                    db.markEventValidationStatus(BaseRepository.TYPE_InValid, inValidEventId);
                }
            }

            for (String eventId : eventIds) {
                db.markEventValidationStatus(BaseRepository.TYPE_Valid, eventId);
            }

        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
        }
    }

    private JSONObject request(List<String> clientIds, List<String> eventIds) {
        try {

            JSONArray clientIdArray = null;
            if (!clientIds.isEmpty()) {
                clientIdArray = new JSONArray(clientIds);
            }

            JSONArray eventIdArray = null;
            if (!eventIds.isEmpty()) {
                eventIdArray = new JSONArray(eventIds);
            }


            if (clientIdArray != null || eventIdArray != null) {
                JSONObject request = new JSONObject();
                if (clientIdArray != null) {
                    request.put(context.getString(R.string.clients_key), clientIdArray);
                }

                if (eventIdArray != null) {
                    request.put(context.getString(R.string.events_key), eventIdArray);
                }

                return request;

            }

        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
        }
        return null;
    }


}