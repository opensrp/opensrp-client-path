/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.domain.ProfileImage;
import org.smartregister.path.domain.EditWrapper;
import org.smartregister.repository.ImageRepository;

import java.io.File;
import java.text.SimpleDateFormat;


/**
 * @author Maimoona
 *         Class containing some static utility methods.
 */
public class Utils {
    private static final String TAG = "Utils";
    private static final SimpleDateFormat UI_DF = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat UI_DTF = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final SimpleDateFormat DB_DF = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DB_DTF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Utils() {
    }

    public static TableRow getDataRow(Context context, String label, String value, TableRow row) {
        TableRow tr = row;
        if (row == null) {
            tr = new TableRow(context);
            TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tr.setLayoutParams(trlp);
            tr.setPadding(10, 5, 10, 5);
        }

        TextView l = new TextView(context);
        l.setText(label + ": ");
        l.setPadding(20, 2, 20, 2);
        l.setTextColor(Color.BLACK);
        l.setTextSize(14);
        l.setBackgroundColor(Color.WHITE);
        tr.addView(l);

        TextView v = new TextView(context);
        v.setText(value);
        v.setPadding(20, 2, 20, 2);
        v.setTextColor(Color.BLACK);
        v.setTextSize(14);
        v.setBackgroundColor(Color.WHITE);
        tr.addView(v);

        return tr;
    }

    public static TableRow getDataRow(Context context, String label, String value, String field, TableRow row) {
        TableRow tr = row;
        if (row == null) {
            tr = new TableRow(context);
            TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tr.setLayoutParams(trlp);
            tr.setPadding(10, 5, 10, 5);
        }

        TextView l = new TextView(context);
        l.setText(label + ": ");
        l.setPadding(20, 2, 20, 2);
        l.setTextColor(Color.BLACK);
        l.setTextSize(14);
        l.setBackgroundColor(Color.WHITE);
        tr.addView(l);

        EditWrapper editWrapper = new EditWrapper();
        editWrapper.setCurrentValue(value);
        editWrapper.setField(field);

        EditText e = new EditText(context);
        e.setTag(editWrapper);
        e.setText(value);
        e.setPadding(20, 2, 20, 2);
        e.setTextColor(Color.BLACK);
        e.setTextSize(14);
        e.setBackgroundColor(Color.WHITE);
        e.setInputType(InputType.TYPE_NULL);
        tr.addView(e);

        return tr;
    }

    public static TableRow getDataRow(Context context) {
        TableRow tr = new TableRow(context);
        TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tr.setLayoutParams(trlp);
        tr.setPadding(0, 0, 0, 0);
        // tr.setBackgroundColor(Color.BLUE);
        return tr;
    }

    public static int addAsInts(boolean ignoreEmpty, String... vals) {
        int i = 0;
        for (String v : vals) {
            i += ignoreEmpty && StringUtils.isBlank(v) ? 0 : Integer.parseInt(v);
        }
        return i;
    }

    public static TableRow addToRow(Context context, String value, TableRow row) {
        return addToRow(context, value, row, false, 1);
    }

    public static TableRow addToRow(Context context, String value, TableRow row, int weight) {
        return addToRow(context, value, row, false, weight);
    }

    public static TableRow addToRow(Context context, String value, TableRow row, boolean compact) {
        return addToRow(context, value, row, compact, 1);
    }

    public static TableRow addToRow(Context context, String value, TableRow row, boolean compact, int weight) {
        return addToRow(context, Html.fromHtml(value), row, compact, weight);
    }

    public static TableRow addToRow(Context context, Spanned value, TableRow row, boolean compact, int weight) {
        TextView v = new TextView(context);
        v.setText(value);
        if (compact) {
            v.setPadding(15, 4, 1, 1);
        } else {
            v.setPadding(2, 15, 2, 15);
        }
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT, weight
        );
        params.setMargins(0, 0, 1, 0);
        v.setLayoutParams(params);
        v.setTextColor(Color.BLACK);
        v.setTextSize(14);
        v.setBackgroundColor(Color.WHITE);
        row.addView(v);

        return row;
    }


    public static void setProfiePic(Context context, ImageView mImageView, String entityId, Object watermark) {
        ProfileImage photo = ((ImageRepository) org.smartregister.Context.getInstance().imageRepository()).findByEntityId(entityId);
        if (photo != null) {
            setProfiePicFromPath(context, mImageView, photo.getFilepath(), watermark);
        }
    }

    public static void setProfiePicFromPath(Context context, ImageView mImageView, String photoPath, Object watermark) {
        mImageView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        if (watermark == null) {
            Picasso.with(context).load(new File(photoPath)).into(mImageView);
        } else {
            Picasso.with(context).load(new File(photoPath))
                    .transform(new WatermarkTransformation(watermark))
                    .into(mImageView);
        }
    }

    public static void setProfiePic(Context context, ImageView mImageView, int photoResId, Object watermark) {
        mImageView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        if (watermark == null) {
            Picasso.with(context).load(photoResId).into(mImageView);
        } else {
            Picasso.with(context).load(photoResId)
                    .transform(new WatermarkTransformation(watermark))
                    .into(mImageView);
        }
    }


}