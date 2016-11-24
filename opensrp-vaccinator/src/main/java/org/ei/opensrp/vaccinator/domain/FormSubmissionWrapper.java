package org.ei.opensrp.vaccinator.domain;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.vaccinator.db.VaccineRepo;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.json.XML;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import util.VaccinateActionUtils;

/**
 * Created by keyman on 22/11/2016.
 */
public class FormSubmissionWrapper implements Serializable {

    String formData;

    String entityId;

    String formName;

    String metaData;

    String category;

    HashMap<VaccineRepo.Vaccine, VaccineWrapper> map;

    public FormSubmissionWrapper(String formData, String entityId, String formName, String metaData, String category) {
        this.formData = formData;
        this.entityId = entityId;
        this.formName = formName;
        this.metaData = metaData;
        this.category = category;
    }

    public void add(VaccineWrapper tag) {
        if (tag.getUpdatedVaccineDate() != null) {
            map().put(tag.getVaccine(), tag);
        }
    }

    public void remove(VaccineWrapper tag) {
        map().remove(tag.getVaccine());
    }

    public int updates() {
        return map().size();
    }

    public HashMap<VaccineRepo.Vaccine, VaccineWrapper> map() {
        if (map == null) {
            map = new HashMap<VaccineRepo.Vaccine, VaccineWrapper>();
        }
        return map;
    }

    public String updateFormSubmission() {
        try {
            if (updates() <= 0)
                return null;


            String parent = "";
            if (category.equals("child")) {
                parent = "Child_Vaccination_Followup";
            } else if (category.equals("woman")) {
                parent = "Woman_TT_Followup_Form";
            }

            JSONObject formSubmission = XML.toJSONObject(formData);

            JSONObject encounterJson = VaccinateActionUtils.find(formSubmission, parent);

            String vaccines = "";
            String vaccines2 = "";

            for (Map.Entry<VaccineRepo.Vaccine, VaccineWrapper> entry : map().entrySet()) {
                VaccineRepo.Vaccine vaccine = entry.getKey();

                VaccineWrapper tag = entry.getValue();
                String formatedDate = tag.getUpdatedVaccineDate().toString("yyyy-MM-dd");

                String lastChar = vaccine.name().substring(vaccine.name().length() - 1);

                if (!tag.isToday()) {
                    String fieldName = vaccine.name() + "_retro";

                    JSONObject parentJson = null;
                    if (category.equals("child")) {
                        parentJson = VaccinateActionUtils.find(encounterJson, "vaccines_group");
                    } else if (category.equals("woman")) {
                        parentJson = encounterJson;
                    }

                    if(parentJson != null) {
                        VaccinateActionUtils.updateJson(parentJson, fieldName, formatedDate);
                        if (StringUtils.isNumeric(lastChar)) {
                            VaccinateActionUtils.updateJson(parentJson, vaccine.name() + "_dose", lastChar);
                        }
                        vaccines += vaccine.name() + " ";
                    }
                } else {
                    VaccinateActionUtils.updateJson(encounterJson, vaccine.name(), formatedDate);
                    if (StringUtils.isNumeric(lastChar)) {
                        VaccinateActionUtils.updateJson(encounterJson, vaccine.name() + "_dose_today", lastChar);
                    }
                    vaccines2 += vaccine.name() + " ";
                }

            }

            if (StringUtils.isNotBlank(vaccines)) {
                vaccines.trim();
                VaccinateActionUtils.updateJson(encounterJson, "vaccines", vaccines);
            }

            if (StringUtils.isNotBlank(vaccines2)) {
                vaccines2.trim();
                VaccinateActionUtils.updateJson(encounterJson, "vaccines_2", vaccines2);
            }

            VaccinateActionUtils.updateJson(encounterJson, "address_change", "no");
            VaccinateActionUtils.updateJson(encounterJson, "reminders_approval", "no");

            DateTime currentDateTime = new DateTime(new Date());
            VaccinateActionUtils.updateJson(encounterJson, "start", currentDateTime.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
            VaccinateActionUtils.updateJson(encounterJson, "end", currentDateTime.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
            VaccinateActionUtils.updateJson(encounterJson, "today", currentDateTime.toString("yyyy-MM-dd"));

            VaccinateActionUtils.updateJson(encounterJson, "deviceid", "Error: could not determine deviceID");
            VaccinateActionUtils.updateJson(encounterJson, "subscriberid", "no subscriberid property in enketo");
            VaccinateActionUtils.updateJson(encounterJson, "simserial", "no simserial property in enketo");
            VaccinateActionUtils.updateJson(encounterJson, "phonenumber", "no phonenumber property in enketo");


            String data = XML.toString(formSubmission);
            return data;
        } catch (Exception e) {
            Log.e(FormSubmissionWrapper.class.getName(), "", e);
        }
        return null;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getFormName() {
        return formName;
    }

    public JSONObject getOverrides() {
        return VaccinateActionUtils.retrieveFieldOverides(metaData);
    }

}
