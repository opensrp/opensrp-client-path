package org.smartregister.path.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.smartregister.path.R;
import org.smartregister.path.activity.AnnualCoverageReportCsoActivity;
import org.smartregister.path.activity.BaseActivity;
import org.smartregister.path.activity.BaseReportActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.CoverageHolder;
import org.smartregister.path.view.LocationPickerView;

import util.JsonFormUtils;

/**
 * Created by keyman on 22/12/17.
 */
public class SetCsoDialogFragment extends DialogFragment {

    private CoverageHolder holder;
    private OnSetCsoListener listener;

    public static SetCsoDialogFragment newInstance(CoverageHolder holder) {
        SetCsoDialogFragment f = new SetCsoDialogFragment();
        f.setHolder(holder);
        return f;
    }

    public void setHolder(CoverageHolder holder) {
        this.holder = holder;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.dialog_fragment_set_cso,
                container, false);


        TextView title = (TextView) view.findViewById(R.id.cso_title);
        title.setText(String.format(getString(R.string.set_cso_title), BaseReportActivity.getYear(holder.getDate())));

        final EditText setCso = (EditText) view.findViewById(R.id.set_cso);
        setCso.setHint(String.format(getString(R.string.enter_cso_hint), getDefaultLocation()));

        if (holder.getSize() != null) {
            setCso.setText(holder.getSize().toString());
        }

        setCso.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (actionId == KeyEvent.KEYCODE_ENTER)) {
                    csoSet(setCso, holder);
                    return true;
                }
                return false;
            }
        });

        Button cancelButton = (Button) view.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetCsoDialogFragment.this.dismiss();
            }
        });

        Button okButton = (Button) view.findViewById(R.id.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                csoSet(setCso, holder);
            }
        });

        return view;
    }

    private void csoSet(EditText setCsoEditText, CoverageHolder holder) {
        String value = setCsoEditText.getText().toString();
        if (StringUtils.isNotBlank(value) && StringUtils.isNumeric(value)) {
            SetCsoDialogFragment.this.dismiss();

            if (listener != null) {
                listener.updateCsoTargetView(holder, Long.valueOf(value));
            }
        } else {
            setCsoEditText.setError(getString(R.string.cso_target_cannot_be_blank));

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // without a handler, the window sizes itself correctly
        // but the keyboard does not show up
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Window window = getDialog().getWindow();
                Point size = new Point();

                Display display = window.getWindowManager().getDefaultDisplay();
                display.getSize(size);

                int width = size.x;

                window.setLayout((int) (width * 0.7), FrameLayout.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the OnSetCsoListener so we can send events to the host
            listener = (OnSetCsoListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement OnSetCsoListener");
        }
    }

    public static SetCsoDialogFragment launchDialog(BaseActivity activity,
                                                    String dialogTag, CoverageHolder holder) {
        SetCsoDialogFragment dialogFragment = SetCsoDialogFragment.newInstance(holder);
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        Fragment prev = activity.getFragmentManager().findFragmentByTag(dialogTag);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        dialogFragment.show(ft, dialogTag);

        return dialogFragment;
    }

    private String getDefaultLocation() {
        JSONArray rawDefaultLocation = JsonFormUtils
                .generateDefaultLocationHierarchy(VaccinatorApplication.getInstance().context(), LocationPickerView.ALLOWED_LEVELS);

        if (rawDefaultLocation != null && rawDefaultLocation.length() > 0) {
            try {
                return rawDefaultLocation.getString(rawDefaultLocation.length() - 1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    public interface OnSetCsoListener {
        void updateCsoTargetView(CoverageHolder holder, Long newValue);
    }
}
