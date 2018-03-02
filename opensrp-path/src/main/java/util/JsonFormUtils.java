package util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Address;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.FormEntityConstants;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.ProfileImage;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.path.R;
import org.smartregister.path.activity.ChildSmartRegisterActivity;
import org.smartregister.path.activity.PathJsonFormActivity;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.domain.jsonmapping.FormLocation;
import org.smartregister.path.repository.UniqueIdRepository;
import org.smartregister.path.service.intent.CoverageDropoutIntentService;
import org.smartregister.path.sync.ECSyncUpdater;
import org.smartregister.path.sync.PathClientProcessorForJava;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.ImageRepository;
import org.smartregister.sync.ClientProcessor;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import id.zelory.compressor.Compressor;

/**
 * Created by keyman on 08/02/2017.
 */
public class JsonFormUtils extends org.smartregister.util.JsonFormUtils {
    private static final String TAG = "JsonFormUtils";

    public static final String MOTHER_DEFAULT_DOB = "01-01-1960";
    private static final String ENCOUNTER = "encounter";
    public static final String RELATIONAL_ID = "relational_id";
    public static final String ENCOUNTER_TYPE = "encounter_type";
    public static final String CURRENT_ZEIR_ID = "current_zeir_id";
    public static final String READ_ONLY = "read_only";
    private static final String METADATA = "metadata";
    public static final String ZEIR_ID = "ZEIR_ID";
    private static final String M_ZEIR_ID = "M_ZEIR_ID";
    public static final String encounterType = "Update Birth Registration";
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final SimpleDateFormat dd_MM_yyyy = new SimpleDateFormat("dd-MM-yyyy");
    //public static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    //2007-03-31T04:00:00.000Z
    public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").registerTypeAdapter(DateTime.class, new DateTimeTypeConverter()).create();


    public static void saveForm(Context context, org.smartregister.Context openSrpContext,
                                String jsonString, String providerId) {
        try {
            JSONObject form = new JSONObject(jsonString);
            if (form.getString(ENCOUNTER_TYPE).equals(PathConstants.EventType.OUT_OF_CATCHMENT_SERVICE)) {
                saveOutOfAreaService(context, openSrpContext, jsonString);
            } else if (form.getString(ENCOUNTER_TYPE).equals(PathConstants.EventType.BITRH_REGISTRATION)) {
                saveBirthRegistration(context, openSrpContext, jsonString, providerId, "Child_Photo", "child", "mother");
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void saveAdverseEvent(String jsonString, String locationId, String baseEntityId,
                                        String providerId) {
        org.smartregister.util.Utils.startAsyncTask(
                new SaveAdverseEventTask(jsonString, locationId, baseEntityId, providerId), null);
    }

    private static void saveBirthRegistration(Context context, org.smartregister.Context openSrpContext,
                                              String jsonString, String providerId, String imageKey, String bindType,
                                              String subBindType) {
        if (context == null || openSrpContext == null || StringUtils.isBlank(providerId)
                || StringUtils.isBlank(jsonString)) {
            return;
        }

        org.smartregister.util.Utils.startAsyncTask(
                new SaveBirthRegistrationTask(context, openSrpContext, jsonString, providerId, imageKey, bindType, subBindType), null
        );
    }

    public static void editsave(Context context, org.smartregister.Context openSrpContext, String jsonString, String providerId, String imageKey, String bindType, String subBindType) {
        if (context == null || StringUtils.isBlank(providerId) || StringUtils.isBlank(jsonString)) {
            return;
        }

        try {
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

            JSONObject jsonForm = new JSONObject(jsonString);

            String entityId = getString(jsonForm, ENTITY_ID);
            String relationalId = getString(jsonForm, RELATIONAL_ID);

            if (StringUtils.isBlank(entityId)) {
                entityId = generateRandomUUIDString();
            }

            JSONArray fields = fields(jsonForm);
            if (fields == null) {
                return;
            }

            for (int i = 0; i < fields.length(); i++) {
                String key = fields.getJSONObject(i).getString("key");
                if ("Home_Facility".equals(key)
                        || "Birth_Facility_Name".equals(key)
                        || "Residential_Area".equals(key)) {
                    try {
                        String rawValue = fields.getJSONObject(i).getString("value");
                        JSONArray valueArray = new JSONArray(rawValue);
                        if (valueArray.length() > 0) {
                            String lastLocationName = valueArray.getString(valueArray.length() - 1);
                            String lastLocationId = LocationUtils.getOpenMrsLocationId(lastLocationName);
                            fields.getJSONObject(i).put("value", lastLocationId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                } else if ("Mother_Guardian_Date_Birth".equals(key)) {
                    if (TextUtils.isEmpty(fields.getJSONObject(i).optString("value"))) {
                        fields.getJSONObject(i).put("value", MOTHER_DEFAULT_DOB);
                    }
                }
            }

            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            Client baseClient = JsonFormUtils.createBaseClient(fields, entityId);
            Event e = JsonFormUtils.createEvent(openSrpContext, fields, metadata, entityId, encounterType, providerId, bindType);

            Client subFormClient = null;

            JSONObject lookUpJSONObject = getJSONObject(metadata, "look_up");
            String lookUpEntityId = "";
            String lookUpBaseEntityId = "";
            if (lookUpJSONObject != null) {
                lookUpEntityId = getString(lookUpJSONObject, "entity_id");
                lookUpBaseEntityId = getString(lookUpJSONObject, "value");
            }

            if ("mother".equals(lookUpEntityId) && StringUtils.isNotBlank(lookUpBaseEntityId)) {
                Client ss = new Client(lookUpBaseEntityId);
                addRelationship(context, ss, baseClient);
            }

            if (StringUtils.isNotBlank(subBindType)) {
                subFormClient = JsonFormUtils.createSubformClient(context, fields, baseClient, subBindType, relationalId);
            }
            Event se = null;
            if (subFormClient != null && e != null) {
                JSONObject subBindTypeJson = getJSONObject(jsonForm, subBindType);
                if (subBindTypeJson != null) {
                    String subBindTypeEncounter = getString(subBindTypeJson, ENCOUNTER_TYPE);
                    if (StringUtils.isNotBlank(subBindTypeEncounter)) {
                        se = JsonFormUtils.createSubFormEvent(null, metadata, e, subFormClient.getBaseEntityId(), subBindTypeEncounter, providerId, subBindType);
                    }
                }
            }
            if (baseClient != null) {
                mergeAndSaveClient(context, baseClient);

            }
            if (e != null) {

                JSONObject eventJson = new JSONObject(gson.toJson(e));
                ecUpdater.addEvent(e.getBaseEntityId(), eventJson);

            }
            if (subFormClient != null) {
                mergeAndSaveClient(context, subFormClient);

            }
            if (se != null) {
                JSONObject eventJson = new JSONObject(gson.toJson(se));
                ecUpdater.addEvent(se.getBaseEntityId(), eventJson);
            }


            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);
            long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            PathClientProcessorForJava.getInstance(context).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
            allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());

            String imageLocation = getFieldValue(fields, imageKey);
            if (!TextUtils.isEmpty(imageLocation)) {
                saveImage(context, providerId, entityId, imageLocation);
            }

            // Unassign current id
            if (baseClient != null) {
                String newZeirId = baseClient.getIdentifier(ZEIR_ID).replace("-", "");
                String currentZeirId = getString(jsonForm, "current_zeir_id").replace("-", "");
                if (!newZeirId.equals(currentZeirId)) {
                    //ZEIR_ID was changed
                    VaccinatorApplication.getInstance().uniqueIdRepository().open(currentZeirId);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    private static void mergeAndSaveClient(Context context, Client baseClient) throws Exception {
        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        JSONObject updatedClientJson = new JSONObject(gson.toJson(baseClient));

        JSONObject originalClientJsonObject = ecUpdater.getClient(baseClient.getBaseEntityId());

        JSONObject mergedJson = merge(originalClientJsonObject, updatedClientJson);

        //TODO Save edit log

        //save the updated client (the one updated and generated from the form) as EditClient to keep an edit log of the client doc
        // originalClient.setType("PristineClient");
        //originalClient.setRev(null);
        //cloudantDataHandler.addClient(originalClient);

        ecUpdater.addClient(baseClient.getBaseEntityId(), mergedJson);


    }

    private static void saveOutOfAreaService(Context context, org.smartregister.Context openSrpContext,
                                             String jsonString) {
        SaveOutOfAreaServiceTask saveOutOfAreaServiceTask = new SaveOutOfAreaServiceTask(context,
                openSrpContext, jsonString);

        org.smartregister.util.Utils.startAsyncTask(saveOutOfAreaServiceTask, null);
    }

    /**
     * Constructs a weight object using the out of service area form
     *
     * @param openSrpContext The context to work with
     * @param outOfAreaForm  Out of area form to extract the weight form
     * @return A weight object if weight recorded in form, or {@code null} if weight not recorded
     * @throws Exception
     */
    private static Weight getWeightObject(org.smartregister.Context openSrpContext, JSONObject outOfAreaForm) throws Exception {
        Weight weight = null;
        JSONArray fields = outOfAreaForm.getJSONObject("step1").getJSONArray("fields");
        String serviceDate = null;
        String zeirId = null;

        int foundFields = 0;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject curField = fields.getJSONObject(i);
            if (curField.getString("key").equals("Weight_Kg")) {
                foundFields++;
                if (StringUtils.isNotEmpty(curField.getString("value"))) {
                    weight = new Weight();
                    weight.setBaseEntityId("");
                    weight.setKg(Float.parseFloat(curField.getString("value")));
                    weight.setAnmId(openSrpContext.allSharedPreferences().fetchRegisteredANM());
                    weight.setLocationId(outOfAreaForm.getJSONObject("metadata")
                            .getString("encounter_location"));
                    weight.setUpdatedAt(null);
                }
            } else if (curField.getString("key").equals("OA_Service_Date")) {
                foundFields++;
                serviceDate = curField.getString("value");
            } else if (curField.getString("key").equals("ZEIR_ID")) {
                foundFields++;
                zeirId = formatChildUniqueId(curField.getString("value"));
            }

            if (foundFields == 3) {
                break;
            }
        }

        if (weight != null && serviceDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            weight.setDate(dateFormat.parse(serviceDate));
        }

        if (weight != null && zeirId != null) {
            weight.setProgramClientId(zeirId);
        }

        return weight;
    }

    /**
     * Constructs a list of recorded vaccines from the out of area form provided
     *
     * @param openSrpContext The context to use
     * @param outOfAreaForm  Out of area form to extract recorded vaccines from
     * @return A list of recorded vaccines
     */
    private static ArrayList<Vaccine> getVaccineObjects(Context context,
                                                        org.smartregister.Context openSrpContext,
                                                        JSONObject outOfAreaForm) throws Exception {
        ArrayList<Vaccine> vaccines = new ArrayList<>();
        JSONArray fields = outOfAreaForm.getJSONObject("step1").getJSONArray("fields");
        String serviceDate = null;
        String zeirId = null;

        for (int i = 0; i < fields.length(); i++) {
            JSONObject curField = fields.getJSONObject(i);
            if (curField.has("is_vaccine_group")
                    && curField.getBoolean("is_vaccine_group")
                    && curField.getString("type").equals("check_box")) {
                JSONArray options = curField.getJSONArray("options");
                for (int j = 0; j < options.length(); j++) {
                    JSONObject curOption = options.getJSONObject(j);
                    if (curOption.getString("value").equalsIgnoreCase(Boolean.TRUE.toString())) {
                        Vaccine curVaccine = new Vaccine();
                        curVaccine.setBaseEntityId("");
                        curVaccine.setName(curOption.getString("key"));
                        curVaccine.setAnmId(openSrpContext.allSharedPreferences().fetchRegisteredANM());
                        curVaccine.setLocationId(outOfAreaForm.getJSONObject("metadata")
                                .getString("encounter_location"));
                        curVaccine.setCalculation(VaccinatorUtils.getVaccineCalculation(context,
                                curVaccine.getName()));
                        curVaccine.setUpdatedAt(null);

                        vaccines.add(curVaccine);
                    }
                }
            } else if (curField.getString("key").equals("OA_Service_Date")) {
                serviceDate = curField.getString("value");
            } else if (curField.getString("key").equals("ZEIR_ID")) {
                zeirId = formatChildUniqueId(curField.getString("value"));
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        for (Vaccine curVaccine : vaccines) {
            if (serviceDate != null) {
                curVaccine.setDate(dateFormat.parse(serviceDate));
            }

            if (zeirId != null) {
                curVaccine.setProgramClientId(zeirId);
            }
        }

        return vaccines;
    }

    public static void saveImage(Context context, String providerId, String entityId, String imageLocation) {
        if (StringUtils.isBlank(imageLocation)) {
            return;
        }


        File file = new File(imageLocation);

        if (!file.exists()) {
            return;
        }

        Bitmap compressedImageFile = Compressor.getDefault(context).compressToBitmap(file);
        saveStaticImageToDisk(compressedImageFile, providerId, entityId);

    }

    private static void saveStaticImageToDisk(Bitmap image, String providerId, String entityId) {
        if (image == null || StringUtils.isBlank(providerId) || StringUtils.isBlank(entityId)) {
            return;
        }
        OutputStream os = null;
        try {

            if (entityId != null && !entityId.isEmpty()) {
                final String absoluteFileName = DrishtiApplication.getAppDir() + File.separator + entityId + ".JPEG";

                File outputFile = new File(absoluteFileName);
                os = new FileOutputStream(outputFile);
                Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                if (compressFormat != null) {
                    image.compress(compressFormat, 100, os);
                } else {
                    throw new IllegalArgumentException("Failed to save static image, could not retrieve image compression format from name "
                            + absoluteFileName);
                }
                // insert into the db
                ProfileImage profileImage = new ProfileImage();
                profileImage.setImageid(UUID.randomUUID().toString());
                profileImage.setAnmId(providerId);
                profileImage.setEntityID(entityId);
                profileImage.setFilepath(absoluteFileName);
                profileImage.setFilecategory("profilepic");
                profileImage.setSyncStatus(ImageRepository.TYPE_Unsynced);
                ImageRepository imageRepo = VaccinatorApplication.getInstance().context().imageRepository();
                imageRepo.add(profileImage);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to save static image to disk");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close static images output stream after attempting to write image");
                }
            }
        }

    }

    private static Client createBaseClient(JSONArray fields, String entityId) {

        String firstName = getFieldValue(fields, FormEntityConstants.Person.first_name);
        String middleName = getFieldValue(fields, FormEntityConstants.Person.middle_name);
        String lastName = getFieldValue(fields, FormEntityConstants.Person.last_name);
        String bd = getFieldValue(fields, FormEntityConstants.Person.birthdate);
        Date birthdate = formatDate(bd, true);
        String dd = getFieldValue(fields, FormEntityConstants.Person.deathdate);
        Date deathdate = formatDate(dd, true);
        String aproxbd = getFieldValue(fields, FormEntityConstants.Person.birthdate_estimated);
        Boolean birthdateApprox = false;
        if (!StringUtils.isEmpty(aproxbd) && NumberUtils.isNumber(aproxbd)) {
            int bde = 0;
            try {
                bde = Integer.parseInt(aproxbd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            birthdateApprox = bde > 0;
        }
        String aproxdd = getFieldValue(fields, FormEntityConstants.Person.deathdate_estimated);
        Boolean deathdateApprox = false;
        if (!StringUtils.isEmpty(aproxdd) && NumberUtils.isNumber(aproxdd)) {
            int dde = 0;
            try {
                dde = Integer.parseInt(aproxdd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            deathdateApprox = dde > 0;
        }
        String gender = getFieldValue(fields, FormEntityConstants.Person.gender);

        List<Address> addresses = new ArrayList<>(extractAddresses(fields).values());

        Client c = (Client) new Client(entityId)
                .withFirstName(firstName)
                .withMiddleName(middleName)
                .withLastName(lastName)
                .withBirthdate((birthdate != null ? birthdate : null), birthdateApprox)
                .withDeathdate(deathdate != null ? deathdate : null, deathdateApprox)
                .withGender(gender).withDateCreated(new Date());

        c.withAddresses(addresses)
                .withAttributes(extractAttributes(fields))
                .withIdentifiers(extractIdentifiers(fields));
        return c;

    }

    private static Event createEvent(org.smartregister.Context openSrpContext,
                                     JSONArray fields, JSONObject metadata, String entityId,
                                     String encounterType, String providerId, String bindType) {

        String encounterDateField = getFieldValue(fields, FormEntityConstants.Encounter.encounter_date);
        String encounterLocation = null;

        Date encounterDate = new Date();
        if (StringUtils.isNotBlank(encounterDateField)) {
            Date dateTime = formatDate(encounterDateField, false);
            if (dateTime != null) {
                encounterDate = dateTime;
            }
        }
        try {
            encounterLocation = metadata.getString("encounter_location");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        Event e = (Event) new Event()
                .withBaseEntityId(entityId)//should be different for main and subform
                .withEventDate(encounterDate)
                .withEventType(encounterType)
                .withLocationId(encounterLocation)
                .withProviderId(providerId)
                .withEntityType(bindType)
                .withFormSubmissionId(generateRandomUUIDString())
                .withDateCreated(new Date());

        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            String value = getString(jsonObject, VALUE);
            if (StringUtils.isNotBlank(value)) {
                addObservation(e, jsonObject);
            }
        }

        if (metadata != null) {
            Iterator<?> keys = metadata.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject jsonObject = getJSONObject(metadata, key);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                    if (entityVal != null) {
                        if (entityVal.equals(CONCEPT)) {
                            addToJSONObject(jsonObject, KEY, key);
                            addObservation(e, jsonObject);
                        } else if (entityVal.equals(ENCOUNTER)) {
                            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                            if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                Date eDate = formatDate(value, false);
                                if (eDate != null) {
                                    e.setEventDate(eDate);
                                }
                            }
                        }
                    }
                }
            }
        }

        return e;

    }

    public static void fillIdentifiers(Map<String, String> pids, JSONObject jsonObject) {

        String value = getString(jsonObject, VALUE);
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (StringUtils.isNotBlank(getString(jsonObject, ENTITY_ID))) {
            return;
        }

        String entity = PERSON_INDENTIFIER;
        String entityVal = getString(jsonObject, OPENMRS_ENTITY);

        if (entityVal != null && entityVal.equals(entity)) {
            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);

            if (entityIdVal.equals(ZEIR_ID)) {
                value = formatChildUniqueId(value);
            }

            pids.put(entityIdVal, value);
        }

    }

    /**
     * This method formats the child unique id obtained from a JSON Form to something that is useable
     *
     * @param unformattedId The unformatted unique identifier
     * @return A formatted ID or the original id if method is unable to format
     */
    private static String formatChildUniqueId(String unformattedId) {
        if (StringUtils.isNotBlank(unformattedId) && !unformattedId.contains("-")) {
            StringBuilder stringBuilder = new StringBuilder(unformattedId);
            stringBuilder.insert(unformattedId.length() - 1, '-');
            unformattedId = stringBuilder.toString();
        }

        return unformattedId;
    }


    private static Client createSubformClient(Context context, JSONArray fields, Client parent, String bindType, String relationalId) throws ParseException {

        if (StringUtils.isBlank(bindType)) {
            return null;
        }

        String entityId = relationalId == null ? generateRandomUUIDString() : relationalId;
        String firstName = getSubFormFieldValue(fields, FormEntityConstants.Person.first_name, bindType);
        String gender = getSubFormFieldValue(fields, FormEntityConstants.Person.gender, bindType);
        String bb = getSubFormFieldValue(fields, FormEntityConstants.Person.birthdate, bindType);

        Map<String, String> idents = extractIdentifiers(fields, bindType);
        String parentIdentifier = parent.getIdentifier(ZEIR_ID);
        if (StringUtils.isNotBlank(parentIdentifier)) {
            String identifier = parentIdentifier.concat("_").concat(bindType);
            idents.put(M_ZEIR_ID, identifier);
        }

        String middleName = getSubFormFieldValue(fields, FormEntityConstants.Person.middle_name, bindType);
        String lastName = getSubFormFieldValue(fields, FormEntityConstants.Person.last_name, bindType);
        Date birthdate = formatDate(bb, true);
        String dd = getSubFormFieldValue(fields, FormEntityConstants.Person.deathdate, bindType);
        Date deathdate = formatDate(dd, true);
        String aproxbd = getSubFormFieldValue(fields, FormEntityConstants.Person.birthdate_estimated, bindType);
        Boolean birthdateApprox = false;
        if (!StringUtils.isEmpty(aproxbd) && NumberUtils.isNumber(aproxbd)) {
            int bde = 0;
            try {
                bde = Integer.parseInt(aproxbd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            birthdateApprox = bde > 0;
        }
        String aproxdd = getSubFormFieldValue(fields, FormEntityConstants.Person.deathdate_estimated, bindType);
        Boolean deathdateApprox = false;
        if (!StringUtils.isEmpty(aproxdd) && NumberUtils.isNumber(aproxdd)) {
            int dde = 0;
            try {
                dde = Integer.parseInt(aproxdd);
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            deathdateApprox = dde > 0;
        }

        List<Address> addresses = new ArrayList<>(extractAddresses(fields, bindType).values());

        Client c = (Client) new Client(entityId)
                .withFirstName(firstName)
                .withMiddleName(middleName)
                .withLastName(lastName)
                .withBirthdate(birthdate, birthdateApprox)
                .withDeathdate(deathdate, deathdateApprox)
                .withGender(gender).withDateCreated(new Date());

        c.withAddresses(addresses)
                .withAttributes(extractAttributes(fields, bindType))
                .withIdentifiers(idents);

        if (addresses.isEmpty()) {
            c.withAddresses(parent.getAddresses());
        }

        addRelationship(context, c, parent);

        return c;
    }

    private static Event createSubFormEvent(JSONArray fields, JSONObject metadata, Event parent, String entityId, String encounterType, String providerId, String bindType) {


        Event e = (Event) new Event()
                .withBaseEntityId(entityId)//should be different for main and subform
                .withEventDate(parent.getEventDate())
                .withEventType(encounterType)
                .withLocationId(parent.getLocationId())
                .withProviderId(providerId)
                .withEntityType(bindType)
                .withFormSubmissionId(generateRandomUUIDString())
                .withDateCreated(new Date());

        if (fields != null && fields.length() != 0)
            for (int i = 0; i < fields.length(); i++) {
                JSONObject jsonObject = getJSONObject(fields, i);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    addObservation(e, jsonObject);
                }
            }

        if (metadata != null) {
            Iterator<?> keys = metadata.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject jsonObject = getJSONObject(metadata, key);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                    if (entityVal != null) {
                        if (entityVal.equals(CONCEPT)) {
                            addToJSONObject(jsonObject, KEY, key);
                            addObservation(e, jsonObject);
                        } else if (entityVal.equals(ENCOUNTER)) {
                            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                            if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                Date eDate = formatDate(value, false);
                                if (eDate != null) {
                                    e.setEventDate(eDate);
                                }
                            }
                        }
                    }
                }
            }
        }

        return e;

    }

    private static void addRelationship(Context context, Client parent, Client child) {
        try {
            String relationships = AssetHandler.readFileFromAssetsFolder(FormUtils.ecClientRelationships, context);
            JSONArray jsonArray = null;

            jsonArray = new JSONArray(relationships);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject rObject = jsonArray.getJSONObject(i);
                if (rObject.has("field") && getString(rObject, "field").equals(ENTITY_ID)) {
                    child.addRelationship(rObject.getString("client_relationship"), parent.getBaseEntityId());
                } /* else {
                    //TODO how to add other kind of relationships
                  } */
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    public static void fillSubFormIdentifiers(Map<String, String> pids, JSONObject jsonObject, String bindType) {

        String value = getString(jsonObject, VALUE);
        if (StringUtils.isBlank(value)) {
            return;
        }

        String bind = getString(jsonObject, ENTITY_ID);
        if (bind == null || !bind.equals(bindType)) {
            return;
        }

        String entity = PERSON_INDENTIFIER;
        String entityVal = getString(jsonObject, OPENMRS_ENTITY);

        if (entityVal != null && entityVal.equals(entity)) {
            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);

            if (entityIdVal.equals(ZEIR_ID) && StringUtils.isNotBlank(value) && !value.contains("-")) {
                StringBuilder stringBuilder = new StringBuilder(value);
                stringBuilder.insert(value.length() - 1, '-');
                value = stringBuilder.toString();
            }

            pids.put(entityIdVal, value);
        }
    }

    public static Map<String, String> extractIdentifiers(JSONArray fields) {
        Map<String, String> pids = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            fillIdentifiers(pids, jsonObject);
        }
        return pids;
    }


    public static Map<String, String> extractIdentifiers(JSONArray fields, String bindType) {
        Map<String, String> pids = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            fillSubFormIdentifiers(pids, jsonObject, bindType);
        }
        return pids;
    }


    public static void addChildRegLocHierarchyQuestions(JSONObject form) {
        try {
            JSONArray questions = form.getJSONObject("step1").getJSONArray("fields");
            ArrayList<String> allLevels = new ArrayList<>();
            allLevels.add("Country");
            allLevels.add("Province");
            allLevels.add("District");
            allLevels.add("Health Facility");
            allLevels.add("Zone");
            allLevels.add("Residential Area");

            ArrayList<String> healthFacilities = new ArrayList<>();
            healthFacilities.add("Country");
            healthFacilities.add("Province");
            healthFacilities.add("District");
            healthFacilities.add("Health Facility");

            List<String> defaultLocation = LocationUtils.generateDefaultLocationHierarchy(allLevels);
            List<String> defaultFacility = LocationUtils.generateDefaultLocationHierarchy(healthFacilities);
            List<FormLocation> upToFacilities = LocationUtils.generateLocationHierarchyTree(false, healthFacilities);
            List<FormLocation> upToFacilitiesWithOther = LocationUtils.generateLocationHierarchyTree(true, healthFacilities);
            List<FormLocation> entireTree = LocationUtils.generateLocationHierarchyTree(true, allLevels);

            String defaultLocationString = AssetHandler.javaToJsonString(defaultLocation,
                    new TypeToken<List<String>>() {
                    }.getType());

            String defaultFacilityString = AssetHandler.javaToJsonString(defaultFacility,
                    new TypeToken<List<String>>() {
                    }.getType());

            String upToFacilitiesString = AssetHandler.javaToJsonString(upToFacilities,
                    new TypeToken<List<FormLocation>>() {
                    }.getType());

            String upToFacilitiesWithOtherString = AssetHandler.javaToJsonString(upToFacilitiesWithOther,
                    new TypeToken<List<FormLocation>>() {
                    }.getType());

            String entireTreeString = AssetHandler.javaToJsonString(entireTree,
                    new TypeToken<List<FormLocation>>() {
                    }.getType());

            for (int i = 0; i < questions.length(); i++) {
                if (questions.getJSONObject(i).getString("key").equals("Home_Facility")) {
                    if (StringUtils.isNotBlank(upToFacilitiesString)) {
                        questions.getJSONObject(i).put("tree", new JSONArray(upToFacilitiesString));
                    }
                    if (StringUtils.isNotBlank(defaultFacilityString)) {
                        questions.getJSONObject(i).put("default", defaultFacilityString);
                    }
                } else if (questions.getJSONObject(i).getString("key").equals("Birth_Facility_Name")) {
                    if (StringUtils.isNotBlank(upToFacilitiesWithOtherString)) {
                        questions.getJSONObject(i).put("tree", new JSONArray(upToFacilitiesWithOtherString));
                    }
                    if (StringUtils.isNotBlank(defaultFacilityString)) {
                        questions.getJSONObject(i).put("default", defaultFacilityString);
                    }
                } else if (questions.getJSONObject(i).getString("key").equals("Residential_Area")) {
                    if (StringUtils.isNotBlank(entireTreeString)) {
                        questions.getJSONObject(i).put("tree", new JSONArray(entireTreeString));
                    }
                    if (StringUtils.isNotBlank(defaultLocationString)) {
                        questions.getJSONObject(i).put("default", defaultLocationString);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static void addAddAvailableVaccines(Context context, JSONObject form) {
        String supportedVaccinesString = VaccinatorUtils.getSupportedVaccines(context);
        if (StringUtils.isNotEmpty(supportedVaccinesString) && form != null) {
            // For each of the vaccine groups, create a checkbox question
            try {
                JSONArray questionList = form.getJSONObject("step1").getJSONArray("fields");
                JSONObject vaccinationLabel = new JSONObject();
                vaccinationLabel.put("key", "Vaccines_Provided_Label");
                vaccinationLabel.put("type", "label");
                vaccinationLabel.put("text", "Which vaccinations were provided?");
                vaccinationLabel.put("openmrs_entity_parent", "-");
                vaccinationLabel.put("openmrs_entity", "-");
                vaccinationLabel.put("openmrs_entity_id", "-");
                questionList.put(vaccinationLabel);
                JSONArray supportedVaccines = new JSONArray(supportedVaccinesString);

                HashMap<String, ArrayList<JSONObject>> vaccineTypeConstraints = new HashMap<>();
                for (int i = 0; i < supportedVaccines.length(); i++) {
                    JSONObject curVaccineGroup = supportedVaccines.getJSONObject(i);
                    JSONArray vaccines = curVaccineGroup.getJSONArray("vaccines");
                    for (int j = 0; j < vaccines.length(); j++) {
                        JSONObject curVaccine = vaccines.getJSONObject(j);
                        if (!vaccineTypeConstraints.containsKey(curVaccine.getString("type"))) {
                            vaccineTypeConstraints.put(curVaccine.getString("type"),
                                    new ArrayList<JSONObject>());
                        }
                        ArrayList<String> vaccineNamesDefined = new ArrayList<>();
                        if (curVaccine.has("vaccine_separator")) {
                            String unsplitNames = curVaccine.getString("name");
                            String separator = curVaccine.getString("vaccine_separator");
                            String[] splitValues = unsplitNames.split(separator);
                            for (String splitValue : splitValues) {
                                vaccineNamesDefined.add(splitValue);
                            }
                        } else {
                            vaccineNamesDefined.add(curVaccine.getString("name"));
                        }

                        for (String curVaccineName : vaccineNamesDefined) {
                            JSONObject curConstraint = new JSONObject();
                            curConstraint.put("vaccine", curVaccineName);
                            curConstraint.put("type", "array");
                            curConstraint.put("ex",
                                    "notEqualTo(step1:" + curVaccineGroup.getString("id") + ", \"[\"" + curVaccineName + "\"]\")");
                            curConstraint.put("err", "Cannot be given with the other " + curVaccine.getString("type") + " dose");
                            vaccineTypeConstraints.get(curVaccine.getString("type")).add(curConstraint);
                        }
                    }
                }

                for (int i = 0; i < supportedVaccines.length(); i++) {
                    JSONObject curVaccineGroup = supportedVaccines.getJSONObject(i);
                    JSONObject curQuestion = new JSONObject();
                    curQuestion.put("key", curVaccineGroup.getString("id"));
                    curQuestion.put("type", "check_box");
                    curQuestion.put("is_vaccine_group", true);
                    curQuestion.put("label", curVaccineGroup.getString("name"));
                    curQuestion.put("openmrs_entity_parent", "-");
                    curQuestion.put("openmrs_entity", "-");
                    curQuestion.put("openmrs_entity_id", "-");

                    JSONArray vaccines = curVaccineGroup.getJSONArray("vaccines");
                    JSONArray options = new JSONArray();
                    for (int j = 0; j < vaccines.length(); j++) {
                        ArrayList<String> definedVaccineNames = new ArrayList<>();
                        if (vaccines.getJSONObject(j).has("vaccine_separator")) {
                            String rawNames = vaccines.getJSONObject(j).getString("name");
                            String separator = vaccines.getJSONObject(j).getString("vaccine_separator");
                            String[] split = rawNames.split(separator);
                            for (String aSplit : split) {
                                definedVaccineNames.add(aSplit);
                            }
                        } else {
                            definedVaccineNames.add(vaccines.getJSONObject(j).getString("name"));
                        }

                        for (String curVaccineName : definedVaccineNames) {
                            JSONObject curVaccines = new JSONObject();
                            curVaccines.put("key", curVaccineName);
                            curVaccines.put("text", curVaccineName);
                            curVaccines.put("value", "false");
                            JSONArray constraints = new JSONArray();

                            // Add the constraints
                            if (vaccineTypeConstraints.containsKey(vaccines.getJSONObject(j).getString("type"))) {
                                for (JSONObject curConstraint : vaccineTypeConstraints.get(vaccines.getJSONObject(j).getString("type"))) {
                                    if (!curConstraint.getString("vaccine")
                                            .equals(curVaccineName)) {
                                        JSONObject constraintClone = new JSONObject(curConstraint.toString());
                                        constraintClone.remove("vaccine");
                                        constraints.put(constraintClone);
                                    }
                                }
                            }

                            if (constraints.length() > 0) {
                                curVaccines.put("constraints", constraints);
                            }

                            options.put(curVaccines);
                        }
                    }

                    curQuestion.put("options", options);
                    questionList.put(curQuestion);
                }
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    public static void saveReportDeceased(Context context, org.smartregister.Context openSrpContext,
                                          String jsonString, String providerId, String locationId, String entityId) {

        try {
            EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();

            JSONObject jsonForm = new JSONObject(jsonString);

            JSONArray fields = fields(jsonForm);
            if (fields == null) {
                return;
            }

            String encounterDateField = getFieldValue(fields, "Date_of_Death");

            String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            Date encounterDate = new Date();
            String encounterDateTimeString = null;
            if (StringUtils.isNotBlank(encounterDateField)) {
                encounterDateTimeString = formatDate(encounterDateField);
                Date dateTime = formatDate(encounterDateField, false);
                if (dateTime != null) {
                    encounterDate = dateTime;
                }
            }

            Event event = (Event) new Event()
                    .withBaseEntityId(entityId) //should be different for main and subform
                    .withEventDate(encounterDate)
                    .withEventType(encounterType)
                    .withLocationId(locationId)
                    .withProviderId(providerId)
                    .withEntityType(PathConstants.EntityType.CHILD)
                    .withFormSubmissionId(generateRandomUUIDString())
                    .withDateCreated(new Date());

            for (int i = 0; i < fields.length(); i++) {
                JSONObject jsonObject = getJSONObject(fields, i);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    addObservation(event, jsonObject);
                }
            }

            if (metadata != null) {
                Iterator<?> keys = metadata.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    JSONObject jsonObject = getJSONObject(metadata, key);
                    String value = getString(jsonObject, VALUE);
                    if (StringUtils.isNotBlank(value)) {
                        String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                        if (entityVal != null) {
                            if (entityVal.equals(CONCEPT)) {
                                addToJSONObject(jsonObject, KEY, key);
                                addObservation(event, jsonObject);
                            } else if (entityVal.equals(ENCOUNTER)) {
                                String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                                if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                    Date eDate = formatDate(value, false);
                                    if (eDate != null) {
                                        event.setEventDate(eDate);
                                    }
                                }
                            }
                        }
                    }
                }
            }


            if (event != null) {
                JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(event));

                //After saving, Unsync(remove) this event's details
                //List<JSONObject> jsonEvents = new ArrayList<>();
                ///jsonEvents.add(eventJson);

                //Update client to deceased
                JSONObject client = db.getClientByBaseEntityId(eventJson.getString(ClientProcessor.baseEntityIdJSONKey));
                client.put("deathdate", encounterDateTimeString);
                client.put("deathdateApprox", false);
                db.addorUpdateClient(entityId, client);

                //Add Death Event for child to flag for Server delete
                db.addEvent(event.getBaseEntityId(), eventJson);

                //Update Child Entity to include death date
                Event updateChildDetailsEvent = (Event) new Event()
                        .withBaseEntityId(entityId) //should be different for main and subform
                        .withEventDate(encounterDate)
                        .withEventType(JsonFormUtils.encounterType)
                        .withLocationId(locationId)
                        .withProviderId(providerId)
                        .withEntityType(PathConstants.EntityType.CHILD)
                        .withFormSubmissionId(generateRandomUUIDString())
                        .withDateCreated(new Date());
                JsonFormUtils.addMetaData(context, updateChildDetailsEvent, new Date());
                JSONObject eventJsonUpdateChildEvent = new JSONObject(JsonFormUtils.gson.toJson(updateChildDetailsEvent));

                db.addEvent(entityId, eventJsonUpdateChildEvent); //Add event to flag server update

                //Update REGISTER and FTS Tables
                String tableName = PathConstants.CHILD_TABLE_NAME;
                AllCommonsRepository allCommonsRepository = openSrpContext.allCommonsRepositoryobjects(tableName);
                if (allCommonsRepository != null) {
                    ContentValues values = new ContentValues();
                    values.put(PathConstants.EC_CHILD_TABLE.DOD, PathConstants.DEFAULT_DATE_STRING);
                    allCommonsRepository.update(tableName, values, entityId);
                    allCommonsRepository.updateSearch(entityId);

                    // Update coverage reports
                    CoverageDropoutIntentService.unregister(context, entityId);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Starts an instance of JsonFormActivity with the provided form details
     *
     * @param context                     The activity form is being launched from
     * @param jsonFormActivityRequestCode The request code to be used to launch {@link PathJsonFormActivity}
     * @param formName                    The name of the form to launch
     * @param entityId                    The unique entity id for the form (e.g child's ZEIR id)
     * @param currentLocationId           OpenMRS id for the current device's location
     * @throws Exception
     */
    public static void startForm(Activity context,
                                 int jsonFormActivityRequestCode,
                                 String formName, String uniqueId,
                                 String currentLocationId) throws Exception {
        Intent intent = new Intent(context, PathJsonFormActivity.class);

        String entityId = uniqueId;
        JSONObject form = FormUtils.getInstance(context).getFormJson(formName);
        if (form != null) {
            form.getJSONObject("metadata").put("encounter_location", currentLocationId);

            if ("child_enrollment".equals(formName)) {
                if (StringUtils.isBlank(entityId)) {
                    UniqueIdRepository uniqueIdRepo = VaccinatorApplication.getInstance().uniqueIdRepository();
                    entityId = uniqueIdRepo.getNextUniqueId() != null ? uniqueIdRepo.getNextUniqueId().getOpenmrsId() : "";
                    if (entityId.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.no_openmrs_id), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if (StringUtils.isNotBlank(entityId)) {
                    entityId = entityId.replace("-", "");
                }

                JsonFormUtils.addChildRegLocHierarchyQuestions(form);

                // Inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.getString(JsonFormUtils.KEY)
                            .equalsIgnoreCase(JsonFormUtils.ZEIR_ID)) {
                        jsonObject.remove(JsonFormUtils.VALUE);
                        jsonObject.put(JsonFormUtils.VALUE, entityId);
                    }
                }
            } else if ("out_of_catchment_service".equals(formName)) {
                if (StringUtils.isNotBlank(entityId)) {
                    entityId = entityId.replace("-", "");
                } else {
                    JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        if (fields.getJSONObject(i).getString("key").equals("ZEIR_ID")) {
                            fields.getJSONObject(i).put(READ_ONLY, false);
                            break;
                        }
                    }
                }

                JSONObject stepOne = form.getJSONObject(JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.getString(JsonFormUtils.KEY)
                            .equalsIgnoreCase(JsonFormUtils.ZEIR_ID)) {
                        jsonObject.remove(JsonFormUtils.VALUE);
                        jsonObject.put(JsonFormUtils.VALUE, entityId);
                    }
                }

                JsonFormUtils.addAddAvailableVaccines(context, form);
            } else {
                Log.w(TAG, "Unsupported form requested for launch " + formName);
            }

            intent.putExtra("json", form.toString());
            Log.d(TAG, "form is " + form.toString());
            context.startActivityForResult(intent, jsonFormActivityRequestCode);
        }
    }

    public static Event addMetaData(Context context, Event event, Date start) throws JSONException {
        Map<String, String> metaFields = new HashMap<>();
        metaFields.put("deviceid", "163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        metaFields.put("end", "163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        metaFields.put("start", "163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        Calendar calendar = Calendar.getInstance();

        String end = DATE_TIME_FORMAT.format(calendar.getTime());

        Obs obs = new Obs();
        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(DATE_TIME_FORMAT.format(start));
        obs.setFieldType("concept");
        obs.setFieldDataType("start");
        event.addObs(obs);


        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(end);
        obs.setFieldDataType("end");
        event.addObs(obs);

        TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String deviceId = mTelephonyManager.getSimSerialNumber();

        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(deviceId);
        obs.setFieldDataType("deviceid");
        event.addObs(obs);
        return event;
    }

    public static Event createMoveToCatchmentEvent(Context context, org.smartregister.domain.db.Event referenceEvent, String fromLocationId, String toProviderId, String toLocationId) {

        try {

            //Same location/provider, no need to move
            if (toLocationId.equals(fromLocationId) || referenceEvent.getProviderId().equals(toProviderId)) {
                return null;
            }

            final String FORM_SUBMISSION_FIELD = "formsubmissionField";
            final String DATA_TYPE = "text";

            Event event = (Event) new Event()
                    .withBaseEntityId(referenceEvent.getBaseEntityId())
                    .withEventDate(new Date())
                    .withEventType(MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT)
                    .withLocationId(fromLocationId)
                    .withProviderId(referenceEvent.getProviderId())
                    .withEntityType("child")
                    .withFormSubmissionId(JsonFormUtils.generateRandomUUIDString())
                    .withDateCreated(new Date());


            String formSubmissionField = "From_ProviderId";
            List<Object> vall = new ArrayList<>();
            vall.add(referenceEvent.getProviderId());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            formSubmissionField = "From_LocationId";
            vall = new ArrayList<>();
            vall.add(fromLocationId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            formSubmissionField = "To_ProviderId";
            vall = new ArrayList<>();
            vall.add(toProviderId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            formSubmissionField = "To_LocationId";
            vall = new ArrayList<>();
            vall.add(toLocationId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField,
                    "", vall, new ArrayList<>(), null, formSubmissionField));

            addMetaData(context, event, new Date());

            return event;

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////
    private static class SaveAdverseEventTask extends AsyncTask<Void, Void, Void> {
        private final String jsonString;
        private final String locationId;
        private final String baseEntityId;
        private final String providerId;

        private SaveAdverseEventTask(String jsonString, String locationId, String baseEntityId,
                                     String providerId) {
            this.jsonString = jsonString;
            this.locationId = locationId;
            this.baseEntityId = baseEntityId;
            this.providerId = providerId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                EventClientRepository db = VaccinatorApplication.getInstance().eventClientRepository();

                JSONObject jsonForm = new JSONObject(jsonString);

                JSONArray fields = fields(jsonForm);
                if (fields == null) {
                    return null;
                }

                String bindType = "child";
                String encounterDateField = getFieldValue(fields, "Date_Reaction");

                String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
                JSONObject metadata = getJSONObject(jsonForm, METADATA);

                Date encounterDate = new Date();
                if (StringUtils.isNotBlank(encounterDateField)) {
                    Date dateTime = formatDate(encounterDateField, false);
                    if (dateTime != null) {
                        encounterDate = dateTime;
                    }
                }

                Event event = (Event) new Event()
                        .withBaseEntityId(baseEntityId) //should be different for main and subform
                        .withEventDate(encounterDate)
                        .withEventType(encounterType)
                        .withLocationId(locationId)
                        .withProviderId(providerId)
                        .withEntityType(bindType)
                        .withFormSubmissionId(generateRandomUUIDString())
                        .withDateCreated(new Date());

                for (int i = 0; i < fields.length(); i++) {
                    JSONObject jsonObject = getJSONObject(fields, i);
                    String value = getString(jsonObject, VALUE);
                    if (StringUtils.isNotBlank(value)) {
                        addObservation(event, jsonObject);
                    }
                }

                if (metadata != null) {
                    Iterator<?> keys = metadata.keys();

                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        JSONObject jsonObject = getJSONObject(metadata, key);
                        String value = getString(jsonObject, VALUE);
                        if (StringUtils.isNotBlank(value)) {
                            String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                            if (entityVal != null) {
                                if (entityVal.equals(CONCEPT)) {
                                    addToJSONObject(jsonObject, KEY, key);
                                    addObservation(event, jsonObject);
                                } else if (entityVal.equals(ENCOUNTER)) {
                                    String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                                    if (entityIdVal.equals(
                                            FormEntityConstants.Encounter.encounter_date.name())) {
                                        Date eDate = formatDate(value, false);
                                        if (eDate != null) {
                                            event.setEventDate(eDate);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                if (event != null) {
                    JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(event));
                    db.addEvent(event.getBaseEntityId(), eventJson);

                }

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }
    }

    private static class SaveOutOfAreaServiceTask extends AsyncTask<Void, Void, Void> {

        private final Context context;
        private final org.smartregister.Context openSrpContext;
        private final String formString;

        private SaveOutOfAreaServiceTask(Context context, org.smartregister.Context openSrpContext, String formString) {
            this.context = context;
            this.openSrpContext = openSrpContext;
            this.formString = formString;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                JSONObject form = new JSONObject(formString);

                // Create a weight object if weight was recorded
                Weight weight = getWeightObject(openSrpContext, form);
                if (weight != null) {
                    WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
                    weightRepository.add(weight);
                }

                // Create a vaccine object for all recorded vaccines
                ArrayList<Vaccine> vaccines = getVaccineObjects(context, openSrpContext, form);
                if (vaccines.size() > 0) {
                    VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
                    for (Vaccine curVaccine : vaccines) {
                        Utils.addVaccine(vaccineRepository, curVaccine);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }
    }

    private static class SaveBirthRegistrationTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private org.smartregister.Context openSrpContext;
        private String jsonString;
        private String providerId;
        private String imageKey;
        private String bindType;
        private String subBindType;

        private SaveBirthRegistrationTask(Context context, org.smartregister.Context openSrpContext,
                                          String jsonString, String providerId, String imageKey, String bindType,
                                          String subBindType) {
            this.context = context;
            this.openSrpContext = openSrpContext;
            this.jsonString = jsonString;
            this.providerId = providerId;
            this.imageKey = imageKey;
            this.bindType = bindType;
            this.subBindType = subBindType;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (context instanceof ChildSmartRegisterActivity) {
                ChildSmartRegisterActivity childSmartRegisterActivity = ((ChildSmartRegisterActivity) context);
                childSmartRegisterActivity.refreshList(FetchStatus.fetched);
                childSmartRegisterActivity.hideProgressDialog();
            }
        }

        @Override
        protected void onPreExecute() {
            if (context instanceof ChildSmartRegisterActivity) {
                ((ChildSmartRegisterActivity) context).showProgressDialog();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

                JSONObject jsonForm = new JSONObject(jsonString);

                String entityId = getString(jsonForm, ENTITY_ID);
                if (StringUtils.isBlank(entityId)) {
                    entityId = generateRandomUUIDString();
                }

                JSONArray fields = fields(jsonForm);
                if (fields == null) {
                    return null;
                }

                String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
                JSONObject metadata = getJSONObject(jsonForm, METADATA);

                // Replace values for location questions with their corresponding location IDs
                for (int i = 0; i < fields.length(); i++) {
                    String key = fields.getJSONObject(i).getString("key");
                    if ("Home_Facility".equals(key)
                            || "Birth_Facility_Name".equals(key)
                            || "Residential_Area".equals(key)) {
                        try {
                            String rawValue = fields.getJSONObject(i).getString("value");
                            JSONArray valueArray = new JSONArray(rawValue);
                            if (valueArray.length() > 0) {
                                String lastLocationName = valueArray.getString(valueArray.length() - 1);
                                String lastLocationId = LocationUtils.getOpenMrsLocationId(lastLocationName);
                                fields.getJSONObject(i).put("value", lastLocationId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    } else if ("Mother_Guardian_Date_Birth".equals(key)) {
                        if (TextUtils.isEmpty(fields.getJSONObject(i).optString("value"))) {
                            fields.getJSONObject(i).put("value", MOTHER_DEFAULT_DOB);
                        }
                    }
                }

                JSONObject lookUpJSONObject = getJSONObject(metadata, "look_up");
                String lookUpEntityId = "";
                String lookUpBaseEntityId = "";
                if (lookUpJSONObject != null) {
                    lookUpEntityId = getString(lookUpJSONObject, "entity_id");
                    lookUpBaseEntityId = getString(lookUpJSONObject, "value");
                }
                Client baseClient = JsonFormUtils.createBaseClient(fields, entityId);
                Event baseEvent = JsonFormUtils.createEvent(openSrpContext, fields, metadata, entityId, encounterType, providerId, bindType);

                Client subformClient = null;
                Event subformEvent = null;
                if ("mother".equals(lookUpEntityId) && StringUtils.isNotBlank(lookUpBaseEntityId)) {
                    Client motherClient = new Client(lookUpBaseEntityId);
                    addRelationship(context, motherClient, baseClient);
                } else {
                    if (StringUtils.isNotBlank(subBindType)) {
                        subformClient = JsonFormUtils.createSubformClient(context, fields, baseClient, subBindType, null);
                    }

                    if (subformClient != null && baseEvent != null) {
                        JSONObject subBindTypeJson = getJSONObject(jsonForm, subBindType);
                        if (subBindTypeJson != null) {
                            String subBindTypeEncounter = getString(subBindTypeJson, ENCOUNTER_TYPE);
                            if (StringUtils.isNotBlank(subBindTypeEncounter)) {
                                subformEvent = JsonFormUtils.createSubFormEvent(null, metadata, baseEvent, subformClient.getBaseEntityId(), subBindTypeEncounter, providerId, subBindType);
                            }
                        }
                    }
                }

                if (baseClient != null) {
                    JSONObject clientJson = new JSONObject(gson.toJson(baseClient));
                    ecUpdater.addClient(baseClient.getBaseEntityId(), clientJson);
                }

                if (baseEvent != null) {
                    JSONObject eventJson = new JSONObject(gson.toJson(baseEvent));
                    ecUpdater.addEvent(baseEvent.getBaseEntityId(), eventJson);
                }

                if (subformClient != null) {
                    JSONObject clientJson = new JSONObject(gson.toJson(subformClient));
                    ecUpdater.addClient(subformClient.getBaseEntityId(), clientJson);
                }

                if (subformEvent != null) {
                    JSONObject eventJson = new JSONObject(gson.toJson(subformEvent));
                    ecUpdater.addEvent(subformEvent.getBaseEntityId(), eventJson);
                }

                String zeirId = baseClient.getIdentifier(ZEIR_ID);
                //mark zeir id as used
                VaccinatorApplication.getInstance().uniqueIdRepository().close(zeirId);

                String imageLocation = getFieldValue(fields, imageKey);
                saveImage(context, providerId, entityId, imageLocation);

                long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
                Date lastSyncDate = new Date(lastSyncTimeStamp);
                PathClientProcessorForJava.getInstance(context).processClient(ecUpdater.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));
                allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }
    }
}
