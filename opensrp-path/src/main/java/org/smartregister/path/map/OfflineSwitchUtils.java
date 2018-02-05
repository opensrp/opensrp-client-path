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
import android.util.Log;
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
 * Provides utilities for:
 * <ul>
 *     <li>Adding listeners to the Switch layout</li>
 *     <li>Persisting the offline status</li>
 *     <li>Showing dialog box to confirm deletion of already downloaded maps</li>
 *     <li>Making an intent request for an Offline Map download</li>
 *     <li>Making an intent request for an Offline Map deletion</li>
 *     <li>Registering and unregistering the BroadcastReceiver for Offline Map updates(download progress & deletion activities)</li>
 *     <li>Retrieving children details from the database</li>
 *     <li>Converting children records to GeoJSON features</li>
 * </ul>
 *
 * Created by Ephraim Kigamba - ekigamba@ona.io on 02/02/2018.
 */

public class OfflineSwitchUtils {

    public static final String TAG = OfflineSwitchUtils.class.getName();
    private Context context;
    private AlertDialog deleteMapsDialog;
    private OfflineMapUpdatesReceiver offlineMapUpdatesReceiver;
    private ArrayList<LatLng> childPoints = new ArrayList<>();
    private String mapName;

    public OfflineSwitchUtils(@NonNull Context context, @NonNull OfflineMapUpdatesReceiver offlineMapUpdatesReceiver, @NonNull String mapName) {
        this.context = context;
        this.offlineMapUpdatesReceiver = offlineMapUpdatesReceiver;
        this.mapName = mapName;
    }

    private void offlineSwitchClicked(@NonNull Switch offlineSwitch) {
        updateOfflineModeStatus(offlineSwitch.isChecked());

        if (isOfflineModeSwitchedOn()) {
            registerOfflineMapDownloadUpdatesReceiver(offlineMapUpdatesReceiver);
            requestForOfflineMapDownload(getChildrenLocations());
        } else {
            // Request for a delete of the offline map or something
            if (offlineMapUpdatesReceiver.isDownloading()) {
                callStopDownloadIntent(offlineMapUpdatesReceiver.isDownloading());
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
    }

    private void updateOfflineModeStatus(boolean offlineModeStatus) {
        Utils.writePreference(context, PathConstants.PREFERENCE_OFFLINE_MODE, String.valueOf(offlineModeStatus));
    }

    /**
     * Checks if offline mode is activated from Preferences
     *
     * @return {@code true} or {@code false}
     */
    public boolean isOfflineModeSwitchedOn() {
        boolean defaultOfflineModeStatus = false;

        return Boolean.valueOf(Utils.getPreference(context, PathConstants.PREFERENCE_OFFLINE_MODE, String.valueOf(defaultOfflineModeStatus)));
    }

    /**
     * Registers a {@link BroadcastReceiver} with the {@link LocalBroadcastManager} that receives updates
     * from the Offline Map service. The updates are download progress updates, deletion action responses
     * & errors that occur during either downloads or deletion of Offline Maps
     *
     * @param broadcastReceiver
     */
    public void registerOfflineMapDownloadUpdatesReceiver(@NonNull BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_MAP_DOWNLOAD_SERVICE_STATUS_UPDATES));
    }

    /**
     * Unregisters the {@link BroadcastReceiver} that receives updates from the Offline Map service
     * registered using {@link OfflineSwitchUtils#registerOfflineMapDownloadUpdatesReceiver(BroadcastReceiver)}
     *
     * @param broadcastReceiver
     */
    public void unregisterOfflineMapDownloadUpdatesReceiver(@NonNull BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(broadcastReceiver);
    }

    private void requestForOfflineMapDownload(@NonNull LatLng[] mapPoints) {
        MapHelper mapHelper = new MapHelper();
        LatLng[] bounds = mapHelper.getBounds(mapPoints);

        offlineMapUpdatesReceiver.setWaitingForDownloadToStart(true);
        mapHelper.requestOfflineMap(context, mapName, "mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9", BuildConfig.MAPBOX_SDK_ACCESS_TOKEN, bounds[0], bounds[1], 11.1, 20.0);

        // Cache the style
        (new MapBoxWebServiceApi(context, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN))
                .retrieveStyleJSON("mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "Successfully downloaded Mapbox style for caching");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error trying to cache Mapbox style : " + error.getMessage());
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

    /**
     * Converts clients details to GeoJSON features by using the geopoint as the location and the
     * rest of the details as GeoJSON feature properties
     *
     * @param clientDetails
     * @return
     */
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

    /**
     * Adds listeners to the {@link Switch} and the encompassing {@link LinearLayout}
     *
     * @param drawerLayout
     */
    public void initializeOfflineSwitchLayout(@NonNull DrawerLayout drawerLayout) {
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
