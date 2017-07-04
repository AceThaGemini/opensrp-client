package org.ei.opensrp.core.db.repository;

import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObject;
import org.ei.opensrp.core.db.domain.ClientEvent;
import org.ei.opensrp.core.utils.Utils;
import org.ei.opensrp.domain.ProfileImage;
import org.ei.opensrp.repository.ImageRepository;
import org.ei.opensrp.repository.ReportRepository;
import org.json.JSONException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ei.opensrp.commonregistry.CommonRepository.DETAILS_COLUMN;
import static org.ei.opensrp.commonregistry.CommonRepository.ID_COLUMN;
import static org.ei.opensrp.commonregistry.CommonRepository.Relational_ID;

/**
 * Created by Maimoona on 1/9/2017.
 */
public class RegisterRepository {
    public static Cursor query(String table, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        return Context.getInstance().commonrepository(table).getMasterRepository().getReadableDatabase().query(table, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public static Cursor queryCE(String table, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        return getDatabase().query(table, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public static Cursor queryCE(String addressType, String selection, String group, String sortOrder) {
        return CESQLiteHelper.getClientEventDb(Context.getInstance().applicationContext()).getClientEventCursor(true, selection, addressType, group, sortOrder);
    }

    public static int countCE(String addressType, String selection, String group) {
        try {
            return CESQLiteHelper.getClientEventDb(Context.getInstance().applicationContext()).getClientEventCount(true, selection, addressType, group);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Cursor queryCELeftJoin(String table, String id, String referenceTable, String referenceColumn,
                                       String[] projection, String selection, String groupBy, String sortOrder){
        String sql = SQLiteQueryBuilder.buildQueryString(false, table, projection, selection, groupBy, null, sortOrder, null);
        // left join pkindividual on pkhousehold.id=pkindividual.relationalid
        String[] sl = sql.split("(?i)from[\\s]+"+table);
        Log.v(ReportRepository.class.getName(), "SQL:"+sql+"::SL:"+sl);
        String join = " LEFT JOIN "+referenceTable+" ON "+referenceTable+"."+referenceColumn+"="+table+"."+id;

        String finalSql = sl[0]+" FROM "+table+" "+join+" "+sl[1];

        return getDatabase().rawQuery(finalSql, null);
    }

    public static Cursor queryLeftJoin(String table, String id, String referenceTable, String referenceColumn,
                               String[] projection, String selection, String groupBy, String sortOrder){
        String sql = SQLiteQueryBuilder.buildQueryString(false, table, projection, selection, groupBy, null, sortOrder, null);
        // left join pkindividual on pkhousehold.id=pkindividual.relationalid
        String[] sl = sql.split("(?i)from[\\s]+"+table);
        Log.v(ReportRepository.class.getName(), "SQL:"+sql+"::SL:"+sl);
        String join = " LEFT JOIN "+referenceTable+" ON "+referenceTable+"."+referenceColumn+"="+table+"."+id;

        String finalSql = sl[0]+" FROM "+table+" "+join+" "+sl[1];

        return Context.getInstance().commonrepository(table).getMasterRepository().getReadableDatabase().rawQuery(finalSql, null);
    }

    public static List<CommonPersonObject> queryData(String table, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        Cursor c = query(table, projection, selection, selectionArgs, sortOrder);
        return readList(c);
    }

    public static List<CommonPersonObject> rawQueryData(String table, String sql){
        Cursor c = Context.getInstance().commonrepository(table).getMasterRepository().getReadableDatabase().rawQuery(sql, null);
        return readList(c);
    }

    public static void createTemporaryTable(String contextTable, String table, String sql){
        Context.getInstance().commonrepository(contextTable).getMasterRepository().getWritableDatabase()
                .execSQL("CREATE TEMPORARY TABLE IF NOT EXISTS "+table+" AS "+sql);
    }

    public static int count(String table, String selection, String[] selectionArgs){
        Cursor cursor = getDatabase(table).query(table, new String[]{"COUNT(1) c"}, selection, selectionArgs, null, null, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public static int countCE(String table, String selection){
        Cursor cursor = getDatabase().query(table, new String[]{"COUNT(1) c"}, selection, null, null, null, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public static ProfileImage findImageByEntityId(String bindType, String entityId, String type) {
        SQLiteDatabase database = getDatabase(bindType);
        Cursor cursor = database.query(ImageRepository.Image_TABLE_NAME, ImageRepository.Image_TABLE_COLUMNS,
                ImageRepository.entityID_COLUMN + " = ? AND " + ImageRepository.filecategory_COLUMN + " = ? ",
                new String[]{entityId, type}, null, null, ImageRepository.ID_COLUMN, null);
        List<ProfileImage> l = readAllImages(cursor);
        return l.size() == 0?null :l.get(l.size()-1);
    }

    private static SQLiteDatabase getDatabase(String bindType){
        return Context.getInstance().commonrepository(bindType).getMasterRepository().getReadableDatabase();
    }

    private static android.database.sqlite.SQLiteDatabase getDatabase(){
        return CESQLiteHelper.getClientEventDb(Context.getInstance().applicationContext()).getReadableDatabase();
    }

    private static List<ProfileImage> readAllImages(Cursor cursor) {
        cursor.moveToFirst();
        List<ProfileImage> ProfileImages = new ArrayList<ProfileImage>();
        while (!cursor.isAfterLast()) {

            ProfileImages.add(new ProfileImage(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),cursor.getString(4),cursor.getString(5),cursor.getString(6)));

            cursor.moveToNext();
        }
        cursor.close();
        return ProfileImages;
    }

    public static CommonPersonObject convertToCommonObject (Cursor cursor) {
        int columncount = cursor.getColumnCount();
        HashMap<String, String> columns = new HashMap<String, String>();
        for (int i = 0;i < columncount;i++ ){
            String cname = cursor.getColumnName(i);
            if(!cname.equalsIgnoreCase("details")) {
                columns.put(cname, cursor.getString(i));
            }
        }

        String id = Utils.nonEmptyValue(columns, true, false, "_id", "_ID", "_Id", "baseEntityId");
        String relationalid = Utils.nonEmptyValue(columns, true, false, "relationalid", "_relationalid", "relationalId", "RELATIONAL_ID");
        String details = cursor.getColumnIndex(DETAILS_COLUMN)!= -1?cursor.getString(cursor.getColumnIndex(DETAILS_COLUMN)):"{}";
        CommonPersonObject common = new CommonPersonObject(id, relationalid,
                new Gson().<Map<String, String>>fromJson(details, new TypeToken<Map<String, String>>() {
                }.getType()), null);

        common.setColumnmaps(columns);
        return common;
    }

    public static ClientEvent convertToClientEvent(Cursor res) {
        try {
            return CESQLiteHelper.convertToClientEvent(res);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static List<CommonPersonObject> readList(Cursor cursor) {
        cursor.moveToFirst();
        List<CommonPersonObject> commons = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            commons.add(convertToCommonObject(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return commons;
    }
}