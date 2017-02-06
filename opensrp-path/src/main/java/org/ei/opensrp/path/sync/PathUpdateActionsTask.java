package org.ei.opensrp.path.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.ei.opensrp.AllConstants;
import org.ei.opensrp.domain.DownloadStatus;
import org.ei.opensrp.domain.FetchStatus;
import org.ei.opensrp.repository.AllSharedPreferences;
import org.ei.opensrp.service.ActionService;
import org.ei.opensrp.service.AllFormVersionSyncService;
import org.ei.opensrp.service.FormSubmissionSyncService;
import org.ei.opensrp.sync.AdditionalSyncService;
import org.ei.opensrp.sync.AfterFetchListener;
import org.ei.opensrp.sync.ClientProcessor;
import org.ei.opensrp.view.BackgroundAction;
import org.ei.opensrp.view.LockingBackgroundTask;
import org.ei.opensrp.view.ProgressIndicator;

import static org.ei.opensrp.domain.FetchStatus.fetched;
import static org.ei.opensrp.domain.FetchStatus.fetchedFailed;
import static org.ei.opensrp.domain.FetchStatus.nothingFetched;
import static org.ei.opensrp.util.Log.logInfo;

public class PathUpdateActionsTask {
    private final LockingBackgroundTask task;
    private ActionService actionService;
    private Context context;
    private FormSubmissionSyncService formSubmissionSyncService;
    private AllFormVersionSyncService allFormVersionSyncService;
    private AdditionalSyncService additionalSyncService;

    public PathUpdateActionsTask(Context context, ActionService actionService, FormSubmissionSyncService formSubmissionSyncService, ProgressIndicator progressIndicator,
                                 AllFormVersionSyncService allFormVersionSyncService) {
        this.actionService = actionService;
        this.context = context;
        this.formSubmissionSyncService = formSubmissionSyncService;
        this.allFormVersionSyncService = allFormVersionSyncService;
        this.additionalSyncService = null;
        task = new LockingBackgroundTask(progressIndicator);
    }

    public void setAdditionalSyncService(AdditionalSyncService additionalSyncService) {
        this.additionalSyncService = additionalSyncService;
    }

    public void updateFromServer(final AfterFetchListener afterFetchListener) {
        if (org.ei.opensrp.Context.getInstance().IsUserLoggedOut()) {
            logInfo("Not updating from server as user is not logged in.");
            return;
        }

        task.doActionInBackground(new BackgroundAction<FetchStatus>() {
            public FetchStatus actionToDoInBackgroundThread() {

                FetchStatus fetchStatusForForms = sync();
                FetchStatus fetchStatusForActions = actionService.fetchNewActions();
                FetchStatus fetchStatusAdditional = additionalSyncService == null ? nothingFetched : additionalSyncService.sync();

                if (org.ei.opensrp.Context.getInstance().configuration().shouldSyncForm()) {

                    allFormVersionSyncService.verifyFormsInFolder();
                    FetchStatus fetchVersionStatus = allFormVersionSyncService.pullFormDefinitionFromServer();
                    DownloadStatus downloadStatus = allFormVersionSyncService.downloadAllPendingFormFromServer();

                    if (downloadStatus == DownloadStatus.downloaded) {
                        allFormVersionSyncService.unzipAllDownloadedFormFile();
                    }

                    if (fetchVersionStatus == fetched || downloadStatus == DownloadStatus.downloaded) {
                        return fetched;
                    }
                }


                if (fetchStatusForActions == fetched || fetchStatusForForms == fetched || fetchStatusAdditional == fetched)
                    return fetched;

                return fetchStatusForForms;
            }

            public void postExecuteInUIThread(FetchStatus result) {
                if (result != null && context != null && result != nothingFetched) {
                    Toast.makeText(context, result.displayValue(), Toast.LENGTH_SHORT).show();
                }
                afterFetchListener.afterFetch(result);
            }
        });
    }

    private FetchStatus sync() {
        try {
            ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

            // Retrieve database host from preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

            ecUpdater.fetchAllClients(AllConstants.SyncFilters.FILTER_PROVIDER, allSharedPreferences.fetchRegisteredANM());
            ecUpdater.fetchAllEvents(AllConstants.SyncFilters.FILTER_PROVIDER, allSharedPreferences.fetchRegisteredANM());
            ClientProcessor.getInstance(context).processClient(ecUpdater.allEvents());
            return FetchStatus.fetched;
        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
            return fetchedFailed;
        }

    }
}