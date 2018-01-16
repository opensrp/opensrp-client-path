package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.domain.ChildReport;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 11/01/18.
 */
public class ChildReportRepository extends BaseRepository {
    private static final String TAG = ChildReportRepository.class.getCanonicalName();
    private static final String TABLE_NAME = "child_reports";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_BASE_ENTITY_ID = "base_entity_id";
    private static final String COLUMN_COHORT_ID = "cohort_id";
    private static final String COLUMN_VALID_VACCINES = "valid_vaccines";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_BASE_ENTITY_ID, COLUMN_COHORT_ID, COLUMN_VALID_VACCINES, COLUMN_CREATED_AT, COLUMN_UPDATED_AT
    };

    private static final String COHORT_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_BASE_ENTITY_ID + " VARCHAR NOT NULL UNIQUE ON CONFLICT IGNORE," +
            COLUMN_COHORT_ID + " INTEGER NOT NULL," +
            COLUMN_VALID_VACCINES + " VARCHAR NULL," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)";

    private static final String COHORT_ID_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_COHORT_ID + "_index ON " + TABLE_NAME + "(" + COLUMN_COHORT_ID + " );";

    public ChildReportRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(COHORT_SQL);
        database.execSQL(COHORT_ID_INDEX);
    }

    public void add(ChildReport childReport) {
        if (childReport == null) {
            return;
        }

        try {
            if (childReport.getUpdatedAt() == null) {
                childReport.setUpdatedAt(new Date());
            }

            SQLiteDatabase database = getWritableDatabase();
            if (childReport.getId() == null) {
                childReport.setCreatedAt(new Date());
                childReport.setId(database.insert(TABLE_NAME, null, createValuesFor(childReport)));
            } else {
                String idSelection = COLUMN_ID + " = ?";
                database.update(TABLE_NAME, createValuesFor(childReport), idSelection, new String[]{childReport.getId().toString()});
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    public void changeValidVaccines(String validVaccines, Long id) {
        if (id == null || StringUtils.isBlank(validVaccines)) {
            return;
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


    public ChildReport findByBaseEntityId(String baseEntityId) {
        if (StringUtils.isBlank(baseEntityId)) {
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_BASE_ENTITY_ID + " = ? COLLATE NOCASE ", new String[]{baseEntityId}, null, null, null, null);
            List<ChildReport> childReports = readAllDataElements(cursor);
            if (!childReports.isEmpty()) {
                return childReports.get(0);
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

    public long countCohort(Long cohortId) {
        if (cohortId == null) {
            return 0l;
        }


        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"count(*)"}, COLUMN_COHORT_ID + " = ? ", new String[]{cohortId.toString()}, null, null, null);

        try {
            cursor.moveToFirst();
            return cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private List<ChildReport> readAllDataElements(Cursor cursor) {
        List<ChildReport> childReports = new ArrayList<>();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    ChildReport childReport = new ChildReport();
                    childReport.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                    childReport.setBaseEntityId(cursor.getString(cursor.getColumnIndex(COLUMN_BASE_ENTITY_ID)));
                    childReport.setCohortId(cursor.getLong(cursor.getColumnIndex(COLUMN_COHORT_ID)));
                    childReport.setValidVaccines(cursor.getString(cursor.getColumnIndex(COLUMN_VALID_VACCINES)));
                    childReport.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
                    childReport.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT))));
                    childReports.add(childReport);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return childReports;
    }

    private ContentValues createValuesFor(ChildReport childReport) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, childReport.getId());
        values.put(COLUMN_BASE_ENTITY_ID, childReport.getBaseEntityId());
        values.put(COLUMN_COHORT_ID, childReport.getCohortId());
        values.put(COLUMN_VALID_VACCINES, childReport.getValidVaccines());
        values.put(COLUMN_CREATED_AT, childReport.getCreatedAt() != null ? childReport.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, childReport.getUpdatedAt() != null ? childReport.getUpdatedAt().getTime() : null);
        return values;
    }

}
