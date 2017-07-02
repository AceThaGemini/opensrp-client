package org.ei.opensrp.path.widgets;

import android.content.Context;
import android.view.View;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.widgets.EditTextFactory;

import org.ei.opensrp.path.db.VaccineRepo;
import org.ei.opensrp.path.watchers.LookUpTextWatcher;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by keyman on 11/04/2017.
 */
public class PathEditTextFactory extends EditTextFactory {

    @Override
    public void attachJson(String stepName, Context context, JsonFormFragment formFragment, JSONObject jsonObject, MaterialEditText editText) throws Exception {
        super.attachJson(stepName, context, formFragment, jsonObject, editText);

        // lookup hook
        if (jsonObject.has("look_up") && jsonObject.get("look_up").toString().equalsIgnoreCase(Boolean.TRUE.toString())) {

            String entityId = jsonObject.getString("entity_id");

            Map<String, List<View>> lookupMap = formFragment.getLookUpMap();
            List<View> lookUpViews = new ArrayList<>();
            if (lookupMap.containsKey(entityId)) {
                lookUpViews = lookupMap.get(entityId);
            }

            if (!lookUpViews.contains(editText)) {
                lookUpViews.add(editText);
            }
            lookupMap.put(entityId, lookUpViews);

            editText.addTextChangedListener(new LookUpTextWatcher(formFragment, editText, entityId));
            editText.setTag(com.vijay.jsonwizard.R.id.after_look_up, false);
        }

    }

}