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

import org.joda.time.DateTime;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.growthmonitoring.domain.Weight;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import util.PathConstants;


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

    private void createWeightLayout(List<Weight> weightList, LinearLayout fragmentContainer, boolean editmode) {
        LinkedHashMap<Long, Pair<String, String>> weightmap = new LinkedHashMap<>();
        ArrayList<Boolean> weighteditmode = new ArrayList<>();
        ArrayList<View.OnClickListener> listeners = new ArrayList<>();

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
                weightmap.put(weight.getId(), Pair.create(formattedAge, Utils.kgStringSuffix(weight.getKg())));

                boolean lessThanThreeMonthsEventCreated = WeightUtils.lessThanThreeMonths(weight);
                if (lessThanThreeMonthsEventCreated) {
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
            weightmap.put(0l, Pair.create(DateUtil.getDuration(0), Utils.getValue(detailsMap, "Birth_Weight", true) + " kg"));
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

    public void updateVaccinationViews(List<Vaccine> vaccineList, List<Alert> alertList, boolean editVaccineMode) {
        boolean showVaccine = curVaccineMode == null || !curVaccineMode.equals(editVaccineMode);
        if (fragmentContainer != null && showVaccine) {
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
                                   List<ServiceRecord> serviceRecords, List<Alert> alertList, boolean editServiceMode) {
        boolean showService = curServiceMode == null || !curServiceMode.equals(editServiceMode);
        if (fragmentContainer != null && showService) {
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
            Log.e(getClass().getName(), ex.toString(), ex);
        }

        return fileContents;
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
        if (vaccineList == null) vaccineList = new ArrayList<>();

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

        ServiceEditDialogFragment serviceEditDialogFragment = ServiceEditDialogFragment.newInstance(dateTime, serviceRecordList, serviceWrapper, serviceRowGroup, true);
        serviceEditDialogFragment.show(ft, DIALOG_TAG);
    }

}
