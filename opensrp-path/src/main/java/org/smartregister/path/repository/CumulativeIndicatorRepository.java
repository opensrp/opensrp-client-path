package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.path.domain.CumulativeIndicator;
import org.smartregister.repository.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 22/01/18.
 */
public class CumulativeIndicatorRepository extends BaseRepository {
    private static final String TAG = CumulativeIndicatorRepository.class.getCanonicalName();
    public static final SimpleDateFormat DF_YYYYMM = new SimpleDateFormat("yyyy-MM");
    public static final String TABLE_NAME = "cumulative_indicators";
    private static final String COLUMN_ID = "_id";
    public static final String COLUMN_CUMULATIVE_ID = "cumulative_id";
    private static final String COLUMN_MONTH = "month";
    private static final String COLUMN_VACCINE = "vaccine";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_CUMULATIVE_ID, COLUMN_MONTH, COLUMN_VACCINE, COLUMN_VALUE, COLUMN_CREATED_AT, COLUMN_UPDATED_AT

    };

    private static final String CUMULATIVE_INDICATOR_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CUMULATIVE_ID + " INTEGER NOT NULL," +
            COLUMN_MONTH + " VARCHAR NOT NULL," +
            COLUMN_VACCINE + " VARCHAR NOT NULL," +
            COLUMN_VALUE + " INTEGER NOT NULL DEFAULT 0," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, " +
            " UNIQUE(" + COLUMN_VACCINE + ", " + COLUMN_MONTH + ", " + COLUMN_CUMULATIVE_ID + ") ON CONFLICT IGNORE )";
    private static final String CUMULATIVE_INDICATOR_CUMULATIVE_ID_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_CUMULATIVE_ID + "_index ON " + TABLE_NAME + "(" + COLUMN_CUMULATIVE_ID + ");";

    public CumulativeIndicatorRepository(PathRepository pathRepository) {
        super(pathRepository);

    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(CUMULATIVE_INDICATOR_SQL);
        database.execSQL(CUMULATIVE_INDICATOR_CUMULATIVE_ID_INDEX);
    }

    public void add(CumulativeIndicator cumulativeIndicator) {
        if (cumulativeIndicator == null) {
            return;
        }
        try {

            if (cumulativeIndicator.getUpdatedAt() == null) {
                cumulativeIndicator.setUpdatedAt(new Date());
            }

            SQLiteDatabase database = getWritableDatabase();
            if (cumulativeIndicator.getId() == null) {
                cumulativeIndicator.setCreatedAt(new Date());
                cumulativeIndicator.setId(database.insert(TABLE_NAME, null, createValuesFor(cumulativeIndicator)));
            } else {
                //mark the vaccine as unsynced for processing as an updated stock
                String idSelection = COLUMN_ID + " = ?";
                database.update(TABLE_NAME, createValuesFor(cumulativeIndicator), idSelection, new String[]{cumulativeIndicator.getId().toString()});
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public CumulativeIndicator findByVaccineMonthAndCumulativeId(String vaccine, String month, Long cumulativeId) {
        if (StringUtils.isBlank(vaccine) || StringUtils.isBlank(month) || cumulativeId == null) {
            return null;
        }

        Cursor cursor = null;
        CumulativeIndicator cumulativeIndicator = null;

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_CUMULATIVE_ID + " = ? AND " + COLUMN_MONTH + " = ?  AND " + COLUMN_VACCINE + " = ? ", new String[]{cumulativeId.toString(), month, vaccine}, null, null, null, null);
            List<CumulativeIndicator> cumulativeIndicators = readAllDataElements(cursor);
            if (!cumulativeIndicators.isEmpty()) {
                cumulativeIndicator = cumulativeIndicators.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cumulativeIndicator;
    }

    public CumulativeIndicator findByVaccineMonthAndCumulativeId(String vaccine, Date month, Long cumulativeId) {
        if (StringUtils.isBlank(vaccine) || month == null || cumulativeId == null) {
            return null;
        }

        try {
            String monthString = DF_YYYYMM.format(month);
            return findByVaccineMonthAndCumulativeId(vaccine, monthString, cumulativeId);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    public List<CumulativeIndicator> findByCumulativeId(Long cumulativeId) {
        if (cumulativeId == null) {
            return null;
        }

        Cursor cursor = null;
        List<CumulativeIndicator> cumulativeIndicators = new ArrayList<>();

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_CUMULATIVE_ID + " = ? ", new String[]{cumulativeId.toString()}, null, null, null, null);
            cumulativeIndicators = readAllDataElements(cursor);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cumulativeIndicators;
    }

    public long countByCumulativeId(Long cumulativeId) {
        if (cumulativeId == null) {
            return 0L;
        }

        long count = 0L;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"count(*)"}, COLUMN_CUMULATIVE_ID + " = ? ", new String[]{cumulativeId.toString()}, null, null, null);

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

    private List<CumulativeIndicator> readAllDataElements(Cursor cursor) {
        List<CumulativeIndicator> cumulativeIndicators = new ArrayList<>();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    CumulativeIndicator cumulativeIndicator = new CumulativeIndicator();
                    cumulativeIndicator.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                    cumulativeIndicator.setCumulativeId(cursor.getLong(cursor.getColumnIndex(COLUMN_CUMULATIVE_ID)));
                    cumulativeIndicator.setMonth(cursor.getString(cursor.getColumnIndex(COLUMN_MONTH)));
                    cumulativeIndicator.setVaccine(cursor.getString(cursor.getColumnIndex(COLUMN_VACCINE)));
                    cumulativeIndicator.setValue(cursor.getLong(cursor.getColumnIndex(COLUMN_VALUE)));
                    cumulativeIndicator.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
                    cumulativeIndicator.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT))));
                    cumulativeIndicators.add(cumulativeIndicator);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return cumulativeIndicators;

    }

    private ContentValues createValuesFor(CumulativeIndicator cumulativeIndicator) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, cumulativeIndicator.getId());
        values.put(COLUMN_CUMULATIVE_ID, cumulativeIndicator.getCumulativeId());
        values.put(COLUMN_MONTH, cumulativeIndicator.getMonth());
        values.put(COLUMN_VACCINE, cumulativeIndicator.getVaccine());
        values.put(COLUMN_VALUE, cumulativeIndicator.getValue());
        values.put(COLUMN_CREATED_AT, cumulativeIndicator.getCreatedAt() != null ? cumulativeIndicator.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, cumulativeIndicator.getUpdatedAt() != null ? cumulativeIndicator.getUpdatedAt().getTime() : null);
        return values;
    }


}
