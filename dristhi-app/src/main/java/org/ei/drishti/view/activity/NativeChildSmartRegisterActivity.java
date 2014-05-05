package org.ei.drishti.view.activity;

import android.view.View;
import org.ei.drishti.R;
import org.ei.drishti.adapter.SmartRegisterPaginatedAdapter;
import org.ei.drishti.domain.form.FieldOverrides;
import org.ei.drishti.provider.ChildSmartRegisterClientsProvider;
import org.ei.drishti.provider.SmartRegisterClientsProvider;
import org.ei.drishti.view.contract.ECClient;
import org.ei.drishti.view.contract.SmartRegisterClient;
import org.ei.drishti.view.controller.ECSmartRegisterController;
import org.ei.drishti.view.controller.VillageController;
import org.ei.drishti.view.dialog.*;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.toArray;
import static org.ei.drishti.AllConstants.FormNames.*;

public class NativeChildSmartRegisterActivity extends SecuredNativeSmartRegisterActivity {

    private SmartRegisterClientsProvider clientProvider = null;
    private ECSmartRegisterController controller;
    private VillageController villageController;
    private DialogOptionMapper dialogOptionMapper;

    private final ClientActionHandler clientActionHandler = new ClientActionHandler();
    private SmartRegisterPaginatedAdapter smartRegisterPaginatedAdapter;

    public ClientsHeaderProvider dummyHeaderProvider = new ClientsHeaderProvider() {
        @Override
        public int count() {
            return 0;
        }

        @Override
        public int weightSum() {
            return 0;
        }

        @Override
        public int[] weights() {
            return new int[0];
        }

        @Override
        public int[] headerTextResourceIds() {
            return new int[0];
        }

        @Override
        public void onServiceModeSelected(ServiceModeOption serviceModeOption) {

        }
    };

    private ClientsHeaderProvider clientsHeaderProvider = new ClientsHeaderProvider() {
        ClientsHeaderProvider actualHeaderProvider = dummyHeaderProvider;

        @Override
        public int count() {
            return actualHeaderProvider.count();
        }

        @Override
        public int weightSum() {
            return actualHeaderProvider.weightSum();
        }

        @Override
        public int[] weights() {
            return actualHeaderProvider.weights();
        }

        @Override
        public int[] headerTextResourceIds() {
            return actualHeaderProvider.headerTextResourceIds();
        }

        @Override
        public void onServiceModeSelected(ServiceModeOption serviceModeOption) {
            actualHeaderProvider = serviceModeOption.getHeaderProvider();
        }
    };

    @Override
    protected ClientsHeaderProvider clientsHeaderProvider() {
        return clientsHeaderProvider;
    }

    @Override
    protected DefaultOptionsProvider getDefaultOptionsProvider() {
        return new DefaultOptionsProvider() {

            @Override
            public ServiceModeOption serviceMode() {
                return new ChildOverviewServiceMode(clientsProvider(), clientsHeaderProvider());
            }

            @Override
            public FilterOption villageFilter() {
                return new AllClientsFilter();
            }

            @Override
            public SortOption sortOption() {
                return new NameSort();
            }

            @Override
            public String nameInShortFormForTitle() {
                return getResources().getString(R.string.child_register_title_in_short);
            }
        };
    }

    @Override
    protected NavBarOptionsProvider getNavBarOptionsProvider() {
        return new NavBarOptionsProvider() {

            @Override
            public DialogOption[] filterOptions() {
                Iterable<? extends DialogOption> villageFilterOptions =
                        dialogOptionMapper.mapToVillageFilterOptions(villageController.getVillages());
                return toArray(concat(DEFAULT_FILTER_OPTIONS, villageFilterOptions), DialogOption.class);
            }

            @Override
            public DialogOption[] serviceModeOptions() {
                return new DialogOption[]{
                        new ChildOverviewServiceMode(clientsProvider(), clientsHeaderProvider()),
                        new ChildImmunization0to9ServiceMode(clientsProvider(), clientsHeaderProvider()),
                        new ChildImmunization9PlusServiceMode(clientsProvider(), clientsHeaderProvider())
                };
            }

            @Override
            public DialogOption[] sortingOptions() {
                return new DialogOption[]{new NameSort(), new ECNumberSort(),
                        new HighPrioritySort(), new BPLSort(),
                        new SCSort(), new STSort()};
            }
        };
    }

    @Override
    protected SmartRegisterPaginatedAdapter adapter() {
        if (smartRegisterPaginatedAdapter == null) {
            smartRegisterPaginatedAdapter = new SmartRegisterPaginatedAdapter(clientsProvider());
        }
        return smartRegisterPaginatedAdapter;
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        if (clientProvider == null) {
            clientProvider = new ChildSmartRegisterClientsProvider(
                    this, clientActionHandler, controller);
        }
        return clientProvider;
    }

    private DialogOption[] getEditOptions() {
        return new DialogOption[]{
                new OpenFormOption(getString(R.string.str_register_anc_form), ANC_REGISTRATION, formController),
                new OpenFormOption(getString(R.string.str_register_fp_form), FP_CHANGE, formController),
                new OpenFormOption(getString(R.string.str_register_child_form), CHILD_REGISTRATION_EC, formController),
                new OpenFormOption(getString(R.string.str_edit_ec_form), EC_EDIT, formController),
                new OpenFormOption(getString(R.string.str_close_ec_form), EC_CLOSE, formController),
        };
    }

    @Override
    protected void onInitialization() {
        controller = new ECSmartRegisterController(context.allEligibleCouples(),
                context.allBeneficiaries(), context.listCache(),
                context.ecClientsCache());
        villageController = new VillageController(context.allEligibleCouples(),
                context.listCache(), context.villagesCache());
        dialogOptionMapper = new DialogOptionMapper();
    }


    @Override
    protected void startRegistration() {
        FieldOverrides fieldOverrides = new FieldOverrides(context.anmLocationController().getLocationJSON());
        startFormActivity(EC_REGISTRATION, null, fieldOverrides.getJSONString());
    }

    private class ClientActionHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.profile_info_layout:
                    showProfileView((ECClient) view.getTag());
                    break;
                case R.id.btn_edit:
                    showFragmentDialog(new EditDialogOptionModel(), view.getTag());
                    break;
            }
        }

        private void showProfileView(ECClient client) {
            navigationController.startEC(client.entityId());
        }
    }

    private class EditDialogOptionModel implements DialogOptionModel {
        @Override
        public DialogOption[] getDialogOptions() {
            return getEditOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onEditSelection((EditOption) option, (SmartRegisterClient) tag);
        }
    }
}
