package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.domain.CumulativePatient;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 22/01/18.
 */
public class CumulativePatientRepository extends BaseRepository {
    private static final String TAG = CumulativePatientRepository.class.getCanonicalName();
    private static final String TABLE_NAME = "cumulative_child_reports";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_BASE_ENTITY_ID = "base_entity_id";
    private static final String COLUMN_VALID_VACCINES = "valid_vaccines";
    private static final String COLUMN_INVALID_VACCINES = "invalid_vaccines";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_BASE_ENTITY_ID, COLUMN_VALID_VACCINES, COLUMN_INVALID_VACCINES, COLUMN_CREATED_AT, COLUMN_UPDATED_AT
    };

    private static final String CUMULATIVE_CHILD_REPORT_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_BASE_ENTITY_ID + " VARCHAR NOT NULL UNIQUE ON CONFLICT IGNORE," +
            COLUMN_VALID_VACCINES + " VARCHAR NULL," +
            COLUMN_INVALID_VACCINES + " VARCHAR NULL," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)";

    public CumulativePatientRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(CUMULATIVE_CHILD_REPORT_SQL);
    }

    public void add(CumulativePatient cumulativePatient) {
        if (cumulativePatient == null) {
            return;
        }

        try {
            if (cumulativePatient.getUpdatedAt() == null) {
                cumulativePatient.setUpdatedAt(new Date());
            }

            SQLiteDatabase database = getWritableDatabase();
            if (cumulativePatient.getId() == null) {
                cumulativePatient.setCreatedAt(new Date());
                cumulativePatient.setId(database.insert(TABLE_NAME, null, createValuesFor(cumulativePatient)));
            } else {
                String idSelection = COLUMN_ID + " = ?";
                database.update(TABLE_NAME, createValuesFor(cumulativePatient), idSelection, new String[]{cumulativePatient.getId().toString()});
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    public void changeValidVaccines(String validVaccines, Long id) {
        if (id == null) {
            return;
        }

        if (StringUtils.isBlank(validVaccines)){
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

    public void changeInValidVaccines(String inValidVaccines, Long id) {
        if (id == null || StringUtils.isBlank(inValidVaccines)) {
            return;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();

            ContentValues valuesToBeUpdated = new ContentValues();
            valuesToBeUpdated.put(COLUMN_INVALID_VACCINES, inValidVaccines);

            String idSelection = COLUMN_ID + " = ?";
            database.update(TABLE_NAME, valuesToBeUpdated, idSelection,
                    new String[]{id.toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    public CumulativePatient findByBaseEntityId(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        Cursor cursor = null;
        CumulativePatient cumulativePatient = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_BASE_ENTITY_ID + " = ? ", new String[]{baseEntityId}, null, null, null, null);
            List<CumulativePatient> cumulativePatients = readAllDataElements(cursor);
            if (!cumulativePatients.isEmpty()) {
                cumulativePatient = cumulativePatients.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cumulativePatient;
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

    private List<CumulativePatient> readAllDataElements(Cursor cursor) {
        List<CumulativePatient> cumulativePatients = new ArrayList<>();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    CumulativePatient cumulativePatient = new CumulativePatient();
                    cumulativePatient.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                    cumulativePatient.setBaseEntityId(cursor.getString(cursor.getColumnIndex(COLUMN_BASE_ENTITY_ID)));
                    cumulativePatient.setValidVaccines(cursor.getString(cursor.getColumnIndex(COLUMN_VALID_VACCINES)));
                    cumulativePatient.setInvalidVaccines(cursor.getString(cursor.getColumnIndex(COLUMN_INVALID_VACCINES)));
                    cumulativePatient.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
                    cumulativePatient.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT))));
                    cumulativePatients.add(cumulativePatient);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return cumulativePatients;
    }

    private ContentValues createValuesFor(CumulativePatient cumulativePatient) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, cumulativePatient.getId());
        values.put(COLUMN_BASE_ENTITY_ID, cumulativePatient.getBaseEntityId());
        values.put(COLUMN_VALID_VACCINES, cumulativePatient.getValidVaccines());
        values.put(COLUMN_INVALID_VACCINES, cumulativePatient.getInvalidVaccines());
        values.put(COLUMN_CREATED_AT, cumulativePatient.getCreatedAt() != null ? cumulativePatient.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, cumulativePatient.getUpdatedAt() != null ? cumulativePatient.getUpdatedAt().getTime() : null);
        return values;
    }

}
