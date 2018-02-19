package org.smartregister.path.repository;

import android.text.TextUtils;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.repository.Repository;
import org.smartregister.stock.StockLibrary;
import org.smartregister.stock.domain.ActiveChildrenStats;
import org.smartregister.stock.repository.StockExternalRepository;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by samuelgithengi on 2/14/18.
 */

public class PathStockHelperRepository extends StockExternalRepository {

    public PathStockHelperRepository(Repository repository) {
        super(repository);
    }

    @Override
    public int getVaccinesUsedToday(Long date, String vaccineName) {
        int vaccineUsed = 0;
        DateTime thedate = new DateTime(date);
        DateTime startofday = thedate.withTimeAtStartOfDay();
        DateTime endofday = thedate.plusDays(1).withTimeAtStartOfDay();
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.rawQuery("Select count(*) from vaccines where date >= " + startofday.getMillis() + " and date < " + endofday.getMillis() + " and name like '%" + vaccineName + "%'", null);
        c.moveToFirst();
        if (c.getCount() > 0 && !StringUtils.isBlank(c.getString(0))) {
            vaccineUsed = Integer.parseInt(c.getString(0));
        }

        c.close();
        return vaccineUsed;
    }

    @Override
    public int getVaccinesUsedUntilDate(Long date, String vaccineName) {
        int vaccineUsed = 0;
        DateTime thedate = new DateTime(date);
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.rawQuery("Select count(*) from vaccines where date <= " + thedate.getMillis() + " and name like '%" + vaccineName + "%'", null);
        c.moveToFirst();
        if (c.getCount() > 0 && !StringUtils.isBlank(c.getString(0))) {
            vaccineUsed = Integer.parseInt(c.getString(0));
        }
        c.close();
        return vaccineUsed;
    }


    @Override
    public ActiveChildrenStats getActiveChildrenStat() {
        ActiveChildrenStats activeChildrenStats = new ActiveChildrenStats();
        PathRepository repo = (PathRepository) VaccinatorApplication.getInstance().getRepository();
        net.sqlcipher.database.SQLiteDatabase db = repo.getReadableDatabase();
        Cursor c = db.rawQuery("Select dob,client_reg_date from ec_child where inactive != 'true' and lost_to_follow_up != 'true' ", null);
        c.moveToFirst();
        boolean thismonth;

        while (!c.isAfterLast()) {
            thismonth = false;
            String dobString = c.getString(0);
            String createdString = c.getString(1);
            if (!TextUtils.isEmpty(dobString)) {
                DateTime dateTime = new DateTime(dobString);
                Date dob = dateTime.toDate();
                DateTime dateTime2 = new DateTime(createdString);
                DateTime now = new DateTime(System.currentTimeMillis());
                if (now.getMonthOfYear() == dateTime2.getMonthOfYear() && now.getYear() == dateTime2.getYear()) {
                    thismonth = true;
                }


                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    int months = (int) Math.floor((float) timeDiff /
                            TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));
                    int weeks = (int) Math.floor((float) (timeDiff - TimeUnit.MILLISECONDS.convert(
                            months * 30, TimeUnit.DAYS)) /
                            TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));

                    if (weeks >= 4) {
                        weeks = 0;
                        months++;
                    }
                    if (months < 12) {
                        if (thismonth) {
                            activeChildrenStats.setChildrenThisMonthZeroToEleven(activeChildrenStats.getChildrenThisMonthZeroToEleven() + 1);
                        } else {
                            activeChildrenStats.setChildrenLastMonthZeroToEleven(activeChildrenStats.getChildrenLastMonthZeroToEleven() + 1);
                        }
                    } else if (months > 11 && months < 60) {
                        if (thismonth) {
                            activeChildrenStats.setChildrenThisMonthtwelveTofiftyNine(activeChildrenStats.getChildrenThisMonthtwelveTofiftyNine() + 1);
                        } else {
                            activeChildrenStats.setChildrenLastMonthtwelveTofiftyNine(activeChildrenStats.getChildrenLastMonthtwelveTofiftyNine() + 1);
                        }
                    }
                }
            }
            c.moveToNext();
        }
        c.close();
        return activeChildrenStats;
    }

    @Override
    public int getVaccinesDueBasedOnSchedule(JSONObject vaccineobject) {
        int countofNextMonthVaccineDue = 0;
        try {
            Repository repo = StockLibrary.getInstance().getRepository();
            net.sqlcipher.database.SQLiteDatabase db = repo.getReadableDatabase();

            DateTime today = new DateTime(System.currentTimeMillis());

            //////////////////////next month///////////////////////////////////////////////////////////
            DateTime startofNextMonth = today.plusMonths(1).dayOfMonth().withMinimumValue();
//            DateTime EndofNextMonth = today.plusMonths(1).dayOfMonth().withMaximumValue();
            DecimalFormat mFormat = new DecimalFormat("00");
            String monthstring = mFormat.format(startofNextMonth.getMonthOfYear());
            mFormat = new DecimalFormat("0000");

            String yearstring = mFormat.format(startofNextMonth.getYear());
            String nextmonthdateString = yearstring + "-" + monthstring;

            Cursor c = db.rawQuery("Select count(*) from alerts where scheduleName = '" + vaccineobject.getString("name") + "' and startDate like '%" + nextmonthdateString + "%'", null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                countofNextMonthVaccineDue = Integer.parseInt(c.getString(0));
            }
            c.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
        return countofNextMonthVaccineDue;
    }
}
