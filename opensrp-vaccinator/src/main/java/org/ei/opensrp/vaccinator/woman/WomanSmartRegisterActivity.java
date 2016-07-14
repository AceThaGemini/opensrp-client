package org.ei.opensrp.vaccinator.woman;

import org.ei.opensrp.vaccinator.application.common.SmartClientRegisterFragment;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.template.SmartRegisterSecuredActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by muhammad.ahmed@ihsinformatics.com on 13-Oct-15.
 */
public class WomanSmartRegisterActivity extends SmartRegisterSecuredActivity {

    @Override
    public SmartClientRegisterFragment getBaseFragment() {
        return new WomanSmartRegisterFragment(new FormController(this));
    }

    protected String[] buildFormNameList(){
        List<String> formNames = new ArrayList<String>();
        formNames.add("woman_enrollment");
        formNames.add("woman_followup");
        formNames.add("offsite_woman_followup");

        return formNames.toArray(new String[formNames.size()]);
    }

    @Override
    protected void onResumption() {

    }
}
