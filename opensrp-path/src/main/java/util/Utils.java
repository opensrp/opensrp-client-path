/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.path.domain.EditWrapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;


/**
 * @author Maimoona
 *         Class containing some static utility methods.
 */
public class Utils {

    public final static String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String TAG = Utils.class.getCanonicalName();

    private Utils() {
    }

    public static TableRow getDataRow(Context context, String label, String value, TableRow row) {
        TableRow tr = row;
        if (row == null) {
            tr = new TableRow(context);
            TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tr.setLayoutParams(trlp);
            tr.setPadding(10, 5, 10, 5);
        }

        TextView l = new TextView(context);
        l.setText(label + ": ");
        l.setPadding(20, 2, 20, 2);
        l.setTextColor(Color.BLACK);
        l.setTextSize(14);
        l.setBackgroundColor(Color.WHITE);
        tr.addView(l);

        TextView v = new TextView(context);
        v.setText(value);
        v.setPadding(20, 2, 20, 2);
        v.setTextColor(Color.BLACK);
        v.setTextSize(14);
        v.setBackgroundColor(Color.WHITE);
        tr.addView(v);

        return tr;
    }

    public static TableRow getDataRow(Context context, String label, String value, String field, TableRow row) {
        TableRow tr = row;
        if (row == null) {
            tr = new TableRow(context);
            TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tr.setLayoutParams(trlp);
            tr.setPadding(10, 5, 10, 5);
        }

        TextView l = new TextView(context);
        l.setText(label + ": ");
        l.setPadding(20, 2, 20, 2);
        l.setTextColor(Color.BLACK);
        l.setTextSize(14);
        l.setBackgroundColor(Color.WHITE);
        tr.addView(l);

        EditWrapper editWrapper = new EditWrapper();
        editWrapper.setCurrentValue(value);
        editWrapper.setField(field);

        EditText e = new EditText(context);
        e.setTag(editWrapper);
        e.setText(value);
        e.setPadding(20, 2, 20, 2);
        e.setTextColor(Color.BLACK);
        e.setTextSize(14);
        e.setBackgroundColor(Color.WHITE);
        e.setInputType(InputType.TYPE_NULL);
        tr.addView(e);

        return tr;
    }

    public static TableRow getDataRow(Context context) {
        TableRow tr = new TableRow(context);
        TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tr.setLayoutParams(trlp);
        tr.setPadding(0, 0, 0, 0);
        // tr.setBackgroundColor(Color.BLUE);
        return tr;
    }

    public static int addAsInts(boolean ignoreEmpty, String... vals) {
        int i = 0;
        for (String v : vals) {
            i += ignoreEmpty && StringUtils.isBlank(v) ? 0 : Integer.parseInt(v);
        }
        return i;
    }

    public static TableRow addToRow(Context context, String value, TableRow row) {
        return addToRow(context, value, row, false, 1);
    }

    public static TableRow addToRow(Context context, String value, TableRow row, int weight) {
        return addToRow(context, value, row, false, weight);
    }

    public static TableRow addToRow(Context context, String value, TableRow row, boolean compact) {
        return addToRow(context, value, row, compact, 1);
    }

    private static TableRow addToRow(Context context, String value, TableRow row, boolean compact, int weight) {
        return addToRow(context, Html.fromHtml(value), row, compact, weight);
    }

    private static TableRow addToRow(Context context, Spanned value, TableRow row, boolean compact, int weight) {
        TextView v = new TextView(context);
        v.setText(value);
        if (compact) {
            v.setPadding(15, 4, 1, 1);
        } else {
            v.setPadding(2, 15, 2, 15);
        }
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT, weight
        );
        params.setMargins(0, 0, 1, 0);
        v.setLayoutParams(params);
        v.setTextColor(Color.BLACK);
        v.setTextSize(14);
        v.setBackgroundColor(Color.WHITE);
        row.addView(v);

        return row;
    }

    public static void putAll(Map<String, String> map, Map<String, String> extend) {
        Collection<String> values = extend.values();
        while (true) {
            if (!(values.remove(null))) break;
        }
        map.putAll(extend);
    }

    public static void addVaccine(VaccineRepository vaccineRepository, Vaccine vaccine) {
        try {
            if (vaccineRepository == null || vaccine == null) {
                return;
            }

            // Add the vaccine
            vaccineRepository.add(vaccine);

            String name = vaccine.getName();
            if (StringUtils.isBlank(name)) {
                return;
            }

            // Update vaccines in the same group where either can be given
            // For example measles 1 / mr 1
            name = VaccineRepository.removeHyphen(name);
            String ftsVaccineName = null;

            if (VaccineRepo.Vaccine.measles1.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.mr1.display();
            } else if (VaccineRepo.Vaccine.mr1.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.measles1.display();
            } else if (VaccineRepo.Vaccine.measles2.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.mr2.display();
            } else if (VaccineRepo.Vaccine.mr2.display().equalsIgnoreCase(name)) {
                ftsVaccineName = VaccineRepo.Vaccine.measles2.display();
            }

            if (ftsVaccineName != null) {
                ftsVaccineName = VaccineRepository.addHyphen(ftsVaccineName.toLowerCase());
                Vaccine ftsVaccine = new Vaccine();
                ftsVaccine.setBaseEntityId(vaccine.getBaseEntityId());
                ftsVaccine.setName(ftsVaccineName);
                vaccineRepository.updateFtsSearch(ftsVaccine);
            }

        } catch (Exception e) {
            Log.e(Utils.class.getCanonicalName(), Log.getStackTraceString(e));
        }

    }

    public static Date getDateFromString(String date, String dateFormatPattern) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
            return dateFormat.parse(date);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public static int yearFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static Date getCohortEndDate(String vaccine, Date startDate) {

        if (StringUtils.isBlank(vaccine) || startDate == null) {
            return null;
        }

        Calendar endDateCalendar = Calendar.getInstance();
        endDateCalendar.setTime(startDate);

        final String opv0 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.opv0.display().toLowerCase());

        final String rota1 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.rota1.display().toLowerCase());
        final String rota2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.rota2.display().toLowerCase());

        final String measles2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.measles2.display().toLowerCase());
        final String mr2 = VaccineRepository.addHyphen(VaccineRepo.Vaccine.mr2.display().toLowerCase());

        if (vaccine.equals(opv0)) {
            endDateCalendar.add(Calendar.DATE, 13);
        } else if (vaccine.equals(rota1) || vaccine.equals(rota2)) {
            endDateCalendar.add(Calendar.MONTH, 8);
        } else if (vaccine.equals(measles2) || vaccine.equals(mr2)) {
            endDateCalendar.add(Calendar.YEAR, 2);
        } else {
            endDateCalendar.add(Calendar.YEAR, 1);
        }

        return endDateCalendar.getTime();
    }

    public static Date getCohortEndDate(VaccineRepo.Vaccine vaccine, Date startDate) {
        if (vaccine == null || startDate == null) {
            return null;
        }
        String vaccineName = VaccineRepository.addHyphen(vaccine.display().toLowerCase());
        return getCohortEndDate(vaccineName, startDate);

    }

    public static Date getLastDayOfMonth(Date month) {
        if (month == null) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(month);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return c.getTime();
    }

    public static boolean isSameMonthAndYear(Date date1, Date date2) {
        if (date1 != null && date2 != null) {
            DateTime dateTime1 = new DateTime(date1);
            DateTime dateTime2 = new DateTime(date2);

            return dateTime1.getMonthOfYear() == dateTime2.getMonthOfYear() && dateTime1.getYear() == dateTime2.getYear();
        }
        return false;
    }

    public static boolean isSameYear(Date date1, Date date2) {
        if (date1 != null && date2 != null) {
            DateTime dateTime1 = new DateTime(date1);
            DateTime dateTime2 = new DateTime(date2);

            return dateTime1.getYear() == dateTime2.getYear();
        }
        return false;
    }

    public static Date dobStringToDate(String dobString) {
        DateTime dateTime = dobStringToDateTime(dobString);
        if (dateTime != null) {
            return dateTime.toDate();
        }
        return null;
    }

    public static DateTime dobStringToDateTime(String dobString) {
        try {
            if (StringUtils.isBlank(dobString)) {
                return null;
            }
            return new DateTime(dobString);

        } catch (Exception e) {
            return null;
        }
    }

    public static int convertDpToPx(Context context, int dp) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return Math.round(px);
    }

    public static boolean isEmptyMap(Map map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmptyCollection(Collection collection) {
        return collection == null || collection.isEmpty();
    }
}
