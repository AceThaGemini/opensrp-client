package org.ei.opensrp.cursoradapter;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by raihan on 3/17/16.
 */
public class SmartRegisterQueryBuilder {
    private String Selectquery;
    private String table;
    private String mainFilter = "";
    private String order = "";
    private String joins = "";
    private int pageSize = 20;
    private int offset = 0;

    public String table() {
        return table;
    }

    public String mainFilter() {
        return mainFilter;
    }

    public String order() {
        return order;
    }

    public String joins() {
        return joins;
    }

    public int pageSize() {
        return pageSize;
    }

    public int offset() {
        return offset;
    }

    /**
     * @param table
     * @param mainFilter optional
     */
    public SmartRegisterQueryBuilder(String table, String mainFilter) {
        Selectquery = "SELECT id AS _id, t.* FROM "+table+" t ";
        this.table = table;
        if(StringUtils.isNotBlank(mainFilter)){
            this.mainFilter = mainFilter;
        }
    }

    /**
     * @param table
     * @param columns
     * @param mainFilter optional
     */
    public SmartRegisterQueryBuilder(String table, String [] columns, String mainFilter){
        this.table = table;
        if(StringUtils.isNotBlank(mainFilter)){
            this.mainFilter = mainFilter;
        }        Selectquery = "SELECT id AS _id";
        for(int i = 0;i<columns.length;i++){
            Selectquery= Selectquery + " , " + columns[i];
        }
        Selectquery = Selectquery+ " FROM " + table +" t ";
    }

    /*
            This method takes in @param tablename and columns other than ID. Any special conditions
            for sorting if required can also be added in condition string and if not you can pass null.
            Alertname is the name of the alert you would like to sort this by.
             */
    public  String queryForRegisterSortBasedOnRegisterAndAlert(String tablename,String[]columns,String condition,String AlertName){
        Selectquery = "Select id as _id";
        for(int i = 0;i<columns.length;i++){
            Selectquery= Selectquery + " , " + columns[i];
        }
        Selectquery= Selectquery+ " From " + tablename;
        Selectquery = Selectquery+ " LEFT JOIN alerts ";
        Selectquery = Selectquery+ " ON "+ tablename +".id = alerts.caseID";
        if(condition != null){
            Selectquery= Selectquery+ " Where " + condition + " AND";
        }
        Selectquery= Selectquery+ " Where " + "alerts.scheduleName = '" + AlertName + "' ";
        Selectquery = Selectquery + "ORDER BY CASE WHEN alerts.status = 'urgent' THEN '1'\n" +
                "WHEN alerts.status = 'upcoming' THEN '2'\n" +
                "WHEN alerts.status = 'normal' THEN '3'\n" +
                "WHEN alerts.status = 'expired' THEN '4'\n" +
                "WHEN alerts.status is Null THEN '5'\n" +
                "Else alerts.status END ASC";
        return Selectquery;
    }

    public SmartRegisterQueryBuilder limitAndOffset(int limit, int offset){
        this.pageSize = limit;
        this.offset = offset;
        return this;
    }

    public SmartRegisterQueryBuilder addCondition(String condition){
        mainFilter += condition ;
        return this;
    }
    public SmartRegisterQueryBuilder addOrder(String orderCondition){
        if (StringUtils.isNotBlank(order)){
            this.order += ", "+orderCondition;
        }
        else {
            this.order = orderCondition;
        }
        return this;
    }
    public SmartRegisterQueryBuilder joinwithALerts(String alertname){
        joins += " LEFT JOIN alerts ON t._id = alerts.caseID and alerts.scheduleName = '"+alertname+"'" ;
        return this;
    }
    public SmartRegisterQueryBuilder joinwithALerts(){
        joins += " LEFT JOIN alerts ON t._id = alerts.caseID " ;
        return this;
    }

    @Override
    public String toString(){
        String res = Selectquery;
        if (StringUtils.isNotBlank(joins)){
            res += " "+ joins;
        }
        if (StringUtils.isNotBlank(mainFilter)){
            res += " WHERE "+mainFilter;
        }
        if (StringUtils.isNotBlank(order)){
            res += " ORDER BY " + order;
        }

        res += " LIMIT "+offset+","+pageSize;

        return res;
    }

    public String countQuery(){
        String res = "SELECT COUNT(*) FROM "+table;
        if (StringUtils.isNotBlank(joins)){
            res += " "+ joins;
        }
        if (StringUtils.isNotBlank(mainFilter)){
            res += " WHERE "+mainFilter;
        }

        return res;
    }
}