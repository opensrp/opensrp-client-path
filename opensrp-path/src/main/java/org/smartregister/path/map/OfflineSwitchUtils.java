package org.smartregister.path.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.path.BuildConfig;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.ona.kujaku.data.realm.objects.MapBoxOfflineQueueTask;
import io.ona.kujaku.helpers.MapBoxWebServiceApi;
import io.ona.kujaku.services.MapboxOfflineDownloaderService;
import util.PathConstants;
import utils.Constants;
import utils.helpers.converters.GeoJSONFeature;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/02/2018.
 */

public class OfflineSwitchUtils {

    public static final String TAG = OfflineSwitchUtils.class.getName();
    private Context context;
    private AlertDialog deleteMapsDialog;
    private OfflineMapDownloadUpdatesReceiver offlineMapDownloadUpdatesReceiver;
    private ArrayList<LatLng> childPoints = new ArrayList<>();
    private String mapName;

    public OfflineSwitchUtils(@NonNull Context context, @NonNull OfflineMapDownloadUpdatesReceiver offlineMapDownloadUpdatesReceiver, String mapName) {
        this.context = context;
        this.offlineMapDownloadUpdatesReceiver = offlineMapDownloadUpdatesReceiver;
        this.mapName = mapName;
    }


    public void offlineSwitchClicked(Switch offlineSwitch) {
        updateOfflineModeStatus(offlineSwitch.isChecked());

        if (isOfflineModeSwitchedOn()) {
            registerOfflineMapDownloadUpdatesReceiver(offlineMapDownloadUpdatesReceiver);
            requestForOfflineMapDownload(getChildrenLocations());
        } else {
            // Request for a delete of the offline map or something
            if (offlineMapDownloadUpdatesReceiver.isDownloading()) {
                callStopDownloadIntent(offlineMapDownloadUpdatesReceiver.isDownloading());
            } else {
                // Ask whether to delete the already downloaded offline maps
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.delete_maps_dialog_title)
                        .setMessage(R.string.delete_maps_dialog_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                callStopDownloadIntent(false);
                                dismissDeleteMapsDialog();
                            }
                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissDeleteMapsDialog();
                    }
                });

                deleteMapsDialog = builder.create();
                deleteMapsDialog.show();
            }
        }

        //drawer.closeDrawer(GravityCompat.START);
    }

    private void updateOfflineModeStatus(boolean offlineModeStatus) {
        Utils.writePreference(context, PathConstants.PREFERENCE_OFFLINE_MODE, String.valueOf(offlineModeStatus));
    }

    public boolean isOfflineModeSwitchedOn() {
        boolean defaultOfflineModeStatus = false;

        return Boolean.valueOf(Utils.getPreference(context, PathConstants.PREFERENCE_OFFLINE_MODE, String.valueOf(defaultOfflineModeStatus)));
    }

    public void registerOfflineMapDownloadUpdatesReceiver(BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_MAP_DOWNLOAD_SERVICE_STATUS_UPDATES));
    }

    public void unregisterOfflineMapDownloadUpdatesReceiver(BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(broadcastReceiver);
    }

    private void requestForOfflineMapDownload(@NonNull LatLng[] mapPoints) {
        MapHelper mapHelper = new MapHelper();
        LatLng[] bounds = mapHelper.getBounds(mapPoints);

        offlineMapDownloadUpdatesReceiver.setWaitingForDownloadToStart(true);
        mapHelper.requestOfflineMap(context, mapName, "mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9", BuildConfig.MAPBOX_SDK_ACCESS_TOKEN, bounds[0], bounds[1], 11.1, 20.0);

        // Cache the style
        (new MapBoxWebServiceApi(context, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN))
                .retrieveStyleJSON("mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
    }

    private void dismissDeleteMapsDialog() {
        if (deleteMapsDialog != null && deleteMapsDialog.isShowing()) {
            deleteMapsDialog.dismiss();
        }
    }

    private LatLng[] getChildrenLocations() {
        ArrayList<LatLng> childrenLocations = new ArrayList<>();

        ArrayList<String> childrenGeoJSON = new ArrayList<>();
        LinkedHashMap<String, ArrayList<GeoJSONFeature>> geoJSONFeatureCollection = new LinkedHashMap<>();

        boolean added = false;
        Cursor cursor = null;
        try {
            // Get all children
            SmartRegisterQueryBuilder queryBUilder = new SmartRegisterQueryBuilder();
            queryBUilder.SelectInitiateMainTable(PathConstants.CHILD_TABLE_NAME, new String[]{ PathConstants.CHILD_TABLE_NAME + ".base_entity_id"});
            String query = queryBUilder.mainCondition(" dod is NULL OR dod = '' ");
            cursor = VaccinatorApplication.getInstance().context().commonrepository(PathConstants.CHILD_TABLE_NAME).rawCustomQueryForAdapter(query);
            if (cursor != null) {
                cursor.moveToFirst();
                while(!cursor.isAfterLast()) {
                    String entityId = cursor.getString(0);
                    Map<String, String> details = VaccinatorApplication.getInstance().context().detailsRepository().getAllDetailsForClient(entityId);
                    GeoJSONFeature feature = constructGeoJsonFeature(details);
                    if (feature != null) {
                        List<LatLng> points = feature.getFeaturePoints();
                        if (points != null && points.size() > 0) {
                            childrenLocations.add(points.get(0));
                        }
                    }
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return childrenLocations.toArray(new LatLng[childrenLocations.size()]);
    }

    private void callStopDownloadIntent(boolean isMapDownloading) {
        Intent stopDownloadIntent = new Intent(context, MapboxOfflineDownloaderService.class);
        stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN);
        stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME, mapName);
        stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_SERVICE_ACTION, ((isMapDownloading) ? MapboxOfflineDownloaderService.SERVICE_ACTION.STOP_CURRENT_DOWNLOAD : MapboxOfflineDownloaderService.SERVICE_ACTION.DELETE_MAP));
        if (isMapDownloading) {
            stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_DELETE_TASK_TYPE, MapBoxOfflineQueueTask.TASK_TYPE_DOWNLOAD);
        }

        context.startService(stopDownloadIntent);
    }

    public GeoJSONFeature constructGeoJsonFeature(Map<String, String> clientDetails) {
        String latLng = Utils.getValue(clientDetails, "geopoint", false);
        if (!TextUtils.isEmpty(latLng)) {
            String[] coords = latLng.split(" ");
            LatLng coordinates = new LatLng(Double.valueOf(coords[0]), Double.valueOf(coords[1]));
            childPoints.add(coordinates);
            ArrayList featurePoints = new ArrayList<LatLng>();
            featurePoints.add(coordinates);
            GeoJSONFeature feature = new GeoJSONFeature(featurePoints);
            for (String curKey : clientDetails.keySet()) {
                feature.addProperty(curKey, clientDetails.get(curKey));
            }

            if (!feature.hasId()) {
                String id = UUID.randomUUID().toString();
                feature.setId(id);
                feature.addProperty("id", id);
            }
            return feature;
        }
        return null;
    }

    public void initializeOfflineSwitchLayout(DrawerLayout drawerLayout) {
        final LinearLayout offlineModeSwitchLayout = (LinearLayout) drawerLayout.findViewById(R.id.nav_offline_download);
        final Switch offlineSwitch = (Switch) drawerLayout.findViewById(R.id.nav_offlineModeSwitch);
        offlineSwitch.setChecked(isOfflineModeSwitchedOn());

        offlineSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offlineSwitchClicked(offlineSwitch);
            }
        });
        offlineModeSwitchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                offlineSwitch.toggle();
                offlineSwitchClicked(offlineSwitch);
            }
        });
    }

}
