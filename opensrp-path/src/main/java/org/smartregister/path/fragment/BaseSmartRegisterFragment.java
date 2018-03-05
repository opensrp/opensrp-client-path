package org.smartregister.path.fragment;

import android.database.Cursor;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.Context;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.SecuredNativeSmartRegisterCursorAdapterFragment;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.path.R;
import org.smartregister.path.activity.BaseRegisterActivity;
import org.smartregister.path.activity.ChildImmunizationActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.helper.LocationHelper;
import org.smartregister.path.view.LocationPickerView;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;

import util.PathConstants;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class BaseSmartRegisterFragment extends SecuredNativeSmartRegisterCursorAdapterFragment {

    private LocationPickerView clinicSelection;

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {
        return null;
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.NavBarOptionsProvider getNavBarOptionsProvider() {
        return null;
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        return null;
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    protected void startRegistration() {

    }

    @Override
    protected void onCreation() {
    }

    protected final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(final CharSequence cs, int start, int before, int count) {
            filter(cs.toString(), "", "");
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    public void openVaccineCard(final String filterString) {
        FilterForClientTask filterForClientTask = new FilterForClientTask();
        filterForClientTask.execute(filterString);
    }

    @Override
    protected void setupViews(View view) {
        super.setupViews(view);

        View viewParent = (View) appliedSortView.getParent();
        viewParent.setVisibility(View.GONE);

        clinicSelection = (LocationPickerView) view.findViewById(R.id.clinic_selection);
        clinicSelection.init();

    }

    protected void filter(String filterString, String joinTableString, String mainConditionString) {
        filters = filterString;
        joinTable = joinTableString;
        mainCondition = mainConditionString;
        getSearchCancelView().setVisibility(isEmpty(filterString) ? INVISIBLE : VISIBLE);
        filterandSortExecute(countBundle());
    }

    @Override
    public void showProgressView() {
        if (clientsProgressView.getVisibility() == INVISIBLE) {
            clientsProgressView.setVisibility(VISIBLE);
        }
    }

    @Override
    public void hideProgressView() {
        if (clientsProgressView.getVisibility() == VISIBLE) {
            clientsProgressView.setVisibility(INVISIBLE);
        }
    }


    protected void updateLocationText() {
        if (clinicSelection != null) {
            clinicSelection.setText(LocationHelper.getInstance().getOpenMrsReadableName(
                    clinicSelection.getSelectedItem()));
            String locationId = LocationHelper.getInstance().getOpenMrsLocationId(clinicSelection.getSelectedItem());
            context().allSharedPreferences().savePreference(PathConstants.CURRENT_LOCATION_ID, locationId);
        }
    }

    public LocationPickerView getClinicSelection() {
        return clinicSelection;
    }

    public boolean onBackPressed() {
        return false;
    }

    @Override
    protected Context context() {
        return VaccinatorApplication.getInstance().context();
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class FilterForClientTask extends AsyncTask<String, Integer, CommonPersonObjectClient> {
        private String searchQuery;

        private FilterForClientTask() {
            this.searchQuery = null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressView();
        }

        @Override
        protected CommonPersonObjectClient doInBackground(String... params) {
            searchQuery = params[0];
            Cursor cursor = null;
            CommonPersonObjectClient client = null;
            try {
                String query = "";

                SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder(mainSelect);
                String whereOrAnd = " WHERE ";
                if (StringUtils.containsIgnoreCase(sqb.getSelectquery(), whereOrAnd.trim())) {
                    whereOrAnd = " AND ";
                }
                query = sqb.addCondition(whereOrAnd + getTablename() + ".zeir_id = " + searchQuery);
                query = sqb.Endquery(sqb.addlimitandOffset(query, 1, 0));

                cursor = commonRepository().rawCustomQueryForAdapter(query);
                cursor.moveToFirst();

                if (cursor.getCount() > 0) {
                    CommonPersonObject personinlist = commonRepository().readAllcommonforCursorAdapter(cursor);
                    client = new CommonPersonObjectClient(personinlist.getCaseId(), personinlist.getDetails(), personinlist.getDetails().get("FWHOHFNAME"));
                    client.setColumnmaps(personinlist.getColumnmaps());
                }

            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return client;
        }

        @Override
        protected void onPostExecute(CommonPersonObjectClient client) {
            super.onPostExecute(client);
            hideProgressView();
            if (client != null) {
                ChildImmunizationActivity.launchActivity(getActivity(), client, null);
            } else {
                NotInCatchmentDialogFragment.launchDialog((BaseRegisterActivity) getActivity(),
                        DIALOG_TAG, searchQuery);
            }
        }
    }
}
