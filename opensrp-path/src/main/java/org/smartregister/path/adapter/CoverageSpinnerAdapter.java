package org.smartregister.path.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.R;
import org.smartregister.path.domain.CoverageHolder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 12/22/17.
 */

public class CoverageSpinnerAdapter extends ArrayAdapter<CoverageHolder> {

    private Context context;
    private SimpleDateFormat simpleDateFormat;
    private String firstSuffix;
    private boolean toUpperCase = false;


    public CoverageSpinnerAdapter(Context context, int resource, List<CoverageHolder> objects, SimpleDateFormat simpleDateFormat) {
        super(context, resource, objects);
        this.context = context;
        this.simpleDateFormat = simpleDateFormat;
    }

    public void setFirstSuffix(String firstSuffix) {
        this.firstSuffix = firstSuffix;
    }

    public void setToUpperCase(boolean toUpperCase) {
        this.toUpperCase = toUpperCase;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_spinner, parent, false);
        } else {
            view = convertView;
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CoverageHolder holder = getItem(position);
            if (holder != null && holder.getDate() != null) {
                Date date = holder.getDate();
                String dateString = simpleDateFormat.format(date);
                if (toUpperCase) {
                    dateString = dateString.toUpperCase();
                }
                if (position == 0 && StringUtils.isNoneBlank(firstSuffix)) {
                    dateString = dateString + " " + firstSuffix;
                }
                textView.setText(dateString);
                textView.setTag(holder);
            }
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_spinner_drop_down, parent, false);
        } else {
            view = convertView;
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CoverageHolder coverageHolder = getItem(position);
            if (coverageHolder != null && coverageHolder.getDate() != null) {
                Date date = coverageHolder.getDate();

                String dateString = simpleDateFormat.format(date);
                if (toUpperCase) {
                    dateString = dateString.toUpperCase();
                }
                if (position == 0 && StringUtils.isNoneBlank(firstSuffix)) {
                    dateString = dateString + " " + firstSuffix;
                }
                textView.setText(dateString);
                textView.setTag(coverageHolder);
            }
        }
        return view;
    }
}
