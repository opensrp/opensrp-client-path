package org.smartregister.path.service;

import android.database.Cursor;
import android.support.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by coder on 5/19/17.
 */
public class HIA2Service {
    private final String TAG = HIA2Service.class.getCanonicalName();
    public static final DateFormat dfyymm = new SimpleDateFormat("yyyy-MM");
    public static final DateFormat dfyymmdd = new SimpleDateFormat("yyyy-MM-dd");
    public static final String REPORT_NAME = "HIA2";

    public static final String CHN1_005 = "CHN1-005";
    public static final String CHN1_010 = "CHN1-010";
    public static final String CHN1_011 = "CHN1-011";
    public static final String CHN1_015 = "CHN1-015";
    public static final String CHN1_020 = "CHN1-020";
    public static final String CHN1_021 = "CHN1-021";
    public static final String CHN1_025 = "CHN1-025";
    public static final String CHN1_030 = "CHN1-030";
    public static final String CHN2_005 = "CHN2-005";
    public static final String CHN2_010 = "CHN2-010";
    public static final String CHN2_015 = "CHN2-015";
    public static final String CHN2_020 = "CHN2-020";
    public static final String CHN2_025 = "CHN2-025";
    public static final String CHN2_030 = "CHN2-030";
    public static final String CHN2_035 = "CHN2-035";
    public static final String CHN2_040 = "CHN2-040";
    public static final String CHN2_041 = "CHN2-041";
    public static final String CHN2_045 = "CHN2-045";
    public static final String CHN2_050 = "CHN2-050";
    public static final String CHN2_051 = "CHN2-051";
    public static final String CHN2_055 = "CHN2-055";
    public static final String CHN2_060 = "CHN2-060";
    public static final String CHN2_061 = "CHN2-061";
    public static final String CHN2_065 = "CHN2-065";
    public static final String CHN2_070 = "CHN2-070";
    public static final String CHN2_075 = "CHN2-075";
    public static final String CHN2_080 = "CHN2-080";
    public static final String CHN3_005 = "CHN3-005";
    public static final String CHN3_005_O = "CHN3-005-O";
    public static final String CHN3_010 = "CHN3-010";
    public static final String CHN3_010_O = "CHN3-010-O";
    public static final String CHN3_015 = "CHN3-015";
    public static final String CHN3_015_O = "CHN3-015-O";
    public static final String CHN3_020 = "CHN3-020";
    public static final String CHN3_020_O = "CHN3-020-O";
    public static final String CHN3_025 = "CHN3-025";
    public static final String CHN3_025_O = "CHN3-025-O";
    public static final String CHN3_027 = "CHN3-027";
    public static final String CHN3_027_O = "CHN3-027-O";
    public static final String CHN3_030 = "CHN3-030";
    public static final String CHN3_030_O = "CHN3-030-O";
    public static final String CHN3_035 = "CHN3-035";
    public static final String CHN3_035_O = "CHN3-035-O";
    public static final String CHN3_040 = "CHN3-040";
    public static final String CHN3_040_O = "CHN3-040-O";
    public static final String CHN3_045 = "CHN3-045";
    public static final String CHN3_045_O = "CHN3-045-O";
    public static final String CHN3_050 = "CHN3-050";
    public static final String CHN3_050_O = "CHN3-050-O";
    public static final String CHN3_055 = "CHN3-055";
    public static final String CHN3_055_O = "CHN3-055-O";
    public static final String CHN3_060 = "CHN3-060";
    public static final String CHN3_060_O = "CHN3-060-O";
    public static final String CHN3_065 = "CHN3-065";
    public static final String CHN3_065_O = "CHN3-065-O";
    public static final String CHN3_070 = "CHN3-070";
    public static final String CHN3_070_O = "CHN3-070-O";
    public static final String CHN3_075 = "CHN3-075";
    public static final String CHN3_075_O = "CHN3-075-O";
    public static final String CHN3_080 = "CHN3-080";
    public static final String CHN3_080_O = "CHN3-080-O";
    public static final String CHN3_085 = "CHN3-085";
    public static final String CHN3_085_O = "CHN3-085-O";
    public static final String CHN3_090 = "CHN3-090";
    private final Map<String, Object> hia2Report = new HashMap<>();
    private SQLiteDatabase database;
    public static final String PREVIOUS_REPORT_DATES_QUERY = "select distinct strftime('%Y-%m-%d'," + EventClientRepository.event_column.eventDate + ") as eventDate, " + EventClientRepository.event_column.updatedAt + " from " + EventClientRepository.Table.event.name();
    public static final String HIA2_LAST_PROCESSED_DATE = "HIA2_LAST_PROCESSED_DATE";
    private String reportDate;

    //FIXME to uniquely identify out of areas change group by child.base_entity_id to group by zeir_id
    //FIXME add month as a variable to allow generation of previous months reports
    //FIXME add last generated date to make this process incremental, should this date be per indicator? just in case an indicator was skipped due to exceptions

    /**
     * Generate indicators populating them to hia2report map by executing various db queries.
     * Order of execution matters since indicators with total values depend on the values being added together existing in the hia2report map
     *
     * @param _database
     */
    public Map<String, Object> generateIndicators(final SQLiteDatabase _database, String day) {
        database = _database;
        reportDate = day;
        getCHN1005();
        getCHN1010();
        getCHN1011();
        getCHN1015();
        getCHN1020();
        getCHN1021();
        getCHN1025();
        getCHN1030();
        getCHN2005();
        getCHN2010();
        getCHN2015();
        getCHN2020();
        getCHN2025();
        getCHN2030();
        getCHN2035();
        getCHN2040();
        getCHN2041();
        getCHN2045();
        getCHN2050();
        getCHN2051();
        getCHN2055();
        getCHN2060();
        getCHN2061();
        getCHN2065();
        getCHN2070();
        getCHN2075();
        getCHN2080();
        getCHN3005();
        getCHN3005O();
        getCHN3010();
        getCHN3010O();
        getCHN3015();
        getCHN3015O();
        getCHN3020();
        getCHN3020O();
        getCHN3025();
        getCHN3025O();
        getCHN3027();
        getCHN3027O();
        getCHN3030();
        getCHN3030O();
        getCHN3035();
        getCHN3035O();
        getCHN3040();
        getCHN3040O();
        getCHN3045();
        getCHN3045O();
        getCHN3050();
        getCHN3050O();
        getCHN3055();
        getCHN3055O();
        getCHN3060();
        getCHN3060O();
        getCHN3065();
        getCHN3065O();
        getCHN3070();
        getCHN3070O();
        getCHN3075();
        getCHN3075O();
        getCHN3080();
        getCHN3080O();
        getCHN3085();
        getCHN3085O();
        getCHN3090();
        return hia2Report;
    }

    /**
     * Number of male children aged < 12 months who attended a clinic this month.
     */
    private void getCHN1005() {

        try {
            int count = clinicAttendance("Male", "<12");
            hia2Report.put(CHN1_005, count);
        } catch (Exception e) {
            Log.logError(TAG, e.getMessage());

        }


    }

    /**
     * @param gender
     * @param age    in months, can be e.g =12 <12, between 12 and 50
     * @return
     */
    private int clinicAttendance(String gender, String age) {
        int count = 0;
        try {
            String query = "select count(*) as count," + ageQuery() + " from ec_child child inner join " + EventClientRepository.Table.event.name() + " e on e." + EventClientRepository.event_column.baseEntityId.name() + "= child.base_entity_id" +
                    " where age " + age + " and  '" + reportDate + "'=strftime('%Y-%m-%d',e.eventDate) and child.gender='" + (gender.isEmpty() ? "Male" : gender + "'");
            count = executeQueryAndReturnCount(query);
        } catch (Exception e) {
            Log.logError(TAG, e.getMessage());
        }
        return count;
    }

    /**
     * Number of female children aged < 12 months who attended a clinic this month.
     */
    private void getCHN1010() {
        try {
            int count = clinicAttendance("Female", "<12");
            hia2Report.put(CHN1_010, count);
        } catch (Exception e) {
            Log.logError(TAG, e.getMessage());

        }

    }

    /**
     * Number of total children aged < 12 months who attended a clinic this month.	"[CHN1-005] + [CHN1-010]
     * [Non-editable in the form]"
     */
    private void getCHN1011() {

        int totalCount = (Integer) hia2Report.get(CHN1_005) + (Integer) hia2Report.get(CHN1_010);
        hia2Report.put(CHN1_011, totalCount);

    }

    /**
     * Number of male children aged 12 to 59 months who attended a clinic this month
     */
    private void getCHN1015() {
//        gender = gender == null || gender.isEmpty() ? "Male" : gender;
//        String query = "select count(*) as count," + ageQuery() + " from ec_child child inner join event e on e.baseEntityId=child.base_entity_id where  child.gender='" + gender + "' and strftime('%Y-%m-%d',e.eventDate)='"+reportDate+"'  and age between 12 and 59";
        try {
            int count = clinicAttendance("Male", "between 12 and 59");
            hia2Report.put(CHN1_015, count);
        } catch (Exception e) {
            Log.logError(TAG, e.getMessage());

        }

    }

    /**
     * Number of female children aged 12 to 59 months who attended a clinic this month
     */
    private void getCHN1020() {
        try {
            int count = clinicAttendance("Female", "between 12 and 59");
            hia2Report.put(CHN1_020, count);
        } catch (Exception e) {
            Log.logError(TAG, e.getMessage());

        }
    }

    /**
     * Number of Total children aged 12 to 59 months who attended clinic this month
     * [CHN1-015] + [CHN1-020]
     * [Non-editable in the form]
     */
    private void getCHN1021() {
        int totalCount = (Integer) hia2Report.get(CHN1_015) + (Integer) hia2Report.get(CHN1_020);
        hia2Report.put(CHN1_021, totalCount);

    }

    /**
     * Number of total children < 5 who attended a clinic this month
     * "[CHN1-011] + [CHN1-021]
     * [Non-editable in the form]"
     */
    private void getCHN1025() {
        int totalCount = (Integer) hia2Report.get(CHN1_011) + (Integer) hia2Report.get(CHN1_021);
        hia2Report.put(CHN1_025, totalCount);

    }

    /**
     * Number of total children who attended clinic and are not part of clinic's catchment area
     * COUNT Number of total children who attended clinic and are not part of clinic's catchment area (i.e., total number of out of catchment area form submissions that month)
     */
    private void getCHN1030() {
        try {
            String query = "select count(*) as count from ec_child child inner join event e on e.baseEntityId=child.base_entity_id where e.eventType like '%Out of Area Service%' and " + eventDateEqualsCurrentMonthQuery();
            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN1_030, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN1_030 " + e.getMessage());
        }
    }

    /**
     * Number of total children weighed aged 0-23 months who attended  clinic this month
     * using like for event since this total includes out of area service
     */
    private void getCHN2005() {
        try {
            String query = "select count(*) as count," + ageQuery() + " from ec_child child inner join event e on e.baseEntityId=child.base_entity_id " +
                    "where e.eventType like '%Growth Monitoring%' and age <23 and " + eventDateEqualsCurrentMonthQuery();
            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_005, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_005 " + e.getMessage());
        }
    }

    /**
     * Number of total children weighed aged 24-59 months who attended  clinic this month
     */
    private void getCHN2010() {
        try {
            String query = "select count(*) as count," + ageQuery() + " from ec_child child inner join event e on e.baseEntityId=child.base_entity_id " +
                    "where e.eventType like '%Growth Monitoring%' and age between 24 and 59 and " + eventDateEqualsCurrentMonthQuery();
            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_010, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_010 " + e.getMessage());
        }
    }

    /**
     * Number of total children weighed aged < 5 years who attended  clinic this month	"[CHN2-005] + [CHN2-010]
     * [Non-editable in the form]"
     */
    private void getCHN2015() {
        try {
            int totalCount = (Integer) hia2Report.get(CHN2_005) + (Integer) hia2Report.get(CHN2_010);
            hia2Report.put(CHN2_015, totalCount);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_015 " + e.getMessage());

        }
    }

    /**
     * Number of children age 0-23 months who where weighed for = 2 consecutive months who did not gain >100g of weight in those months
     * COUNT number of children 0-23 months [Date_Birth] with [weight current visit - weight previous visits < 100g] who had = 2 consecutive weight encounters at this clinic
     */
    private void getCHN2020() {

        try {

            int count = childBelow100gAgeBetween(0,23);
            hia2Report.put(CHN2_020, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_020 " + e.getMessage());
        }
    }

    /**
     * Number of children 24-59 months who where weighed for = 2 consecutive months who did not gain >100g of weight in those months
     * COUNT number of children 24-59 months [Date_Birth]  with [weight current visit - weight previous visits < 100g] who had = 2 consecutive weight encounters at this clinic
     */
    private void getCHN2025() {

        try {

            int count = childBelow100gAgeBetween(24,59);
            hia2Report.put(CHN2_025, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_025 " + e.getMessage());
        }
    }

    /**
     * Number of total children age < five years who where weighed for = 2 consecutive months who did not gain >100g of weight in those months
     * "[CHN2-020] + [CHN2-025]
     * [Non-editable in the form]"
     */
    private void getCHN2030() {
        try {
            int totalCount = (Integer) hia2Report.get(CHN2_020) + (Integer) hia2Report.get(CHN2_025);
            hia2Report.put(CHN2_030, totalCount);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_030 " + e.getMessage());

        }

    }

    /**
     * Number of total children age 0-23 months whose weight is between -2Z and -3Z scores
     */
    private void getCHN2035() {

        try {

            String query = "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id,"+ageQuery()+
                    "FROM weights w INNER JOIN ec_child child ON w.base_entity_id=child.base_entity_id WHERE '"+reportDate+"'=strftime('%Y-%m-%d',datetime(w.date/1000, 'unixepoch'))"+
                    " AND age <= 23 AND w.z_score BETWEEN -3 AND -2);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_035, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_035 " + e.getMessage());
        }

    }

    /**
     * Number of total children age 24-59 months whose weight is between -2Z and -3Z scores
     */
    private void getCHN2040() {

        try {

            String query =  "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id,"+ageQuery()+
                    "FROM weights w INNER JOIN ec_child child ON w.base_entity_id=child.base_entity_id WHERE '"+reportDate+"'=strftime('%Y-%m-%d',datetime(w.date/1000, 'unixepoch'))"+
                    " AND age BETWEEN 24 AND 59 AND w.z_score BETWEEN -3 AND -2);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_040, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_040 " + e.getMessage());
        }

    }

    /**
     * Number of total children age < 5 years whose weight is between -2Z and -3Z scores
     * "[CHN2-035] + [CHN2-040]
     * [Non-editable in the form]"
     */
    private void getCHN2041() {
        try {
            int totalCount = (Integer) hia2Report.get(CHN2_035) + (Integer) hia2Report.get(CHN2_040);
            hia2Report.put(CHN2_041, totalCount);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_041 " + e.getMessage());

        }

    }

    /**
     * Number of total children age 0-23 months whose weight is below -3Z scores
     */
    private void getCHN2045() {
        try {
            String query = "select count(*) as count," + ageQuery() +
                    "from weights w left join ec_child child on w.base_entity_id=child.base_entity_id" +
                    " where '" + reportDate + "'=strftime('%Y-%m-%d',datetime(w.date/1000, 'unixepoch')) and age<=23 and w.z_score< -3 group by child.base_entity_id;";
            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_045, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_045 " + e.getMessage());
        }
    }

    /**
     * Number of total children age 24-59 months whose weight is below -3Z scores
     */
    private void getCHN2050() {
        try {
            String query = "select count(*) as count," + ageQuery() +
                    "from weights w left join ec_child child on w.base_entity_id=child.base_entity_id" +
                    " where '" + reportDate + "'=strftime('%Y-%m-%d',datetime(w.date/1000, 'unixepoch')) and age between 24 and 59 and w.z_score < -3 group by child.base_entity_id;";
            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_050, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_050 " + e.getMessage());
        }
    }

    /**
     * Number of total children age < 5 years whose weight below -3Z scores
     * [CHN2-045] + [CHN2-050]
     * [Non-editable in the form]
     */
    private void getCHN2051() {
        try {
            int totalCount = (Integer) hia2Report.get(CHN2_045) + (Integer) hia2Report.get(CHN2_050);
            hia2Report.put(CHN2_051, totalCount);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_051 " + e.getMessage());

        }
    }

    /**
     * Number of total children age 0-23 months whose weight is above 2Z scores
     */
    private void getCHN2055() {

        try {

            String query = "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id,"+ ageQuery() +
                    " FROM weights w INNER JOIN ec_child child ON w.base_entity_id = child.base_entity_id WHERE '"+ reportDate +"'=strftime('%Y-%m-%d',datetime(w.date/1000, 'unixepoch')) "+
                    "AND age <= 23 AND w.z_score > 2);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_055, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_055 " + e.getMessage());
        }

    }

    /**
     * Number of total children age 24-59 months whose weight is above 2Z scores
     */
    private void getCHN2060() {

        try {

            String query = "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id,"+ ageQuery() +
                        " FROM weights w INNER JOIN ec_child child ON w.base_entity_id = child.base_entity_id WHERE '"+ reportDate +"'=strftime('%Y-%m-%d',datetime(w.date/1000, 'unixepoch')) "+
                        "AND age BETWEEN 24 AND 59 AND w.z_score > 2);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_060, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_060 " + e.getMessage());
        }

    }

    /**
     * Number of total children age < 5 years whose weight is above 2Z scores
     * [CHN-055] + [CHN-060]
     */
    private void getCHN2061() {
        try {
            int totalCount = (Integer) hia2Report.get(CHN2_055) + (Integer) hia2Report.get(CHN2_060);
            hia2Report.put(CHN2_061, totalCount);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_061 " + e.getMessage());

        }

    }

    /**
     * Number of children age 6-11 months who received vitamin A at this facility in this month
     */
    private void getCHN2065() {

        try {

            String query = "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id," +ageQuery()+
                    "FROM recurring_service_records rsr INNER JOIN recurring_service_types rst ON rsr.recurring_service_id = rst._id INNER JOIN ec_child child ON rsr.base_entity_id = child.base_entity_id" +
                    " WHERE rst.type = 'Vit_A' AND '"+reportDate+"' = strftime('%Y-%m-%d',datetime(rsr.date/1000, 'unixepoch')) AND age BETWEEN 6 AND 11);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_065, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_065 " + e.getMessage());
        }

    }

    /**
     * Vitamin A supplement to infant and children 12-59 months
     */
    private void getCHN2070() {

        try {

            String query = "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id," +ageQuery()+
                    "FROM recurring_service_records rsr INNER JOIN recurring_service_types rst ON rsr.recurring_service_id = rst._id INNER JOIN ec_child child ON rsr.base_entity_id = child.base_entity_id" +
                    " WHERE rst.type = 'Vit_A' AND '"+reportDate+"' = strftime('%Y-%m-%d',datetime(rsr.date/1000, 'unixepoch')) AND age BETWEEN 12 AND 59);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_070, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_070 " + e.getMessage());
        }
    }

    /**
     * Number of children age 12-59 months who received a deworming dose at this facility in this month
     */
    private void getCHN2075() {

        try {

            String query = "SELECT count(*) AS count FROM (SELECT DISTINCT child.base_entity_id," +ageQuery()+
                    "FROM recurring_service_records rsr INNER JOIN recurring_service_types rst ON rsr.recurring_service_id = rst._id INNER JOIN ec_child child ON rsr.base_entity_id = child.base_entity_id" +
                    " WHERE rst.type = 'Deworming' AND '"+reportDate+"' = strftime('%Y-%m-%d',datetime(rsr.date/1000, 'unixepoch')) AND age BETWEEN 12 AND 59);";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_075, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_075 " + e.getMessage());
        }
    }

    /**
     * Number of children who received insecticide treated nets at this facility in this month
     */
    private void getCHN2080() {

        try {

            String query = "SELECT count(*) AS count FROM recurring_service_records rsr INNER JOIN recurring_service_types rst ON rsr.recurring_service_id = rst._id INNER JOIN ec_child child ON rsr.base_entity_id = child.base_entity_id " +
                    "WHERE rst.type = 'ITN' AND '" + reportDate + "' = strftime('%Y-%m-%d',datetime(rsr.date/1000, 'unixepoch'));";

            int count = executeQueryAndReturnCount(query);
            hia2Report.put(CHN2_080, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN2_080 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received BCG dose at this facility in this month
     */
    private void getCHN3005() {
        try {
            int count = getVaccineCount("bcg", "<12", false);
            hia2Report.put(CHN3_005, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_005 " + e.getMessage());
        }

    }

    /**
     * Number of children < one year who received BCG dose at outreach conducted by this facility in this month
     */
    private void getCHN3005O() {
        try {
            int count = getVaccineCount("bcg", "<12", true);
            hia2Report.put(CHN3_005_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_005_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV0 dose at this facility in this month
     */
    private void getCHN3010() {
        try {
            int count = getVaccineCount("opv_0", "<12", false);
            hia2Report.put(CHN3_010, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_010 " + e.getMessage());
        }

    }

    /**
     * Number of children < one year who received OPV0 dose at outreach conducted by this facility in this month
     */
    private void getCHN3010O() {
        try {
            int count = getVaccineCount("opv_0", "<12", true);
            hia2Report.put(CHN3_010_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_010_O " + e.getMessage());
        }

    }

    /**
     * Number of children < one year who received OPV1 dose at this facility in this month
     */
    private void getCHN3015() {
        try {
            int count = getVaccineCount("opv_1", "<12", false);
            hia2Report.put(CHN3_015, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_015 " + e.getMessage());
        }

    }

    /**
     * Number of children < one year who received OPV1 dose at outreach conducted by this facility in this month
     */
    private void getCHN3015O() {
        try {
            int count = getVaccineCount("opv_1", "<12", true);
            hia2Report.put(CHN3_015_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_015_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV2 dose at this facility in this month
     */
    private void getCHN3020() {
        try {
            int count = getVaccineCount("opv_2", "<12", false);
            hia2Report.put(CHN3_020, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_020 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV2 dose at outreach conducted by this facility in this month
     */
    private void getCHN3020O() {
        try {
            int count = getVaccineCount("opv_2", "<12", true);
            hia2Report.put(CHN3_020_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_020_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV3 dose at this facility in this month
     */
    private void getCHN3025() {
        try {
            int count = getVaccineCount("opv_3", "<12", false);
            hia2Report.put(CHN3_025, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_025 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV3 dose at outreach conducted by this facility in this month
     */
    private void getCHN3025O() {
        try {
            int count = getVaccineCount("opv_3", "<12", true);
            hia2Report.put(CHN3_025_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_025 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received IPV dose at this facility in this month
     */
    private void getCHN3027() {
        try {
            int count = getVaccineCount("ipv", "<12", false);
            hia2Report.put(CHN3_027, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_027 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received IPV dose at outreach conducted by this facility in this month
     */
    private void getCHN3027O() {
        try {
            int count = getVaccineCount("ipv", "<12", true);
            hia2Report.put(CHN3_027_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_027_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV4 dose at this facility in this month
     */
    private void getCHN3030() {
        try {
            int count = getVaccineCount("opv_4", "<12", false);
            hia2Report.put(CHN3_030, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_030 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received OPV4 dose at outreach conducted by this facility in this month
     */
    private void getCHN3030O() {
        try {
            int count = getVaccineCount("opv_4", "<12", true);
            hia2Report.put(CHN3_030_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_030_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received DPT-Hib+HepB 1 dose at this facility in this month
     */
    private void getCHN3035() {
        try {
            int count = getVaccineCount("penta_1", "<12", false);
            hia2Report.put(CHN3_035, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_035 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received DPT-Hib+HepB 1 dose at outreach conducted by this facility in this month
     */
    private void getCHN3035O() {
        try {
            int count = getVaccineCount("penta_1", "<12", true);
            hia2Report.put(CHN3_035_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_035_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received DPT-Hib+HepB 2 dose at this facility in this month
     */
    private void getCHN3040() {
        try {
            int count = getVaccineCount("penta_2", "<12", false);
            hia2Report.put(CHN3_040, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_040 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received DPT-Hib+HepB 2 dose at outreach conducted by this facility in this month
     */
    private void getCHN3040O() {
        try {
            int count = getVaccineCount("penta_2", "<12", true);
            hia2Report.put(CHN3_040_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_040_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received DPT-Hib+HepB 3 dose at this facility in this month
     */
    private void getCHN3045() {
        try {
            int count = getVaccineCount("penta_3", "<12", false);
            hia2Report.put(CHN3_045, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_045 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received DPT-Hib+HepB 3 dose at outreach conducted by this facility in this month
     */
    private void getCHN3045O() {
        try {
            int count = getVaccineCount("penta_3", "<12", true);
            hia2Report.put(CHN3_045_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_045_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received PCV 1 dose at this facility in this month
     */
    private void getCHN3050() {
        try {
            int count = getVaccineCount("pcv_1", "<12", false);
            hia2Report.put(CHN3_050, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_050 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received PCV 1 dose at outreach conducted by this facility in this month
     */
    private void getCHN3050O() {
        try {
            int count = getVaccineCount("pcv_1", "<12", true);
            hia2Report.put(CHN3_050_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_050_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received PCV 2 dose at this facility in this month
     */
    private void getCHN3055() {
        try {
            int count = getVaccineCount("pcv_2", "<12", false);
            hia2Report.put(CHN3_055, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_055 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received PCV 2 dose at outreach conducted by this facility in this month
     */
    private void getCHN3055O() {
        try {
            int count = getVaccineCount("pcv_2", "<12", true);
            hia2Report.put(CHN3_055_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_055_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received PCV 3 dose at this facility in this month
     */
    private void getCHN3060() {
        try {
            int count = getVaccineCount("pcv_3", "<12", false);
            hia2Report.put(CHN3_060, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_060 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received PCV 3 dose at outreach conducted by this facility in this month
     */
    private void getCHN3060O() {
        try {
            int count = getVaccineCount("pcv_3", "<12", true);
            hia2Report.put(CHN3_060_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_060_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received RV 1  dose at this facility in this month
     */
    private void getCHN3065() {
        try {
            int count = getVaccineCount("rota_1", "<12", false);
            hia2Report.put(CHN3_065, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_065 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received RV 1 dose at outreach conducted by this facility in this month
     */
    private void getCHN3065O() {
        try {
            int count = getVaccineCount("rota_1", "<12", true);
            hia2Report.put(CHN3_065_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_065_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received RV 2  dose at this facility in this month
     */
    private void getCHN3070() {
        try {
            int count = getVaccineCount("rota_2", "<12", false);
            hia2Report.put(CHN3_070, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_070 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received RV 2 dose at outreach conducted by this facility in this month
     */
    private void getCHN3070O() {
        try {
            int count = getVaccineCount("rota_2", "<12", true);
            hia2Report.put(CHN3_070_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_070_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received Measles/ MR 1 dose at this facility in this month
     */
    private void getCHN3075() {
        try {
            int count = getVaccineCount("measles_1", "<12", false);
            hia2Report.put(CHN3_075, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_075 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who received Measles/ MR dose at outreach conducted by this facility in this month
     */
    private void getCHN3075O() {
        try {
            int count = getVaccineCount("measles_1", "<12", true);
            hia2Report.put(CHN3_075_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_075_O " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who have received the complete BCG, OPV series, DPT-Hib+Hep1 series, PCV series , RV series and measles/MR 1 within 10 days of each antigen being due at this facility
     */
    private void getCHN3080() {
        try {
            int count = countChildFullyImmunized("<12" ,false,false);
            hia2Report.put(CHN3_080, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_080 " + e.getMessage());
        }
    }

    /**
     * Number of children < one year who have received the complete BCG, OPV series, DPT-Hib+Hep1 series, PCV series , RV series and measles/MR 1 within 10 days of each antigen being due at outreach conducted by this facility
     */
    private void getCHN3080O() {
        try {
            int count = countChildFullyImmunized("<12" ,true,false);
            hia2Report.put(CHN3_080_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_080_O " + e.getMessage());
        }
    }

    /**
     * Number of children at 18 months  who received Measles/ MR 2 dose at this facility in this month
     */
    private void getCHN3085() {
        try {
            int count = getVaccineCount("measles_1", "=18", false);
            hia2Report.put(CHN3_085, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_085 " + e.getMessage());
        }
    }

    /**
     * Number of children  at 18 months who received Measles/ MR 2 dose at outreach conducted by this facility in this month
     */
    private void getCHN3085O() {
        try {
            int count = getVaccineCount("measles_1", "=18", true);
            hia2Report.put(CHN3_085_O, count);
        } catch (Exception e) {
            Log.logError(TAG, "CHN3_085_O " + e.getMessage());
        }
    }

    /**
     * Number of days during the month that vaccine storage fridge was not functioning
     * FIXME
     */
    private void getCHN3090() {

    }

    /**
     * @param vaccine
     * @param age       in months specified as e.g <12 or >12 or between 12 and 59
     * @param outOfArea
     * @return
     */
    private int getVaccineCount(String vaccine, String age, boolean outOfArea) {
        int count = 0;
        try {
            String vaccineCondition = vaccine.contains("measles") ? "(lower(v.name)='" + vaccine.toLowerCase() + "' or lower(v.name)='mr_1')" : "lower(v.name)='" + vaccine.toLowerCase() + "'";
            String query = "select count(*) as count, " + ageQuery() + " from vaccines v left join ec_child child on child.base_entity_id=v.base_entity_id " +
                    "where age " + age + " and  '" + reportDate + "'=strftime('%Y-%m-%d',datetime(v.date/1000, 'unixepoch')) and v.out_of_area=" + (outOfArea ? 1 : 0) + " and " + vaccineCondition;
            count = executeQueryAndReturnCount(query);
        } catch (Exception e) {
            Log.logError(TAG, vaccine.toUpperCase() + e.getMessage());
        }

        return count;

    }

    /**
     * @param age       in months specified as e.g <12 or >12 or between 12 and 59
     * @param outOfArea
     * @return
     */
    private int getVaccineCountWithH1A2Status(String age, boolean outOfArea) {
        int count = 0;
        try {
            String vaccineCondition = " lower(v.name) in (" +
                    "'bcg', " +
                    "'opv_0', 'opv_1', 'opv_2', 'opv_3', " +
                    "'ipv', " +
                    "'opv_4', " +
                    "'penta_1', 'penta_2', 'penta_3', " +
                    "'pcv_1', 'pcv_2', 'pcv_3', " +
                    "'rota_1', 'rota_2', " +
                    "'measles_1', 'mr_1'" +
                    ") and v." + VaccineRepository.HIA2_STATUS + " = '" + VaccineRepository.HIA2_Within + "' ";
            String query = "select count(*) as count, " + ageQuery() + " from vaccines v left join ec_child child on child.base_entity_id=v.base_entity_id " +
                    "where age " + age + " and  '" + reportDate + "'=strftime('%Y-%m-%d',datetime(v.date/1000, 'unixepoch')) and v.out_of_area=" + (outOfArea ? 1 : 0) + " and " + vaccineCondition;
            count = executeQueryAndReturnCount(query);
        } catch (Exception e) {
            Log.logError(TAG, "HIA2_Status" + e.getMessage());
        }

        return count;

    }

    private String ageQuery() {
        return " CAST ((julianday('now') - julianday(strftime('%Y-%m-%d',child.dob)))/(365/12) AS INTEGER)as age ";
    }

    private String eventDateEqualsCurrentMonthQuery() {
        return "strftime('%Y-%m-%d',e.eventDate) = '" + reportDate + "'";
    }

    private int executeQueryAndReturnCount(String query) {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = database.rawQuery(query, null);
            if (null != cursor) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = cursor.getInt(0);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.logError(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    //Converts a relation's column values into a string array
    private String[] getColumnValues(@NonNull final SQLiteDatabase database, @NonNull final String query, @NonNull final String columnName) {

        int index = 0;
        int cursorLength;
        String[] values = null;
        Cursor cursor = null;

        try {

            cursor = database.rawQuery(query,null);

            if(cursor != null) {

                cursorLength = cursor.getCount();
                values = new String[cursorLength];

                if(cursorLength > 0)
                    while (cursor.moveToNext())
                        values[index++] = cursor.getString(cursor.getColumnIndex(columnName));
            }
        }catch (Exception e) {
            Log.logError(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return values;
    }

    /**
     * Counts the number of children within a particular age range in months who where weighed for = 2 consecutive months who did not gain >100g of weight in those months
     */
    private int childBelow100gAgeBetween(final int startAge, final int endAge) {

        int count = 0;
        int weightsLength;
        String[] weights;
        double currentWeight;
        double previousWeight;
        double weightDifference;
        final double EXPECTED_WEIGHT_GAIN = 0.1;//100g

        String query = "SELECT base_entity_id FROM (SELECT base_entity_id,strftime('%Y-%m-%d',datetime(weights.date/1000, 'unixepoch'))AS wdate, count(*) AS num_weight_enc FROM "+
                "weights WHERE base_entity_id IN (select beid FROM (SELECT child.base_entity_id beid,strftime('%Y-%m-%d',datetime(weights.date/1000, 'unixepoch'))AS wdate,"+ageQuery()+
                "FROM ec_child child INNER JOIN weights ON child.base_entity_id = weights.base_entity_id WHERE wdate = '"+ reportDate +"' AND age BETWEEN "+startAge+" AND "+endAge+")) "+
                "AND wdate BETWEEN strftime('%Y-%m-%d','"+reportDate+"','-2 months') AND '"+ reportDate +"' GROUP BY base_entity_id HAVING num_weight_enc >= 2);";

        String[] beids = getColumnValues(database, query, "base_entity_id");

        if(beids != null && beids.length > 0)
            for (String beid : beids) {

                query = "SELECT kg, strftime('%Y-%m-%d',datetime(date/1000, 'unixepoch'))" +
                        "AS wdate FROM weights WHERE base_entity_id = '" +beid+ "' AND wdate <= '" + reportDate + "' ORDER BY wdate;";

                weights = getColumnValues(database, query, "kg");

                if(weights != null && weights.length >= 2){

                    weightsLength = weights.length;
                    currentWeight = Double.valueOf(weights[weightsLength - 1]);
                    previousWeight = Double.valueOf(weights[weightsLength - 2]);
                    weightDifference = currentWeight - previousWeight;

                    if (weightDifference < EXPECTED_WEIGHT_GAIN)
                        count++;
                }
            }

        return count;
    }

    private int countChildFullyImmunized(@NonNull final String age, final boolean outOfArea,final boolean checkMeasles2Mr2){

        int count = 0;
        String query = "SELECT child.base_entity_id AS base_entity_id,"+ageQuery()+" FROM vaccines vaccine INNER join ec_child child ON vaccine.base_entity_id = child.base_entity_id WHERE vaccine.name IN "+((checkMeasles2Mr2)? "('measles_2','mr_2')" : "('measles_1','mr_1')")+" AND '"+
                reportDate+"' = strftime('%Y-%m-%d',datetime(vaccine.date/1000,'unixepoch')) AND out_of_area = "+((outOfArea)? 1 : 0)+" AND age "+age+" AND hia2_status = 'Within';";

        String[] beids = getColumnValues(database,query,"base_entity_id");

        if(beids != null && beids.length > 0)
            for (String beid : beids)
                if(isChildFullyImmunized(beid,outOfArea,checkMeasles2Mr2))
                    count++;

        return count;
    }

    private boolean isChildFullyImmunized(String beid, boolean outOfArea,boolean checkMeasles2Mr2) {

        return (
                        (bcgDoseCompleted(beid,outOfArea) &&
                        opvSeriesCompleted(beid,outOfArea) &&
                        pentaSeriesCompleted(beid,outOfArea) &&
                        pcvSeriesCompleted(beid,outOfArea) &&
                        rotaSeriesCompleted(beid,outOfArea) &&
                        measles1Mr1DoseCompleted(beid,outOfArea)) &&
                        (!checkMeasles2Mr2 || measles2Mr2DoseCompleted(beid,outOfArea))
        );
    }

    private boolean vaccineSeriesCompleted(@NonNull final String beid,@NonNull final String vaccineNameWildCard,final boolean outOfArea,final int expectedDifference,@NonNull final String... requiredVaccines){

        Set<String> requiredVaccinesSet;
        Set<String> aquiredVaccinesSet;
        String query = "SELECT name FROM vaccines WHERE base_entity_id = '"+beid+"' AND name LIKE '"+vaccineNameWildCard+"' AND out_of_area = "+((outOfArea)? 1 : 0)+" AND hia2_status = 'Within';";
        String[] aquiredVaccines = getColumnValues(database,query,"name");

        if(aquiredVaccines != null && aquiredVaccines.length > 0) {

            requiredVaccinesSet = new HashSet<>(Arrays.asList(requiredVaccines));
            aquiredVaccinesSet = new HashSet<>(Arrays.asList(aquiredVaccines));

            requiredVaccinesSet.removeAll(aquiredVaccinesSet);

            return requiredVaccinesSet.size() == expectedDifference;
        }
        return false;
    }

    private boolean bcgDoseCompleted(String beid, boolean outOfArea){

        return vaccineSeriesCompleted(beid,"%bcg%",outOfArea,1,"bcg","bcg_2");
    }

    private boolean opvSeriesCompleted(String beid,boolean outOfArea){

        return vaccineSeriesCompleted(beid,"%opv%",outOfArea,1,"opv_0","opv_1","opv_2","opv_3","opv_4");
    }

    private boolean pentaSeriesCompleted(String beid,boolean outOfArea){

        return vaccineSeriesCompleted(beid,"%penta%",outOfArea,0,"penta_1","penta_2","penta_3");
    }

    private boolean pcvSeriesCompleted(String beid,boolean outOfArea){

        return vaccineSeriesCompleted(beid,"%pcv%",outOfArea,0,"pcv_1","pcv_2","pcv_3");
    }

    private boolean rotaSeriesCompleted(String beid,boolean outOfArea){

        return vaccineSeriesCompleted(beid,"%rota%",outOfArea,0,"rota_1","rota_2");
    }

    private boolean measles1Mr1DoseCompleted(String beid,boolean outOfArea){

        return vaccineSeriesCompleted(beid,"m%_1",outOfArea,1,"measles_1","mr_1");
    }

    private boolean measles2Mr2DoseCompleted(String beid,boolean outOfArea){

        return vaccineSeriesCompleted(beid,"m%_2",outOfArea,1,"measles_2","mr_2");
    }
}