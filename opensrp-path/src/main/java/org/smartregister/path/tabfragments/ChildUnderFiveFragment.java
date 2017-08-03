package org.smartregister.path.tabfragments;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.smartregister.Context;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.fragment.ServiceEditDialogFragment;
import org.smartregister.immunization.fragment.VaccinationEditDialogFragment;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.view.ImmunizationRowGroup;
import org.smartregister.immunization.view.ServiceRowGroup;
import org.smartregister.path.R;
import org.smartregister.path.activity.ChildDetailTabbedActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.viewcomponents.WidgetFactory;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.AlertService;
import org.smartregister.util.DateUtil;
import org.smartregister.util.Utils;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ChildUnderFiveFragment extends Fragment {

    private LayoutInflater inflater;
    private CommonPersonObjectClient childDetails;
    private ArrayList<ImmunizationRowGroup> vaccineGroups;
    private ArrayList<ServiceRowGroup> serviceRowGroups;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final String VACCINES_FILE = "vaccines.json";
    private static final String DIALOG_TAG = "ChildImmunoActivity_DIALOG_TAG";
    private Map<String, String> Detailsmap;
    private AlertService alertService;
    private LinearLayout fragmentContainer;

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

        alertService = Context.getInstance().alertService();

        DetailsRepository detailsRepository = ((ChildDetailTabbedActivity) getActivity()).getDetailsRepository();
        childDetails = childDetails != null ? childDetails : ((ChildDetailTabbedActivity) getActivity()).getChildDetails();
        Detailsmap = detailsRepository.getAllDetailsForClient(childDetails.entityId());

        loadView(false, false, false);
        return underFiveFragment;
    }

    public void loadView(boolean editVaccineMode, boolean editServiceMode, boolean editWeightMode) {
        if (fragmentContainer != null) {
            createPTCMTVIEW(fragmentContainer, "PMTCT: ", Utils.getValue(childDetails.getColumnmaps(), "pmtct_status", true));
            createWeightLayout(fragmentContainer, editWeightMode);

            updateVaccinationViews(fragmentContainer, editVaccineMode);
            updateServiceViews(fragmentContainer, editServiceMode);
        }
    }

    private void createWeightLayout(LinearLayout fragmentContainer, boolean editmode) {
        LinkedHashMap<Long, Pair<String, String>> weightmap = new LinkedHashMap<>();
        ArrayList<Boolean> weighteditmode = new ArrayList<Boolean>();
        ArrayList<View.OnClickListener> listeners = new ArrayList<View.OnClickListener>();

        WeightRepository wp = VaccinatorApplication.getInstance().weightRepository();
        List<Weight> weightlist = wp.findLast5(childDetails.entityId());
        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(getActivity());


        for (int i = 0; i < weightlist.size(); i++) {
            Weight weight = weightlist.get(i);
            String formattedAge = "";
            if (weight.getDate() != null) {

                Date weighttaken = weight.getDate();
                String birthdate = Utils.getValue(childDetails.getColumnmaps(), "dob", false);
                DateTime birthday = new DateTime(birthdate);
                Date birth = birthday.toDate();
                long timeDiff = weighttaken.getTime() - birth.getTime();
                Log.v("timeDiff is ", timeDiff + "");
                if (timeDiff >= 0) {
                    formattedAge = DateUtil.getDuration(timeDiff);
                    Log.v("age is ", formattedAge);
                }
            }
            if (!formattedAge.equalsIgnoreCase("0d")) {
                weightmap.put(weight.getId(), Pair.create(formattedAge, Utils.kgStringSuffix(weight.getKg())));

                ////////////////////////check 3 months///////////////////////////////
                boolean less_than_three_months_event_created = false;

                org.smartregister.domain.db.Event event = null;
                EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();
                if (weight.getEventId() != null) {
                    event = ecUpdater.convert(db.getEventsByEventId(weight.getEventId()), org.smartregister.domain.db.Event.class);
                } else if (weight.getFormSubmissionId() != null) {
                    event = ecUpdater.convert(db.getEventsByFormSubmissionId(weight.getFormSubmissionId()), org.smartregister.domain.db.Event.class);
                }
                if (event != null) {
                    Date weight_create_date = event.getDateCreated().toDate();
                    if (!ChildDetailTabbedActivity.check_if_date_three_months_older(weight_create_date)) {
                        less_than_three_months_event_created = true;
                    }
                } else {
                    less_than_three_months_event_created = true;
                }
                ///////////////////////////////////////////////////////////////////////
                if (less_than_three_months_event_created) {
                    weighteditmode.add(editmode);
                } else {
                    weighteditmode.add(false);
                }

                final int finalI = i;
                View.OnClickListener onclicklistener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((ChildDetailTabbedActivity) getActivity()).showWeightDialog(finalI);
                    }
                };
                listeners.add(onclicklistener);
            }

        }
        if (weightmap.size() < 5) {
            weightmap.put(0l, Pair.create(DateUtil.getDuration(0), Utils.getValue(Detailsmap, "Birth_Weight", true) + " kg"));
            weighteditmode.add(false);
            listeners.add(null);
        }

        WidgetFactory wd = new WidgetFactory();
        if (weightmap.size() > 0) {
            wd.createWeightWidget(inflater, fragmentContainer, weightmap, listeners, weighteditmode);
        }
    }

    private void createPTCMTVIEW(LinearLayout fragmentContainer, String labelString, String valueString) {
        TableRow tableRow = (TableRow) fragmentContainer.findViewById(R.id.tablerowcontainer);
        TextView label = (TextView) tableRow.findViewById(R.id.label);
        TextView value = (TextView) tableRow.findViewById(R.id.value);

        label.setText(labelString);
        value.setText(valueString);
    }

    private void updateVaccinationViews(LinearLayout fragmentContainer, boolean editmode) {
        vaccineGroups = new ArrayList<>();
        VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
        List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());
        LinearLayout vaccineGroupCanvasLL = (LinearLayout) fragmentContainer.findViewById(R.id.immunizations);
        vaccineGroupCanvasLL.removeAllViews();

        CustomFontTextView title = new CustomFontTextView(getActivity());
        title.setAllCaps(true);
        title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
        title.setTextColor(getResources().getColor(R.color.text_black));
        title.setText(getString(R.string.immunizations));
        vaccineGroupCanvasLL.addView(title);

        List<Alert> alertList = new ArrayList<>();
        if (alertService != null) {
            alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(),
                    VaccinateActionUtils.allAlertNames("child"));
        }

        String supportedVaccinesString = readAssetContents(VACCINES_FILE);
        try {
            JSONArray supportedVaccines = new JSONArray(supportedVaccinesString);
            for (int i = 0; i < supportedVaccines.length(); i++) {
                ImmunizationRowGroup curGroup = new ImmunizationRowGroup(getActivity(), editmode);
                curGroup.setData(supportedVaccines.getJSONObject(i), childDetails, vaccineList, alertList);
                curGroup.setOnVaccineUndoClickListener(new ImmunizationRowGroup.OnVaccineUndoClickListener() {
                    @Override
                    public void onUndoClick(ImmunizationRowGroup vaccineGroup, VaccineWrapper vaccine) {
                        addVaccinationDialogFragment(Arrays.asList(vaccine), vaccineGroup);

                    }
                });

                vaccineGroupCanvasLL.addView(curGroup);
                vaccineGroups.add(curGroup);
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }

    }

    private void updateServiceViews(LinearLayout fragmentContainer, boolean editmode) {
        serviceRowGroups = new ArrayList<>();

        List<ServiceRecord> serviceRecords = new ArrayList<>();

        RecurringServiceTypeRepository recurringServiceTypeRepository = ((ChildDetailTabbedActivity) getActivity()).getVaccinatorApplicationInstance().recurringServiceTypeRepository();
        RecurringServiceRecordRepository recurringServiceRecordRepository = ((ChildDetailTabbedActivity) getActivity()).getVaccinatorApplicationInstance().recurringServiceRecordRepository();

        if (recurringServiceRecordRepository != null) {
            serviceRecords = recurringServiceRecordRepository.findByEntityId(childDetails.entityId());
        }

        Map<String, List<ServiceType>> serviceTypeMap = new LinkedHashMap<>();
        if (recurringServiceTypeRepository != null) {
            List<String> types = recurringServiceTypeRepository.fetchTypes();
            for (String type : types) {
                List<ServiceType> subTypes = recurringServiceTypeRepository.findByType(type);
                serviceTypeMap.put(type, subTypes);
            }
        }

        LinearLayout serviceGroupCanvasLL = (LinearLayout) fragmentContainer.findViewById(R.id.services);
        serviceGroupCanvasLL.removeAllViews();

        CustomFontTextView title = new CustomFontTextView(getActivity());
        title.setAllCaps(true);
        title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
        title.setTextColor(getResources().getColor(R.color.text_black));
        title.setText(getString(R.string.recurring));
        serviceGroupCanvasLL.addView(title);

        List<Alert> alertList = new ArrayList<>();
        if (alertService != null) {
            alertList = alertService.findByEntityIdAndAlertNames(childDetails.entityId(),
                    VaccinateActionUtils.allAlertNames(serviceTypeMap.values()));
        }

        try {
            for (String type : serviceTypeMap.keySet()) {
                ServiceRowGroup curGroup = new ServiceRowGroup(getActivity(), editmode);
                curGroup.setData(childDetails, serviceTypeMap.get(type), serviceRecords, alertList);
                curGroup.setOnServiceUndoClickListener(new ServiceRowGroup.OnServiceUndoClickListener() {
                    @Override
                    public void onUndoClick(ServiceRowGroup serviceRowGroup, ServiceWrapper service) {
                        addServiceDialogFragment(service, serviceRowGroup);
                    }
                });

                serviceGroupCanvasLL.addView(curGroup);
                serviceRowGroups.add(curGroup);
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), Log.getStackTraceString(e));
        }

    }

    private String readAssetContents(String path) {
        String fileContents = null;
        try {
            InputStream is = getActivity().getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            fileContents = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            android.util.Log.e(getClass().getName(), ex.toString(), ex);
        }

        return fileContents;
    }


    public void addVaccinationDialogFragment(List<VaccineWrapper> vaccineWrappers, ImmunizationRowGroup vaccineGroup) {
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        android.app.Fragment prev = getActivity().getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        String dobString = Utils.getValue(childDetails.getColumnmaps(), "dob", false);
        Date dob = Calendar.getInstance().getTime();
        if (!TextUtils.isEmpty(dobString)) {
            DateTime dateTime = new DateTime(dobString);
            dob = dateTime.toDate();
        }

        List<Vaccine> vaccineList = VaccinatorApplication.getInstance().vaccineRepository()
                .findByEntityId(childDetails.entityId());
        if (vaccineList == null) vaccineList = new ArrayList<>();

        VaccinationEditDialogFragment vaccinationDialogFragment = VaccinationEditDialogFragment.newInstance(getActivity(), dob, vaccineList, vaccineWrappers, vaccineGroup);
        vaccinationDialogFragment.show(ft, DIALOG_TAG);
    }

    public void addServiceDialogFragment(ServiceWrapper serviceWrapper, ServiceRowGroup serviceRowGroup) {
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        android.app.Fragment prev = getActivity().getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        List<ServiceRecord> serviceRecordList = VaccinatorApplication.getInstance().recurringServiceRecordRepository()
                .findByEntityId(childDetails.entityId());

        ServiceEditDialogFragment serviceEditDialogFragment = ServiceEditDialogFragment.newInstance(serviceRecordList, serviceWrapper, serviceRowGroup);
        serviceEditDialogFragment.show(ft, DIALOG_TAG);
    }

    public void setAlertService(AlertService alertService) {
        this.alertService = alertService;
    }

}
