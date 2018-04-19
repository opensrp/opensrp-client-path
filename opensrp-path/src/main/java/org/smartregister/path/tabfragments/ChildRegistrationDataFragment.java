package org.smartregister.path.tabfragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;

import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.path.R;
import org.smartregister.path.activity.ChildDetailTabbedActivity;
import org.smartregister.path.helper.LocationHelper;
import org.smartregister.util.DateUtil;
import org.smartregister.util.Utils;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import util.JsonFormUtils;
import util.PathConstants;


public class ChildRegistrationDataFragment extends Fragment {
    public CommonPersonObjectClient childDetails;
    private View fragmentView;

    public static ChildRegistrationDataFragment newInstance(Bundle bundle) {
        Bundle args = bundle;
        ChildRegistrationDataFragment fragment = new ChildRegistrationDataFragment();
        if (args == null) {
            args = new Bundle();
        }
        fragment.setArguments(args);
        return fragment;
    }

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
        return fragmentView;
    }

    public void updateChildDetails(CommonPersonObjectClient childDetails) {
        this.childDetails = childDetails;
    }

    public void loadData(Map<String, String> detailsMap) {
        if (fragmentView != null) {

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

            Map<String, String> childDetailsColumnMaps = childDetails.getColumnmaps();

            tvChildsHomeHealthFacility.setText(LocationHelper.getInstance().getOpenMrsReadableName(LocationHelper.getInstance().getOpenMrsLocationName(Utils.getValue(detailsMap, "Home_Facility", false))));
            tvChildsZeirID.setText(Utils.getValue(childDetailsColumnMaps, "zeir_id", false));
            tvChildsRegisterCardNumber.setText(Utils.getValue(detailsMap, "Child_Register_Card_Number", false));
            tvChildsBirthCertificateNumber.setText(Utils.getValue(detailsMap, "Child_Birth_Certificate", false));
            tvChildsFirstName.setText(Utils.getValue(childDetailsColumnMaps, "first_name", true));
            tvChildsLastName.setText(Utils.getValue(childDetailsColumnMaps, "last_name", true));
            tvChildsSex.setText(Utils.getValue(childDetailsColumnMaps, "gender", true));

            String formattedAge = "";
            String dobString = Utils.getValue(childDetailsColumnMaps, PathConstants.EC_CHILD_TABLE.DOB, false);
            Date dob = util.Utils.dobStringToDate(dobString);
            if (dob != null) {
                String childsDateOfBirth = ChildDetailTabbedActivity.DATE_FORMAT.format(dob);
                tvChildsDOB.setText(childsDateOfBirth);

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

            String motherDobString = Utils.getValue(childDetails, "mother_dob", true);
            Date motherDob = util.Utils.dobStringToDate(motherDobString);
            if (motherDob != null) {
                motherDobString = ChildDetailTabbedActivity.DATE_FORMAT.format(motherDob);
            }


            // If default mother dob ... set it as blank
            if (motherDobString != null && motherDobString.equals(JsonFormUtils.MOTHER_DEFAULT_DOB)) {
                motherDobString = "";
            }

            tvMotherDOB.setText(motherDobString);
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
            tvChildsBirthHealthFacility.setText(LocationHelper.getInstance().getOpenMrsReadableName(LocationHelper.getInstance().getOpenMrsLocationName(childsBirthHealthFacility)));

            if (LocationHelper.getInstance().getOpenMrsReadableName(LocationHelper.getInstance().getOpenMrsLocationName(childsBirthHealthFacility)).equalsIgnoreCase("other")) {
                tableRowChildsOtherBirthFacility.setVisibility(View.VISIBLE);
                tvChildsOtherBirthFacility.setText(Utils.getValue(detailsMap, "Birth_Facility_Name_Other", true));
            }

            String childsResidentialArea = Utils.getValue(detailsMap, "address3", false);
            tvChildsResidentialArea.setText(LocationHelper.getInstance().getOpenMrsReadableName(LocationHelper.getInstance().getOpenMrsLocationName(childsResidentialArea)));
            if (LocationHelper.getInstance().getOpenMrsReadableName(LocationHelper.getInstance().getOpenMrsLocationName(childsResidentialArea)).equalsIgnoreCase("other")) {
                tableRowChildsOtherResidentialArea.setVisibility(View.VISIBLE);
                tvChildsOtherResidentialArea.setText(Utils.getValue(detailsMap, "address5", true));
            }

            tvChildsHomeAddress.setText(Utils.getValue(detailsMap, "address2", true));
            tvLandmark.setText(Utils.getValue(detailsMap, "address1", true));
            tvChwName.setText(Utils.getValue(detailsMap, "CHW_Name", true));
            tvChwPhoneNumber.setText(Utils.getValue(detailsMap, "CHW_Phone_Number", true));
            tvHivExposure.setText(Utils.getValue(childDetailsColumnMaps, "pmtct_status", true));
        }
    }
}
