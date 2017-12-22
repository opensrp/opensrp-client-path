package org.smartregister.path.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.json.JSONException;
import org.smartregister.Context;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.FetchStatus;
import org.smartregister.path.BuildConfig;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.map.MapHelper;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.repository.StockRepository;
import org.smartregister.path.service.intent.SyncService;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.ona.kujaku.data.realm.objects.MapBoxOfflineQueueTask;
import io.ona.kujaku.helpers.MapBoxWebServiceApi;
import io.ona.kujaku.services.MapboxOfflineDownloaderService;
import util.PathConstants;
import utils.Constants;
import utils.exceptions.InvalidMapBoxStyleException;
import utils.helpers.converters.GeoJSONFeature;
import utils.helpers.converters.GeoJSONHelper;

import static org.smartregister.immunization.util.VaccinatorUtils.nextVaccineDue;

import util.ServiceTools;

/**
 * Base activity class for path regiters views
 * Created by keyman.
 */
public abstract class BaseRegisterActivity extends SecuredNativeSmartRegisterActivity
        implements NavigationView.OnNavigationItemSelectedListener, SyncStatusBroadcastReceiver.SyncStatusListener {

    public static final String TAG = BaseRegisterActivity.class.getSimpleName();
    public static final String IS_REMOTE_LOGIN = "is_remote_login";
    private Snackbar syncStatusSnackbar;
    private ArrayList<LatLng> childPoints = new ArrayList<>();
    private Toast toast;

    private enum SERVICE_ACTION_RESULT {
        SUCCESSFUL,
        FAILED
    }
    private static final String OFFLINE_MAP_NAME = "ZEIR Services Coverage";
    private boolean waitingForDownloadToStart = false;
    private boolean isDownloading = false;

    private AlertDialog deleteMapsDialog;
    private BroadcastReceiver offlineMapDownloadUpdatesReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        BaseActivityToggle toggle = new BaseActivityToggle(this, drawer,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

            }
        };

        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            boolean isRemote = extras.getBoolean(IS_REMOTE_LOGIN);
            if (isRemote) {
                updateFromServer();
            }
        }

        offlineMapDownloadUpdatesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                Log.i("KUJAKU SAMPLE APP TAG", intent.getExtras().toString());

                if (intent.hasExtra(MapboxOfflineDownloaderService.KEY_RESULT_STATUS)) {
                    String resultStatus = intent.getStringExtra(MapboxOfflineDownloaderService.KEY_RESULT_STATUS);
                    MapboxOfflineDownloaderService.SERVICE_ACTION serviceAction = (MapboxOfflineDownloaderService.SERVICE_ACTION) intent.getExtras().get(MapboxOfflineDownloaderService.KEY_RESULTS_PARENT_ACTION);

                    if (serviceAction == MapboxOfflineDownloaderService.SERVICE_ACTION.DOWNLOAD_MAP) {
                        if (resultStatus.equals(SERVICE_ACTION_RESULT.SUCCESSFUL.name())) {
                            //Todo: Figure out how to better tell if the Service and/or SDK is downloading
                            isDownloading = true;
                            if (waitingForDownloadToStart) {
                                showInfoToast("Offline Map has started downloading");
                                waitingForDownloadToStart = false;
                            }
                        } else if (resultStatus.equals(SERVICE_ACTION_RESULT.FAILED.name())) {
                            //An error occurred trying to download the map
                            String message = "Oops! Offline map cannot be downloaded";
                            if (intent.hasExtra(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE)) {
                                message = intent.getStringExtra(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE);
                            }
                            isDownloading = false;
                            showInfoToast(message);
                        }
                    }
                } else {
                    Log.e(TAG, "Unknown OfflineMapDownloadService Message : \n" + intent.getExtras().toString());
                }
            }
        };

        if (isOfflineModeSwitchedOn()) {
            // Show a toast that I have started downloading based on response from the LocalBroadcastManager
            registerOfflineMapDownloadUpdatesReceiver(offlineMapDownloadUpdatesReceiver);
        }
    }

    @Override
    public void onSyncStart() {
        refreshSyncStatusViews(null);
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        refreshSyncStatusViews(fetchStatus);
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        refreshSyncStatusViews(fetchStatus);
    }

    public abstract void refreshList(FetchStatus fetchStatus);

    private void registerSyncStatusBroadcastReceiver() {
        SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(this);
    }

    private void unregisterSyncStatusBroadcastReceiver() {
        SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(this);
    }

    private void updateFromServer() {
        ServiceTools.startService(getApplicationContext(), SyncService.class);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSyncStatusBroadcastReceiver();
        initViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSyncStatusBroadcastReceiver();
    }

    private void initViews() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Button logoutButton = (Button) navigationView.findViewById(R.id.logout_b);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrishtiApplication application = (DrishtiApplication) getApplication();
                application.logoutCurrentUser();
                finish();
            }
        });

        ImageButton cancelButton = (ImageButton) navigationView.findViewById(R.id.cancel_b);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = (DrawerLayout) BaseRegisterActivity.this.findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        TextView initialsTV = (TextView) navigationView.findViewById(R.id.initials_tv);
        String preferredName = context().allSharedPreferences().getANMPreferredName(
                context().allSharedPreferences().fetchRegisteredANM());
        if (!TextUtils.isEmpty(preferredName)) {
            String[] initialsArray = preferredName.split(" ");
            String initials = "";
            if (initialsArray.length > 0) {
                initials = initialsArray[0].substring(0, 1);
                if (initialsArray.length > 1) {
                    initials = initials + initialsArray[1].substring(0, 1);
                }
            }

            initialsTV.setText(initials.toUpperCase());
        }

        TextView nameTV = (TextView) navigationView.findViewById(R.id.name_tv);
        nameTV.setText(preferredName);
        refreshSyncStatusViews(null);
        initializeCustomNavbarLIsteners();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_register) {
            startFormActivity("child_enrollment", null, null);
        } else if (id == R.id.nav_record_vaccination_out_catchment) {
            startFormActivity("out_of_catchment_service", null, null);
        } else if (id == R.id.stock) {
            Intent intent = new Intent(this, StockActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_sync) {
            startSync();
        } else if (id == R.id.nav_hia2) {
            Intent intent = new Intent(this, HIA2ReportsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_clients_map) {
            openMapViewOfChildren();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startSync() {
        ServiceTools.startService(getApplicationContext(), SyncService.class);
    }

    /////////////////////////for custom navigation //////////////////////////////////////////////////////
    private void refreshSyncStatusViews(FetchStatus fetchStatus) {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null && navigationView.getMenu() != null) {
            LinearLayout syncMenuItem = (LinearLayout) navigationView.findViewById(R.id.nav_sync);
            if (syncMenuItem != null) {
                if (SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                    ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                    if (syncStatusSnackbar != null) syncStatusSnackbar.dismiss();
                    syncStatusSnackbar = Snackbar.make(rootView, R.string.syncing,
                            Snackbar.LENGTH_LONG);
                    syncStatusSnackbar.show();
                    ((TextView) syncMenuItem.findViewById(R.id.nav_synctextview)).setText(R.string.syncing);
                } else {
                    if (fetchStatus != null) {
                        if (syncStatusSnackbar != null) syncStatusSnackbar.dismiss();
                        ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                        if (fetchStatus.equals(FetchStatus.fetchedFailed)) {
                            syncStatusSnackbar = Snackbar.make(rootView, R.string.sync_failed, Snackbar.LENGTH_INDEFINITE);
                            syncStatusSnackbar.setActionTextColor(getResources().getColor(R.color.snackbar_action_color));
                            syncStatusSnackbar.setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startSync();
                                }
                            });
                        } else if (fetchStatus.equals(FetchStatus.fetched)
                                || fetchStatus.equals(FetchStatus.nothingFetched)) {
                            syncStatusSnackbar = Snackbar.make(rootView, R.string.sync_complete, Snackbar.LENGTH_LONG);
                        } else if (fetchStatus.equals(FetchStatus.noConnection)) {
                            syncStatusSnackbar = Snackbar.make(rootView, R.string.sync_failed_no_internet, Snackbar.LENGTH_LONG);
                        }
                        syncStatusSnackbar.show();
                    }

//<<<<<<< HEAD
//                    if (!TextUtils.isEmpty(lastSync)) {
//                        lastSync = " " + String.format(getString(R.string.last_sync), lastSync);
//                    }
//                    ((TextView)syncMenuItem.findViewById(R.id.nav_synctextview)).setText(String.format(getString(R.string.sync_), lastSync));
//=======
                    updateLastSyncText();
                }
            }
        }
    }

    private void initializeCustomNavbarLIsteners() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        LinearLayout syncMenuItem = (LinearLayout) drawer.findViewById(R.id.nav_sync);
        syncMenuItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSync();
                drawer.closeDrawer(GravityCompat.START);
            }
        });
        LinearLayout addchild = (LinearLayout) drawer.findViewById(R.id.nav_register);
        addchild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFormActivity("child_enrollment", null, null);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout outofcatchment = (LinearLayout) drawer.findViewById(R.id.nav_record_vaccination_out_catchment);
        outofcatchment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFormActivity("out_of_catchment_service", null, null);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout stockregister = (LinearLayout) drawer.findViewById(R.id.stock_control);
        stockregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StockActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout hia2 = (LinearLayout) drawer.findViewById(R.id.hia2_reports);
        hia2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HIA2ReportsActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout childregister = (LinearLayout) drawer.findViewById(R.id.child_register);
        childregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VaccinatorApplication.setCrashlyticsUser(VaccinatorApplication.getInstance().context());
                Intent intent = new Intent(getApplicationContext(), ChildSmartRegisterActivity.class);
                intent.putExtra(BaseRegisterActivity.IS_REMOTE_LOGIN, false);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
            }
        });
        LinearLayout coverage = (LinearLayout) drawer.findViewById(R.id.coverage_reports);
        coverage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CoverageReportsActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout dropout = (LinearLayout) drawer.findViewById(R.id.dropout_reports);
        dropout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DropoutReportsActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);
            }
        });

        LinearLayout clientLocations = (LinearLayout) drawer.findViewById(R.id.nav_clients_map);
        clientLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.closeDrawer(GravityCompat.START);
                openMapViewOfChildren();
            }
        });

        final LinearLayout offlineModeSwitchLayout = (LinearLayout) drawer.findViewById(R.id.nav_offline_download);
        final Switch offlineSwitch = (Switch) drawer.findViewById(R.id.nav_offlineModeSwitch);
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

    private void offlineSwitchClicked(Switch offlineSwitch) {
        updateOfflineModeStatus(offlineSwitch.isChecked());
        if (isOfflineModeSwitchedOn()) {
            registerOfflineMapDownloadUpdatesReceiver(offlineMapDownloadUpdatesReceiver);
            requestForOfflineMap(getChildrenLocations());
        } else {
            // Request for a delete of the offline map or something
            if (isDownloading) {
                callStopDownloadIntent(isDownloading);
            } else {
                // Ask whether to delete the already downloaded offline maps
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_maps_dialog_title)
                        .setMessage(R.string.delete_maps_dialog_message)
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

    private void requestForOfflineMap(@NonNull LatLng[] mapPoints) {
        boolean isBoundsChanged = true;

        MapHelper mapHelper = new MapHelper();
        LatLng[] bounds = mapHelper.getBounds(mapPoints);
        /*LatLng[] bounds = new LatLng[]{
                new LatLng(
                        -17.854564,
                        25.854782
                ),
                new LatLng(
                        -17.875469,
                        25.876589
                )
        };*/

        String mapName = OFFLINE_MAP_NAME;

        waitingForDownloadToStart = true;
        mapHelper.requestOfflineMap(this, mapName, "mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9", BuildConfig.MAPBOX_SDK_ACCESS_TOKEN, bounds[0], bounds[1], 11.1, 20.0);
        // Cache the style
        (new MapBoxWebServiceApi(this, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN))
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

    private void showInfoToast(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void updateLastSyncText() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null && navigationView.getMenu() != null) {
            TextView syncMenuItem = ((TextView) navigationView.findViewById(R.id.nav_synctextview));
            if (syncMenuItem != null) {
                String lastSync = getLastSyncTime();

                if (!TextUtils.isEmpty(lastSync)) {
                    lastSync = " " + String.format(getString(R.string.last_sync), lastSync);
                }
                syncMenuItem.setText(String.format(getString(R.string.sync_), lastSync));
            }
        }
    }

    private String getLastSyncTime() {
        String lastSync = "";
        long milliseconds = ECSyncUpdater.getInstance(this).getLastCheckTimeStamp();
        if (milliseconds > 0) {
            DateTime lastSyncTime = new DateTime(milliseconds);
            DateTime now = new DateTime(Calendar.getInstance());
            Minutes minutes = Minutes.minutesBetween(lastSyncTime, now);
            if (minutes.getMinutes() < 1) {
                Seconds seconds = Seconds.secondsBetween(lastSyncTime, now);
                lastSync = seconds.getSeconds() + "s";
            } else if (minutes.getMinutes() >= 1 && minutes.getMinutes() < 60) {
                lastSync = minutes.getMinutes() + "m";
            } else if (minutes.getMinutes() >= 60 && minutes.getMinutes() < 1440) {
                Hours hours = Hours.hoursBetween(lastSyncTime, now);
                lastSync = hours.getHours() + "h";
            } else {
                Days days = Days.daysBetween(lastSyncTime, now);
                lastSync = days.getDays() + "d";
            }
        }
        return lastSync;
    }

    private void openMapViewOfChildren() {
        new DataTask().execute();
    }

    private GeoJSONFeature constructGeoJsonFeature(Map<String, String> clientDetails) {
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

    private class DataTask extends AsyncTask<Void, Void, Void> {
        String[] childGeoJson;
        ArrayList<String> attachmentLayers;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            attachmentLayers = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                childGeoJson = getChildrenGeoJSON();
                for (int i = 0; i < childGeoJson.length; i++) {
                    Log.e("geoJson", childGeoJson[i]);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        private String[] getChildrenGeoJSON()  throws JSONException {
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
                            added = true;

                            String dobString = Utils.getValue(details, PathConstants.KEY.DOB, false);
                            String alertLayer = ChildDetailTabbedActivity.getCurrentAlertLayer(entityId, dobString);

                            // Add GeoJSON Feature to Appropriate feature collection
                            ArrayList<GeoJSONFeature> layerFeatures = new ArrayList<>();
                            if (geoJSONFeatureCollection.containsKey(alertLayer)) {
                                layerFeatures = geoJSONFeatureCollection.get(alertLayer);
                            }

                            layerFeatures.add(feature);
                            geoJSONFeatureCollection.put(alertLayer, layerFeatures);

                            if (!attachmentLayers.contains(alertLayer)) {
                                attachmentLayers.add(alertLayer);
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

            if (added) {
                //childrenGeoJSON.add(new GeoJSONHelper(geoJSONFeatures.toArray(new GeoJSONFeature[geoJSONFeatures.size()])).getGeoJsonData().toString());
                for(String attachmentLayer: attachmentLayers) {
                    if (geoJSONFeatureCollection.containsKey(attachmentLayer)) {
                        ArrayList<GeoJSONFeature> geoJSONFeatures = geoJSONFeatureCollection.get(attachmentLayer);
                        childrenGeoJSON.add(
                                new GeoJSONHelper(geoJSONFeatures.toArray(new GeoJSONFeature[geoJSONFeatures.size()]))
                                .getGeoJsonData()
                        );
                    }
                }
            }

            return childrenGeoJSON.toArray(new String[childrenGeoJSON.size()]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (childGeoJson != null && attachmentLayers != null) {
                MapHelper mapHelper = new MapHelper();
                try {
                    String[] attachmentLayerArray = attachmentLayers.toArray(new String[attachmentLayers.size()]);
                    LatLng[] bounds = mapHelper.getBounds(childPoints);

                    mapHelper.launchMap(
                            BaseRegisterActivity.this,
                            "mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9",
                            mapHelper.constructKujakuConfig(attachmentLayerArray),
                            childGeoJson,
                            attachmentLayerArray,
                            BuildConfig.MAPBOX_SDK_ACCESS_TOKEN,
                            getLayersToDisable(attachmentLayerArray),
                            bounds[0],
                            bounds[1]
                    );
                } catch (JSONException | InvalidMapBoxStyleException | IOException e) {
                    Log.e("BaseRegisterActivity", Log.getStackTraceString(e));
                }
            }
        }
    }

    private String[] getLayersToDisable(String[] attachmentLayers) {
        return (new MapHelper())
                .getLayersToHide(attachmentLayers);
    }

    private void updateOfflineModeStatus(boolean offlineModeStatus) {
        Utils.writePreference(this, PathConstants.PREFERENCE_OFFLINE_MODE, String.valueOf(offlineModeStatus));
    }

    private boolean isOfflineModeSwitchedOn() {
        boolean defaultOfflineModeStatus = false;

        return Boolean.valueOf(Utils.getPreference(this, PathConstants.PREFERENCE_OFFLINE_MODE, String.valueOf(defaultOfflineModeStatus)));
    }

    private void registerOfflineMapDownloadUpdatesReceiver(BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_MAP_DOWNLOAD_SERVICE_STATUS_UPDATES));
    }

    private void unregisterOfflineMapDownloadUpdatesReceiver(BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver);
    }

    private void callStopDownloadIntent(boolean isMapDownloading) {
        Intent stopDownloadIntent = new Intent(this, MapboxOfflineDownloaderService.class);
        stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN);
        stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME, OFFLINE_MAP_NAME);
        stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_SERVICE_ACTION, ((isMapDownloading) ? MapboxOfflineDownloaderService.SERVICE_ACTION.STOP_CURRENT_DOWNLOAD : MapboxOfflineDownloaderService.SERVICE_ACTION.DELETE_MAP));
        if (isMapDownloading) {
            stopDownloadIntent.putExtra(Constants.PARCELABLE_KEY_DELETE_TASK_TYPE, MapBoxOfflineQueueTask.TASK_TYPE_DOWNLOAD);
        }

        startService(stopDownloadIntent);
    }

    @Override
    protected Context context() {
        return VaccinatorApplication.getInstance().context();
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private class BaseActivityToggle extends ActionBarDrawerToggle {

        private BaseActivityToggle(Activity activity, DrawerLayout drawerLayout, @StringRes int openDrawerContentDescRes, @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        /*public BaseActivityToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, @StringRes int openDrawerContentDescRes, @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }*/

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            if (!SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                updateLastSyncText();
            }
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
        }
    }


}

