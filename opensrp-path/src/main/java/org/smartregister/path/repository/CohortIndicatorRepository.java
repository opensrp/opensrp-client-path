package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.domain.CohortIndicator;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 11/01/18.
 */
public class CohortIndicatorRepository extends BaseRepository {
    private static final String TAG = CohortIndicatorRepository.class.getCanonicalName();
    private static final String TABLE_NAME = "cohort_indicators";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_COHORT_ID = "cohort_id";
    private static final String COLUMN_VACCINE = "vaccine";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_COHORT_ID, COLUMN_VACCINE, COLUMN_VALUE, COLUMN_CREATED_AT, COLUMN_UPDATED_AT
    };

    private static final String COHORT_INDICATOR_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_COHORT_ID + " INTEGER NOT NULL," +
            COLUMN_VACCINE + " VARCHAR NOT NULL," +
            COLUMN_VALUE + " INTEGER NOT NULL DEFAULT 0," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, " +
            " UNIQUE(" + COLUMN_VACCINE + ", " + COLUMN_COHORT_ID + ") ON CONFLICT IGNORE )";
    private static final String COHORT_INDICATOR_COHORT_ID_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_COHORT_ID + "_index ON " + TABLE_NAME + "(" + COLUMN_COHORT_ID + ");";

    public CohortIndicatorRepository(PathRepository pathRepository) {
        super(pathRepository);

    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(COHORT_INDICATOR_SQL);
        database.execSQL(COHORT_INDICATOR_COHORT_ID_INDEX);
    }

    public void add(CohortIndicator cohortIndicator) {
        if (cohortIndicator == null) {
            return;
        }
        try {

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
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public CohortIndicator findByVaccineAndCohort(String vaccine, Long cohortId) {
        if (StringUtils.isBlank(vaccine) || cohortId == null) {
            return null;
        }

        Cursor cursor = null;
        CohortIndicator cohortIndicator = null;

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_COHORT_ID + " = ? AND " + COLUMN_VACCINE + " = ? ", new String[]{cohortId.toString(), vaccine}, null, null, null, null);
            List<CohortIndicator> cohortIndicators = readAllDataElements(cursor);
            if (!cohortIndicators.isEmpty()) {
                cohortIndicator = cohortIndicators.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cohortIndicator;
    }

    public List<CohortIndicator> findByCohort(Long cohortId) {
        if (cohortId == null) {
            return null;
        }

        Cursor cursor = null;
        List<CohortIndicator> cohortIndicators = new ArrayList<>();

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_COHORT_ID + " = ? ", new String[]{cohortId.toString()}, null, null, null, null);
            cohortIndicators = readAllDataElements(cursor);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cohortIndicators;
    }

    public void changeValue(Long value, Long id) {
        if (id == null || value == null) {
            return;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();

            ContentValues valuesToBeUpdated = new ContentValues();
            valuesToBeUpdated.put(COLUMN_VALUE, value);

            String idSelection = COLUMN_ID + " = ?";
            database.update(TABLE_NAME, valuesToBeUpdated, idSelection,
                    new String[]{id.toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();
            int rowsAffected = database.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{id.toString()});
            if (rowsAffected > 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
        return false;
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
        values.put(COLUMN_VALUE, cohortIndicator.getValue());
        values.put(COLUMN_CREATED_AT, cohortIndicator.getCreatedAt() != null ? cohortIndicator.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, cohortIndicator.getUpdatedAt() != null ? cohortIndicator.getUpdatedAt().getTime() : null);
        return values;
    }


}
