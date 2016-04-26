package org.ei.opensrp.commonregistry;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import org.apache.commons.lang3.ArrayUtils;
import org.ei.opensrp.repository.DrishtiRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sqlcipher.DatabaseUtils.longForQuery;
import static org.apache.commons.lang3.StringUtils.repeat;

/**
 * Created by Raihan Ahmed on 4/15/15.
 */
public class CommonRepository extends DrishtiRepository {
    private static final String TAG = "CommonRepository";
    private String common_SQL = "CREATE TABLE common(id VARCHAR PRIMARY KEY,details VARCHAR)";
    public static final String ID_COLUMN = "id";
    public static final String Relational_ID = "relationalid";
    public static final String DETAILS_COLUMN = "details";
    public String TABLE_NAME = "common";
    public String[] common_TABLE_COLUMNS = new String[]{ID_COLUMN, Relational_ID, DETAILS_COLUMN};
    public String[] additionalcolumns;

    public CommonRepository(String tablename, String[] columns) {
        super();
        additionalcolumns = columns;
        common_TABLE_COLUMNS = ArrayUtils.addAll(common_TABLE_COLUMNS, columns);
        TABLE_NAME = tablename;
        common_SQL = "CREATE TABLE " + TABLE_NAME + "(id VARCHAR PRIMARY KEY,relationalid VARCHAR, details VARCHAR, is_closed TINYINT DEFAULT 0";
        for (int i = 0; i < columns.length; i++) {
            if (i == 0) {
                common_SQL = common_SQL + ", ";
            }
            if (i != columns.length - 1) {
                common_SQL = common_SQL + columns[i] + " VARCHAR,";
            } else {
                common_SQL = common_SQL + columns[i] + " VARCHAR ";
            }
        }
        common_SQL = common_SQL + ")";
    }

    @Override
    protected void onCreate(SQLiteDatabase database) {
        database.execSQL(common_SQL);
    }

    public void add(CommonPersonObject common) {
        SQLiteDatabase database = masterRepository.getWritableDatabase();
        database.insert(TABLE_NAME, null, createValuesFor(common));
    }

    public void updateDetails(String caseId, Map<String, String> details) {
        SQLiteDatabase database = masterRepository.getWritableDatabase();

        CommonPersonObject common = findByCaseID(caseId);
        if (common == null) {
            return;
        }

        ContentValues valuesToUpdate = new ContentValues();
        valuesToUpdate.put(DETAILS_COLUMN, new Gson().toJson(details));
        database.update(TABLE_NAME, valuesToUpdate, ID_COLUMN + " = ?", new String[]{caseId});
    }

    public void mergeDetails(String caseId, Map<String, String> details) {
        SQLiteDatabase database = masterRepository.getWritableDatabase();

        CommonPersonObject common = findByCaseID(caseId);
        if (common == null) {
            return;
        }

        Map<String, String> mergedDetails = new HashMap<String, String>(common.getDetails());
        mergedDetails.putAll(details);
        ContentValues valuesToUpdate = new ContentValues();
        valuesToUpdate.put(DETAILS_COLUMN, new Gson().toJson(mergedDetails));
        database.update(TABLE_NAME, valuesToUpdate, ID_COLUMN + " = ?", new String[]{caseId});
    }

    public List<CommonPersonObject> allcommon() {
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, common_TABLE_COLUMNS, null, null, null, null, null, null);
        return readAllcommon(cursor);
    }

    public List<CommonPersonObject> findByCaseIDs(String... caseIds) {
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(String.format("SELECT * FROM %s WHERE %s IN (%s)", TABLE_NAME, ID_COLUMN,
                insertPlaceholdersForInClause(caseIds.length)), caseIds);
        return readAllcommon(cursor);
    }

    public List<CommonPersonObject> findByRelationalIDs(String... caseIds) {
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(String.format("SELECT * FROM %s WHERE %s IN (%s)", TABLE_NAME, Relational_ID,
                insertPlaceholdersForInClause(caseIds.length)), caseIds);
        return readAllcommon(cursor);
    }

    public CommonPersonObject findByCaseID(String caseId) {
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, common_TABLE_COLUMNS, ID_COLUMN + " = ?", new String[]{caseId},
                null, null, null, null);
        List<CommonPersonObject> commons = readAllcommon(cursor);
        if (commons.isEmpty()) {
            return null;
        }
        return commons.get(0);
    }

    public CommonPersonObject findHHByGOBHHID(String caseId) {
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, common_TABLE_COLUMNS, "FWGOBHHID" + " = ?", new String[]{caseId},
                null, null, null, null);
        List<CommonPersonObject> commons = readAllcommon(cursor);
        if (commons.isEmpty()) {
            return null;
        }
        return commons.get(0);
    }

    public long count() {
        return longForQuery(masterRepository.getReadableDatabase(), "SELECT COUNT(1) FROM " + TABLE_NAME
                , new String[0]);
    }


    public void close(String caseId) {
//        ContentValues values = new ContentValues();
//        masterRepository.getWritableDatabase().update(EC_TABLE_NAME, values, ID_COLUMN + " = ?", new String[]{caseId});
    }

    private ContentValues createValuesFor(CommonPersonObject common) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, common.getCaseId());
        values.put(Relational_ID, common.getRelationalId());
        values.put(DETAILS_COLUMN, new Gson().toJson(common.getDetails()));
        return values;
    }

    private List<CommonPersonObject> readAllcommon(Cursor cursor) {
        cursor.moveToFirst();
        List<CommonPersonObject> commons = new ArrayList<CommonPersonObject>();
        while (!cursor.isAfterLast()) {
            int columncount = cursor.getColumnCount();
            HashMap<String, String> columns = new HashMap<String, String>();
            for (int i = 3; i < columncount; i++) {
                columns.put(additionalcolumns[i - 3], cursor.getString(i));
            }
            CommonPersonObject common = new CommonPersonObject(cursor.getString(0), cursor.getString(1), new Gson().<Map<String, String>>fromJson(cursor.getString(2), new TypeToken<Map<String, String>>() {
            }.getType()), TABLE_NAME);
            common.setColumnmaps(columns);

            commons.add(common);
            cursor.moveToNext();
        }
        cursor.close();
        return commons;
    }

    private String insertPlaceholdersForInClause(int length) {
        return repeat("?", ",", length);
    }


    private List<Map<String, String>> readDetailsList(Cursor cursor) {
        cursor.moveToFirst();
        List<Map<String, String>> detailsList = new ArrayList<Map<String, String>>();
        while (!cursor.isAfterLast()) {
            String detailsJSON = cursor.getString(0);
            detailsList.add(new Gson().<Map<String, String>>fromJson(detailsJSON, new TypeToken<HashMap<String, String>>() {
            }.getType()));
            cursor.moveToNext();
        }
        cursor.close();
        return detailsList;
    }


    public void updateColumn(String tableName, ContentValues contentValues, String caseId) {
        SQLiteDatabase database = masterRepository.getWritableDatabase();
        database.update(tableName, contentValues, ID_COLUMN + " = ?", new String[]{caseId});
    }

    public List<CommonPersonObject> customQuery(String sql, String[] selections, String tableName) {

        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, selections);
        // database.
        return readAllcommonForField(cursor, tableName);
    }


    public List<CommonPersonObject> readAllcommonForField(Cursor cursor, String tableName) {
        List<CommonPersonObject> commons = new ArrayList<CommonPersonObject>();
        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                int columncount = cursor.getColumnCount();
                HashMap<String, String> columns = new HashMap<String, String>();
                for (int i = 0; i < columncount; i++) {
                    columns.put(cursor.getColumnName(i), String.valueOf(cursor.getInt(i)));
                }
                CommonPersonObject common = new CommonPersonObject("1", "0", null, tableName);
                common.setColumnmaps(columns);

                commons.add(common);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        return commons;
    }

    public List<CommonPersonObject> customQueryForCompleteRow(String sql, String[] selections, String tableName) {

        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, selections);
        // database.
        return readAllcommonFor(cursor, tableName);
    }

    private List<CommonPersonObject> readAllcommonFor(Cursor cursor, String tableName) {
        List<CommonPersonObject> commons = new ArrayList<CommonPersonObject>();
        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                int columncount = cursor.getColumnCount();
                HashMap<String, String> columns = new HashMap<String, String>();
                for (int i = 3; i < columncount; i++) {
                    columns.put(additionalcolumns[i - 3], cursor.getString(i));
                }
                CommonPersonObject common = new CommonPersonObject("1", "0", new Gson().<Map<String, String>>fromJson(cursor.getString(cursor.getColumnIndex("details")), new TypeToken<Map<String, String>>() {
                }.getType()), tableName);
                common.setColumnmaps(columns);

                commons.add(common);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        return commons;
    }

    public Cursor CustomQueryForAdapter(String[] columns, String tableName, String limit, String offset) {

        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.query(tableName, columns, null, null, null, null, null, offset + "," + limit);

        return cursor;
    }

    public Cursor RawCustomQueryForAdapter(String query) {

        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(query, null);
        return cursor;
    }

    public CommonPersonObject readAllcommonforCursorAdapter(Cursor cursor) {


        int columncount = cursor.getColumnCount();
        HashMap<String, String> columns = new HashMap<String, String>();
        for (int i = 0; i < columncount; i++) {
            String columnName = cursor.getColumnName(i);
            String value = cursor.getString(cursor.getColumnIndex(columnName));
            columns.put(columnName, value);
        }
        //CommonPersonObject common = new CommonPersonObject(cursor.getString(0),cursor.getString(1),new Gson().<Map<String, String>>fromJson(cursor.getString(2), new TypeToken<Map<String, String>>() {
        //}.getType()),TABLE_NAME);
        CommonPersonObject common = getCommonPersonObjectFromCursor(cursor);
        common.setColumnmaps(columns);


        return common;
    }

    public CommonPersonObject getCommonPersonObjectFromCursor(Cursor cursor) {
        CommonPersonObject commonPersonObject = null;
        String caseId = cursor.getString(cursor.getColumnIndex("_id"));
        String relationalid = cursor.getString(cursor.getColumnIndex("relationalid"));
        Map<String, String> details = sqliteRowToMap(cursor);
        commonPersonObject = new CommonPersonObject(caseId, relationalid, details, TABLE_NAME);
        return commonPersonObject;
    }

    /**
     * Insert the a new record to the database and returns its id
     **/
    public Long executeInsertStatement(ContentValues values, String tableName) {
        SQLiteDatabase database = masterRepository.getWritableDatabase();
        String baseEntityId = values.getAsString("base_entity_id");
        values = addMissingContentValuesForRecordId(baseEntityId, tableName, values);
        //hack the id above is not set to be autogenerated so we'll reuse the base entity id
        values.put("id", baseEntityId);
        Long id = database.insertWithOnConflict(tableName, BaseColumns._ID, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        return id;
    }

    private ContentValues addMissingContentValuesForRecordId(String baseEntityId, String tableName, ContentValues cv) {
        Map<String, String> dbValues = new HashMap<String, String>();
        SQLiteDatabase db = masterRepository.getWritableDatabase();
        String query = "SELECT  * FROM " + tableName + " WHERE base_entity_id = '" + baseEntityId + "'";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            dbValues = sqliteRowToMap(cursor);

            for (String key : dbValues.keySet()) {
                if (!cv.containsKey(key)) {
                    cv.put(key, dbValues.get(key));
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return cv;
    }

    public Map<String, String> sqliteRowToMap(Cursor cursor) {
        int totalColumn = cursor.getColumnCount();
        Map<String, String> rowObject = new HashMap<String, String>();
        if (cursor != null) {
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                }
            }
        }
        return rowObject;
    }

    public Cursor queryTable(String query) {
        SQLiteDatabase db = masterRepository.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }
    /**
     * Closes a case with the given baseEntityId
     * @param baseEntityId
     */
    public void closeCase(String baseEntityId, String tableName){
        try {
            StringBuilder sql = new StringBuilder().append("UPDATE ")
                    .append(tableName)
                    .append(" SET is_closed = 1 WHERE base_entity_id = '")
                    .append(baseEntityId)
                    .append("'");
            SQLiteDatabase db = masterRepository.getWritableDatabase();
            db.execSQL(sql.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
