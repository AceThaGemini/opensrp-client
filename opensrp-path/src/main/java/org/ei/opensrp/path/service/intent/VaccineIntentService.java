package org.ei.opensrp.path.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.ei.opensrp.Context;
import org.ei.opensrp.domain.Vaccine;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.repository.VaccineRepository;
import org.ei.opensrp.path.repository.WeightRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;

import util.JsonFormUtils;
import util.VaccinatorUtils;


/**
 * Created by keyman on 3/01/2017.
 */
public class VaccineIntentService extends IntentService {
    private static final String TAG = VaccineIntentService.class.getCanonicalName();
    public static final String EVENT_TYPE = "Vaccination";
    public static final String ENTITY_TYPE = "vaccination";
    private VaccineRepository vaccineRepository;
    private JSONArray availableVaccines;

    public VaccineIntentService() {
        super("VaccineService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (availableVaccines == null) {
            try {
                availableVaccines = new JSONArray(VaccinatorUtils.getSupportedVaccines(this));
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        final String entityId = "1410AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final String calId = "1418AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final String dateDataType = "date";
        final String calculationDataType = "calculate";
        final String concept = "concept";

        try {
            List<Vaccine> vaccines = vaccineRepository.findUnSyncedBeforeTime(24);
            if (!vaccines.isEmpty()) {
                for (Vaccine vaccine : vaccines) {

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String formattedDate = simpleDateFormat.format(vaccine.getDate());

                    JSONArray jsonArray = new JSONArray();

                    String vaccineName = vaccine.getName().replace(" ", "_");

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(JsonFormUtils.KEY, vaccineName);
                    jsonObject.put(JsonFormUtils.OPENMRS_ENTITY, concept);
                    jsonObject.put(JsonFormUtils.OPENMRS_ENTITY_ID, entityId);
                    jsonObject.put(JsonFormUtils.OPENMRS_ENTITY_PARENT, getParentId(vaccine.getName()));
                    jsonObject.put(JsonFormUtils.OPENMRS_DATA_TYPE, dateDataType);
                    jsonObject.put(JsonFormUtils.VALUE, formattedDate);
                    jsonArray.put(jsonObject);

                    if (vaccine.getCalculation() != null && vaccine.getCalculation().intValue() >= 0) {
                        jsonObject = new JSONObject();
                        jsonObject.put(JsonFormUtils.KEY, vaccineName + "_dose");
                        jsonObject.put(JsonFormUtils.OPENMRS_ENTITY, concept);
                        jsonObject.put(JsonFormUtils.OPENMRS_ENTITY_ID, calId);
                        jsonObject.put(JsonFormUtils.OPENMRS_ENTITY_PARENT, getParentId(vaccine.getName()));
                        jsonObject.put(JsonFormUtils.OPENMRS_DATA_TYPE, calculationDataType);
                        jsonObject.put(JsonFormUtils.VALUE, vaccine.getCalculation());
                        jsonArray.put(jsonObject);
                    }
                    JsonFormUtils.createVaccineEvent(getApplicationContext(), vaccine, EVENT_TYPE, ENTITY_TYPE, jsonArray);
                    vaccineRepository.close(vaccine.getId());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private String getParentId(String name) {
        try {
            for (int i = 0; i < availableVaccines.length(); i++) {
                JSONObject curVaccineGroup = availableVaccines.getJSONObject(i);
                for (int j = 0; j < curVaccineGroup.getJSONArray("vaccines").length(); j++) {
                    if (curVaccineGroup.getJSONArray("vaccines").getJSONObject(j).getString("name")
                            .equalsIgnoreCase(name)) {
                        return curVaccineGroup.getJSONArray("vaccines").getJSONObject(j)
                                .getJSONObject("openmrs_date").getString("parent_entity");
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return "";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vaccineRepository = VaccinatorApplication.getInstance().vaccineRepository();
        return super.onStartCommand(intent, flags, startId);
    }
}
