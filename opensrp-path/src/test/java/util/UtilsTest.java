package util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import shared.BaseUnitTest;

/**
 * Created by ndegwamartin on 06/10/2017.
 */

public class UtilsTest extends BaseUnitTest {

    @Test
    public void callingGetDateFromStringWithInvalidDateReturnsNull() {
        Date date = Utils.getDateFromString("wrong date-09-09", "yyyy-MM-dd");
        org.junit.Assert.assertNull(date);
    }

    @Test
    public void callingGetDateFromStringWithInvalidDatePatternThrowsIllegalArgException() {
        try {
            Utils.getDateFromString("2017-09-09", "invalid date format");
        } catch (Exception e) {
            org.junit.Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void callingGetDateFromStringWithCorrectParametersReturnsCorrectDateObject() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2017);
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 9);
        calendar.set(Calendar.HOUR, 9);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date date = Utils.getDateFromString("2017-09-09", "yyyy-MM-dd");
        org.junit.Assert.assertEquals(calendar.getTime().toString(), date.toString());


    }

}
