package org.smartregister.path.tabfragments;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.domain.Photo;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.domain.WeightWrapper;
import org.smartregister.growthmonitoring.fragment.EditWeightDialogFragment;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.util.WeightUtils;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.fragment.ServiceEditDialogFragment;
import org.smartregister.immunization.fragment.VaccinationEditDialogFragment;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.immunization.view.ImmunizationRowGroup;
import org.smartregister.immunization.view.ServiceRowGroup;
import org.smartregister.path.R;
import org.smartregister.path.activity.ChildDetailTabbedActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.viewcomponents.WidgetFactory;
import org.smartregister.util.DateUtil;
import org.smartregister.util.Utils;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import util.ImageUtils;
import util.PathConstants;

import static org.smartregister.util.Utils.getName;
import static org.smartregister.util.Utils.getValue;


public class ChildUnderFiveFragment extends Fragment {

    private LayoutInflater inflater;
    private CommonPersonObjectClient childDetails;
    private static final String DIALOG_TAG = "ChildImmunoActivity_DIALOG_TAG";
    private Map<String, String> detailsMap;
    private LinearLayout fragmentContainer;

    private Boolean curVaccineMode;
    private Boolean curServiceMode;
    private Boolean curWeightMode;

    public static ChildUnderFiveFragment newInstance(Bundle args) {
        ChildUnderFiveFragment fragment = new ChildUnderFiveFragment();
        if (args == null) {
            args = new Bundle();
        }
        fragment.setArguments(args);
        return fragment;
    }

    public ChildUnderFiveFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        if (this.getArguments() != null) {
            Serializable serializable = getArguments().getSerializable(ChildDetailTabbedActivity.EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }
        }
        View underFiveFragment = inflater.inflate(R.layout.child_under_five_fragment, container, false);
        fragmentContainer = (LinearLayout) underFiveFragment.findViewById(R.id.container);

        return underFiveFragment;
    }

    public void setDetailsMap(Map<String, String> detailsMap) {
        this.detailsMap = detailsMap;
    }

    public void loadWeightView(List<Weight> weightList, boolean editWeightMode) {
        boolean showWeight = curWeightMode == null || !curWeightMode.equals(editWeightMode);
        if (fragmentContainer != null && showWeight) {
            createPTCMTVIEW(fragmentContainer, "PMTCT: ", Utils.getValue(childDetails.getColumnmaps(), "pmtct_status", true));
            createWeightLayout(weightList, fragmentContainer, editWeightMode);
            curWeightMode = editWeightMode;
        }
    }

    private void createWeightLayout(List<Weight> weights, LinearLayout fragmentContainer, boolean editmode) {
        LinkedHashMap<Long, Pair<String, String>> weightMap = new LinkedHashMap<>();
        ArrayList<Boolean> weightEditMode = new ArrayList<>();
        ArrayList<View.OnClickListener> listeners = new ArrayList<>();

        List<Weight> weightList = new ArrayList<>();
        if (weights != null && !weights.isEmpty()) {
            if (weights.size() <= 5) {
                weightList = weights;
            } else {
                weightList = weights.subList(0, 5);
            }
        }

        for (int i = 0; i < weightList.size(); i++) {
            Weight weight = weightList.get(i);
            String formattedAge = "";
            if (weight.getDate() != null) {
                Date weighttaken = weight.getDate();
                String birthdate = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
                Date birth = util.Utils.dobStringToDate(birthdate);
                if (birth != null) {
                    long timeDiff = weighttaken.getTime() - birth.getTime();
                    Log.v("timeDiff is ", timeDiff + "");
                    if (timeDiff >= 0) {
                        formattedAge = DateUtil.getDuration(timeDiff);
                        Log.v("age is ", formattedAge);
                    }
                }
            }

            if (!formattedAge.equalsIgnoreCase("0d")) {
                weightMap.put(weight.getId(), Pair.create(formattedAge, Utils.kgStringSuffix(weight.getKg())));

                boolean lessThanThreeMonthsEventCreated = WeightUtils.lessThanThreeMonths(weight);
                if (lessThanThreeMonthsEventCreated) {
                    weightEditMode.add(editmode);
                } else {
                    weightEditMode.add(false);
                }

                final int finalI = i;
                View.OnClickListener onClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showWeightDialog(finalI);
                    }
                };
                listeners.add(onClickListener);
            }

        }

        if (weightMap.size() < 5) {
            weightMap.put(0l, Pair.create(DateUtil.getDuration(0), Utils.getValue(detailsMap, "Birth_Weight", true) + " kg"));
            weightEditMode.add(false);
            listeners.add(null);
        }

        WidgetFactory wd = new WidgetFactory();
        if (weightMap.size() > 0) {
            wd.createWeightWidget(inflater, fragmentContainer, weightMap, listeners, weightEditMode);
        }
    }

    private void createPTCMTVIEW(LinearLayout fragmentContainer, String labelString, String valueString) {
        TableRow tableRow = (TableRow) fragmentContainer.findViewById(R.id.tablerowcontainer);
        TextView label = (TextView) tableRow.findViewById(R.id.label);
        TextView value = (TextView) tableRow.findViewById(R.id.value);

        label.setText(labelString);
        value.setText(valueString);
    }

    public void updateVaccinationViews(List<Vaccine> vaccines, List<Alert> alertList, boolean editVaccineMode) {
        boolean showVaccine = curVaccineMode == null || !curVaccineMode.equals(editVaccineMode);
        if (fragmentContainer != null && showVaccine) {

            List<Vaccine> vaccineList = new ArrayList<>();
            if (vaccines != null && !vaccines.isEmpty()) {
                vaccineList = vaccines;
            }

            LinearLayout vaccineGroupCanvasLL = (LinearLayout) fragmentContainer.findViewById(R.id.immunizations);
            vaccineGroupCanvasLL.removeAllViews();

            CustomFontTextView title = new CustomFontTextView(getActivity());
            title.setAllCaps(true);
            title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
            title.setTextColor(getResources().getColor(R.color.text_black));
            title.setText(getString(R.string.immunizations));
            vaccineGroupCanvasLL.addView(title);

            List<VaccineGroup> supportedVaccines = VaccinatorUtils.getSupportedVaccines(getActivity());
            for (VaccineGroup vaccineGroup : supportedVaccines) {

                VaccinateActionUtils.addBcg2SpecialVaccine(getActivity(), vaccineGroup, vaccineList);
                ImmunizationRowGroup curGroup = new ImmunizationRowGroup(getActivity(), editVaccineMode);
                curGroup.setData(vaccineGroup, childDetails, vaccineList, alertList);
                curGroup.setOnVaccineUndoClickListener(new ImmunizationRowGroup.OnVaccineUndoClickListener() {
                    @Override
                    public void onUndoClick(ImmunizationRowGroup vaccineGroup, VaccineWrapper vaccine) {
                        addVaccinationDialogFragment(Arrays.asList(vaccine), vaccineGroup);

                    }
                });

                vaccineGroupCanvasLL.addView(curGroup);
            }

            curVaccineMode = editVaccineMode;
        }
    }

    public void updateServiceViews(Map<String, List<ServiceType>> serviceTypeMap,
                                   List<ServiceRecord> services, List<Alert> alertList, boolean editServiceMode) {
        boolean showService = curServiceMode == null || !curServiceMode.equals(editServiceMode);
        if (fragmentContainer != null && showService) {

            List<ServiceRecord> serviceRecords = new ArrayList<>();
            if (services != null && !services.isEmpty()) {
                serviceRecords = services;
            }

            LinearLayout serviceGroupCanvasLL = (LinearLayout) fragmentContainer.findViewById(R.id.services);
            serviceGroupCanvasLL.removeAllViews();

            CustomFontTextView title = new CustomFontTextView(getActivity());
            title.setAllCaps(true);
            title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
            title.setTextColor(getResources().getColor(R.color.text_black));
            title.setText(getString(R.string.recurring));
            serviceGroupCanvasLL.addView(title);

            try {
                for (String type : serviceTypeMap.keySet()) {
                    ServiceRowGroup curGroup = new ServiceRowGroup(getActivity(), editServiceMode);
                    curGroup.setData(childDetails, serviceTypeMap.get(type), serviceRecords, alertList);
                    curGroup.setOnServiceUndoClickListener(new ServiceRowGroup.OnServiceUndoClickListener() {
                        @Override
                        public void onUndoClick(ServiceRowGroup serviceRowGroup, ServiceWrapper service) {
                            addServiceDialogFragment(service, serviceRowGroup);
                        }
                    });

                    serviceGroupCanvasLL.addView(curGroup);
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), Log.getStackTraceString(e));
            }

            curServiceMode = editServiceMode;
        }
    }

    private void addVaccinationDialogFragment(List<VaccineWrapper> vaccineWrappers, ImmunizationRowGroup vaccineGroup) {
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        android.app.Fragment prev = getActivity().getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        Date dob = util.Utils.dobStringToDate(dobString);
        if (dob == null) {
            dob = Calendar.getInstance().getTime();
        }

        List<Vaccine> vaccineList = VaccinatorApplication.getInstance().vaccineRepository()
                .findByEntityId(childDetails.entityId());
        if (vaccineList == null) {
            vaccineList = new ArrayList<>();
        }

        VaccinationEditDialogFragment vaccinationDialogFragment = VaccinationEditDialogFragment.newInstance(getActivity(), dob, vaccineList, vaccineWrappers, vaccineGroup, true);
        vaccinationDialogFragment.show(ft, DIALOG_TAG);
    }

    private void addServiceDialogFragment(ServiceWrapper serviceWrapper, ServiceRowGroup serviceRowGroup) {
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        android.app.Fragment prev = getActivity().getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        String dobString = Utils.getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        DateTime dateTime = util.Utils.dobStringToDateTime(dobString);
        if (dateTime == null) {
            dateTime = DateTime.now();
        }

        List<ServiceRecord> serviceRecordList = VaccinatorApplication.getInstance().recurringServiceRecordRepository()
                .findByEntityId(childDetails.entityId());
        if (serviceRecordList == null) {
            serviceRecordList = new ArrayList<>();
        }

        ServiceEditDialogFragment serviceEditDialogFragment = ServiceEditDialogFragment.newInstance(dateTime, serviceRecordList, serviceWrapper, serviceRowGroup, true);
        serviceEditDialogFragment.show(ft, DIALOG_TAG);
    }

    public void showWeightDialog(int i) {
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        android.app.Fragment prev = getActivity().getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);


        String childName = constructChildName();
        String gender = getValue(childDetails.getColumnmaps(), "gender", true);
        String motherFirstName = getValue(childDetails.getColumnmaps(), "mother_first_name", true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }
        String zeirId = getValue(childDetails.getColumnmaps(), "zeir_id", false);
        String duration = "";
        String dobString = getValue(childDetails.getColumnmaps(), PathConstants.EC_CHILD_TABLE.DOB, false);
        DateTime dateTime = util.Utils.dobStringToDateTime(dobString);

        Date dob = null;
        if (dateTime != null) {
            duration = DateUtil.getDuration(dateTime);
            dob = dateTime.toDate();
        }

        if (dob == null) {
            dob = Calendar.getInstance().getTime();
        }

        Photo photo = getProfilePhotoByClient();

        WeightWrapper weightWrapper = new WeightWrapper();
        weightWrapper.setId(childDetails.entityId());

        WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
        List<Weight> weightList = weightRepository.findByEntityId(childDetails.entityId());
        if (!weightList.isEmpty()) {
            weightWrapper.setWeight(weightList.get(i).getKg());
            weightWrapper.setUpdatedWeightDate(new DateTime(weightList.get(i).getDate()), false);
            weightWrapper.setDbKey(weightList.get(i).getId());
        }

        weightWrapper.setGender(gender);
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(zeirId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(getValue(childDetails.getColumnmaps(), ChildDetailTabbedActivity.PMTCT_STATUS_LOWER_CASE, false));

        EditWeightDialogFragment editWeightDialogFragment = EditWeightDialogFragment.newInstance(getActivity(), dob, weightWrapper);
        editWeightDialogFragment.show(ft, DIALOG_TAG);

    }

    protected Photo getProfilePhotoByClient() {
        return ImageUtils.profilePhotoByClient(childDetails);
    }

    private String constructChildName() {
        String firstName = getValue(childDetails.getColumnmaps(), "first_name", true);
        String lastName = getValue(childDetails.getColumnmaps(), "last_name", true);
        return getName(firstName, lastName).trim();
    }

}
