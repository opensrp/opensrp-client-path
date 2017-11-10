package org.smartregister.path.map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Child;
import org.smartregister.view.contract.ECClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import io.ona.kujaku.activities.MapActivity;
import io.ona.kujaku.exceptions.InvalidMapBoxStyleException;
import io.ona.kujaku.helpers.MapBoxStyleHelper;
import io.ona.kujaku.helpers.converters.GeoJSONHelper;
import io.ona.kujaku.utils.Constants;

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

    public void launchMap(Activity activity, String stylePath, String[] geoJSONDataSources, String[] attachmentLayers, String mapBoxAccessToken) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        String finalStyle = combineStyleToGeoJSONStylePath(stylePath, geoJSONDataSources, attachmentLayers);

        Intent mapViewIntent = new Intent(Constants.INTENT_ACTION_SHOW_MAP);
        mapViewIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, mapBoxAccessToken);
        mapViewIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_STYLES, new String[]{finalStyle});

        activity.startActivityForResult(mapViewIntent, MAP_ACTIVITY_REQUEST_CODE);
    }

    private String combineStyleToGeoJSONStylePath(String stylePath, String[] geoJSONDataSources, String[] attachmentLayers) throws JSONException
            , InvalidMapBoxStyleException
            , IOException {
        // Expected to be local for offline
        String mapBoxStyle = readFile(stylePath);
        return combineStyleToGeoJSON(mapBoxStyle, geoJSONDataSources, attachmentLayers);
    }

    private String combineStyleToGeoJSON(String mapboxStyle, String[] geoJSONDataSources, String[] attachmentLayers) throws JSONException, InvalidMapBoxStyleException {
        JSONObject[] geoJsonDataSourceObjects = new JSONObject[geoJSONDataSources.length];

        for (int i = 0; i < geoJSONDataSources.length; i++) {
            geoJsonDataSourceObjects[i] = new JSONObject(geoJSONDataSources[i]);
        }

        return combineStyleToGeoJSON(new JSONObject(mapboxStyle), geoJsonDataSourceObjects, attachmentLayers).toString();
    }

    private JSONObject combineStyleToGeoJSON(JSONObject styleObject, JSONObject[] geoJSONDataSources, String[] attachmentLayers) throws JSONException, InvalidMapBoxStyleException {
        MapBoxStyleHelper mapBoxStyleHelper = new MapBoxStyleHelper(styleObject);

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
}
