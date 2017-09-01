package org.smartregister.path.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.smartregister.path.R;
import org.smartregister.path.activity.BaseActivity;
import org.smartregister.path.view.SiblingPicture;

import java.util.ArrayList;

/**
 * Created by Jason Rogena - jrogena@ona.io on 09/05/2017.
 */

public class SiblingPictureAdapter extends RecyclerView.Adapter<SiblingPicture> {

    private final BaseActivity baseActivity;
    private final ArrayList<String> siblingIds;

    public SiblingPictureAdapter(BaseActivity baseActivity, ArrayList<String> siblingIds) {
        this.baseActivity = baseActivity;
        this.siblingIds = siblingIds;
    }

    @Override
    public SiblingPicture onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(baseActivity)
                .inflate(R.layout.view_sibling_picture, parent, false);
        return new SiblingPicture(view);
    }

    @Override
    public void onBindViewHolder(SiblingPicture siblingPicture, int position) {
        if (siblingIds.size() > position) {
            siblingPicture.setChildBaseEntityId(baseActivity, siblingIds.get(position));
        }
    }

    @Override
    public long getItemId(int position) {
        return 4223 + position;
    }

    @Override
    public int getItemCount() {
        return siblingIds.size();
    }

}
