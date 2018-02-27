package org.smartregister.path.provider;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterCLientsProviderForCursorAdapter;
import org.smartregister.domain.Alert;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.path.R;
import org.smartregister.path.fragment.AdvancedSearchFragment;
import org.smartregister.path.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.path.wrapper.VaccineViewRecordUpdateWrapper;
import org.smartregister.path.wrapper.WeightViewRecordUpdateWrapper;
import org.smartregister.service.AlertService;
import org.smartregister.util.DateUtil;
import org.smartregister.util.OpenSRPImageLoader;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import util.ImageUtils;
import util.PathConstants;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static org.smartregister.immunization.util.VaccinatorUtils.generateScheduleList;
import static org.smartregister.immunization.util.VaccinatorUtils.nextVaccineDue;
import static org.smartregister.immunization.util.VaccinatorUtils.receivedVaccines;
import static org.smartregister.util.Utils.fillValue;
import static org.smartregister.util.Utils.getName;
import static org.smartregister.util.Utils.getValue;
import static util.Utils.LINE_SEPARATOR;

/**
 * Created by Ahmed on 13-Oct-15.
 */
public class ChildSmartClientsProvider implements SmartRegisterCLientsProviderForCursorAdapter {
    private final LayoutInflater inflater;
    private final Context context;
    private final View.OnClickListener onClickListener;
    private final AlertService alertService;
    private final VaccineRepository vaccineRepository;
    private final WeightRepository weightRepository;
    private final AbsListView.LayoutParams clientViewLayoutParams;
    private final CommonRepository commonRepository;

    public ChildSmartClientsProvider(Context context, View.OnClickListener onClickListener,
                                     AlertService alertService, VaccineRepository vaccineRepository, WeightRepository weightRepository, CommonRepository commonRepository) {
        this.onClickListener = onClickListener;
        this.context = context;
        this.alertService = alertService;
        this.vaccineRepository = vaccineRepository;
        this.weightRepository = weightRepository;
        this.commonRepository = commonRepository;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        clientViewLayoutParams = new AbsListView.LayoutParams(MATCH_PARENT, (int) context.getResources().getDimension(org.smartregister.R.dimen.list_item_height));
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, final View convertView) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        fillValue((TextView) convertView.findViewById(R.id.child_zeir_id), getValue(pc.getColumnmaps(), PathConstants.KEY.ZEIR_ID, false));

        String firstName = getValue(pc.getColumnmaps(), PathConstants.KEY.FIRST_NAME, true);
        String lastName = getValue(pc.getColumnmaps(), PathConstants.KEY.LAST_NAME, true);
        String childName = getName(firstName, lastName);

        String motherFirstName = getValue(pc.getColumnmaps(), PathConstants.KEY.MOTHER_FIRST_NAME, true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }
        fillValue((TextView) convertView.findViewById(R.id.child_name), childName);

        String motherName = getValue(pc.getColumnmaps(), PathConstants.KEY.MOTHER_FIRST_NAME, true) + " " + getValue(pc, PathConstants.KEY.MOTHER_LAST_NAME, true);
        if (!StringUtils.isNotBlank(motherName)) {
            motherName = "M/G: " + motherName.trim();
        }
        fillValue((TextView) convertView.findViewById(R.id.child_mothername), motherName);

        String durationString = "";
        String dobString = getValue(pc.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        DateTime birthDateTime = util.Utils.dobStringToDateTime(dobString);
        if (birthDateTime != null) {
            try {
                String duration = DateUtil.getDuration(birthDateTime);
                if (duration != null) {
                    durationString = duration;
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString(), e);
            }
        }
        fillValue((TextView) convertView.findViewById(R.id.child_age), durationString);

        fillValue((TextView) convertView.findViewById(R.id.child_card_number), pc.getColumnmaps(), PathConstants.KEY.EPI_CARD_NUMBER, false);

        String gender = getValue(pc.getColumnmaps(), PathConstants.KEY.GENDER, true);

        final ImageView profilePic = (ImageView) convertView.findViewById(R.id.child_profilepic);
        int defaultImageResId = ImageUtils.profileImageResourceByGender(gender);
        profilePic.setImageResource(defaultImageResId);
        if (pc.entityId() != null && !SyncStatusBroadcastReceiver.getInstance().isSyncing()) { //image already in local storage most likely ):
            //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
            profilePic.setTag(org.smartregister.R.id.entity_id, pc.entityId());
            DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(pc.entityId(), OpenSRPImageLoader.getStaticImageListener(profilePic, 0, 0));
        }

        convertView.findViewById(R.id.child_profile_info_layout).setTag(client);
        convertView.findViewById(R.id.child_profile_info_layout).setOnClickListener(onClickListener);

        View recordWeight = convertView.findViewById(R.id.record_weight);
        recordWeight.setBackground(context.getResources().getDrawable(R.drawable.record_weight_bg));
        recordWeight.setTag(client);
        recordWeight.setOnClickListener(onClickListener);
        recordWeight.setVisibility(View.INVISIBLE);

        View recordVaccination = convertView.findViewById(R.id.record_vaccination);
        recordVaccination.setTag(client);
        recordVaccination.setOnClickListener(onClickListener);
        recordVaccination.setVisibility(View.INVISIBLE);

        String lostToFollowUp = getValue(pc.getColumnmaps(), PathConstants.KEY.LOST_TO_FOLLOW_UP, false);
        String inactive = getValue(pc.getColumnmaps(), PathConstants.KEY.INACTIVE, false);

        boolean showButtons = !ChildSmartClientsProvider.class.equals(this.getClass()) || !SyncStatusBroadcastReceiver.getInstance().isSyncing();
        if (showButtons) {
            try {
                Utils.startAsyncTask(new WeightAsyncTask(convertView, pc.entityId(), lostToFollowUp, inactive, client, cursor), null);
                Utils.startAsyncTask(new VaccinationAsyncTask(convertView, pc.entityId(), dobString, lostToFollowUp, inactive, client, cursor), null);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
        }

    }

    private void updateRecordWeight(WeightViewRecordUpdateWrapper updateWrapper) {

        View recordWeight = updateWrapper.getConvertView().findViewById(R.id.record_weight);
        recordWeight.setVisibility(View.VISIBLE);

        if (updateWrapper.getWeight() != null) {
            TextView recordWeightText = (TextView) updateWrapper.getConvertView().findViewById(R.id.record_weight_text);
            recordWeightText.setText(Utils.kgStringSuffix(updateWrapper.getWeight().getKg()));

            ImageView recordWeightCheck = (ImageView) updateWrapper.getConvertView().findViewById(R.id.record_weight_check);
            recordWeightCheck.setVisibility(View.VISIBLE);

            recordWeight.setClickable(false);
            recordWeight.setBackground(new ColorDrawable(context.getResources()
                    .getColor(android.R.color.transparent)));
        } else {
            TextView recordWeightText = (TextView) updateWrapper.getConvertView().findViewById(R.id.record_weight_text);
            recordWeightText.setText(context.getString(R.string.record_weight_with_nl));

            ImageView recordWeightCheck = (ImageView) updateWrapper.getConvertView().findViewById(R.id.record_weight_check);
            recordWeightCheck.setVisibility(View.GONE);
            recordWeight.setClickable(true);
        }

        // Update active/inactive/lostToFollowup status
        if (updateWrapper.getLostToFollowUp().equals(Boolean.TRUE.toString()) || updateWrapper.getInactive().equals(Boolean.TRUE.toString())) {
            recordWeight.setVisibility(View.INVISIBLE);
        }

        //Update Out of Catchment
        if (updateWrapper.getCursor() instanceof AdvancedSearchFragment.AdvancedMatrixCursor) {
            updateViews(updateWrapper.getConvertView(), updateWrapper.getClient(), true);
        }
    }

    private void updateRecordVaccination(VaccineViewRecordUpdateWrapper updateWrapper) {
        View recordVaccination = updateWrapper.getConvertView().findViewById(R.id.record_vaccination);
        recordVaccination.setVisibility(View.VISIBLE);

        TextView recordVaccinationText = (TextView) updateWrapper.getConvertView().findViewById(R.id.record_vaccination_text);
        ImageView recordVaccinationCheck = (ImageView) updateWrapper.getConvertView().findViewById(R.id.record_vaccination_check);
        recordVaccinationCheck.setVisibility(View.GONE);

        updateWrapper.getConvertView().setLayoutParams(clientViewLayoutParams);

        State state = State.FULLY_IMMUNIZED;
        String stateKey = null;

        Map<String, Object> nv = updateWrapper.getNv();

        if (nv != null) {
            DateTime dueDate = (DateTime) nv.get(PathConstants.KEY.DATE);
            VaccineRepo.Vaccine vaccine = (VaccineRepo.Vaccine) nv.get(PathConstants.KEY.VACCINE);
            stateKey = VaccinateActionUtils.stateKey(vaccine);
            if (nv.get(PathConstants.KEY.ALERT) == null) {
                state = State.NO_ALERT;
            } else if (((Alert) nv.get(PathConstants.KEY.ALERT)).status().value().equalsIgnoreCase(PathConstants.KEY.NORMAL)) {
                state = State.DUE;
            } else if (((Alert) nv.get(PathConstants.KEY.ALERT)).status().value().equalsIgnoreCase(PathConstants.KEY.UPCOMING)) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                if (dueDate.getMillis() >= (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)) && dueDate.getMillis() < (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS))) {
                    state = State.UPCOMING_NEXT_7_DAYS;
                } else {
                    state = State.UPCOMING;
                }
            } else if (((Alert) nv.get(PathConstants.KEY.ALERT)).status().value().equalsIgnoreCase(PathConstants.KEY.URGENT)) {
                state = State.OVERDUE;
            } else if (((Alert) nv.get(PathConstants.KEY.ALERT)).status().value().equalsIgnoreCase(PathConstants.KEY.EXPIRED)) {
                state = State.EXPIRED;
            }
        } else {
            state = State.WAITING;
        }


        // Update active/inactive/lostToFollowup status
        if (updateWrapper.getLostToFollowUp().equals(Boolean.TRUE.toString())) {
            state = State.LOST_TO_FOLLOW_UP;
        }

        if (updateWrapper.getInactive().equals(Boolean.TRUE.toString())) {
            state = State.INACTIVE;
        }

        if (state.equals(State.FULLY_IMMUNIZED)) {
            recordVaccinationText.setText(R.string.fully_immunized_label);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccinationCheck.setImageResource(R.drawable.ic_action_check);
            recordVaccinationCheck.setVisibility(View.VISIBLE);

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);

        } else if (state.equals(State.INACTIVE)) {
            recordVaccinationText.setText(R.string.inactive);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccinationCheck.setImageResource(R.drawable.ic_icon_status_inactive);
            recordVaccinationCheck.setVisibility(View.VISIBLE);

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);


        } else if (state.equals(State.LOST_TO_FOLLOW_UP)) {
            recordVaccinationText.setText(R.string.lost_to_follow_up_with_nl);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccinationCheck.setImageResource(R.drawable.ic_icon_status_losttofollowup);
            recordVaccinationCheck.setVisibility(View.VISIBLE);

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);

        } else if (state.equals(State.WAITING)) {
            recordVaccinationText.setText(R.string.waiting_label);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else if (state.equals(State.EXPIRED)) {
            recordVaccinationText.setText(R.string.expired_label);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else if (state.equals(State.UPCOMING)) {
            recordVaccinationText.setText(context.getString(R.string.upcoming_label) + LINE_SEPARATOR + stateKey);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else if (state.equals(State.UPCOMING_NEXT_7_DAYS)) {
            recordVaccinationText.setText(context.getString(R.string.upcoming_label) + LINE_SEPARATOR + stateKey);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.due_vaccine_light_blue_bg));
            recordVaccination.setEnabled(true);
        } else if (state.equals(State.DUE)) {
            recordVaccinationText.setText(context.getString(R.string.record_label) + LINE_SEPARATOR + stateKey);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.status_bar_text_almost_white));

            recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.due_vaccine_blue_bg));
            recordVaccination.setEnabled(true);
        } else if (state.equals(State.OVERDUE)) {
            recordVaccinationText.setText(context.getString(R.string.record_label) + LINE_SEPARATOR + stateKey);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.status_bar_text_almost_white));

            recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.due_vaccine_red_bg));
            recordVaccination.setEnabled(true);
        } else if (state.equals(State.NO_ALERT)) {
            if (StringUtils.isNotBlank(stateKey) && (StringUtils.containsIgnoreCase(stateKey, PathConstants.KEY.WEEK) || StringUtils.containsIgnoreCase(stateKey, PathConstants.KEY.MONTH)) && !updateWrapper.getVaccines().isEmpty()) {
                Vaccine vaccine = updateWrapper.getVaccines().isEmpty() ? null : updateWrapper.getVaccines().get(updateWrapper.getVaccines().size() - 1);
                String previousStateKey = VaccinateActionUtils.previousStateKey(PathConstants.KEY.CHILD, vaccine);
                if (previousStateKey != null) {
                    recordVaccinationText.setText(previousStateKey);
                } else {
                    recordVaccinationText.setText(stateKey);
                }
                recordVaccinationCheck.setImageResource(R.drawable.ic_action_check);
                recordVaccinationCheck.setVisibility(View.VISIBLE);
            } else {
                recordVaccinationText.setText(context.getString(R.string.upcoming_label) + LINE_SEPARATOR + stateKey);
            }
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else {
            recordVaccinationText.setText("");
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        }

        //Update Out of Catchment
        if (updateWrapper.getCursor() instanceof AdvancedSearchFragment.AdvancedMatrixCursor) {
            updateViews(updateWrapper.getConvertView(), updateWrapper.getClient(), false);
        }
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption villageFilter, ServiceModeOption
            serviceModeOption, FilterOption searchFilter, SortOption sortOption) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {

    }

    @Override
    public OnClickFormLauncher newFormLauncher(String formName, String entityId, String
            metaData) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public View inflatelayoutForCursorAdapter() {
        return inflater().inflate(R.layout.smart_register_child_client, null);
    }

    public LayoutInflater inflater() {
        return inflater;
    }

    public void updateViews(View convertView, SmartRegisterClient client, boolean isWeightRecord) {

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        if (commonRepository != null) {
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(pc.entityId());

            View recordVaccination = convertView.findViewById(R.id.record_vaccination);
            recordVaccination.setVisibility(View.VISIBLE);

            View moveToCatchment = convertView.findViewById(R.id.move_to_catchment);
            moveToCatchment.setVisibility(View.GONE);

            if (commonPersonObject == null) { //Out of area -- doesn't exist in local database

                convertView.findViewById(R.id.child_profile_info_layout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(context, context.getString(R.string.show_vaccine_card_disabled), Toast.LENGTH_SHORT).show();
                    }
                });

                if (isWeightRecord) {
                    TextView recordWeightText = (TextView) convertView.findViewById(R.id.record_weight_text);
                    recordWeightText.setText("Record\nservice");

                    String zeirId = getValue(pc.getColumnmaps(), PathConstants.KEY.ZEIR_ID, false);

                    View recordWeight = convertView.findViewById(R.id.record_weight);
                    recordWeight.setBackground(context.getResources().getDrawable(R.drawable.record_weight_bg));
                    recordWeight.setTag(zeirId);
                    recordWeight.setClickable(true);
                    recordWeight.setEnabled(true);
                    recordWeight.setOnClickListener(onClickListener);
                } else {

                    TextView moveToCatchmentText = (TextView) convertView.findViewById(R.id.move_to_catchment_text);
                    moveToCatchmentText.setText("Move to my\ncatchment");

                    String motherBaseEntityId = getValue(pc.getColumnmaps(), PathConstants.KEY.MOTHER_BASE_ENTITY_ID, false);
                    String entityId = pc.entityId();

                    List<String> ids = new ArrayList<>();
                    ids.add(motherBaseEntityId);
                    ids.add(entityId);

                    moveToCatchment.setBackground(context.getResources().getDrawable(R.drawable.record_weight_bg));
                    moveToCatchment.setTag(ids);
                    moveToCatchment.setClickable(true);
                    moveToCatchment.setEnabled(true);
                    moveToCatchment.setOnClickListener(onClickListener);

                    moveToCatchment.setVisibility(View.VISIBLE);
                    recordVaccination.setVisibility(View.GONE);
                }
            }

        }
    }

    public enum State {
        DUE,
        OVERDUE,
        UPCOMING_NEXT_7_DAYS,
        UPCOMING,
        INACTIVE,
        LOST_TO_FOLLOW_UP,
        EXPIRED,
        WAITING,
        NO_ALERT,
        FULLY_IMMUNIZED
    }

    private class WeightAsyncTask extends AsyncTask<Void, Void, Void> {
        private final View convertView;
        private final String entityId;
        private final String lostToFollowUp;
        private final String inactive;
        private Weight weight;
        private SmartRegisterClient client;
        private Cursor cursor;

        private WeightAsyncTask(View convertView,
                                String entityId,
                                String lostToFollowUp,
                                String inactive,
                                SmartRegisterClient smartRegisterClient,
                                Cursor cursor) {
            this.convertView = convertView;
            this.entityId = entityId;
            this.lostToFollowUp = lostToFollowUp;
            this.inactive = inactive;
            this.client = smartRegisterClient;
            this.cursor = cursor;
        }


        @Override
        protected Void doInBackground(Void... params) {
            weight = weightRepository.findUnSyncedByEntityId(entityId);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            WeightViewRecordUpdateWrapper wrapper = new WeightViewRecordUpdateWrapper();
            wrapper.setWeight(weight);
            wrapper.setLostToFollowUp(lostToFollowUp);
            wrapper.setInactive(inactive);
            wrapper.setClient(client);
            wrapper.setCursor(cursor);
            wrapper.setConvertView(convertView);
            updateRecordWeight(wrapper);

        }
    }

    private class VaccinationAsyncTask extends AsyncTask<Void, Void, Void> {
        private final View convertView;
        private final String entityId;
        private final String dobString;
        private final String lostToFollowUp;
        private final String inactive;
        private List<Vaccine> vaccines = new ArrayList<>();
        private SmartRegisterClient client;
        private Cursor cursor;
        private Map<String, Object> nv = null;

        private VaccinationAsyncTask(View convertView,
                                     String entityId,
                                     String dobString,
                                     String lostToFollowUp,
                                     String inactive,
                                     SmartRegisterClient smartRegisterClient,
                                     Cursor cursor) {
            this.convertView = convertView;
            this.entityId = entityId;
            this.dobString = dobString;
            this.lostToFollowUp = lostToFollowUp;
            this.inactive = inactive;
            this.client = smartRegisterClient;
            this.cursor = cursor;
        }


        @Override
        protected Void doInBackground(Void... params) {
            vaccines = vaccineRepository.findByEntityId(entityId);
            List<Alert> alerts = alertService.findByEntityIdAndAlertNames(entityId, VaccinateActionUtils.allAlertNames(PathConstants.KEY.CHILD));

            Map<String, Date> recievedVaccines = receivedVaccines(vaccines);

            DateTime dateTime = util.Utils.dobStringToDateTime(dobString);
            List<Map<String, Object>> sch = generateScheduleList(PathConstants.KEY.CHILD, dateTime, recievedVaccines, alerts);

            if (vaccines.isEmpty()) {
                List<VaccineRepo.Vaccine> vList = Arrays.asList(VaccineRepo.Vaccine.values());
                nv = nextVaccineDue(sch, vList);
            }

            if (nv == null) {
                Date lastVaccine = null;
                if (!vaccines.isEmpty()) {
                    Vaccine vaccine = vaccines.get(vaccines.size() - 1);
                    lastVaccine = vaccine.getDate();
                }

                nv = nextVaccineDue(sch, lastVaccine);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {

            VaccineViewRecordUpdateWrapper wrapper = new VaccineViewRecordUpdateWrapper();
            wrapper.setVaccines(vaccines);
            wrapper.setLostToFollowUp(lostToFollowUp);
            wrapper.setInactive(inactive);
            wrapper.setClient(client);
            wrapper.setCursor(cursor);
            wrapper.setConvertView(convertView);
            wrapper.setNv(nv);
            updateRecordVaccination(wrapper);

        }
    }

}
