package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.path.domain.Cumulative;
import org.smartregister.repository.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keyman on 22/01/18.
 */
public class CumulativeRepository extends BaseRepository {
    private static final String TAG = CumulativeRepository.class.getCanonicalName();
    public static final SimpleDateFormat DF_YYYY = new SimpleDateFormat("yyyy");
    private static final String TABLE_NAME = "cumulatives";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_CSO_NUMBER = "cso_number";
    private static final String COLUMN_ZEIR_NUMBER = "zeir_number";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_YEAR, COLUMN_CSO_NUMBER, COLUMN_ZEIR_NUMBER, COLUMN_CREATED_AT, COLUMN_UPDATED_AT
    };

    private static final String CUMULATIVE_SQL = "CREATE TABLE " + TABLE_NAME +
            " (" + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_YEAR + " INTEGER NOT NULL UNIQUE ON CONFLICT IGNORE," +
            COLUMN_CSO_NUMBER + " INTEGER NULL," +
            COLUMN_ZEIR_NUMBER + " INTEGER NOT NULL," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)";

    public CumulativeRepository(PathRepository pathRepository) {
        super(pathRepository);

    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(CUMULATIVE_SQL);
    }

    public void add(Cumulative cumulative) {
        if (cumulative == null) {
            return;
        }
        try {
            if (cumulative.getUpdatedAt() == null) {
                cumulative.setUpdatedAt(new Date());
            }

            SQLiteDatabase database = getWritableDatabase();
            if (cumulative.getId() == null) {
                cumulative.setCreatedAt(new Date());
                cumulative.setId(database.insert(TABLE_NAME, null, createValuesFor(cumulative)));
            } else {
                String idSelection = COLUMN_ID + " = ?";
                database.update(TABLE_NAME, createValuesFor(cumulative), idSelection, new String[]{cumulative.getId().toString()});
            }

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private Cumulative findByYear(Integer year) {
        if (year == null) {
            return null;
        }

        Cursor cursor = null;
        Cumulative cumulative = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_YEAR + " = ? ", new String[]{year.toString()}, null, null, null, null);
            List<Cumulative> cumulatives = readAllDataElements(cursor);
            if (!cumulatives.isEmpty()) {
                cumulative = cumulatives.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cumulative;
    }

    public Cumulative findByYear(Date year) {
        if (year == null) {
            return null;
        }
        try {
            String yearString = DF_YYYY.format(year);
            return findByYear(Integer.valueOf(yearString));

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    public Cumulative findById(Long id) {
        if (id == null) {
            return null;
        }

        Cursor cursor = null;
        Cumulative cumulative = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS, COLUMN_ID + " = ? ", new String[]{id.toString()}, null, null, null, null);
            List<Cumulative> cumulatives = readAllDataElements(cursor);
            if (!cumulatives.isEmpty()) {
                cumulative = cumulatives.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cumulative;
    }

    public void changeCsoNumber(Long csoNumber, Long id) {
        if (id == null || csoNumber == null) {
            return;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();

            ContentValues valuesToBeUpdated = new ContentValues();
            valuesToBeUpdated.put(COLUMN_CSO_NUMBER, csoNumber);

            String idSelection = COLUMN_ID + " = ?";
            database.update(TABLE_NAME, valuesToBeUpdated, idSelection,
                    new String[]{id.toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void changeZeirNumber(Long zeirNumber, Long id) {
        if (id == null || zeirNumber == null) {
            return;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();

            ContentValues valuesToBeUpdated = new ContentValues();
            valuesToBeUpdated.put(COLUMN_ZEIR_NUMBER, zeirNumber);

            String idSelection = COLUMN_ID + " = ?";
            database.update(TABLE_NAME, valuesToBeUpdated, idSelection,
                    new String[]{id.toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public List<Cumulative> fetchAllWithIndicators() {
        Cursor cursor = null;
        List<Cumulative> cumulatives = new ArrayList<>();

        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS,
                    COLUMN_ID + " IN (SELECT DISTINCT " + CumulativeIndicatorRepository.COLUMN_CUMULATIVE_ID + " FROM " + CumulativeIndicatorRepository.TABLE_NAME + " )", null, null, null, null);
            cumulatives = readAllDataElements(cursor);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cumulatives;
    }

    public long executeQueryAndReturnCount(String query, String[] selectionArgs) {
        Cursor cursor = null;
        long count = 0;
        try {
            cursor = getReadableDatabase().rawQuery(query, selectionArgs);
            if (null != cursor) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = cursor.getLong(0);
                }
                cursor.close();
            }
        } catch (Exception e) {
            org.smartregister.util.Log.logError(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    private List<Cumulative> readAllDataElements(Cursor cursor) {
        List<Cumulative> cumulatives = new ArrayList<>();
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    Cumulative cumulative = new Cumulative();
                    cumulative.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                    cumulative.setYear(cursor.getInt(cursor.getColumnIndex(COLUMN_YEAR)));
                    cumulative.setCsoNumber(cursor.getLong(cursor.getColumnIndex(COLUMN_CSO_NUMBER)));
                    cumulative.setZeirNumber(cursor.getLong(cursor.getColumnIndex(COLUMN_ZEIR_NUMBER)));
                    cumulative.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT))));
                    cumulative.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT))));
                    cumulatives.add(cumulative);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return cumulatives;

    }

    private ContentValues createValuesFor(Cumulative cumulative) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, cumulative.getId());
        values.put(COLUMN_YEAR, cumulative.getYear());
        values.put(COLUMN_CSO_NUMBER, cumulative.getCsoNumber());
        values.put(COLUMN_ZEIR_NUMBER, cumulative.getZeirNumber());
        values.put(COLUMN_CREATED_AT, cumulative.getCreatedAt() != null ? cumulative.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, cumulative.getUpdatedAt() != null ? cumulative.getUpdatedAt().getTime() : null);
        return values;
    }
}
