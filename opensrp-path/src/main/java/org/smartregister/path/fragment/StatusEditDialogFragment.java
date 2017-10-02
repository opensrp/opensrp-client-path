package org.smartregister.path.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.smartregister.path.R;
import org.smartregister.path.listener.StatusChangeListener;
import org.smartregister.util.Utils;

import java.util.Map;

@SuppressLint("ValidFragment")
public class StatusEditDialogFragment extends DialogFragment {
    private StatusChangeListener listener;
    private static Map<String, String> details;
    private static final String inactive = "inactive";
    private static final String lostToFollowUp = "lost_to_follow_up";

    private StatusEditDialogFragment(Map<String, String> details) {
        StatusEditDialogFragment.details = details;
    }

    public static StatusEditDialogFragment newInstance(Map<String, String> details) {

        return new StatusEditDialogFragment(details);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup dialogView = (ViewGroup) inflater.inflate(R.layout.status_edit_dialog_view, container, false);
        LinearLayout activeLayout = (LinearLayout) dialogView.findViewById(R.id.activelayout);
        LinearLayout inactiveLayout = (LinearLayout) dialogView.findViewById(R.id.inactivelayout);
        LinearLayout lostToFollowUpLayout = (LinearLayout) dialogView.findViewById(R.id.losttofollowuplayout);
        final ImageView activeImageView = (ImageView) dialogView.findViewById(R.id.active_check);
        final ImageView inactiveImageView = (ImageView) dialogView.findViewById(R.id.inactive_check);
        final ImageView lostToFollowUpImageView = (ImageView) dialogView.findViewById(R.id.lost_to_followup_check);

        activeImageView.setVisibility(View.INVISIBLE);
        inactiveImageView.setVisibility(View.INVISIBLE);
        lostToFollowUpImageView.setVisibility(View.INVISIBLE);

        if ((!details.containsKey(inactive)) || (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(""))) {
            activeImageView.setVisibility(View.VISIBLE);
            inactiveImageView.setVisibility(View.INVISIBLE);
            lostToFollowUpImageView.setVisibility(View.INVISIBLE);
        }
        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.FALSE.toString())) {
            activeImageView.setVisibility(View.VISIBLE);
            inactiveImageView.setVisibility(View.INVISIBLE);
            lostToFollowUpImageView.setVisibility(View.INVISIBLE);

        }
        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
            inactiveImageView.setVisibility(View.VISIBLE);
            activeImageView.setVisibility(View.INVISIBLE);
            lostToFollowUpImageView.setVisibility(View.INVISIBLE);

        }
        if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
            lostToFollowUpImageView.setVisibility(View.VISIBLE);
            inactiveImageView.setVisibility(View.INVISIBLE);
            activeImageView.setVisibility(View.INVISIBLE);

        }


        activeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateView updateView = new UpdateView(STATUS.ACTIVE, listener, details);
                updateView.setImageView(activeImageView, inactiveImageView, lostToFollowUpImageView);

                Utils.startAsyncTask(updateView, null);

            }
        });

        inactiveLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateView updateView = new UpdateView(STATUS.IN_ACTIVE, listener, details);
                updateView.setImageView(activeImageView, inactiveImageView, lostToFollowUpImageView);

                Utils.startAsyncTask(updateView, null);
            }
        });

        lostToFollowUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateView updateView = new UpdateView(STATUS.LOST_TO_FOLLOW_UP, listener, details);
                updateView.setImageView(activeImageView, inactiveImageView, lostToFollowUpImageView);

                Utils.startAsyncTask(updateView, null);
            }
        });


        return dialogView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (StatusChangeListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement StatusChangeListener");
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
                getDialog().getWindow().setLayout(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

            }
        });

    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private class UpdateView extends AsyncTask<Void, Void, Boolean> {
        private StatusChangeListener listener;
        private STATUS status;
        private Map<String, String> details;
        private ImageView activeImageView;
        private ImageView inactiveImageView;
        private ImageView lostToFollowUpImageView;
        private ProgressDialog progressDialog;

        private UpdateView(STATUS status, StatusChangeListener listener, Map<String, String> details) {
            this.status = status;
            this.listener = listener;
            this.details = details;

            this.progressDialog = new ProgressDialog(getActivity());
            this.progressDialog.setCancelable(false);
            this.progressDialog.setTitle(getString(R.string.updating_dialog_title));
            this.progressDialog.setMessage(getString(R.string.please_wait_message));
        }

        private void setImageView(ImageView activeImageView, ImageView inactiveImageView, ImageView lostToFollowUpImageView) {
            this.activeImageView = activeImageView;
            this.inactiveImageView = inactiveImageView;
            this.lostToFollowUpImageView = lostToFollowUpImageView;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean updateViews = false;
            switch (status) {
                case ACTIVE:
                    if ((details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString()))) {
                        listener.updateClientAttribute(inactive, false);
                        updateViews = true;
                    }

                    if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
                        listener.updateClientAttribute(lostToFollowUp, false);
                        updateViews = true;
                    }

                    break;

                case IN_ACTIVE:
                    if ((details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.FALSE.toString())) || (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(""))) {
                        listener.updateClientAttribute(inactive, true);
                        if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
                            listener.updateClientAttribute(lostToFollowUp, false);
                        }
                        updateViews = true;

                    } else if (!details.containsKey(inactive)) {
                        listener.updateClientAttribute(inactive, true);
                        if (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.TRUE.toString())) {
                            listener.updateClientAttribute(lostToFollowUp, false);
                        }
                        updateViews = true;
                    }
                    break;
                case LOST_TO_FOLLOW_UP:
                    if ((details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(Boolean.FALSE.toString())) || (details.containsKey(lostToFollowUp) && details.get(lostToFollowUp).equalsIgnoreCase(""))) {
                        listener.updateClientAttribute(lostToFollowUp, true);
                        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
                            listener.updateClientAttribute(inactive, false);
                        }
                        updateViews = true;
                    } else if (!details.containsKey(lostToFollowUp)) {
                        listener.updateClientAttribute(lostToFollowUp, true);
                        if (details.containsKey(inactive) && details.get(inactive).equalsIgnoreCase(Boolean.TRUE.toString())) {
                            listener.updateClientAttribute(inactive, false);
                        }
                        updateViews = true;
                    }
                    break;
                default:
                    break;

            }

            return updateViews;
        }

        @Override
        protected void onPreExecute() {
            this.progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean updateViews) {
            this.progressDialog.dismiss();
            if (updateViews) {
                switch (status) {
                    case ACTIVE:
                        activeImageView.setVisibility(View.VISIBLE);
                        inactiveImageView.setVisibility(View.INVISIBLE);
                        lostToFollowUpImageView.setVisibility(View.INVISIBLE);
                        break;
                    case IN_ACTIVE:
                        activeImageView.setVisibility(View.INVISIBLE);
                        inactiveImageView.setVisibility(View.VISIBLE);
                        lostToFollowUpImageView.setVisibility(View.INVISIBLE);
                        break;
                    case LOST_TO_FOLLOW_UP:
                        activeImageView.setVisibility(View.INVISIBLE);
                        inactiveImageView.setVisibility(View.INVISIBLE);
                        lostToFollowUpImageView.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }

            listener.updateStatus();
            dismiss();

        }
    }

    private enum STATUS {
        ACTIVE, IN_ACTIVE, LOST_TO_FOLLOW_UP
    }

}
