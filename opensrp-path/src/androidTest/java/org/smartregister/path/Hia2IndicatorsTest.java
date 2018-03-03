package org.smartregister.path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.InstrumentationRegistry;
import net.sqlcipher.database.SQLiteDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.smartregister.path.service.HIA2Service;

/**
 * Created on 12/7/17.
 */

@RunWith(AndroidJUnit4.class)
public class Hia2IndicatorsTest {

    private Context mContext;
    private String mTemporalDatabaseFilePath;
    private File mTemporalDatabaseFile;
    private SQLiteDatabase mSqLiteDatabase;
    private HIA2Service mHia2Service;
    private Map<String,Integer> mExpected;
    private Map<String,Object> mActual;
    private final String TEST_DATABASE_NAME = "test.db";
    private final char[] PASSWORD = "7bfb4bb3-2689-404c-a5d4-f5cbe1aea9c4".toCharArray();
    private final String DATE = "2017-11-03";


    //Copy test database to a temporal location on the device
    private void copyTestDatabaseTo(File fileHandler) throws IOException{



        InputStream input = mContext.getAssets().open(TEST_DATABASE_NAME);

        if(fileHandler != null ) {

            if(fileHandler.exists()) fileHandler.delete();

            String outFileName = fileHandler.getPath();
            OutputStream output = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
            output.close();
            input.close();
        }
        else throw new IOException("Null pointer on file handler");
    }

    //Open temporal database file
    private SQLiteDatabase openDatabase(File fileHandler) throws IOException{

        if(fileHandler != null && fileHandler.exists())
            return SQLiteDatabase.openDatabase(fileHandler.getPath(), PASSWORD,null,SQLiteDatabase.OPEN_READONLY);
        throw new IOException("Null pointer on file handler or file does not exit");
    }

    //Initialize expected values to be asserted
    private void initializeExpectedValues(){

        mExpected = new HashMap<>();

        //Clinic attendance HIA2 indicators
        mExpected.put(HIA2Service.CHN1_005, 3);
        mExpected.put(HIA2Service.CHN1_010, 2);
        mExpected.put(HIA2Service.CHN1_011, 5);
        mExpected.put(HIA2Service.CHN1_015, 3);
        mExpected.put(HIA2Service.CHN1_020, 5);
        mExpected.put(HIA2Service.CHN1_021, 8);
        mExpected.put(HIA2Service.CHN1_025, 13);
        mExpected.put(HIA2Service.CHN1_030, 2);

        //Growth monitoring HIA2 indicators
        mExpected.put(HIA2Service.CHN2_005, 5);
        mExpected.put(HIA2Service.CHN2_010, 1);
        mExpected.put(HIA2Service.CHN2_015, 6);
        mExpected.put(HIA2Service.CHN2_020, 2);
        mExpected.put(HIA2Service.CHN2_025, 1);
        mExpected.put(HIA2Service.CHN2_030, 3);
        mExpected.put(HIA2Service.CHN2_035, 3);
        mExpected.put(HIA2Service.CHN2_040, 1);
        mExpected.put(HIA2Service.CHN2_041, 4);
        mExpected.put(HIA2Service.CHN2_045, 1);
        mExpected.put(HIA2Service.CHN2_050, 1);
        mExpected.put(HIA2Service.CHN2_051, 2);
        mExpected.put(HIA2Service.CHN2_055, 2);
        mExpected.put(HIA2Service.CHN2_060, 2);
        mExpected.put(HIA2Service.CHN2_061, 4);

        //Vitamin A, Deworming and ITN HIA2 indicators
        mExpected.put(HIA2Service.CHN2_065, 4);
        mExpected.put(HIA2Service.CHN2_070, 1);
        mExpected.put(HIA2Service.CHN2_075, 2);
        mExpected.put(HIA2Service.CHN2_080, 6);

        //Immunisation HIA2 indicators
        mExpected.put(HIA2Service.CHN3_005, 1);
        mExpected.put(HIA2Service.CHN3_010, 2);
        mExpected.put(HIA2Service.CHN3_015, 2);
        mExpected.put(HIA2Service.CHN3_020, 1);
        mExpected.put(HIA2Service.CHN3_025, 2);
        mExpected.put(HIA2Service.CHN3_027, 2);
        mExpected.put(HIA2Service.CHN3_030, 2);
        mExpected.put(HIA2Service.CHN3_035, 2);
        mExpected.put(HIA2Service.CHN3_040, 2);
        mExpected.put(HIA2Service.CHN3_045, 2);
        mExpected.put(HIA2Service.CHN3_050, 2);
        mExpected.put(HIA2Service.CHN3_055, 2);
        mExpected.put(HIA2Service.CHN3_060, 2);
        mExpected.put(HIA2Service.CHN3_065, 2);
        mExpected.put(HIA2Service.CHN3_070, 2);
        mExpected.put(HIA2Service.CHN3_075, 1);
        mExpected.put(HIA2Service.CHN3_080, 1);
        mExpected.put(HIA2Service.CHN3_085, 2);
        mExpected.put(HIA2Service.CHN3_005_O, 3);
        mExpected.put(HIA2Service.CHN3_010_O, 1);
        mExpected.put(HIA2Service.CHN3_015_O, 1);
        mExpected.put(HIA2Service.CHN3_020_O, 2);
        mExpected.put(HIA2Service.CHN3_025_O, 1);
        mExpected.put(HIA2Service.CHN3_027_O, 1);
        mExpected.put(HIA2Service.CHN3_030_O, 1);
        mExpected.put(HIA2Service.CHN3_035_O, 1);
        mExpected.put(HIA2Service.CHN3_040_O, 1);
        mExpected.put(HIA2Service.CHN3_045_O, 1);
        mExpected.put(HIA2Service.CHN3_050_O, 1);
        mExpected.put(HIA2Service.CHN3_055_O, 1);
        mExpected.put(HIA2Service.CHN3_060_O, 1);
        mExpected.put(HIA2Service.CHN3_065_O, 1);
        mExpected.put(HIA2Service.CHN3_070_O, 1);
        mExpected.put(HIA2Service.CHN3_075_O, 2);
        mExpected.put(HIA2Service.CHN3_080_O, 1);
        mExpected.put(HIA2Service.CHN3_085_O, 1);
    }

    //Initialize actual values to be asserted
    private void initializeActualValues(){

        mActual = mHia2Service.generateIndicators(mSqLiteDatabase,DATE);
    }

    //Assert a particular indicator's expected and actual value
    private void assertIndicator(final String indicator){

        Assert.assertEquals(indicator + " failed.",mExpected.get(indicator), mActual.get(indicator));
    }


    @Before
    //Initialize resources for the test
    public void initializeTest()throws IOException {

        mContext = InstrumentationRegistry.getTargetContext();

        mTemporalDatabaseFilePath = "/data/data/" + mContext.getPackageName() + "/databases/" + TEST_DATABASE_NAME;

        mTemporalDatabaseFile = new File(mTemporalDatabaseFilePath);

        copyTestDatabaseTo(mTemporalDatabaseFile);

        mSqLiteDatabase = openDatabase(mTemporalDatabaseFile);

        mHia2Service = new HIA2Service();

        initializeExpectedValues();

        initializeActualValues();
    }


    //Testing indicators individually for a HIA2 daily report generated on $DATE
    @Test
    public void ch1005(){assertIndicator(HIA2Service.CHN1_005);}

    @Test
    public void ch1010(){assertIndicator(HIA2Service.CHN1_010);}

    @Test
    public void ch1011(){assertIndicator(HIA2Service.CHN1_011);}

    @Test
    public void ch1015(){assertIndicator(HIA2Service.CHN1_015);}

    @Test
    public void ch1020(){assertIndicator(HIA2Service.CHN1_020);}

    @Test
    public void ch1021(){assertIndicator(HIA2Service.CHN1_021);}

    @Test
    public void ch1025(){assertIndicator(HIA2Service.CHN1_025);}

    @Test
    public void ch1030(){assertIndicator(HIA2Service.CHN1_030);}

    @Test
    public void ch2005(){assertIndicator(HIA2Service.CHN2_005);}

    @Test
    public void ch2010(){assertIndicator(HIA2Service.CHN2_010);}

    @Test
    public void ch2015(){assertIndicator(HIA2Service.CHN2_015);}

    @Test
    public void ch2020(){assertIndicator(HIA2Service.CHN2_020);}

    @Test
    public void ch2025(){assertIndicator(HIA2Service.CHN2_025);}

    @Test
    public void ch2030(){assertIndicator(HIA2Service.CHN2_030);}

    @Test
    public void ch2035(){assertIndicator(HIA2Service.CHN2_035);}

    @Test
    public void ch2040(){assertIndicator(HIA2Service.CHN2_040);}

    @Test
    public void ch2041(){assertIndicator(HIA2Service.CHN2_041);}

    @Test
    public void ch2045(){assertIndicator(HIA2Service.CHN2_045);}

    @Test
    public void ch2050(){assertIndicator(HIA2Service.CHN2_050);}

    @Test
    public void ch2051(){assertIndicator(HIA2Service.CHN2_051);}

    @Test
    public void ch2055(){assertIndicator(HIA2Service.CHN2_055);}

    @Test
    public void ch2060(){assertIndicator(HIA2Service.CHN2_060);}

    @Test
    public void ch2061(){assertIndicator(HIA2Service.CHN2_061);}

    @Test
    public void ch2065(){assertIndicator(HIA2Service.CHN2_065);}

    @Test
    public void ch2070(){assertIndicator(HIA2Service.CHN2_070);}

    @Test
    public void ch2075(){assertIndicator(HIA2Service.CHN2_075);}

    @Test
    public void ch2080(){assertIndicator(HIA2Service.CHN2_080);}

    @Test
    public void ch3005(){assertIndicator(HIA2Service.CHN3_005);}

    @Test
    public void ch3005o(){assertIndicator(HIA2Service.CHN3_005_O);}

    @Test
    public void ch3010(){assertIndicator(HIA2Service.CHN3_010);}

    @Test
    public void ch3010o(){assertIndicator(HIA2Service.CHN3_010_O);}

    @Test
    public void ch3015(){assertIndicator(HIA2Service.CHN3_015);}

    @Test
    public void ch3015o(){assertIndicator(HIA2Service.CHN3_015_O);}

    @Test
    public void ch3020(){assertIndicator(HIA2Service.CHN3_020);}

    @Test
    public void ch3020o(){assertIndicator(HIA2Service.CHN3_020_O);}

    @Test
    public void ch3025(){assertIndicator(HIA2Service.CHN3_025);}

    @Test
    public void ch3025o(){assertIndicator(HIA2Service.CHN3_025_O);}

    @Test
    public void ch3027(){assertIndicator(HIA2Service.CHN3_027);}

    @Test
    public void ch3027o(){assertIndicator(HIA2Service.CHN3_027_O);}

    @Test
    public void ch3030(){assertIndicator(HIA2Service.CHN3_030);}

    @Test
    public void ch3030o(){assertIndicator(HIA2Service.CHN3_030_O);}

    @Test
    public void ch3035(){assertIndicator(HIA2Service.CHN3_035);}

    @Test
    public void ch3035o(){assertIndicator(HIA2Service.CHN3_035_O);}

    @Test
    public void ch3040(){assertIndicator(HIA2Service.CHN3_040);}

    @Test
    public void ch3040o(){assertIndicator(HIA2Service.CHN3_040_O);}

    @Test
    public void ch3045(){assertIndicator(HIA2Service.CHN3_045);}

    @Test
    public void ch3045o(){assertIndicator(HIA2Service.CHN3_045_O);}

    @Test
    public void ch3050(){assertIndicator(HIA2Service.CHN3_050);}

    @Test
    public void ch3050o(){assertIndicator(HIA2Service.CHN3_050_O);}

    @Test
    public void ch3055(){assertIndicator(HIA2Service.CHN3_055);}

    @Test
    public void ch3055o(){assertIndicator(HIA2Service.CHN3_055_O);}

    @Test
    public void ch3060(){assertIndicator(HIA2Service.CHN3_060);}

    @Test
    public void ch3060o(){assertIndicator(HIA2Service.CHN3_060_O);}

    @Test
    public void ch3065(){assertIndicator(HIA2Service.CHN3_065);}

    @Test
    public void ch3065o(){assertIndicator(HIA2Service.CHN3_065_O);}

    @Test
    public void ch3070(){assertIndicator(HIA2Service.CHN3_070);}

    @Test
    public void ch3070o(){assertIndicator(HIA2Service.CHN3_070_O);}

    @Test
    public void ch3075(){assertIndicator(HIA2Service.CHN3_075);}

    @Test
    public void ch3075o(){assertIndicator(HIA2Service.CHN3_075_O);}

    @Test
    public void ch3080(){assertIndicator(HIA2Service.CHN3_080);}

    @Test
    public void ch3080o(){assertIndicator(HIA2Service.CHN3_080_O);}

    @Test
    public void ch3085(){assertIndicator(HIA2Service.CHN3_085);}

    @Test
    public void ch3085o(){assertIndicator(HIA2Service.CHN3_085_O);}


    @After
    //Close database connection and delete temporal database file after test completes
    public void cleanUpTestResources()throws IOException{

        if(mSqLiteDatabase != null)
            mSqLiteDatabase.close();
        else throw new IOException("Can not call close() on a null SqLiteDatabase reference.");

        if(mTemporalDatabaseFile != null && mTemporalDatabaseFile.exists())
            mTemporalDatabaseFile.delete();
        else throw new IOException("Null pointer on file handler or temporal database file does not exist.");
    }
}