package org.ei.opensrp.path.sync;

import android.content.Context;
import android.util.Log;

import org.ei.opensrp.domain.Response;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.repository.PathRepository;
import org.ei.opensrp.service.HTTPAgent;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import util.Utils;

public class ECSyncUpdater {
    public static final String SEARCH_URL = "/rest/event/sync";

    public static final String LAST_SYNC_TIMESTAMP = "LAST_SYNC_TIMESTAMP";
    public static final String LAST_CHECK_TIMESTAMP = "LAST_SYNC_CHECK_TIMESTAMP";

    private PathRepository db;
    private Context context;

    private static ECSyncUpdater instance;

    public static ECSyncUpdater getInstance(Context context) {
        if (instance == null) {
            instance = new ECSyncUpdater(context);
        }
        return instance;
    }

    public ECSyncUpdater(Context context) {
        this.context = context;
        db = (PathRepository) VaccinatorApplication.getInstance().getRepository();
    }


    private JSONObject fetchAsJsonObject(String filter, String filterValue) {
        try {
            HTTPAgent httpAgent = org.ei.opensrp.Context.getInstance().getHttpAgent();
            String baseUrl = org.ei.opensrp.Context.getInstance().configuration().dristhiBaseURL();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            }

            Long lastSyncDatetime = getLastSyncTimeStamp();
            Log.i(ECSyncUpdater.class.getName(), "LAST SYNC DT :" + new DateTime(lastSyncDatetime));

            String url = baseUrl + SEARCH_URL + "?" + filter + "=" + filterValue + "&serverVersion=" + lastSyncDatetime;
            Log.i(ECSyncUpdater.class.getName(), "URL: " + url);

            if (httpAgent == null) {
                throw new Exception(SEARCH_URL + " http agent is null");
            }

            Response resp = httpAgent.fetch(url);
            if (resp.isFailure()) {
                throw new Exception(SEARCH_URL + " not returned data");
            }

            JSONObject jsonObject = new JSONObject((String) resp.payload());
            return jsonObject;
        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
            return new JSONObject();
        }
    }

    public int fetchAllClientsAndEvents(String filterName, String filterValue) {
        try {

            JSONObject jsonObject = fetchAsJsonObject(filterName, filterValue);

            int eventsCount = jsonObject.has("no_of_events") ? jsonObject.getInt("no_of_events"): 0;
            if(eventsCount == 0 ){
                return eventsCount;
            }

            JSONArray events = jsonObject.has("events") ? jsonObject.getJSONArray("events") : new JSONArray();
            JSONArray clients = jsonObject.has("clients") ? jsonObject.getJSONArray("clients") : new JSONArray();

            long lastSyncTimeStamp = batchSave(events, clients);
            if (lastSyncTimeStamp > 0l) {
               updateLastSyncTimeStamp(lastSyncTimeStamp);
            }

            return eventsCount;

        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
            return -1;
        }
    }

    public List<JSONObject> allEvents(long startSyncTimeStamp, long lastSyncTimeStamp) {
        try {
            return db.getEvents(startSyncTimeStamp, lastSyncTimeStamp);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception", e);
        }
        return new ArrayList<>();
    }

    public long getLastSyncTimeStamp(){
        return Long.parseLong(Utils.getPreference(context, LAST_SYNC_TIMESTAMP, "0"));
    }

    public void updateLastSyncTimeStamp(long lastSyncTimeStamp){
        Utils.writePreference(context, LAST_SYNC_TIMESTAMP, lastSyncTimeStamp + "");
    }

    public long getLastCheckTimeStamp(){
        return Long.parseLong(Utils.getPreference(context, LAST_CHECK_TIMESTAMP, "0"));
    }

    public void updateLastCheckTimeStamp(long lastSyncTimeStamp){
        Utils.writePreference(context, LAST_CHECK_TIMESTAMP, lastSyncTimeStamp + "");
    }

    private long batchSave(JSONArray events, JSONArray clients) throws Exception{
        db.batchInsertClients(clients);
        return db.batchInsertEvents(events, getLastSyncTimeStamp());
    }



}