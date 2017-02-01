package org.ei.opensrp.path.fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.cursoradapter.CursorCommonObjectFilterOption;
import org.ei.opensrp.cursoradapter.CursorCommonObjectSort;
import org.ei.opensrp.cursoradapter.CursorSortOption;
import org.ei.opensrp.cursoradapter.SecuredNativeSmartRegisterCursorAdapterFragment;
import org.ei.opensrp.cursoradapter.SmartRegisterPaginatedCursorAdapter;
import org.ei.opensrp.cursoradapter.SmartRegisterQueryBuilder;
import org.ei.opensrp.path.R;
import org.ei.opensrp.path.activity.ChildDetailActivity;
import org.ei.opensrp.path.activity.ChildSmartRegisterActivity;
import org.ei.opensrp.path.activity.LoginActivity;
import org.ei.opensrp.path.db.Client;
import org.ei.opensrp.path.option.BasicSearchOption;
import org.ei.opensrp.path.option.DateSort;
import org.ei.opensrp.path.option.StatusSort;
import org.ei.opensrp.path.provider.ChildSmartClientsProvider;
import org.ei.opensrp.path.servicemode.VaccinationServiceModeOption;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.dialog.DialogOption;
import org.ei.opensrp.view.dialog.DialogOptionModel;
import org.ei.opensrp.view.dialog.EditOption;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.HashMap;
import java.util.Map;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static util.Utils.getValue;
import static util.Utils.nonEmptyValue;
import static util.VaccinatorUtils.providerDetails;

public class ChildSmartRegisterFragment extends SecuredNativeSmartRegisterCursorAdapterFragment {
    private final ClientActionHandler clientActionHandler = new ClientActionHandler();


    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.DefaultOptionsProvider() {
            // FIXME path_conflict
            //@Override
            public FilterOption searchFilterOption() {
                return new BasicSearchOption("", BasicSearchOption.Type.getByRegisterName(getDefaultOptionsProvider().nameInShortFormForTitle()));
            }

            @Override
            public ServiceModeOption serviceMode() {
                return new VaccinationServiceModeOption(null, "Vaccine", new int[]{
                        R.string.child_profile, R.string.birthdate_age, R.string.epi_number, R.string.child_contact_number,
                        R.string.child_last_vaccine, R.string.child_next_vaacine
                }, new int[]{6, 2, 2, 3, 4, 4});
            }

            @Override
            public FilterOption villageFilter() {
                return new CursorCommonObjectFilterOption("no village filter", "");
            }

            @Override
            public SortOption sortOption() {
                return new CursorCommonObjectSort(getResources().getString(R.string.woman_alphabetical_sort), "first_name");
            }

            @Override
            public String nameInShortFormForTitle() {
                return Context.getInstance().getStringResource(R.string.child_register_title);
            }
        };
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.NavBarOptionsProvider getNavBarOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.NavBarOptionsProvider() {

            @Override
            public DialogOption[] filterOptions() {
                return new DialogOption[]{};
            }

            @Override
            public DialogOption[] serviceModeOptions() {
                return new DialogOption[]{
                };
            }

            @Override
            public DialogOption[] sortingOptions() {
                return new DialogOption[]{
                        new CursorCommonObjectSort(getResources().getString(R.string.woman_alphabetical_sort), "first_name"),
                        new DateSort("Age", "dob"),
                        new StatusSort("Due Status"),
                        new CursorCommonObjectSort(getResources().getString(R.string.id_sort), "program_client_id")
                };
            }

            @Override
            public String searchHint() {
                return Context.getInstance().getStringResource(R.string.search_hint);
            }
        };
    }


    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        return null;
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    protected void startRegistration() {
        ((ChildSmartRegisterActivity) getActivity()).startFormActivity("child_enrollment", null, null);
    }

    @Override
    protected void onCreation() {
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        getDefaultOptionsProvider();
        if (isPausedOrRefreshList()) {
            initializeQueries();
        }
        updateSearchView();
        try {
            LoginActivity.setLanguage();
        } catch (Exception e) {

        }

    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        view.findViewById(R.id.btn_report_month).setVisibility(INVISIBLE);
        view.findViewById(R.id.service_mode_selection).setVisibility(INVISIBLE);

        clientsView.setVisibility(View.VISIBLE);
        clientsProgressView.setVisibility(View.INVISIBLE);
        setServiceModeViewDrawableRight(null);
        initializeQueries();
        updateSearchView();
    }

    public void initializeQueries() {
        String tableName = "pkchild";

        ChildSmartClientsProvider hhscp = new ChildSmartClientsProvider(getActivity(),
                clientActionHandler, context().alertService());
        clientAdapter = new SmartRegisterPaginatedCursorAdapter(getActivity(), null, hhscp, Context.getInstance().commonrepository(tableName));
        clientsView.setAdapter(clientAdapter);

        setTablename(tableName);
        SmartRegisterQueryBuilder countqueryBUilder = new SmartRegisterQueryBuilder();
        countqueryBUilder.SelectInitiateMainTableCounts(tableName);
        countSelect = countqueryBUilder.mainCondition("");
        mainCondition = "";
        super.CountExecute();

        SmartRegisterQueryBuilder queryBUilder = new SmartRegisterQueryBuilder();
        queryBUilder.SelectInitiateMainTable(tableName, new String[]{"relationalid", "details", "program_client_id", "first_name", "last_name", "gender", "mother_name", "father_name", "dob", "epi_card_number", "contact_phone_number", "provider_uc", "provider_town", "provider_id", "provider_location_id", "client_reg_date", "vaccines_2", "bcg", "opv0", "pcv1", "opv1", "penta1", "pcv2", "opv2", "penta2", "pcv3", "opv3", "penta3", "ipv", "measles1", "measles2", "bcg_retro", "opv0_retro", "pcv1_retro", "opv1_retro", "penta1_retro", "pcv2_retro", "opv2_retro", "penta2_retro", "pcv3_retro", "opv3_retro", "penta3_retro", "ipv_retro", "measles1_retro", "measles2_retro" });
        mainSelect = queryBUilder.mainCondition("");
        Sortqueries = ((CursorSortOption) getDefaultOptionsProvider().sortOption()).sort();

        currentlimit = 20;
        currentoffset = 0;

        super.filterandSortInInitializeQueries();

        updateSearchView();
        refresh();
    }

    private class ClientActionHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
            HashMap<String, String> map = new HashMap<>();
            map.putAll(followupOverrides(client));
            map.putAll(providerDetails());

            String formName = "child_followup";
            String registerFormName =  "child_enrollment";

            switch (view.getId()) {
                case R.id.child_profile_info_layout:
                    ChildDetailActivity.startDetailActivity(getActivity(), (CommonPersonObjectClient) view.getTag(), map, formName, registerFormName, ChildDetailActivity.class);
                    getActivity().finish();
                    break;
                case R.id.child_next_visit_holder:
                    showFragmentDialog(new EditDialogOptionModel(map), view.getTag());
                    break;
            }
        }
    }

    public void updateSearchView() {
        getSearchView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(final CharSequence cs, int start, int before, int count) {

                if (cs.toString().equalsIgnoreCase("")) {
                    filters = "";
                } else {
                    filters = cs.toString();
                }
                joinTable = "";
                mainCondition = "";
                getSearchCancelView().setVisibility(isEmpty(cs) ? INVISIBLE : VISIBLE);
                CountExecute();
                filterandSortExecute();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private class EditDialogOptionModel implements DialogOptionModel {
        HashMap<String, String> map;

        EditDialogOptionModel(HashMap<String, String> map) {
            this.map = map;
        }

        @Override
        public DialogOption[] getDialogOptions() {
            return getEditOptions(map);
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onEditSelection((EditOption) option, (SmartRegisterClient) tag);
        }

    }

    private DialogOption[] getEditOptions(HashMap<String, String> map) {
        return ((ChildSmartRegisterActivity) getActivity()).getEditOptions(map);
    }

    private HashMap<String, String> getPreloadVaccineData(CommonPersonObjectClient client) {
        HashMap<String, String> map = new HashMap<>();
        map.put("e_bcg", nonEmptyValue(client.getColumnmaps(), true, false, "bcg", "bcg_retro"));
        map.put("e_opv0", nonEmptyValue(client.getColumnmaps(), true, false, "opv0", "opv0_retro"));
        map.put("e_penta1", nonEmptyValue(client.getColumnmaps(), true, false, "penta1", "penta1_retro"));
        map.put("e_opv1", nonEmptyValue(client.getColumnmaps(), true, false, "opv1", "opv1_retro"));
        map.put("e_pcv1", nonEmptyValue(client.getColumnmaps(), true, false, "pcv1", "pcv1_retro"));
        map.put("e_penta2", nonEmptyValue(client.getColumnmaps(), true, false, "penta2", "penta2_retro"));
        map.put("e_opv2", nonEmptyValue(client.getColumnmaps(), true, false, "opv2", "opv2_retro"));
        map.put("e_pcv2", nonEmptyValue(client.getColumnmaps(), true, false, "pcv2", "pcv2_retro"));
        map.put("e_penta3", nonEmptyValue(client.getColumnmaps(), true, false, "penta3", "penta3_retro"));
        map.put("e_opv3", nonEmptyValue(client.getColumnmaps(), true, false, "opv3", "opv3_retro"));
        map.put("e_pcv3", nonEmptyValue(client.getColumnmaps(), true, false, "pcv3", "pcv3_retro"));
        map.put("e_ipv", nonEmptyValue(client.getColumnmaps(), true, false, "ipv", "ipv_retro"));
        map.put("e_measles1", nonEmptyValue(client.getColumnmaps(), true, false, "measles1", "measles1_retro"));
        map.put("e_measles2", nonEmptyValue(client.getColumnmaps(), true, false, "measles2", "measles2_retro"));
        return map;
    }

    //TODO EC model
    /*
    private HashMap<String, String> getPreloadVaccineData(Client client) throws JSONException, ParseException {
        HashMap<String, String> map = new HashMap<>();
        map.put("e_bcg", getObsValue(getClientEventDb(), client, true, "bcg", "bcg_retro"));
        map.put("e_opv0", getObsValue(getClientEventDb(), client, true, "opv0", "opv0_retro"));
        map.put("e_penta1", getObsValue(getClientEventDb(), client, true, "penta1", "penta1_retro"));
        map.put("e_opv1", getObsValue(getClientEventDb(), client, true, "opv1", "opv1_retro"));
        map.put("e_pcv1", getObsValue(getClientEventDb(), client, true, "pcv1", "pcv1_retro"));
        map.put("e_penta2", getObsValue(getClientEventDb(), client, true, "penta2", "penta2_retro"));
        map.put("e_opv2", getObsValue(getClientEventDb(), client, true, "opv2", "opv2_retro"));
        map.put("e_pcv2", getObsValue(getClientEventDb(), client, true, "pcv2", "pcv2_retro"));
        map.put("e_penta3", getObsValue(getClientEventDb(), client, true, "penta3", "penta3_retro"));
        map.put("e_opv3", getObsValue(getClientEventDb(), client, true, "opv3", "opv3_retro"));
        map.put("e_pcv3", getObsValue(getClientEventDb(), client, true, "pcv3", "pcv3_retro"));
        map.put("e_ipv", getObsValue(getClientEventDb(), client, true, "ipv", "ipv_retro"));
        map.put("e_measles1", getObsValue(getClientEventDb(), client, true, "measles1", "measles1_retro"));
        map.put("e_measles2", getObsValue(getClientEventDb(), client, true, "measles2", "measles2_retro"));

        return map;
    }*/

    //TODO path_conflict
    //@Override
    protected String getRegistrationForm(HashMap<String, String> overrides) {
        return "child_enrollment";
    }

    //@Override
    protected String getOAFollowupForm(Client client, HashMap<String, String> overridemap) {
        overridemap.putAll(followupOverrides(client));
        return "offsite_child_followup";
    }

    //@Override
    protected Map<String, String> customFieldOverrides() {
        Map<String, String> m = new HashMap<>();
        return m;
    }

    private Map<String, String> followupOverrides(CommonPersonObjectClient client) {
        Map<String, String> map = new HashMap<>();
        map.put("existing_full_address", getValue(client.getColumnmaps(), "address1", true)
                + ", UC: " + getValue(client.getColumnmaps(), "union_council", true)
                + ", Town: " + getValue(client.getColumnmaps(), "town", true)
                + ", City: " + getValue(client, "city_village", true)
                + ", Province: " + getValue(client, "province", true));
        map.put("existing_program_client_id", getValue(client.getColumnmaps(), "program_client_id", false));
        map.put("program_client_id", getValue(client.getColumnmaps(), "program_client_id", false));

        int days = 0;
        try {
            days = Days.daysBetween(new DateTime(getValue(client.getColumnmaps(), "dob", false)), DateTime.now()).getDays();
        } catch (Exception e) {
            e.printStackTrace();
        }

        map.put("existing_first_name", getValue(client.getColumnmaps(), "first_name", true));
        map.put("existing_last_name", getValue(client.getColumnmaps(), "last_name", true));
        map.put("existing_gender", getValue(client.getColumnmaps(), "gender", true));
        map.put("existing_mother_name", getValue(client.getColumnmaps(), "mother_name", true));
        map.put("existing_father_name", getValue(client.getColumnmaps(), "father_name", true));
        map.put("existing_birth_date", getValue(client.getColumnmaps(), "dob", false));
        map.put("existing_age", days + "");
        map.put("existing_epi_card_number", getValue(client.getColumnmaps(), "epi_card_number", false));
        map.put("reminders_approval", getValue(client.getColumnmaps(), "reminders_approval", false));
        map.put("contact_phone_number", getValue(client.getColumnmaps(), "contact_phone_number", false));

        map.putAll(getPreloadVaccineData(client));

        return map;
    }

    private Map<String, String> followupOverrides(Client client) {
        Map<String, String> map = new HashMap<>();
        map.put("existing_full_address", client.getAddress("usual_residence").getAddressField("address1") +
                ", UC: " + client.getAddress("usual_residence").getSubTown() +
                ", Town: " + client.getAddress("usual_residence").getTown() +
                ", City: " + client.getAddress("usual_residence").getCityVillage() +
                " - " + client.getAddress("usual_residence").getAddressField("landmark"));

        map.put("existing_program_client_id", client.getIdentifier("Program Client ID"));
        map.put("program_client_id", client.getIdentifier("Program Client ID"));

        map.put("existing_first_name", client.getFirstName());
        map.put("existing_last_name", client.getLastName());
        map.put("existing_gender", client.getGender());
        map.put("existing_birth_date", client.getBirthdate().toString("yyyy-MM-dd"));
        int days = 0;
        try {
            days = Days.daysBetween(client.getBirthdate(), DateTime.now()).getDays();
        } catch (Exception e) {
            e.printStackTrace();
        }
        map.put("existing_age", days + "");
        Object epi = client.getAttribute("EPI Card Number");
        map.put("existing_epi_card_number", epi == null ? "" : epi.toString());

        //TODO EC model
        /* try {
            map.put("existing_father_name", getObsValue(getClientEventDb(), client, true, "father_name", "1594AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            map.put("existing_mother_name", getObsValue(getClientEventDb(), client, true, "mother_name", "1593AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            map.put("reminders_approval", getObsValue(getClientEventDb(), client, true, "reminders_approval", "163089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            map.put("contact_phone_number", getObsValue(getClientEventDb(), client, true, "contact_phone_number", "159635AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            map.putAll(getPreloadVaccineData(client));
        } catch (Exception e) {
            e.printStackTrace();
        } */
        return map;
    }
}
