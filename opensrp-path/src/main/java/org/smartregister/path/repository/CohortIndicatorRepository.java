package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.repository.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 11/01/18.
 */
public class CohortIndicatorRepository extends BaseRepository {
    private static final String TAG = CohortIndicatorRepository.class.getCanonicalName();
    public static final SimpleDateFormat DF_YYYYMM = new SimpleDateFormat("yyyy-MM");
    private static final String TABLE_NAME = "cohort_indicators";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_COHORT_ID = "cohort_id";
    private static final String COLUMN_VACCINE = "vaccine";
    private static final String COLUMN_END_DATE = "end_date";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_COHORT_ID, COLUMN_VACCINE, COLUMN_END_DATE, COLUMN_VALUE, COLUMN_CREATED_AT, COLUMN_UPDATED_AT

    };

    private static final String COHORT_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL, " +
            COLUMN_COHORT_ID + " INTEGER NOT NULL," +
            COLUMN_VACCINE + " VARCHAR NOT NULL," +
            COLUMN_END_DATE + " DATETIME NOT NULL," +
            COLUMN_VALUE + " INTEGER NOT NULL DEFAULT 0," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)";
    private static final String VACCINE_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_VACCINE + "_index ON " + TABLE_NAME + "(" + COLUMN_VACCINE + " COLLATE NOCASE);";

    public CohortIndicatorRepository(PathRepository pathRepository) {
        super(pathRepository);

    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(COHORT_SQL);
        database.execSQL(VACCINE_INDEX);
    }

    public void add(CohortIndicator cohortIndicator) {
        if (cohortIndicator == null) {
            return;
        }

        if (cohortIndicator.getUpdatedAt() == null) {
            cohortIndicator.setUpdatedAt(new Date());
        }

        SQLiteDatabase database = getWritableDatabase();
        if (cohortIndicator.getId() == null) {
            cohortIndicator.setCreatedAt(new Date());
            cohortIndicator.setId(database.insert(TABLE_NAME, null, createValuesFor(cohortIndicator)));
        } else {
            //mark the vaccine as unsynced for processing as an updated stock
            String idSelection = COLUMN_ID + " = ?";
            database.update(TABLE_NAME, createValuesFor(cohortIndicator), idSelection, new String[]{cohortIndicator.getId().toString()});
        }
    }

    public CohortIndicator findByVaccine(String vaccine) {
        if (StringUtils.isBlank(vaccine)) {
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_VACCINE + " = ? COLLATE NOCASE ", new String[]{vaccine}, null, null, null, null);
            List<CohortIndicator> cohortIndicators = readAllDataElements(cursor);
            if (!cohortIndicators.isEmpty()) {
                return cohortIndicators.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }


    public CohortIndicator findById(long id) {
        Cursor cursor = null;

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null, null);
            List<CohortIndicator> cohortIndicators = readAllDataElements(cursor);
            if (!cohortIndicators.isEmpty()) {
                return cohortIndicators.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private List<CohortIndicator> readAllDataElements(Cursor cursor) {
        List<CohortIndicator> cohortIndicators = new ArrayList<>();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    CohortIndicator cohortIndicator = new CohortIndicator();
                    cohortIndicator.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                    cohortIndicator.setCohortId(cursor.getLong(cursor.getColumnIndex(COLUMN_COHORT_ID)));
                    cohortIndicator.setVaccine(cursor.getString(cursor.getColumnIndex(COLUMN_VACCINE)));
                    cohortIndicator.setEndDate(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_END_DATE))));
                    cohortIndicator.setValue(cursor.getLong(cursor.getColumnIndex(COLUMN_VALUE)));
                    cohortIndicator.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
                    cohortIndicator.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT))));
                    cohortIndicators.add(cohortIndicator);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return cohortIndicators;

    }

    private ContentValues createValuesFor(CohortIndicator cohortIndicator) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, cohortIndicator.getId());
        values.put(COLUMN_COHORT_ID, cohortIndicator.getCohortId());
        values.put(COLUMN_VACCINE, cohortIndicator.getVaccine());
        values.put(COLUMN_END_DATE, cohortIndicator.getEndDate() != null ? cohortIndicator.getEndDate().getTime() : null);
        values.put(COLUMN_VALUE, cohortIndicator.getValue());
        values.put(COLUMN_CREATED_AT, cohortIndicator.getCreatedAt() != null ? cohortIndicator.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, cohortIndicator.getUpdatedAt() != null ? cohortIndicator.getUpdatedAt().getTime() : null);
        return values;
    }


}
