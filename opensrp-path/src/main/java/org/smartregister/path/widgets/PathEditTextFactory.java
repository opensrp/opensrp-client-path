package org.smartregister.path.widgets;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.rey.material.util.ViewUtil;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.widgets.EditTextFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.path.R;
import org.smartregister.path.watchers.HIA2ReportFormTextWatcher;
import org.smartregister.path.watchers.LookUpTextWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import util.PathConstants;

/**
 * Created by keyman on 11/04/2017.
 */
public class PathEditTextFactory extends EditTextFactory {

    @Override
    public void attachJson(String stepName, Context context, JsonFormFragment formFragment, JSONObject jsonObject, MaterialEditText editText) throws Exception {
        super.attachJson(stepName, context, formFragment, jsonObject, editText);
        // lookup hook
        if (jsonObject.has(PathConstants.KEY.LOOK_UP) && jsonObject.get(PathConstants.KEY.LOOK_UP).toString().equalsIgnoreCase(Boolean.TRUE.toString())) {

            String entityId = jsonObject.getString(PathConstants.KEY.ENTITY_ID);

            Map<String, List<View>> lookupMap = formFragment.getLookUpMap();
            List<View> lookUpViews = new ArrayList<>();
            if (lookupMap.containsKey(entityId)) {
                lookUpViews = lookupMap.get(entityId);
            }

            if (!lookUpViews.contains(editText)) {
                lookUpViews.add(editText);
            }
            lookupMap.put(entityId, lookUpViews);

            editText.addTextChangedListener(new LookUpTextWatcher(formFragment, editText, entityId));
            editText.setTag(com.vijay.jsonwizard.R.id.after_look_up, false);
        }

        if (jsonObject.has(PathConstants.KEY.HIA_2_INDICATOR)) {
            editText.setTag(jsonObject.get(PathConstants.KEY.HIA_2_INDICATOR));
            editText.setFloatingLabelTextSize((int) context.getResources().getDimension(R.dimen.hia2_indicator_hint_font_size));
            editText.setTextSize((int) context.getResources().getDimension(R.dimen.hia2_indicator_default_font_size));

            editText.addTextChangedListener(new HIA2ReportFormTextWatcher(formFragment, jsonObject.get(PathConstants.KEY.HIA_2_INDICATOR).toString()));
        }
    }
}