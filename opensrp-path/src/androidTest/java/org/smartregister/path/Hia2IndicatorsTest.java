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
import java.util.Map.*;
import org.smartregister.path.service.HIA2Service;

/**
 * Created on 12/7/17.
 */

@RunWith(AndroidJUnit4.class)
public class Hia2IndicatorsTest {

    private Context mContext;
    private String mTemporalDatabaseFilePath;
    private String mTestDatabaseName = "test_database.db";
    private File mTemporalDatabaseFile;
    private SQLiteDatabase mSqLiteDatabase;
    private HIA2Service mHia2Service;
    private Map<String,Integer> mExpected;
    private Map<String,Object> mActual;
    private final char[] PASSWORD = "7bfb4bb3-2689-404c-a5d4-f5cbe1aea9c4".toCharArray();
    private final String DATE = "2017-11-03";


    //Copy test database to a temporal location on the device
    private void copyTestDatabaseTo(File fileHandler) throws IOException{

        InputStream input = mContext.getAssets().open(mTestDatabaseName);

        if(fileHandler !=null ) {

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

        if(fileHandler !=null && fileHandler.exists())
            return SQLiteDatabase.openDatabase(fileHandler.getPath(), PASSWORD,null,SQLiteDatabase.OPEN_READONLY);
        throw new IOException("Null pointer on file handler or file does not exit");
    }

    //Initialize expected values to be asserted
    private void initializeExpectedValues(){

        mExpected = new HashMap<>();

        //Clinic attendance HIA2 indicators
        mExpected.put(HIA2Service.CHN1_005,3);
        mExpected.put(HIA2Service.CHN1_010,7);
        mExpected.put(HIA2Service.CHN1_011,10);
        mExpected.put(HIA2Service.CHN1_015,1);
        mExpected.put(HIA2Service.CHN1_020,0);
        mExpected.put(HIA2Service.CHN1_021,1);
        mExpected.put(HIA2Service.CHN1_025,11);
        mExpected.put(HIA2Service.CHN1_030,0);

        //Growth monitoring HIA2 indicators
        mExpected.put(HIA2Service.CHN2_005,6);

    }

    //Initialize actual values to be asserted
    private void initializeActualValues(){
        mActual = mHia2Service.generateIndicators(mSqLiteDatabase,DATE);
    }

    //Assert a particular indicator's expected and actual value
    private void assertIndicator(final String indicator){

        Assert.assertEquals(indicator+" failed.",mExpected.get(indicator), mActual.get(indicator));
    }


    @Before
    //Initialize resources for the test
    public void initializeTest()throws IOException {

        mContext = InstrumentationRegistry.getTargetContext();

        mTemporalDatabaseFilePath = "/data/data/" + mContext.getPackageName() + "/databases/" + mTestDatabaseName;

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


    @After
    //Delete temporal database file after test completes
    public void cleanUpTestResources()throws IOException{

        if(mTemporalDatabaseFile != null && mTemporalDatabaseFile.exists())
            mTemporalDatabaseFile.delete();
        else throw new IOException("Null pointer on file handler or temporal database file does not exist");
    }
}