package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.domain.CohortPatient;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 11/01/18.
 */
public class CohortPatientRepository extends BaseRepository {
    private static final String TAG = CohortPatientRepository.class.getCanonicalName();
    private static final String TABLE_NAME = "cohort_patients";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_BASE_ENTITY_ID = "base_entity_id";
    private static final String COLUMN_COHORT_ID = "cohort_id";
    private static final String COLUMN_VALID_VACCINES = "valid_vaccines";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_BASE_ENTITY_ID, COLUMN_COHORT_ID, COLUMN_VALID_VACCINES, COLUMN_CREATED_AT, COLUMN_UPDATED_AT
    };

    private static final String COHORT_CHILD_REPORT_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_BASE_ENTITY_ID + " VARCHAR NOT NULL UNIQUE ON CONFLICT IGNORE," +
            COLUMN_COHORT_ID + " INTEGER NOT NULL," +
            COLUMN_VALID_VACCINES + " VARCHAR NULL," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)";

    private static final String COHORT_CHILD_REPORT_COHORT_ID_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_COHORT_ID + "_index ON " + TABLE_NAME + "(" + COLUMN_COHORT_ID + " );";

    public CohortPatientRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(COHORT_CHILD_REPORT_SQL);
        database.execSQL(COHORT_CHILD_REPORT_COHORT_ID_INDEX);
    }

    public void add(CohortPatient cohortPatient) {
        if (cohortPatient == null) {
            return;
        }

        try {
            if (cohortPatient.getUpdatedAt() == null) {
                cohortPatient.setUpdatedAt(new Date());
            }

            SQLiteDatabase database = getWritableDatabase();
            if (cohortPatient.getId() == null) {
                cohortPatient.setCreatedAt(new Date());
                cohortPatient.setId(database.insert(TABLE_NAME, null, createValuesFor(cohortPatient)));
            } else {
                String idSelection = COLUMN_ID + " = ?";
                database.update(TABLE_NAME, createValuesFor(cohortPatient), idSelection, new String[]{cohortPatient.getId().toString()});
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    public void changeValidVaccines(String validVaccines, Long id) {
        if (id == null) {
            return;
        }

        if (StringUtils.isBlank(validVaccines)) {
            validVaccines = "";
        }

        try {
            SQLiteDatabase database = getWritableDatabase();

            ContentValues valuesToBeUpdated = new ContentValues();
            valuesToBeUpdated.put(COLUMN_VALID_VACCINES, validVaccines);

            String idSelection = COLUMN_ID + " = ?";
            database.update(TABLE_NAME, valuesToBeUpdated, idSelection,
                    new String[]{id.toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    public CohortPatient findByBaseEntityId(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        Cursor cursor = null;
        CohortPatient cohortPatient = null;

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_BASE_ENTITY_ID + " = ? ", new String[]{baseEntityId}, null, null, null, null);
            List<CohortPatient> cohortPatients = readAllDataElements(cursor);
            if (!cohortPatients.isEmpty()) {
                cohortPatient = cohortPatients.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cohortPatient;
    }

    public List<CohortPatient> findByCohort(Long cohortId) {
        if (cohortId == null) {
            return null;
        }

        Cursor cursor = null;
        List<CohortPatient> cohortPatients = new ArrayList<>();

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_COHORT_ID + " = ? ", new String[]{cohortId.toString()}, null, null, null, null);
            cohortPatients = readAllDataElements(cursor);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cohortPatients;
    }

    public long countCohort(Long cohortId) {
        if (cohortId == null) {
            return 0L;
        }

        long count = 0L;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"count(*)"}, COLUMN_COHORT_ID + " = ? ", new String[]{cohortId.toString()}, null, null, null);

        try {
            cursor.moveToFirst();
            count = cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
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

    private List<CohortPatient> readAllDataElements(Cursor cursor) {
        List<CohortPatient> cohortPatients = new ArrayList<>();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    CohortPatient cohortPatient = new CohortPatient();
                    cohortPatient.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                    cohortPatient.setBaseEntityId(cursor.getString(cursor.getColumnIndex(COLUMN_BASE_ENTITY_ID)));
                    cohortPatient.setCohortId(cursor.getLong(cursor.getColumnIndex(COLUMN_COHORT_ID)));
                    cohortPatient.setValidVaccines(cursor.getString(cursor.getColumnIndex(COLUMN_VALID_VACCINES)));
                    cohortPatient.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
                    cohortPatient.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT))));
                    cohortPatients.add(cohortPatient);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return cohortPatients;
    }

    private ContentValues createValuesFor(CohortPatient cohortPatient) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, cohortPatient.getId());
        values.put(COLUMN_BASE_ENTITY_ID, cohortPatient.getBaseEntityId());
        values.put(COLUMN_COHORT_ID, cohortPatient.getCohortId());
        values.put(COLUMN_VALID_VACCINES, cohortPatient.getValidVaccines());
        values.put(COLUMN_CREATED_AT, cohortPatient.getCreatedAt() != null ? cohortPatient.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, cohortPatient.getUpdatedAt() != null ? cohortPatient.getUpdatedAt().getTime() : null);
        return values;
    }

}
