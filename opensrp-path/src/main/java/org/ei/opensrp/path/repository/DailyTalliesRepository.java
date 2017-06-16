package org.ei.opensrp.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.ei.opensrp.Context;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.domain.DailyTally;
import org.ei.opensrp.path.domain.Hia2Indicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyTalliesRepository extends BaseRepository {
    private static final String TAG = DailyTalliesRepository.class.getCanonicalName();
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final String TABLE_NAME = "daily_tallies";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PROVIDER_ID = "provider_id";
    public static final String COLUMN_INDICATOR_ID = "indicator_id";
    public static final String COLUMN_VALUE = "value";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_INDICATOR_ID, COLUMN_PROVIDER_ID,
            COLUMN_VALUE, COLUMN_DAY, COLUMN_UPDATED_AT
    };
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
            COLUMN_INDICATOR_ID + " INTEGER NOT NULL," +
            COLUMN_PROVIDER_ID + " VARCHAR NOT NULL," +
            COLUMN_VALUE + " VARCHAR NOT NULL," +
            COLUMN_DAY + " DATETIME NOT NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)";
    private static final String INDEX_PROVIDER_ID = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_PROVIDER_ID + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_PROVIDER_ID + " COLLATE NOCASE);";
    private static final String INDEX_INDICATOR_ID = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_INDICATOR_ID + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_INDICATOR_ID + " COLLATE NOCASE);";
    private static final String INDEX_UPDATED_AT = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_UPDATED_AT + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_UPDATED_AT + ");";
    private static final String INDEX_DAY = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_DAY + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_DAY + ");";


    public DailyTalliesRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_QUERY);
        database.execSQL(INDEX_PROVIDER_ID);
        database.execSQL(INDEX_INDICATOR_ID);
        database.execSQL(INDEX_UPDATED_AT);
        database.execSQL(INDEX_DAY);
    }

    /**
     * Saves a set of tallies
     *
     * @param day        The day the tallies correspond to
     * @param hia2Report Object holding the tallies, the first key in the map holds the indicator
     *                   code, and the second the DHIS id for the indicator. It's expected that
     *                   the inner most map will always hold one value
     */
    public void save(String day, Map<String, Object> hia2Report) {
        SQLiteDatabase database = getPathRepository().getWritableDatabase();
        try {
            String userName = Context.getInstance().allSharedPreferences().fetchRegisteredANM();
            database.beginTransaction();
            for (String indicatorCode : hia2Report.keySet()) {
                    String indicatorValue = (String) hia2Report.get(indicatorCode);

                    // Get the HIA2 Indicator corresponding to the current tally
                    Hia2Indicator indicator = VaccinatorApplication.getInstance()
                            .hIA2IndicatorsRepository()
                            .findByIndicatorCode(indicatorCode);

                    if (indicator != null) {
                        Long id = checkIfExists(indicator.getId(), day);

                        ContentValues cv = new ContentValues();
                        cv.put(DailyTalliesRepository.COLUMN_INDICATOR_ID, indicator.getId());
                        cv.put(DailyTalliesRepository.COLUMN_VALUE, indicatorValue);
                        cv.put(DailyTalliesRepository.COLUMN_PROVIDER_ID, userName);
                        cv.put(DailyTalliesRepository.COLUMN_DAY, day);

                        if (id != null) {
                            database.update(TABLE_NAME, cv, COLUMN_ID + " = ?",
                                    new String[]{id.toString()});
                        } else {
                            database.insert(TABLE_NAME, null, cv);
                        }
                    }
            }
            database.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Returns a list of dates for distinct months with daily tallies
     *
     * @param dateFormat The format to use to format the months' dates
     * @return A list of months that have daily tallies
     */
    public List<String> findAllDistinctMonths(SimpleDateFormat dateFormat) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
        Cursor cursor = null;
        try {
            String query = "SELECT DISTINCT strftime('%Y-%m', " + COLUMN_DAY + ") month" +
                    " FROM " + TABLE_NAME;
            cursor = getPathRepository().getReadableDatabase().rawQuery(query, null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                List<String> months = new ArrayList<>();
                while (!cursor.isAfterLast()) {
                    Date curMonth = monthFormat.parse(cursor.getString(cursor.getColumnIndex("month")));
                    months.add(dateFormat.format(curMonth));
                    cursor.moveToNext();
                }

                return months;
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return new ArrayList<>();
    }

    public Map<Long, List<DailyTally>> findTalliesInMonth(Date month) {
        Map<Long, List<DailyTally>> talliesFromMonth = new HashMap<>();
        Cursor cursor = null;
        try {
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
            HashMap<Long, Hia2Indicator> indicatorMap = VaccinatorApplication.getInstance()
                    .hIA2IndicatorsRepository().findAll();

            cursor = getPathRepository().getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS,
                    "strftime('%Y-%m', " + COLUMN_DAY + ") = '" + monthFormat.format(month) + "'",
                    null, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    DailyTally curTally = extractDailyTally(indicatorMap, cursor);
                    if (curTally != null) {
                        if (!talliesFromMonth.containsKey(curTally.getIndicator().getId())) {
                            talliesFromMonth.put(
                                    curTally.getIndicator().getId(),
                                    new ArrayList<DailyTally>());
                        }

                        talliesFromMonth.get(curTally.getIndicator().getId()).add(curTally);
                    }
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return talliesFromMonth;
    }

    public List<DailyTally> findByProviderIdAndDay(String providerId, Date day) {
        return findByProviderIdAndDay(providerId, DAY_FORMAT.format(day));
    }

    public List<DailyTally> findByProviderIdAndDay(String providerId, String day) {
        List<DailyTally> tallies = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getPathRepository().getReadableDatabase()
                    .query(TABLE_NAME, TABLE_COLUMNS,
                            COLUMN_PROVIDER_ID + " = ? AND " + COLUMN_DAY + "=?",
                            new String[]{providerId, day},
                            null, null, null, null);
            tallies = readAllDataElements(cursor);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return tallies;
    }

    private List<DailyTally> readAllDataElements(Cursor cursor) {
        List<DailyTally> tallies = new ArrayList<>();
        HashMap<Long, Hia2Indicator> indicatorMap = VaccinatorApplication.getInstance()
                .hIA2IndicatorsRepository().findAll();

        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    DailyTally curTally = extractDailyTally(indicatorMap, cursor);
                    if (curTally != null) {
                        tallies.add(curTally);
                    }

                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            cursor.close();
        }

        return tallies;
    }

    private DailyTally extractDailyTally(HashMap<Long, Hia2Indicator> indicatorMap, Cursor cursor) {
        long indicatorId = cursor.getLong(cursor.getColumnIndex(COLUMN_INDICATOR_ID));
        if (indicatorMap.containsKey(indicatorId)) {
            DailyTally curTally = new DailyTally();
            curTally.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
            curTally.setProviderId(
                    cursor.getString(cursor.getColumnIndex(COLUMN_PROVIDER_ID)));
            curTally.setIndicator(indicatorMap.get(indicatorId));
            curTally.setValue(cursor.getString(cursor.getColumnIndex(COLUMN_VALUE)));
            curTally.setDay(
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_DAY))));
            curTally.setUpdatedAt(
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT)))
            );

            return curTally;
        }

        return null;
    }

    private Long checkIfExists(long indicatorId, String day) {
        Cursor mCursor = null;
        try {
            String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_NAME +
                    " WHERE " + COLUMN_INDICATOR_ID + " = " + String.valueOf(indicatorId)
                    + " and " + COLUMN_DAY + "='" + day + "'";
            mCursor = getPathRepository().getWritableDatabase().rawQuery(query, null);
            if (mCursor != null && mCursor.moveToFirst()) {
                return mCursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return null;
    }

}
