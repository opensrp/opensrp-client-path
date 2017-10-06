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
    private static final int HIA2_REPORT_FORM_HINT_FONT_SIZE = 51;

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
            editText.setFloatingLabelTextSize(HIA2_REPORT_FORM_HINT_FONT_SIZE);
            editText.addTextChangedListener(new HIA2ReportFormTextWatcher(formFragment, jsonObject.get(PathConstants.KEY.HIA_2_INDICATOR).toString()));
        }
    }

    @Override
    public List<View> getViewsFromJson(String stepName, Context context, JsonFormFragment formFragment, JSONObject jsonObject, CommonListener listener) throws Exception {
        if (jsonObject.has(PathConstants.KEY.NUMBER_PICKER) && jsonObject.get(PathConstants.KEY.NUMBER_PICKER).toString().equalsIgnoreCase(Boolean.TRUE.toString())) {
            List<View> views = new ArrayList<>(1);

            RelativeLayout rootLayout = (RelativeLayout) LayoutInflater.from(context).inflate(
                    R.layout.item_edit_text_number_picker, null);
            final MaterialEditText editText = (MaterialEditText) rootLayout.findViewById(R.id.edit_text);

            attachJson(stepName, context, formFragment, jsonObject, editText);

            JSONArray canvasIds = new JSONArray();
            rootLayout.setId(ViewUtil.generateViewId());
            canvasIds.put(rootLayout.getId());
            editText.setTag(com.vijay.jsonwizard.R.id.canvas_ids, canvasIds.toString());

            ((JsonApi) context).addFormDataView(editText);
            views.add(rootLayout);

            Button plusbutton = (Button) rootLayout.findViewById(R.id.addbutton);
            Button minusbutton = (Button) rootLayout.findViewById(R.id.minusbutton);

            plusbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String edittesxtstring = editText.getText().toString();
                    if (edittesxtstring.equalsIgnoreCase("")) {
                        editText.setText("0");
                    } else {
                        edittesxtstring = "" + (Integer.parseInt(edittesxtstring) + 1);
                        editText.setText(edittesxtstring);
                    }
                }
            });
            minusbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String edittesxtstring = editText.getText().toString();
                    if (edittesxtstring.equalsIgnoreCase("")) {
                        editText.setText("0");
                    } else {
                        edittesxtstring = "" + (Integer.parseInt(edittesxtstring) - 1);
                        editText.setText(edittesxtstring);
                    }
                }
            });

            editText.setInputType(InputType.TYPE_CLASS_NUMBER |
                    InputType.TYPE_NUMBER_FLAG_SIGNED);


            return views;
        } else {
            return super.getViewsFromJson(stepName, context, formFragment, jsonObject, listener);
        }


    }

}
