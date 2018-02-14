package org.smartregister.path.sync;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.DateUtil;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.domain.db.Client;
import org.smartregister.domain.db.Event;
import org.smartregister.domain.db.EventClient;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.service.intent.WeightIntentService;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceSchedule;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.service.intent.RecurringIntentService;
import org.smartregister.immunization.service.intent.VaccineIntentService;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.path.service.intent.CoverageDropoutIntentService;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.sync.ClientProcessor;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.CloudantDataHandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.MoveToMyCatchmentUtils;
import util.PathConstants;
import util.Utils;

public class PathClientProcessorForJava extends ClientProcessorForJava {

    private static final String TAG = PathClientProcessorForJava.class.getName();
    private static PathClientProcessorForJava instance;

    private PathClientProcessorForJava(Context context) {
        super(context);
    }

    public static PathClientProcessorForJava getInstance(Context context) {
        if (instance == null) {
            instance = new PathClientProcessorForJava(context);
        }
        return instance;
    }

    @Override
    public void processClient(List<EventClient> eventClients) throws Exception {

        String clientClassificationStr = getFileContents("ec_client_classification.json");
        String clientVaccineStr = getFileContents("ec_client_vaccine.json");
        String clientWeightStr = getFileContents("ec_client_weight.json");
        String clientServiceStr = getFileContents("ec_client_service.json");

        if (!eventClients.isEmpty()) {
            List<Event> unsyncEvents = new ArrayList<>();
            for (EventClient eventClient : eventClients) {
                Event event = eventClient.getEvent();
                if (event == null) {
                    return;
                }

                String eventType = event.getEventType();
                if (eventType == null) {
                    continue;
                }

                if (eventType.equals(VaccineIntentService.EVENT_TYPE) || eventType.equals(VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT)) {
                    JSONObject clientVaccineClassificationJson = new JSONObject(clientVaccineStr);
                    if (isNullOrEmptyJSONObject(clientVaccineClassificationJson)) {
                        continue;
                    }

                    processVaccine(event, clientVaccineClassificationJson, eventType.equals(VaccineIntentService.EVENT_TYPE_OUT_OF_CATCHMENT));
                } else if (eventType.equals(WeightIntentService.EVENT_TYPE) || eventType.equals(WeightIntentService.EVENT_TYPE_OUT_OF_CATCHMENT)) {
                    JSONObject clientWeightClassificationJson = new JSONObject(clientWeightStr);
                    if (isNullOrEmptyJSONObject(clientWeightClassificationJson)) {
                        continue;
                    }

                    processWeight(event, clientWeightClassificationJson, eventType.equals(WeightIntentService.EVENT_TYPE_OUT_OF_CATCHMENT));
                } else if (eventType.equals(RecurringIntentService.EVENT_TYPE)) {
                    JSONObject clientServiceClassificationJson = new JSONObject(clientServiceStr);
                    if (isNullOrEmptyJSONObject(clientServiceClassificationJson)) {
                        continue;
                    }
                    processService(event, clientServiceClassificationJson);
                } else if (eventType.equals(MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT)) {
                    unsyncEvents.add(event);
                } else if (eventType.equals(PathConstants.EventType.DEATH)) {
                    unsyncEvents.add(event);
                } else if (eventType.equals(PathConstants.EventType.BITRH_REGISTRATION) || eventType.equals(PathConstants.EventType.UPDATE_BITRH_REGISTRATION) || eventType.equals(PathConstants.EventType.NEW_WOMAN_REGISTRATION)) {
                    JSONObject clientClassificationJson = new JSONObject(clientClassificationStr);
                    if (isNullOrEmptyJSONObject(clientClassificationJson)) {
                        continue;
                    }

                    Client client = eventClient.getClient();
                    //iterate through the events
                    if (client != null) {
                        processEvent(event, client, clientClassificationJson);

                    }
                }
            }

            // Unsync events that are should not be in this device
            if (!unsyncEvents.isEmpty()) {
                unSync(unsyncEvents);
            }
        }

    }

    private Boolean processVaccine(Event vaccine, JSONObject clientVaccineClassificationJson, boolean outOfCatchment) throws Exception {

        try {

            if (vaccine == null) {
                return false;
            }

            if (clientVaccineClassificationJson == null || clientVaccineClassificationJson.length() == 0) {
                return false;
            }

            ContentValues contentValues = processCaseModel(vaccine, clientVaccineClassificationJson);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(contentValues.getAsString(VaccineRepository.DATE));

                VaccineRepository vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
                Vaccine vaccineObj = new Vaccine();
                vaccineObj.setBaseEntityId(contentValues.getAsString(VaccineRepository.BASE_ENTITY_ID));
                vaccineObj.setName(contentValues.getAsString(VaccineRepository.NAME));
                if (contentValues.containsKey(VaccineRepository.CALCULATION)) {
                    vaccineObj.setCalculation(parseInt(contentValues.getAsString(VaccineRepository.CALCULATION)));
                }
                vaccineObj.setDate(date);
                vaccineObj.setAnmId(contentValues.getAsString(VaccineRepository.ANMID));
                vaccineObj.setLocationId(contentValues.getAsString(VaccineRepository.LOCATIONID));
                vaccineObj.setSyncStatus(VaccineRepository.TYPE_Synced);
                vaccineObj.setFormSubmissionId(vaccine.getFormSubmissionId());
                vaccineObj.setEventId(vaccine.getEventId());
                vaccineObj.setOutOfCatchment(outOfCatchment ? 1 : 0);

                Utils.addVaccine(vaccineRepository, vaccineObj);
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
            return null;
        }
    }

    private Boolean processWeight(Event weight, JSONObject clientWeightClassificationJson, boolean outOfCatchment) throws Exception {

        try {

            if (weight == null) {
                return false;
            }

            if (clientWeightClassificationJson == null || clientWeightClassificationJson.length() == 0) {
                return false;
            }

            ContentValues contentValues = processCaseModel(weight, clientWeightClassificationJson);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {
                Date date = DateUtil.getDateFromString(contentValues.getAsString(WeightRepository.DATE));
                if (date == null) {
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                        date = dateFormat.parse(contentValues.getAsString(WeightRepository.DATE));
                    } catch (Exception e) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                        date = dateFormat.parse(contentValues.getAsString(WeightRepository.DATE));
                    }
                }

                WeightRepository weightRepository = VaccinatorApplication.getInstance().weightRepository();
                Weight weightObj = new Weight();
                weightObj.setBaseEntityId(contentValues.getAsString(WeightRepository.BASE_ENTITY_ID));
                if (contentValues.containsKey(WeightRepository.KG)) {
                    weightObj.setKg(parseFloat(contentValues.getAsString(WeightRepository.KG)));
                }
                weightObj.setDate(date);
                weightObj.setAnmId(contentValues.getAsString(WeightRepository.ANMID));
                weightObj.setLocationId(contentValues.getAsString(WeightRepository.LOCATIONID));
                weightObj.setSyncStatus(WeightRepository.TYPE_Synced);
                weightObj.setFormSubmissionId(weight.getFormSubmissionId());
                weightObj.setEventId(weight.getEventId());
                weightObj.setOutOfCatchment(outOfCatchment ? 1 : 0);


                weightRepository.add(weightObj);
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
            return null;
        }
    }

    private Boolean processService(Event service, JSONObject clientVaccineClassificationJson) throws Exception {

        try {

            if (service == null) {
                return false;
            }

            if (clientVaccineClassificationJson == null || clientVaccineClassificationJson.length() == 0) {
                return false;
            }

            ContentValues contentValues = processCaseModel(service, clientVaccineClassificationJson);

            // save the values to db
            if (contentValues != null && contentValues.size() > 0) {

                String name = contentValues.getAsString(RecurringServiceTypeRepository.NAME);
                if (StringUtils.isNotBlank(name)) {
                    name = name.replaceAll("_", " ").replace("dose", "").trim();
                }

                Date date = null;
                String eventDateStr = contentValues.getAsString(RecurringServiceRecordRepository.DATE);
                if (StringUtils.isNotBlank(eventDateStr)) {
                    date = DateUtil.getDateFromString(eventDateStr);
                    if (date == null) {
                        try {
                            date = DateUtil.parseDate(eventDateStr);
                        } catch (ParseException e) {
                            Log.e(TAG, e.toString(), e);
                        }
                    }
                }

                String value = null;

                if (StringUtils.containsIgnoreCase(name, "ITN")) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String itnDateString = contentValues.getAsString("itn_date");
                    if (StringUtils.isNotBlank(itnDateString)) {
                        date = simpleDateFormat.parse(itnDateString);
                    }


                    value = RecurringIntentService.ITN_PROVIDED;
                    if (contentValues.getAsString("itn_has_net") != null) {
                        value = RecurringIntentService.CHILD_HAS_NET;
                    }

                }

                RecurringServiceTypeRepository recurringServiceTypeRepository = VaccinatorApplication.getInstance().recurringServiceTypeRepository();
                List<ServiceType> serviceTypeList = recurringServiceTypeRepository.searchByName(name);
                if (serviceTypeList == null || serviceTypeList.isEmpty()) {
                    return false;
                }

                if (date == null) {
                    return false;
                }

                RecurringServiceRecordRepository recurringServiceRecordRepository = VaccinatorApplication.getInstance().recurringServiceRecordRepository();
                ServiceRecord serviceObj = new ServiceRecord();
                serviceObj.setBaseEntityId(contentValues.getAsString(RecurringServiceRecordRepository.BASE_ENTITY_ID));
                serviceObj.setName(name);
                serviceObj.setDate(date);
                serviceObj.setAnmId(contentValues.getAsString(RecurringServiceRecordRepository.ANMID));
                serviceObj.setLocationId(contentValues.getAsString(RecurringServiceRecordRepository.LOCATIONID));
                serviceObj.setSyncStatus(RecurringServiceRecordRepository.TYPE_Synced);
                serviceObj.setFormSubmissionId(service.getFormSubmissionId());
                serviceObj.setEventId(service.getEventId()); //FIXME hard coded id
                serviceObj.setValue(value);
                serviceObj.setRecurringServiceId(serviceTypeList.get(0).getId());

                recurringServiceRecordRepository.add(serviceObj);
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
            return null;
        }
    }


    private ContentValues processCaseModel(Event event, JSONObject clientClassificationJson) {
        try {
            JSONArray columns = clientClassificationJson.getJSONArray("columns");

            ContentValues contentValues = new ContentValues();

            for (int i = 0; i < columns.length(); i++) {
                JSONObject colObject = columns.getJSONObject(i);
                String columnName = colObject.getString("column_name");
                JSONObject jsonMapping = colObject.getJSONObject("json_mapping");
                String dataSegment = null;
                String fieldName = jsonMapping.getString("field");
                String fieldValue = null;
                String responseKey = null;
                String valueField = jsonMapping.has("value_field") ? jsonMapping.getString("value_field") : null;
                if (fieldName != null && fieldName.contains(".")) {
                    String fieldNameArray[] = fieldName.split("\\.");
                    dataSegment = fieldNameArray[0];
                    fieldName = fieldNameArray[1];
                    fieldValue = jsonMapping.has("concept") ? jsonMapping.getString("concept") : (jsonMapping.has("formSubmissionField") ? jsonMapping.getString("formSubmissionField") : null);
                    if (fieldValue != null) {
                        responseKey = VALUES_KEY;
                    }
                }

                Object docSegment = null;

                if (StringUtils.isNotBlank(dataSegment)) {
                    //pick data from a specific section of the doc
                    docSegment = getValue(event, dataSegment);

                } else {
                    //else the use the main doc as the doc segment
                    docSegment = event;
                }

                if (docSegment == null) {
                    return contentValues;
                }

                if (docSegment instanceof List) {
                    List docSegmentList = (List) docSegment;

                    for (Object segment : docSegmentList) {
                        String columnValue = null;
                        if (fieldValue == null) {
                            //this means field_value and response_key are null so pick the value from the json object for the field_name
                            columnValue = getValueAsString(segment, fieldName);
                        } else {
                            //this means field_value and response_key are not null e.g when retrieving some value in the events obs section
                            String expectedFieldValue = getValueAsString(segment, fieldName);
                            //some events can only be differentiated by the event_type value eg pnc1,pnc2, anc1,anc2

                            if (expectedFieldValue.equalsIgnoreCase(fieldValue)) {
                                if (StringUtils.isNotBlank(valueField)) {
                                    columnValue = getValueAsString(segment, valueField);
                                }

                                if (columnValue == null) {
                                    Object valueList = getValue(segment, responseKey);
                                    if (valueList instanceof List) {
                                        List<String> values = getValues((List) valueList);
                                        if (!values.isEmpty()) {
                                            columnValue = values.get(0);
                                        }
                                    }
                                }
                            }
                        }
                        // after successfully retrieving the column name and value store it in Content value
                        if (columnValue != null) {
                            columnValue = getHumanReadableConceptResponse(columnValue, segment);
                            contentValues.put(columnName, columnValue);
                        }
                    }

                } else {
                    //e.g client attributes section
                    String columnValue = null;

                    columnValue = getValueAsString(docSegment, fieldName);
                    // after successfully retrieving the column name and value store it in Content value
                    if (columnValue != null) {
                        columnValue = getHumanReadableConceptResponse(columnValue, docSegment);
                        contentValues.put(columnName, columnValue);
                    }

                }


            }

            return contentValues;
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
        return null;
    }

    @Override
    public void updateFTSsearch(String tableName, String entityId, ContentValues contentValues) {
        if (PathConstants.MOTHER_TABLE_NAME.equals(tableName)) {
            return;
        }

        Log.i(TAG, "Starting updateFTSsearch table: " + tableName);

        AllCommonsRepository allCommonsRepository = org.smartregister.CoreLibrary.getInstance().context().
                allCommonsRepositoryobjects(tableName);

        if (allCommonsRepository != null) {
            allCommonsRepository.updateSearch(entityId);
        }

        if (contentValues != null && StringUtils.containsIgnoreCase(tableName, "child")) {
            String dob = contentValues.getAsString("dob");

            if (StringUtils.isBlank(dob)) {
                return;
            }

            DateTime birthDateTime = new DateTime(dob);
            VaccineSchedule.updateOfflineAlerts(entityId, birthDateTime, "child");
            ServiceSchedule.updateOfflineAlerts(entityId, birthDateTime);
        }

        Log.i(TAG, "Finished updateFTSsearch table: " + tableName);
    }

    @Override
    public String[] getOpenmrsGenIds() {
        return new String[]{"zeir_id"};
    }

    private boolean unSync(List<Event> events) {
        try {

            if (events == null || events.isEmpty()) {
                return false;
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);
            String registeredAnm = allSharedPreferences.fetchRegisteredANM();

            String clientClassificationStr = getFileContents("ec_client_fields.json");
            JSONObject clientClassificationJson = new JSONObject(clientClassificationStr);
            JSONArray bindObjects = clientClassificationJson.getJSONArray("bindobjects");

            DetailsRepository detailsRepository = VaccinatorApplication.getInstance().context().detailsRepository();
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(getContext());

            for (Event event : events) {
                unSync(ecUpdater, detailsRepository, bindObjects, event, registeredAnm);
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }

        return false;
    }

    private boolean unSync(ECSyncUpdater ecUpdater, DetailsRepository detailsRepository, JSONArray bindObjects, Event event, String registeredAnm) {
        try {
            String baseEntityId = event.getBaseEntityId();
            String providerId = event.getProviderId();

            if (providerId.equals(registeredAnm)) {
                boolean eventDeleted = ecUpdater.deleteEventsByBaseEntityId(baseEntityId);
                boolean clientDeleted = ecUpdater.deleteClient(baseEntityId);
                Log.d(getClass().getName(), "EVENT_DELETED: " + eventDeleted);
                Log.d(getClass().getName(), "ClIENT_DELETED: " + clientDeleted);

                boolean detailsDeleted = detailsRepository.deleteDetails(baseEntityId);
                Log.d(getClass().getName(), "DETAILS_DELETED: " + detailsDeleted);

                for (int i = 0; i < bindObjects.length(); i++) {

                    JSONObject bindObject = bindObjects.getJSONObject(i);
                    String tableName = bindObject.getString("name");

                    boolean caseDeleted = deleteCase(tableName, baseEntityId);
                    Log.d(getClass().getName(), "CASE_DELETED: " + caseDeleted);
                }

                // Update coverage reports
                CoverageDropoutIntentService.unregister(getContext(), baseEntityId);

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
        return false;
    }

    private Integer parseInt(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString(), e);
        }
        return null;
    }

    private Float parseFloat(String string) {
        try {
            return Float.valueOf(string);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString(), e);
        }
        return null;
    }
}
