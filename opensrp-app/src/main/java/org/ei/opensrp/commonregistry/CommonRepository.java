package org.ei.opensrp.commonregistry;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
    private String common_SQL = "CREATE TABLE common(id VARCHAR PRIMARY KEY,details VARCHAR)";
    private String common_ID_INDEX_SQL =  "CREATE INDEX common_id_index ON common(id COLLATE NOCASE) ;";
    private String common_Relational_ID_INDEX_SQL = null;
    public static final String ID_COLUMN = "id";
    public static final String Relational_ID = "relationalid";
    public static final String DETAILS_COLUMN = "details";
    public String TABLE_NAME = "common";
    public  String[] common_TABLE_COLUMNS = new String[]{ID_COLUMN,Relational_ID,DETAILS_COLUMN};
    public String [] additionalcolumns;
    public CommonRepository(String tablename, String[] columns) {
        super();
        additionalcolumns = columns;
        common_TABLE_COLUMNS = ArrayUtils.addAll(common_TABLE_COLUMNS, columns);
        TABLE_NAME = tablename;
        common_SQL = "CREATE TABLE "+ TABLE_NAME + "(" + ID_COLUMN + " VARCHAR PRIMARY KEY," + Relational_ID + " VARCHAR," + DETAILS_COLUMN + " VARCHAR";
        for(int i = 0;i<columns.length;i++){
            if(i ==0){
                common_SQL = common_SQL + ", ";
            }
            if(i!=columns.length-1) {
                common_SQL = common_SQL + columns[i] + " VARCHAR,";
            }else{
                common_SQL = common_SQL + columns[i] + " VARCHAR ";
            }
        }
        common_SQL = common_SQL +")";
        common_ID_INDEX_SQL = "CREATE INDEX " + TABLE_NAME + "_" + ID_COLUMN + "_index ON " + TABLE_NAME + "(" + ID_COLUMN + " COLLATE NOCASE);";
        common_Relational_ID_INDEX_SQL = "CREATE INDEX " + TABLE_NAME + "_" + Relational_ID + "_index ON " + TABLE_NAME + "(" + Relational_ID + " COLLATE NOCASE);";
    }

    @Override
    protected void onCreate(SQLiteDatabase database) {
        database.execSQL(common_SQL);
        if(StringUtils.isNotBlank(common_ID_INDEX_SQL)) {
            database.execSQL(common_ID_INDEX_SQL);
        }
        if(StringUtils.isNotBlank(common_Relational_ID_INDEX_SQL)) {
            database.execSQL(common_Relational_ID_INDEX_SQL);
        }
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
            HashMap <String, String> columns = new HashMap<String, String>();
            for (int i = 3;i < columncount;i++ ){
                columns.put(additionalcolumns[i-3],cursor.getString(i));
            }
            CommonPersonObject common = new CommonPersonObject(cursor.getString(0),cursor.getString(1),new Gson().<Map<String, String>>fromJson(cursor.getString(2), new TypeToken<Map<String, String>>() {
                    }.getType()),TABLE_NAME);
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


    public void updateColumn(String tableName,ContentValues contentValues,String caseId){
        SQLiteDatabase database = masterRepository.getWritableDatabase();
        database.update(tableName, contentValues, ID_COLUMN + " = ?", new String[]{caseId});
    }

    public  List<CommonPersonObject> customQuery(String sql ,String[] selections,String tableName){

        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql,selections);
        // database.
        return readAllcommonForField(cursor, tableName);
    }

    public  ArrayList<HashMap<String, String>> rawQuery(String sql){
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        ArrayList<HashMap<String, String>> maplist = new ArrayList<HashMap<String, String>>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                for(int i=0; i<cursor.getColumnCount();i++)
                {
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                }

                maplist.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return contact list
        return maplist;
    }

    public List<CommonPersonObject> readAllcommonForField(Cursor cursor ,String tableName) {
        List<CommonPersonObject> commons = new ArrayList<CommonPersonObject>();
        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                int columncount = cursor.getColumnCount();
                HashMap<String, String> columns = new HashMap<String, String>();
                for (int i = 0; i < columncount; i++) {
                    columns.put(cursor.getColumnName(i), String.valueOf(cursor.getInt(i)));
                }
                CommonPersonObject common = new CommonPersonObject("1","0", null, tableName);
                common.setColumnmaps(columns);

                commons.add(common);
                cursor.moveToNext();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            cursor.close();
        }

        return commons;
    }

    public  List<CommonPersonObject> customQueryForCompleteRow(String sql ,String[] selections,String tableName){

        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql,selections);
        // database.
        return readAllcommonFor(cursor, tableName);
    }

    private List<CommonPersonObject> readAllcommonFor(Cursor cursor ,String tableName) {
        List<CommonPersonObject> commons = new ArrayList<CommonPersonObject>();
        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                int columncount = cursor.getColumnCount();
                HashMap<String, String> columns = new HashMap<String, String>();
                for (int i = 3; i < columncount; i++) {
                    columns.put(additionalcolumns[i - 3], cursor.getString(i));
                }
                CommonPersonObject common = new CommonPersonObject("1","0", new Gson().<Map<String, String>>fromJson(cursor.getString(cursor.getColumnIndex("details")), new TypeToken<Map<String, String>>() {
                }.getType()), tableName);
                common.setColumnmaps(columns);

                commons.add(common);
                cursor.moveToNext();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            cursor.close();
        }

        return commons;
    }
    public Cursor CustomQueryForAdapter(String[] columns,String tableName,String limit,String offset){

        SQLiteDatabase database = masterRepository.getReadableDatabase();
    Cursor cursor = database.query(tableName, columns, null, null, null, null, null, offset + "," + limit);

        return cursor;
    }
    public Cursor RawCustomQueryForAdapter(String query){
        SQLiteDatabase database = masterRepository.getReadableDatabase();
        Cursor cursor = database.rawQuery(query, null);
        return cursor;
    }
    public CommonPersonObject readAllcommonforCursorAdapter (Cursor cursor) {
        int columncount = cursor.getColumnCount();
        HashMap <String, String> columns = new HashMap<String, String>();
        for (int i = 0;i < columncount;i++ ){
            String cname = cursor.getColumnName(i);
            if(!cname.equalsIgnoreCase("details")) {
                columns.put(cname, cursor.getString(i));
            }
        }

        CommonPersonObject common = new CommonPersonObject(columns.get(ID_COLUMN), columns.get(Relational_ID),
                new Gson().<Map<String, String>>fromJson(cursor.getString(cursor.getColumnIndex(DETAILS_COLUMN)), new TypeToken<Map<String, String>>() {
        }.getType()),TABLE_NAME);

        common.setColumnmaps(columns);
        return common;
    }

    public ContentValues populateSearchValues(String caseId){
        CommonPersonObject commonPersonObject = findByCaseID(caseId);
        if (commonPersonObject == null) {
            return null;
        }

        try {
            Map<String, String> columnMaps = commonPersonObject.getColumnmaps();
            String programClientId = withSub(columnMaps.get("program_client_id"));
            String epiCardNumber = withSub(columnMaps.get("epi_card_number"));
            String firstName = withSub(columnMaps.get("first_name"));
            String lastName = withSub(columnMaps.get("last_name"));
            String fatherName = withSub(columnMaps.get("father_name"));
            String motherName = withSub(columnMaps.get("mother_name"));
            String husbandName = withSub(columnMaps.get("husband_name"));
            String phoneNumber = withSub(columnMaps.get("contact_phone_number"));

            String motherOrHusbandName = "";
            if (TABLE_NAME.equals("pkchild"))
                motherOrHusbandName = motherName;
            else
                motherOrHusbandName = husbandName;

            String phraseSeparator = " | ";
            String phrase  = programClientId + phraseSeparator + epiCardNumber + phraseSeparator +firstName + phraseSeparator + lastName + phraseSeparator + fatherName + phraseSeparator + motherOrHusbandName + phraseSeparator + phoneNumber;

            ContentValues searchValues = new ContentValues();
            searchValues.put("phrase", phrase);

            String firstNameSort = columnMaps.get("first_name") == null ? "" : columnMaps.get("first_name");
            String dobSort = columnMaps.get("dob") == null ? "" : columnMaps.get("dob");
            String programClientIdSort = columnMaps.get("program_client_id") == null ? "" : columnMaps.get("program_client_id");

            searchValues.put("first_name", firstNameSort);
            searchValues.put("dob", dobSort);
            searchValues.put("program_client_id", programClientIdSort);

            return searchValues;
        }catch (Exception e){
            Log.e("", "Update Search Error", e);
            return null;
        }
    }

    public boolean searchBatchInserts(Map<String, ContentValues> searchMap){
        SQLiteDatabase database = masterRepository.getWritableDatabase();

        database.beginTransaction();
        try {
            for(String caseId: searchMap.keySet()) {
                String[] args = {caseId, TABLE_NAME};
                ContentValues searchValues = searchMap.get(caseId);
                ArrayList<HashMap<String, String>> mapList = rawQuery(String.format("SELECT search_rowid FROM search_relations WHERE object_id = '%s' AND object_type = '%s'", caseId, TABLE_NAME));
                if (!mapList.isEmpty()) {
                    String searchRowId = mapList.get(0).get("search_rowid");
                    database.update("search", searchValues, " rowid = ?", new String[]{String.valueOf(searchRowId)});

                } else {
                    long searchRowId = database.insert("search", null, searchValues);
                    ContentValues searchRelationsValues = new ContentValues();
                    searchRelationsValues.put("search_rowid", searchRowId);
                    searchRelationsValues.put("object_id", caseId);
                    searchRelationsValues.put("object_type", TABLE_NAME);
                    database.insert("search_relations", null, searchRelationsValues);
                }
            }
            database.setTransactionSuccessful();
            database.endTransaction();

            return true;
        }catch (Exception e){
            Log.e("", "Update Search Error", e);
            database.endTransaction();
            return false;
        }
    }

    public List<String> findSearchIds(String query){

        SQLiteDatabase database = masterRepository.getReadableDatabase();

        Log.i(getClass().getName(), query);
        Cursor cursor = database.rawQuery(query, null);

        List<String> ids  = new ArrayList<String>();

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(0);
                ids.add(id);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return ids;
    }

    public String[] ftsTables(){
        String[] ftsTables = {"pkchild", "pkwoman"};
        return ftsTables;
    }

    private String withSub(String s){
        String withSub = "";
        if(s == null || s.isEmpty()){
            return withSub;
        }
        int length = s.length();

        for (int i = 0; i < length; i++) {
            withSub += s.substring(i) + " ";
        }
        return withSub.trim();
    }
}
