package org.ei.opensrp.commonregistry;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by keyman on 29/08/16.
 */
public class CommonFtsObject {
    private String[] tables;
    private Map<String, String[]> searchMap;
    private Map<String, String[]> sortMap;
    public static final String idColumn = "object_id";
    public static final String phraseColumnName = "phrase";

    public CommonFtsObject(String[] tables) {
        this.tables = tables;
        this.searchMap = new HashMap<String, String[]>();
        this.sortMap = new HashMap<String, String[]>();
    }

    public void updateSearchFields(String table, String[] searchFields){
        if(containsTable(table) && searchFields != null){
            searchMap.put(table, searchFields);
        }
    }

    public void updateSortFields(String table, String[] sortFields){
        if(containsTable(table) && sortFields != null){
            sortMap.put(table, sortFields);
        }
    }

    public String[] getTables() {
        return tables;
    }

    public String[] getSearchFields(String table) {
        return searchMap.get(table);
    }

    public String[] getSortFields(String table) {
        return sortMap.get(table);
    }

    private boolean containsTable(String table){
        if(tables == null || StringUtils.isBlank(table)){
            return false;
        }

        List<String> tableList = Arrays.asList(table);
        return tableList.contains(table);
    }

    public static String searchTableName(String table){
        return table + "_search";
    }

}
