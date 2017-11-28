package org.smartregister.path.tabfragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.smartregister.Context;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.path.R;
import org.smartregister.path.activity.ChildDetailTabbedActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.map.MapHelper;
import org.smartregister.path.viewcomponents.WidgetFactory;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.service.AlertService;
import org.smartregister.util.DateUtil;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import util.JsonFormUtils;
import util.PathConstants;
import utils.exceptions.InvalidMapBoxStyleException;
import utils.helpers.converters.GeoJSONFeature;
import utils.helpers.converters.GeoJSONHelper;

import static org.smartregister.immunization.util.VaccinatorUtils.generateScheduleList;
import static org.smartregister.immunization.util.VaccinatorUtils.nextVaccineDue;
import static org.smartregister.immunization.util.VaccinatorUtils.receivedVaccines;


public class ChildRegistrationDataFragment extends Fragment {
    public CommonPersonObjectClient childDetails;
    private View fragmentView;

    public ChildRegistrationDataFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (this.getArguments() != null) {
            Serializable serializable = getArguments().getSerializable(ChildDetailTabbedActivity.EXTRA_CHILD_DETAILS);
            if (serializable != null && serializable instanceof CommonPersonObjectClient) {
                childDetails = (CommonPersonObjectClient) serializable;
            }
        }
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.child_registration_data_fragment, container, false);
        loadData();
        return fragmentView;
    }

    public void loadData() {
        if (fragmentView != null) {
            Map<String, String> detailsMap;

            CustomFontTextView tvChildsHomeHealthFacility = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_home_health_facility);
            CustomFontTextView tvChildsZeirID = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_zeir_id);
            CustomFontTextView tvChildsRegisterCardNumber = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_register_card_number);
            CustomFontTextView tvChildsBirthCertificateNumber = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_birth_certificate_number);
            CustomFontTextView tvChildsFirstName = (CustomFontTextView) fragmentView.findViewById(R.id.value_first_name);
            CustomFontTextView tvChildsLastName = (CustomFontTextView) fragmentView.findViewById(R.id.value_last_name);
            CustomFontTextView tvChildsSex = (CustomFontTextView) fragmentView.findViewById(R.id.value_sex);
            CustomFontTextView tvChildsDOB = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_dob);
            CustomFontTextView tvChildsAge = (CustomFontTextView) fragmentView.findViewById(R.id.value_age);
            CustomFontTextView tvChildDateFirstSeen = (CustomFontTextView) fragmentView.findViewById(R.id.value_date_first_seen);
            CustomFontTextView tvChildsBirthWeight = (CustomFontTextView) fragmentView.findViewById(R.id.value_birth_weight);
            CustomFontTextView tvMotherFirstName = (CustomFontTextView) fragmentView.findViewById(R.id.value_mother_guardian_first_name);
            CustomFontTextView tvMotherLastName = (CustomFontTextView) fragmentView.findViewById(R.id.value_mother_guardian_last_name);
            CustomFontTextView tvMotherDOB = (CustomFontTextView) fragmentView.findViewById(R.id.value_mother_guardian_dob);
            CustomFontTextView tvMotherNRCNo = (CustomFontTextView) fragmentView.findViewById(R.id.value_mother_guardian_nrc_number);
            CustomFontTextView tvMotherPhoneNumber = (CustomFontTextView) fragmentView.findViewById(R.id.value_mother_guardian_phone_number);
            CustomFontTextView tvFatherFullName = (CustomFontTextView) fragmentView.findViewById(R.id.value_father_guardian_full_name);
            CustomFontTextView tvFatherNRCNo = (CustomFontTextView) fragmentView.findViewById(R.id.value_father_guardian_nrc_number);
            CustomFontTextView tvChildsPlaceOfBirth = (CustomFontTextView) fragmentView.findViewById(R.id.value_place_of_birth);
            CustomFontTextView tvChildsBirthHealthFacility = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_birth_health_facility);
            CustomFontTextView tvChildsOtherBirthFacility = (CustomFontTextView) fragmentView.findViewById(R.id.value_other_birth_facility);
            CustomFontTextView tvChildsResidentialArea = (CustomFontTextView) fragmentView.findViewById(R.id.value_childs_residential_area);
            CustomFontTextView tvChildsOtherResidentialArea = (CustomFontTextView) fragmentView.findViewById(R.id.value_other_childs_residential_area);
            CustomFontTextView tvChildsHomeAddress = (CustomFontTextView) fragmentView.findViewById(R.id.value_home_address);
            CustomFontTextView tvLandmark = (CustomFontTextView) fragmentView.findViewById(R.id.value_landmark);
            CustomFontTextView tvChwName = (CustomFontTextView) fragmentView.findViewById(R.id.value_chw_name);
            CustomFontTextView tvChwPhoneNumber = (CustomFontTextView) fragmentView.findViewById(R.id.value_chw_phone_number);
            CustomFontTextView tvHivExposure = (CustomFontTextView) fragmentView.findViewById(R.id.value_hiv_exposure);

            TableRow tableRowChildsOtherBirthFacility = (TableRow) fragmentView.findViewById(R.id.tableRow_childRegDataFragment_childsOtherBirthFacility);
            TableRow tableRowChildsOtherResidentialArea = (TableRow) fragmentView.findViewById(R.id.tableRow_childRegDataFragment_childsOtherResidentialArea);

            DetailsRepository detailsRepository = ((ChildDetailTabbedActivity) getActivity()).getDetailsRepository();
            childDetails = childDetails != null ? childDetails : ((ChildDetailTabbedActivity) getActivity()).getChildDetails();
            detailsMap = detailsRepository.getAllDetailsForClient(childDetails.entityId());

            Map<String, String> childDetailsColumnMaps = childDetails.getColumnmaps();
            WidgetFactory wd = new WidgetFactory();

            tvChildsHomeHealthFacility.setText(JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName(VaccinatorApplication.getInstance().context(), Utils.getValue(detailsMap, "Home_Facility", false))));
            tvChildsZeirID.setText(Utils.getValue(childDetailsColumnMaps, "zeir_id", false));
            tvChildsRegisterCardNumber.setText(Utils.getValue(detailsMap, "Child_Register_Card_Number", false));
            tvChildsBirthCertificateNumber.setText(Utils.getValue(detailsMap, "Child_Birth_Certificate", false));
            tvChildsFirstName.setText(Utils.getValue(childDetailsColumnMaps, "first_name", true));
            tvChildsLastName.setText(Utils.getValue(childDetailsColumnMaps, "last_name", true));
            tvChildsSex.setText(Utils.getValue(childDetailsColumnMaps, "gender", true));

            boolean containsDOB = Utils.getValue(childDetails.getColumnmaps(), "dob", true).isEmpty();
            String childsDateOfBirth = !containsDOB ? ChildDetailTabbedActivity.DATE_FORMAT.format(new DateTime(Utils.getValue(childDetails.getColumnmaps(), "dob", true)).toDate()) : "";

            tvChildsDOB.setText(childsDateOfBirth);

            String formattedAge = "";
            final String dobString = Utils.getValue(childDetailsColumnMaps, "dob", false);
            if (!TextUtils.isEmpty(dobString)) {
                DateTime dateTime = new DateTime(dobString);
                Date dob = dateTime.toDate();
                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    formattedAge = DateUtil.getDuration(timeDiff);
                }
            }

            tvChildsAge.setText(formattedAge);

            String dateString = Utils.getValue(detailsMap, "First_Health_Facility_Contact", false);
            if (!TextUtils.isEmpty(dateString)) {
                Date date = JsonFormUtils.formatDate(dateString, false);
                if (date != null) {
                    dateString = ChildDetailTabbedActivity.DATE_FORMAT.format(date);
                }
            }

            tvChildDateFirstSeen.setText(dateString);
            tvChildsBirthWeight.setText(Utils.kgStringSuffix(Utils.getValue(detailsMap, "Birth_Weight", true)));
            tvMotherFirstName.setText(Utils.getValue(childDetailsColumnMaps, "mother_first_name", true).isEmpty() ? Utils.getValue(childDetails.getDetails(), "mother_first_name", true) : Utils.getValue(childDetailsColumnMaps, "mother_first_name", true));
            tvMotherLastName.setText(Utils.getValue(childDetailsColumnMaps, "mother_last_name", true).isEmpty() ? Utils.getValue(childDetails.getDetails(), "mother_last_name", true) : Utils.getValue(childDetailsColumnMaps, "mother_last_name", true));

            String motherDob = Utils.getValue(childDetails, "mother_dob", true);

            try {
                DateTime dateTime = new DateTime(motherDob);
                Date mother_dob = dateTime.toDate();
                motherDob = ChildDetailTabbedActivity.DATE_FORMAT.format(mother_dob);
            } catch (Exception e) {
                Log.e(getClass().getCanonicalName(), e.getMessage());
            }

            // If default mother dob ... set it as blank
            if (motherDob != null && motherDob.equals(JsonFormUtils.MOTHER_DEFAULT_DOB)) {
                motherDob = "";
            }

            tvMotherDOB.setText(motherDob);
            tvMotherNRCNo.setText(Utils.getValue(childDetails, "mother_nrc_number", true));
            tvMotherPhoneNumber.setText(Utils.getValue(detailsMap, "Mother_Guardian_Number", true));
            tvFatherFullName.setText(Utils.getValue(detailsMap, "Father_Guardian_Name", true));
            tvFatherNRCNo.setText(Utils.getValue(detailsMap, "Father_NRC_Number", true));

            String placeOfBirthChoice = Utils.getValue(detailsMap, "Place_Birth", true);
            if (placeOfBirthChoice.equalsIgnoreCase("1588AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                placeOfBirthChoice = "Health facility";
            }

            if (placeOfBirthChoice.equalsIgnoreCase("1536AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                placeOfBirthChoice = "Home";
            }

            tvChildsPlaceOfBirth.setText(placeOfBirthChoice);
            String childsBirthHealthFacility = Utils.getValue(detailsMap, "Birth_Facility_Name", false);
            tvChildsBirthHealthFacility.setText(JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName(VaccinatorApplication.getInstance().context(), childsBirthHealthFacility)));

            if (JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName(VaccinatorApplication.getInstance().context(), childsBirthHealthFacility)).equalsIgnoreCase("other")) {
                tableRowChildsOtherBirthFacility.setVisibility(View.VISIBLE);
                tvChildsOtherBirthFacility.setText(Utils.getValue(detailsMap, "Birth_Facility_Name_Other", true));
            }

            String childsResidentialArea = Utils.getValue(detailsMap, "address3", false);
            tvChildsResidentialArea.setText(JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName(VaccinatorApplication.getInstance().context(), childsResidentialArea)));
            if (JsonFormUtils.getOpenMrsReadableName(JsonFormUtils.getOpenMrsLocationName( VaccinatorApplication.getInstance().context(), childsResidentialArea)).equalsIgnoreCase("other")) {
                tableRowChildsOtherResidentialArea.setVisibility(View.VISIBLE);
                tvChildsOtherResidentialArea.setText(Utils.getValue(detailsMap, "address5", true));
            }

            tvChildsHomeAddress.setText(Utils.getValue(detailsMap, "address2", true));
            tvLandmark.setText(Utils.getValue(detailsMap, "address1", true));
            tvChwName.setText(Utils.getValue(detailsMap, "CHW_Name", true));
            tvChwPhoneNumber.setText(Utils.getValue(detailsMap, "CHW_Phone_Number", true));
            tvHivExposure.setText(Utils.getValue(childDetailsColumnMaps, "pmtct_status", true));

            final String entityId = childDetails.entityId();

            ImageView mapIcon = (ImageView) fragmentView.findViewById(R.id.imgv_childRegistrationDataFragment_mapIcon);
            mapIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMyPositionOnMapView(entityId, dobString);
                }
            });
        }
    }

    private void showMyPositionOnMapView(String entityId, String dobString) {

        MapHelper mapHelper = new MapHelper();

        String[] attachmentLayers = getLayers(entityId, dobString);
        try {
            mapHelper.launchMap(getActivity(), "mapbox://styles/ona/cja9rm6rg1syx2smiivtzsmr9",
                    mapHelper.constructKujakuConfig(attachmentLayers),
                    getGeoJSONData(),
                    attachmentLayers,
                    "pk.eyJ1Ijoib25hIiwiYSI6IlVYbkdyclkifQ.0Bz-QOOXZZK01dq4MuMImQ",
                    mapHelper.getLayersToHide(attachmentLayers));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InvalidMapBoxStyleException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*Intent intent = new Intent(getActivity(), MapActivity.class);
        intent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, "pk.eyJ1Ijoib25hIiwiYSI6IlVYbkdyclkifQ.0Bz-QOOXZZK01dq4MuMImQ");
        startActivity(intent);*/
    }

    private String[] getGeoJSONData() throws JSONException {

        LatLng myPosition = new MapHelper().generateRandomLatLng();

        GeoJSONFeature myPositionFeature = new GeoJSONFeature();
        myPositionFeature.addPoint(myPosition);


        GeoJSONHelper geoJSONHelper = new GeoJSONHelper(myPositionFeature);

        GeoJSONFeature geoJSONFeature = new GeoJSONFeature();
        GeoJSONFeature geoJSONFeature2 = new GeoJSONFeature();
        GeoJSONFeature geoJSONFeature3 = new GeoJSONFeature();
        GeoJSONFeature geoJSONFeature4 = new GeoJSONFeature();

        return new String[]{
                geoJSONHelper.getGeoJsonData()/*,
                new GeoJSONHelper(geoJSONFeature).getGeoJsonData(),
                new GeoJSONHelper(geoJSONFeature2).getGeoJsonData(),
                new GeoJSONHelper(geoJSONFeature3).getGeoJsonData(),
                new GeoJSONHelper(geoJSONFeature4).getGeoJsonData()*/
        };
    }

    private String[] getLayers(final String entityId, String dobString) {
        return new String[]{
                getCurrentAlertLayer(entityId, dobString)
        };
    }

    public static String getCurrentAlertLayer(String entityId, String dobString) {
        AlertService alertService;
        VaccineRepository vaccineRepository;

        List<Vaccine> vaccines = new ArrayList<>();
        SmartRegisterClient client;
        Cursor cursor;
        
        Context context = VaccinatorApplication.getInstance().context();
        alertService = context.alertService();
        vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();


        vaccines = vaccineRepository.findByEntityId(entityId);
        List<Alert> alerts = alertService.findByEntityIdAndAlertNames(entityId, VaccinateActionUtils.allAlertNames(PathConstants.KEY.CHILD));

        Map<String, Date> receivedVaccines = receivedVaccines(vaccines);

        List<Map<String, Object>> sch = generateScheduleList(PathConstants.KEY.CHILD, new DateTime(dobString), receivedVaccines, alerts);

        Map<String, Object> currentAlert = null;

        if (vaccines.isEmpty()) {
            List<VaccineRepo.Vaccine> vList = Arrays.asList(VaccineRepo.Vaccine.values());
            currentAlert = nextVaccineDue(sch, vList);
        }

        if (currentAlert == null) {
            Date lastVaccine = null;
            if (!vaccines.isEmpty()) {
                Vaccine vaccine = vaccines.get(vaccines.size() - 1);
                lastVaccine = vaccine.getDate();
            }

            currentAlert = nextVaccineDue(sch, lastVaccine);
        }

        if (currentAlert != null) {

            Alert alert = (Alert) currentAlert.get(PathConstants.KEY.ALERT);
            if (alert == null) {
                return "white kids";
            }

            DateTime dueDate = (DateTime) currentAlert.get(PathConstants.KEY.DATE);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            String alertValue = alert.status().value();

            switch (alertValue) {
                case PathConstants.KEY.URGENT:
                    return "red kids";

                case PathConstants.KEY.NORMAL:
                    return "blue kids";

                case PathConstants.KEY.UPCOMING:
                    if (dueDate.getMillis() >= (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)) && dueDate.getMillis() < (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS))) {
                        return "light blue kids";
                    } else {
                        return "white kids";
                    }

                case PathConstants.KEY.EXPIRED:
                    return "red kids";

                default:
                    return "green kids";
            }
        }

        return "green kids";
    }

}
