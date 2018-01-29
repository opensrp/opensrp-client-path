package org.smartregister.path.adapter;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.apache.commons.lang3.tuple.Triple;
import org.smartregister.path.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by keyman on 12/06/2017.
 */
public class ExpandedListAdapter<K, L, T> extends BaseExpandableListAdapter {

    private final Context context;
    private LinkedHashMap<K, List<ItemData<L, T>>> map = new LinkedHashMap<>();
    private List<K> headers = new ArrayList<>();
    private final int headerLayout;
    private final int childLayout;


    public ExpandedListAdapter(Context context, LinkedHashMap<K, List<ItemData<L, T>>> map, int headerLayout, int childLayout) {
        this.context = context;
        if (map != null && !map.isEmpty()) {
            this.map = map;
            for (Map.Entry<K, List<ItemData<L, T>>> entry : map.entrySet()) {
                this.headers.add(entry.getKey());
            }
        }

        this.headerLayout = headerLayout;
        this.childLayout = childLayout;


    }

    @Override
    public ItemData<L, T> getChild(int groupPosition, int childPosititon) {
        return map.get(headers.get(groupPosition)).get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(childLayout, null);
        }

        ItemData<L, T> childObject = getChild(groupPosition, childPosition);
        if (childObject != null) {

            String text = null;
            String details = null;
            String other = null;

            if (childObject.getLabelData() instanceof String) {
                text = (String) getChild(groupPosition, childPosition).getLabelData();

            } else if (childObject.getLabelData() instanceof Pair) {
                Pair<String, String> pair = (Pair<String, String>) getChild(groupPosition, childPosition).getLabelData();
                text = pair.first;
                details = pair.second;
            } else if (childObject.getLabelData() instanceof Triple) {
                Triple<String, String, String> triple = (Triple<String, String, String>) getChild(groupPosition, childPosition).getLabelData();
                text = triple.getLeft();
                details = triple.getMiddle();
                other = triple.getRight();

            }

            View tvView = convertView.findViewById(R.id.tv);
            if (tvView != null && text != null) {
                TextView tv = (TextView) tvView;
                tv.setText(text);
                convertView.setTag(text);
            }

            View detailView = convertView.findViewById(R.id.details);
            if (detailView != null && details != null) {
                TextView detailTextView = (TextView) detailView;
                detailTextView.setText(details);
            }

            View otherView = convertView.findViewById(R.id.other);
            if (otherView != null && other != null) {
                TextView otherTextView = (TextView) otherView;
                otherTextView.setText(other);
            }
        }

        View dividerBottom = convertView.findViewById(R.id.adapter_divider_bottom);
        if (dividerBottom != null) {
            boolean lastChild = (getChildrenCount(groupPosition) - 1) == childPosition;
            if (lastChild) {
                dividerBottom.setVisibility(View.VISIBLE);
            } else {
                dividerBottom.setVisibility(View.GONE);
            }
        }

        convertView.setTag(R.id.item_data, childObject.getTagData());

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return map.get(headers.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return headers.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(headerLayout, null);
        }

        Object group = getGroup(groupPosition);

        if (group != null) {
            String header = null;
            String details = null;

            if (group instanceof String) {
                header = group.toString();
            } else if (group instanceof Pair) {
                Pair<String, String> pair = (Pair<String, String>) group;
                header = pair.first;
                details = pair.second;
            }

            View tvView = convertView.findViewById(R.id.tv);
            if (tvView != null) {
                TextView tv = (TextView) tvView;
                tv.setText(header);
                convertView.setTag(header);
            }

            View detailView = convertView.findViewById(R.id.details);
            if (detailView != null && details != null) {
                TextView detailTextView = (TextView) detailView;
                detailTextView.setText(details);
            }

        }


        ExpandableListView mExpandableListView = (ExpandableListView) parent;
        mExpandableListView.expandGroup(groupPosition);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public static class ItemData<L, T> {
        private final L labelData;
        private final T tagData;

        public ItemData(L labelData, T tagData) {
            this.labelData = labelData;
            this.tagData = tagData;
        }

        public L getLabelData() {
            return labelData;
        }

        public T getTagData() {
            return tagData;
        }
    }
}
