package org.smartregister.path.sync;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.domain.db.EventClient;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.repository.EventClientRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import util.MoveToMyCatchmentUtils;
import org.smartregister.repository.AllSharedPreferences;


public class ECSyncUpdater {

    private final EventClientRepository db;
    private final Context context;
    private AllSharedPreferences mSharedPreferences;

    private static ECSyncUpdater instance;

    public static ECSyncUpdater getInstance(Context context) {
        if (instance == null) {
            instance = new ECSyncUpdater(context);
        }
        return instance;
    }

    private ECSyncUpdater(Context context) {
        this.context = context;
        db = VaccinatorApplication.getInstance().eventClientRepository();
        mSharedPreferences = CoreLibrary.getInstance().context().allSharedPreferences();
    }

    public boolean saveAllClientsAndEvents(JSONObject jsonObject) {
        try {
            if (jsonObject == null) {
                return false;
            }

            JSONArray events = jsonObject.has("events") ? jsonObject.getJSONArray("events") : new JSONArray();
            JSONArray clients = jsonObject.has("clients") ? jsonObject.getJSONArray("clients") : new JSONArray();

            batchSave(events, clients);


            return true;
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
            return false;
        }
    }

    public List<EventClient> allEventClients(long startSyncTimeStamp, long lastSyncTimeStamp) {
        try {
            return db.fetchEventClients(startSyncTimeStamp, lastSyncTimeStamp);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
        return new ArrayList<>();
    }

    public List<EventClient> getEvents(Date lastSyncDate, String syncStatus) {
        try {
            return db.fetchEventClients(lastSyncDate, syncStatus);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
        return new ArrayList<>();
    }

    public JSONObject getClient(String baseEntityId) {
        try {
            return db.getClientByBaseEntityId(baseEntityId);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
        return null;
    }

    public void addClient(String baseEntityId, JSONObject jsonObject) {
        try {
            db.addorUpdateClient(baseEntityId, jsonObject);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
    }

    public void addEvent(String baseEntityId, JSONObject jsonObject) {
        try {
            db.addEvent(baseEntityId, jsonObject);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
    }

    public void  addReport(JSONObject jsonObject) {
        try {
            VaccinatorApplication.getInstance().hia2ReportRepository().addReport(jsonObject);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
    }

    public long getLastSyncTimeStamp() {
        return mSharedPreferences.fetchLastSyncDate(0);
    }

    public void updateLastSyncTimeStamp(long lastSyncTimeStamp) {
        mSharedPreferences.saveLastSyncDate(lastSyncTimeStamp);
    }

    public long getLastCheckTimeStamp() {
        return mSharedPreferences.fetchLastCheckTimeStamp();
    }

    public void updateLastCheckTimeStamp(long lastSyncTimeStamp) {
        mSharedPreferences.updateLastCheckTimeStamp(lastSyncTimeStamp);
    }

    public void batchSave(JSONArray events, JSONArray clients) throws Exception {
        batchInsertClients(clients);
        batchInsertEvents(events);
    }

    public void batchInsertClients(JSONArray clients) {
        db.batchInsertClients(clients);
    }

    private void batchInsertEvents(JSONArray events) {
        db.batchInsertEvents(events, getLastSyncTimeStamp());
    }

    public <T> T convert(JSONObject jo, Class<T> t) {
        return db.convert(jo, t);
    }

    public JSONObject convertToJson(Object object) {
        return db.convertToJson(object);
    }

    public boolean deleteClient(String baseEntityId) {
        return db.deleteClient(baseEntityId);
    }

    public boolean deleteEventsByBaseEntityId(String baseEntityId) {
        return db.deleteEventsByBaseEntityId(baseEntityId, MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT);
    }
}
