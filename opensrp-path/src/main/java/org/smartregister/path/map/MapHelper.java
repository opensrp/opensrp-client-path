package org.smartregister.path.map;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Child;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import io.ona.kujaku.activities.MapActivity;
import io.ona.kujaku.helpers.MapBoxWebServiceApi;
import utils.Constants;
import utils.exceptions.InvalidMapBoxStyleException;
import utils.helpers.MapBoxStyleHelper;


/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 09/11/2017.
 */

public class MapHelper {

    private static final int MAP_ACTIVITY_REQUEST_CODE = 9892;

    public void callMap() {}

    public void convertChildrenToFeatures(Child[] childrenGroups, String[] layersToCombine, String[] childPropertiesToAdd) {
        // Add the child gps coordinates to geometry & child details to properties
    }

    public void convertChildrenToFeatures(ArrayList<Child> childrenGroups, ArrayList<String> layersToCombine) {
        convertChildrenToFeatures(childrenGroups.toArray(new Child[childrenGroups.size()]), layersToCombine.toArray(new String[layersToCombine.size()]), new String[]{});
    }

    public void launchMap(final Activity activity, String stylePath, String[] geoJSONDataSources, String[] attachmentLayers, final String mapBoxAccessToken, @Nullable String[] layersToHide) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        combineStyleToGeoJSONStylePath(activity, mapBoxAccessToken, stylePath, geoJSONDataSources, attachmentLayers, layersToHide, new OnCreateMapBoxStyle() {
            @Override
            public void onStyleJSONRetrieved(String styleJSON) {
                callIntent(activity, mapBoxAccessToken, styleJSON);
            }

            @Override
            public void onError(String error) {
                //Todo: Do something here
            }
        });

    }

    public void launchMap(final Activity activity, String stylePath, String[] geoJSONDataSources, String[] attachmentLayers, final String mapBoxAccessToken) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        launchMap(activity, stylePath, geoJSONDataSources, attachmentLayers, mapBoxAccessToken, null);
    }

    private void callIntent(Activity activity, String mapBoxAccessToken, String finalStyle) {
        Intent mapViewIntent = new Intent(activity, MapActivity.class);
        mapViewIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, mapBoxAccessToken);
        mapViewIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_STYLES, new String[]{finalStyle});

        activity.startActivityForResult(mapViewIntent, MAP_ACTIVITY_REQUEST_CODE);
    }

    private void combineStyleToGeoJSONStylePath(Activity activity, String mapBoxAccessToken, final String stylePath, final String[] geoJSONDataSources, final String[] attachmentLayers, final String[] layersToHide, final OnCreateMapBoxStyle onCreateMapBoxStyle) throws JSONException
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
                        myMergedStyle = combineStyleToGeoJSON(response, geoJSONDataSources, attachmentLayers, layersToHide);
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
            mergedMapBoxStyle = combineStyleToGeoJSON(mapBoxStyle, geoJSONDataSources, attachmentLayers, layersToHide);
            onCreateMapBoxStyle.onStyleJSONRetrieved(mergedMapBoxStyle);
        } else if (stylePath.startsWith("asset://")) {
            // Todo: Complete this
            mapBoxStyle = "";
            mergedMapBoxStyle = combineStyleToGeoJSON(mapBoxStyle, geoJSONDataSources, attachmentLayers, layersToHide);
            onCreateMapBoxStyle.onStyleJSONRetrieved(mergedMapBoxStyle);
        }
    }

    private String combineStyleToGeoJSON(String mapboxStyle, String[] geoJSONDataSources, String[] attachmentLayers, @Nullable String[] layersToHide) throws JSONException, InvalidMapBoxStyleException {
        JSONObject[] geoJsonDataSourceObjects = new JSONObject[geoJSONDataSources.length];

        for (int i = 0; i < geoJSONDataSources.length; i++) {
            geoJsonDataSourceObjects[i] = new JSONObject(geoJSONDataSources[i]);
        }

        return combineStyleToGeoJSON(new JSONObject(mapboxStyle), geoJsonDataSourceObjects, attachmentLayers, layersToHide).toString();
    }

    private JSONObject combineStyleToGeoJSON(JSONObject styleObject, JSONObject[] geoJSONDataSources, String[] attachmentLayers, @Nullable String[] layersToHide) throws JSONException, InvalidMapBoxStyleException {
        MapBoxStyleHelper mapBoxStyleHelper = new MapBoxStyleHelper(styleObject);
        mapBoxStyleHelper.disableLayers(layersToHide);
        String sourceName = "opensrp-custom-data-source";

        for (int i = 0; i < geoJSONDataSources.length; i++) {
            mapBoxStyleHelper.insertGeoJsonDataSource(sourceName + "-" + i, geoJSONDataSources[i], attachmentLayers[i]);
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
                        -17.876469,
                        25.877589
                )
        );
    }

    public LatLng[] getBounds(@NonNull LatLng[] points) {
        if (points.length < 1) {
            return null;
        }

        LatLng highestPoint = points[0];
        LatLng lowestPoint = points[0];

        for(LatLng latLng: points) {
            if (isLatLngHigher(latLng, highestPoint)) {
                highestPoint = latLng;
            }

            if (isLatLngLower(latLng, lowestPoint)) {
                lowestPoint = latLng;
            }
        }

        return (new LatLng[]{highestPoint, lowestPoint});
    }

    public LatLng getTopLeftBound(@NonNull LatLng[] points) {
        if (points.length < 1) {
            return null;
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
            return null;
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
