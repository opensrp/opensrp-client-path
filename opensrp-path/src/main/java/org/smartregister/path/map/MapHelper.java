package org.smartregister.path.map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Child;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import io.ona.kujaku.activities.MapActivity;
import io.ona.kujaku.helpers.MapBoxWebServiceApi;
import io.ona.kujaku.services.MapboxOfflineDownloaderService;
import utils.Constants;
import utils.exceptions.InvalidMapBoxStyleException;
import utils.helpers.MapBoxStyleHelper;


/**
 * It interfaces with Kujaku library and simplifies calls to the library
 *
 * Created by Ephraim Kigamba - ekigamba@ona.io on 09/11/2017.
 */

public class MapHelper {

    private static final int MAP_ACTIVITY_REQUEST_CODE = 9892;
    private static final String TAG = MapHelper.class.getSimpleName();

    /**
     * Opens the MapActivity on Kujaku which renders the MapBox Style
     *
     * @param activity
     * @param stylePath
     * @param kujakuConfig
     * @param geoJSONDataSources
     * @param attachmentLayers
     * @param mapBoxAccessToken
     * @param layersToHide
     * @param topLeftBound
     * @param bottomRightBound
     * @throws JSONException
     * @throws InvalidMapBoxStyleException
     * @throws IOException
     */
    public void launchMap(final Activity activity, String stylePath, JSONObject kujakuConfig,
                          String[] geoJSONDataSources, String[] attachmentLayers, final String mapBoxAccessToken,
                          @Nullable String[] layersToHide, @Nullable final LatLng topLeftBound, final @Nullable LatLng bottomRightBound) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        combineMapBoxStyleSubSections(activity, mapBoxAccessToken, stylePath, kujakuConfig, geoJSONDataSources, attachmentLayers, layersToHide, new OnCreateMapBoxStyle() {
            @Override
            public void onStyleJSONRetrieved(String styleJSON) {
                callIntent(activity, mapBoxAccessToken, styleJSON, topLeftBound, bottomRightBound);
            }

            @Override
            public void onError(String error) {
                //Todo: Do something here
                Log.e(TAG, "Error occured Combining GeoJSON Style Path : " + error);
            }
        });

    }

    public JSONObject constructKujakuConfig(String[] attachmentLayerArray) throws JSONException {
        JSONArray dataSourceLayers = new JSONArray();
        for (int i = 0; i < attachmentLayerArray.length; i++) {
            dataSourceLayers.put(attachmentLayerArray[i]);
        }
        JSONArray sortFields = new JSONArray();
        JSONObject dateAddedSortField = new JSONObject();
        dateAddedSortField.put("type", "date");
        dateAddedSortField.put("data_field", "client_reg_date");
        sortFields.put(dateAddedSortField);

        JSONObject kujakuConfig = new JSONObject();
        kujakuConfig.put("sort_fields", sortFields);
        // Should convert these dataSourceLayers to dataSourceNames
        //kujakuConfig.put("data_source_names", dataSourceLayers);

        return kujakuConfig;
    }

    public void launchMap(final Activity activity, String stylePath, JSONObject kujakuConfig,
                          String[] geoJSONDataSources, String[] attachmentLayers, final String mapBoxAccessToken,
                          @Nullable LatLng topLeftBound, @Nullable LatLng bottomRightBound) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        launchMap(activity, stylePath, kujakuConfig, geoJSONDataSources, attachmentLayers, mapBoxAccessToken, null, topLeftBound, bottomRightBound);
    }

    private void callIntent(Activity activity, String mapBoxAccessToken, String finalStyle, @Nullable LatLng topLeftBound, @Nullable LatLng bottomRightBound) {
        Intent mapViewIntent = new Intent(activity, MapActivity.class);
        mapViewIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, mapBoxAccessToken);
        mapViewIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_STYLES, new String[]{finalStyle});

        if (topLeftBound != null & bottomRightBound != null) {
            mapViewIntent.putExtra(Constants.PARCELABLE_KEY_TOP_LEFT_BOUND, topLeftBound);
            mapViewIntent.putExtra(Constants.PARCELABLE_KEY_BOTTOM_RIGHT_BOUND, bottomRightBound);
        }

        activity.startActivityForResult(mapViewIntent, MAP_ACTIVITY_REQUEST_CODE);
    }

    public void requestOfflineMap(Context context, String mapName, String mapboxStyleUrl, String mapBoxAccessToken, LatLng topLeftBound, LatLng bottomRightBound, double minZoom, double maxZoom) {
        Intent intent = new Intent(context, MapboxOfflineDownloaderService.class);
        intent.putExtra(Constants.PARCELABLE_KEY_SERVICE_ACTION, MapboxOfflineDownloaderService.SERVICE_ACTION.DOWNLOAD_MAP);
        intent.putExtra(Constants.PARCELABLE_KEY_STYLE_URL, mapboxStyleUrl);
        intent.putExtra(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME, mapName);
        intent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, mapBoxAccessToken);
        intent.putExtra(Constants.PARCELABLE_KEY_TOP_LEFT_BOUND, topLeftBound);
        intent.putExtra(Constants.PARCELABLE_KEY_BOTTOM_RIGHT_BOUND, bottomRightBound);
        intent.putExtra(Constants.PARCELABLE_KEY_MIN_ZOOM, minZoom);
        intent.putExtra(Constants.PARCELABLE_KEY_MAX_ZOOM, maxZoom);

        context.startService(intent);
    }

    private void combineMapBoxStyleSubSections(Activity activity, String mapBoxAccessToken, final String stylePath, final JSONObject kujakuConfig, final String[] geoJSONDataSources, final String[] attachmentLayers, final String[] layersToHide, final OnCreateMapBoxStyle onCreateMapBoxStyle) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        // Expected to be local for offline
        String mapBoxStyle = "";
        String mergedMapBoxStyle = "";
        if (stylePath.startsWith("mapbox://")) {
            MapBoxWebServiceApi mapBoxWebServiceApi = new MapBoxWebServiceApi(activity, mapBoxAccessToken);
            mapBoxWebServiceApi.retrieveStyleJSON(stylePath, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    String myMergedStyle = "";
                    try {
                        myMergedStyle = combineMapBoxStyleSubSections(response, kujakuConfig, geoJSONDataSources, attachmentLayers, layersToHide);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (InvalidMapBoxStyleException e) {
                        e.printStackTrace();
                    }
                    onCreateMapBoxStyle.onStyleJSONRetrieved(myMergedStyle);

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onCreateMapBoxStyle.onError(error.getMessage());
                }
            });
        } else if (stylePath.startsWith("file://")) {
            mapBoxStyle = readFile(stylePath);
            mergedMapBoxStyle = combineMapBoxStyleSubSections(mapBoxStyle, kujakuConfig, geoJSONDataSources, attachmentLayers, layersToHide);
            onCreateMapBoxStyle.onStyleJSONRetrieved(mergedMapBoxStyle);
        } else if (stylePath.startsWith("asset://")) {
            // Todo: Complete this
            mapBoxStyle = "";
            mergedMapBoxStyle = combineMapBoxStyleSubSections(mapBoxStyle, kujakuConfig, geoJSONDataSources, attachmentLayers, layersToHide);
            onCreateMapBoxStyle.onStyleJSONRetrieved(mergedMapBoxStyle);
        }
    }

    private String combineMapBoxStyleSubSections(String mapboxStyle, JSONObject kujakuConfig, String[] geoJSONDataSources, String[] attachmentLayers, @Nullable String[] layersToHide) throws JSONException, InvalidMapBoxStyleException {
        JSONObject[] geoJsonDataSourceObjects = new JSONObject[geoJSONDataSources.length];

        for (int i = 0; i < geoJSONDataSources.length; i++) {
            geoJsonDataSourceObjects[i] = new JSONObject(geoJSONDataSources[i]);
        }

        return combineMapBoxStyleSubSections(new JSONObject(mapboxStyle), kujakuConfig, geoJsonDataSourceObjects, attachmentLayers, layersToHide).toString();
    }

    private JSONObject combineMapBoxStyleSubSections(JSONObject styleObject, JSONObject kujakuConfig, JSONObject[] geoJSONDataSources, String[] attachmentLayers, @Nullable String[] layersToHide) throws JSONException, InvalidMapBoxStyleException {
        MapBoxStyleHelper mapBoxStyleHelper = new MapBoxStyleHelper(styleObject);
        mapBoxStyleHelper.disableLayers(layersToHide);
        String sourceName = "opensrp-custom-data-source";
        JSONArray kujakuDataSourceNames = new JSONArray();

        for (int i = 0; i < geoJSONDataSources.length; i++) {
            String dataSourceName = sourceName + "-" + i;
            mapBoxStyleHelper.insertGeoJsonDataSource(dataSourceName, geoJSONDataSources[i], attachmentLayers[i]);
            kujakuDataSourceNames.put(dataSourceName);
        }

        if (kujakuConfig != null) {
            // Add correct source layer names
            kujakuConfig.put("data_source_names", kujakuDataSourceNames);
            mapBoxStyleHelper.insertKujakuConfig(kujakuConfig);
        }

        return mapBoxStyleHelper.getStyleObject();
    }

    private String readFile(String filePath) throws FileNotFoundException
            , IOException{
        File file = new File(filePath);

        if (file.exists()) {
            StringBuffer output = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
            String line = "";
            while ((line = br.readLine()) != null) {
                output.append(line +"\n");
            }
            return output.toString();
        }

        return "";
    }

    public LatLng generateRandomLatLng(LatLng topLeftBound, LatLng bottomRightBound) {
        double latDisplacement = Math.random() * (topLeftBound.getLatitude() - bottomRightBound.getLatitude());
        double lngDisplacement = Math.random() * (bottomRightBound.getLongitude() - topLeftBound.getLongitude());

        return new LatLng(
                bottomRightBound.getLatitude() + latDisplacement,
                topLeftBound.getLongitude() + lngDisplacement
        );
    }

    // Todo: Remove this coordinates for Livingstone area && Replace with world-limits
    public LatLng generateRandomLatLng() {
        return generateRandomLatLng(
                new LatLng(
                        -17.854564,
                        25.854782
                ),
                new LatLng(
                        -17.875469,
                        25.876589
                )
        );
    }

    public LatLng[] getBounds(@NonNull ArrayList<LatLng> points) {
        return getBounds(points.toArray(new LatLng[points.size()]));
    }

    public LatLng[] getBounds(@NonNull LatLng[] points) {
        if (points.length < 1) {
            return new LatLng[]{
                    new LatLng(
                            -17.854564,
                            25.854782
                    ),
                    new LatLng(
                            -17.875469,
                            25.876589
                    )};
        }

        LatLng highestPoint = points[0];
        LatLng lowestPoint = points[0];

        if (points.length == 1) {
            // Create a default 0.021103 bound length
            double defaultBoundLength = 0.021103;
            double halfBoundLength = defaultBoundLength/2;
            highestPoint = new LatLng(
                    highestPoint.getLatitude() + halfBoundLength,
                    highestPoint.getLongitude() - halfBoundLength
            );

            lowestPoint = new LatLng(
                    lowestPoint.getLatitude() - halfBoundLength,
                    lowestPoint.getLongitude() + halfBoundLength
            );

            return (new LatLng[]{highestPoint, lowestPoint});
        }

        for(LatLng latLng: points) {
            if (isLatLngHigher(latLng, highestPoint)) {
                highestPoint = new LatLng(latLng.getLatitude(), latLng.getLongitude());
            }

            if (isLatLngLower(latLng, lowestPoint)) {
                lowestPoint = new LatLng(latLng.getLatitude(), latLng.getLongitude());
            }
        }

        // If the bounds are less than 0.021103 apart either longitude or latitude, then increase the bound to a minimum of that
        double defaultBoundDifference = 0.021103;
        double latDifference = highestPoint.getLatitude() - lowestPoint.getLatitude();
        double lngDifference = highestPoint.getLongitude() - lowestPoint.getLongitude();
        if (latDifference < defaultBoundDifference) {
            double increaseDifference = (defaultBoundDifference - latDifference)/2;
            highestPoint.setLatitude(highestPoint.getLatitude() + increaseDifference);
            lowestPoint.setLatitude(lowestPoint.getLatitude() - increaseDifference);
        }

        if (lngDifference < defaultBoundDifference) {
            double increaseDifference = (defaultBoundDifference - lngDifference)/2;
            highestPoint.setLongitude(highestPoint.getLongitude() - increaseDifference);
            lowestPoint.setLongitude(lowestPoint.getLongitude() + increaseDifference);
        }

        return (new LatLng[]{highestPoint, lowestPoint});
    }

    /**
     * Returns the array of layers to be hidden given the layers that have data
     *
     * @param layersBeingUsed
     * @return
     */
    public String[] getLayersToHide(String[] layersBeingUsed) {
        String[] allLayers = new String[] {
                "red kids",
                "white kids",
                "blue kids",
                "light blue kids",
                "green kids"
        };

        ArrayList<String> layersList = new ArrayList<>(Arrays.asList(allLayers));

        for (Iterator<String> layersIterator = layersList.iterator(); layersIterator.hasNext();) {
            String layer = layersIterator.next();

            for (String layerBeingUsed: layersBeingUsed) {
                if (layerBeingUsed.equals(layer)) {
                    layersIterator.remove();
                }
            }
        }

        return layersList.toArray(new String[layersList.size()]);
    }

    public LatLng getTopLeftBound(@NonNull LatLng[] points) {
        if (points.length < 1) {
            return new LatLng(
                    -17.854564,
                    25.854782
            );
        }

        LatLng highestPoint = points[0];

        for(LatLng latLng: points) {
            if (isLatLngHigher(latLng, highestPoint)) {
                highestPoint = latLng;
            }
        }

        return highestPoint;
    }

    public LatLng getBottomRightBound(@NonNull LatLng[] points) {
        if (points.length < 1) {
            return new LatLng(
                    -17.875469,
                    25.876589
            );
        }

        LatLng lowestPoint = points[0];

        for (LatLng latLng : points) {
            if (isLatLngLower(latLng, lowestPoint)) {
                lowestPoint = latLng;
            }
        }

        return lowestPoint;
    }

    public boolean isLatLngHigher(LatLng firstCoord, LatLng secondCoord) {
        if ((firstCoord.getLatitude() >= secondCoord.getLatitude()) && (firstCoord.getLatitude() <= secondCoord.getLongitude()) ) {
            return true;
        }

        return false;
    }

    public boolean isLatLngLower(LatLng firstCoord, LatLng secondCoord) {
        if ((firstCoord.getLatitude() <= secondCoord.getLatitude()) && (firstCoord.getLongitude() >= secondCoord.getLongitude())) {
            return true;
        }

        return false;
    }

    public interface OnCreateMapBoxStyle {

        void onStyleJSONRetrieved(String styleJSON);

        void onError(String error);
    }
}
