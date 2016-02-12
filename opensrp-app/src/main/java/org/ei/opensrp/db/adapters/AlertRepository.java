package org.ei.opensrp.db.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.ei.opensrp.db.RepositoryManager;
import org.ei.opensrp.domain.Alert;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.ei.drishti.dto.AlertStatus.complete;
import static org.ei.drishti.dto.AlertStatus.from;
import static org.ei.drishti.dto.AlertStatus.inProcess;

/**
 * Created by koros on 2/12/16.
 */
public class AlertRepository {

    private static final String ALERTS_TABLE_NAME = "alerts";
    public static final String ALERTS_CASEID_COLUMN = "caseID";
    public static final String ALERTS_SCHEDULE_NAME_COLUMN = "scheduleName";
    public static final String ALERTS_VISIT_CODE_COLUMN = "visitCode";
    public static final String ALERTS_STATUS_COLUMN = "status";
    public static final String ALERTS_STARTDATE_COLUMN = "startDate";
    public static final String ALERTS_EXPIRYDATE_COLUMN = "expiryDate";
    public static final String ALERTS_COMPLETIONDATE_COLUMN = "completionDate";
    private static final String[] ALERTS_TABLE_COLUMNS = new String[]{
            ALERTS_CASEID_COLUMN,
            ALERTS_SCHEDULE_NAME_COLUMN,
            ALERTS_VISIT_CODE_COLUMN,
            ALERTS_STATUS_COLUMN,
            ALERTS_STARTDATE_COLUMN,
            ALERTS_EXPIRYDATE_COLUMN,
            ALERTS_COMPLETIONDATE_COLUMN
    };

    public static final String CASE_AND_VISIT_CODE_COLUMN_SELECTIONS = ALERTS_CASEID_COLUMN + " = ? AND " + ALERTS_VISIT_CODE_COLUMN + " = ?";

    private Context context;
    private String password;

    public AlertRepository(Context context, String password){
        this.context = context;
        this.password = password;
    }

    public List<Alert> allAlerts() {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        Cursor cursor = database.query(ALERTS_TABLE_NAME, ALERTS_TABLE_COLUMNS, null, null, null, null, null, null);
        return readAllAlerts(cursor);
    }

    public List<Alert> allActiveAlertsForCase(String caseId) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        Cursor cursor = database.query(ALERTS_TABLE_NAME, ALERTS_TABLE_COLUMNS, ALERTS_CASEID_COLUMN + " = ?", new String[]{caseId}, null, null, null, null);
        return filterActiveAlerts(readAllAlerts(cursor));
    }

    public void createAlert(Alert alert) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        String[] caseAndScheduleNameColumnValues = {alert.caseId(), alert.scheduleName()};

        String caseAndScheduleNameColumnSelections = ALERTS_CASEID_COLUMN + " = ? AND " + ALERTS_SCHEDULE_NAME_COLUMN + " = ?";
        Cursor cursor = database.query(ALERTS_TABLE_NAME, ALERTS_TABLE_COLUMNS, caseAndScheduleNameColumnSelections, caseAndScheduleNameColumnValues, null, null, null, null);
        List<Alert> existingAlerts = readAllAlerts(cursor);

        ContentValues values = createValuesFor(alert);
        if (existingAlerts.isEmpty()) {
            database.insert(ALERTS_TABLE_NAME, null, values);
        } else {
            database.update(ALERTS_TABLE_NAME, values, caseAndScheduleNameColumnSelections, caseAndScheduleNameColumnValues);
        }
    }

    public void markAlertAsClosed(String caseId, String visitCode, String completionDate) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        String[] caseAndVisitCodeColumnValues = {caseId, visitCode};

        ContentValues valuesToBeUpdated = new ContentValues();
        valuesToBeUpdated.put(ALERTS_STATUS_COLUMN, complete.value());
        valuesToBeUpdated.put(ALERTS_COMPLETIONDATE_COLUMN, completionDate);
        database.update(ALERTS_TABLE_NAME, valuesToBeUpdated, CASE_AND_VISIT_CODE_COLUMN_SELECTIONS, caseAndVisitCodeColumnValues);
    }

    public void deleteAllAlertsForEntity(String caseId) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        database.delete(ALERTS_TABLE_NAME, ALERTS_CASEID_COLUMN + "= ?", new String[]{caseId});
    }

    public void deleteAllAlerts() {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        database.delete(ALERTS_TABLE_NAME, null, null);
    }

    private List<Alert> readAllAlerts(Cursor cursor) {
        cursor.moveToFirst();
        List<Alert> alerts = new ArrayList<Alert>();
        while (!cursor.isAfterLast()) {
            alerts.add(
                    new Alert(cursor.getString(cursor.getColumnIndex(ALERTS_CASEID_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(ALERTS_SCHEDULE_NAME_COLUMN)), cursor.getString(cursor.getColumnIndex(ALERTS_VISIT_CODE_COLUMN)),
                            from(cursor.getString(cursor.getColumnIndex(ALERTS_STATUS_COLUMN))),
                            cursor.getString(cursor.getColumnIndex(ALERTS_STARTDATE_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(ALERTS_EXPIRYDATE_COLUMN))
                    )
                            .withCompletionDate(cursor.getString(cursor.getColumnIndex(ALERTS_COMPLETIONDATE_COLUMN))));
            cursor.moveToNext();
        }
        cursor.close();
        return alerts;
    }

    private List<Alert> filterActiveAlerts(List<Alert> alerts) {
        List<Alert> activeAlerts = new ArrayList<Alert>();
        for (Alert alert : alerts) {
            LocalDate today = LocalDate.now();
            if (LocalDate.parse(alert.expiryDate()).isAfter(today) || (complete.equals(alert.status()) && LocalDate.parse(alert.completionDate()).isAfter(today.minusDays(3)))) {
                activeAlerts.add(alert);
            }
        }
        return activeAlerts;
    }

    private ContentValues createValuesFor(Alert alert) {
        ContentValues values = new ContentValues();
        values.put(ALERTS_CASEID_COLUMN, alert.caseId());
        values.put(ALERTS_SCHEDULE_NAME_COLUMN, alert.scheduleName());
        values.put(ALERTS_VISIT_CODE_COLUMN, alert.visitCode());
        values.put(ALERTS_STATUS_COLUMN, alert.status().value());
        values.put(ALERTS_STARTDATE_COLUMN, alert.startDate());
        values.put(ALERTS_EXPIRYDATE_COLUMN, alert.expiryDate());
        values.put(ALERTS_COMPLETIONDATE_COLUMN, alert.completionDate());
        return values;
    }

    public List<Alert> findByEntityIdAndAlertNames(String entityId, String... names) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        Cursor cursor = database.rawQuery(format("SELECT * FROM %s WHERE %s = ? AND %s IN (%s) ORDER BY DATE(%s)",
                ALERTS_TABLE_NAME, ALERTS_CASEID_COLUMN, ALERTS_VISIT_CODE_COLUMN,
                insertPlaceholdersForInClause(names.length), ALERTS_STARTDATE_COLUMN), addAll(new String[]{entityId}, names));
        return readAllAlerts(cursor);
    }

    private String insertPlaceholdersForInClause(int length) {
        return repeat("?", ",", length);
    }

    public void changeAlertStatusToInProcess(String entityId, String alertName) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        String[] caseAndVisitCodeColumnValues = {entityId, alertName};

        ContentValues valuesToBeUpdated = new ContentValues();
        valuesToBeUpdated.put(ALERTS_STATUS_COLUMN, inProcess.value());
        database.update(ALERTS_TABLE_NAME, valuesToBeUpdated, CASE_AND_VISIT_CODE_COLUMN_SELECTIONS, caseAndVisitCodeColumnValues);
    }
    public void changeAlertStatusToComplete(String entityId, String alertName) {
        SQLiteDatabase database = RepositoryManager.getDatabase(context, password);
        String[] caseAndVisitCodeColumnValues = {entityId, alertName};

        ContentValues valuesToBeUpdated = new ContentValues();
        valuesToBeUpdated.put(ALERTS_STATUS_COLUMN, complete.value());
        database.update(ALERTS_TABLE_NAME, valuesToBeUpdated, CASE_AND_VISIT_CODE_COLUMN_SELECTIONS, caseAndVisitCodeColumnValues);
    }
}
