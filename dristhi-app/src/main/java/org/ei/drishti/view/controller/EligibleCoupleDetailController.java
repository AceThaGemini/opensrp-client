package org.ei.drishti.view.controller;

import android.content.Context;
import android.content.Intent;
import com.google.gson.Gson;
import org.ei.drishti.AllConstants;
import org.ei.drishti.domain.EligibleCouple;
import org.ei.drishti.repository.AllAlerts;
import org.ei.drishti.repository.AllEligibleCouples;
import org.ei.drishti.repository.AllTimelineEvents;
import org.ei.drishti.util.DateUtil;
import org.ei.drishti.util.TimelineEventComparator;
import org.ei.drishti.view.activity.CameraLaunchActivity;
import org.ei.drishti.view.activity.FormActivity;
import org.ei.drishti.view.contract.*;
import org.joda.time.LocalDate;
import org.ocpsoft.pretty.time.Duration;
import org.ocpsoft.pretty.time.PrettyTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.min;
import static org.ei.drishti.AllConstants.*;

public class EligibleCoupleDetailController {
    private final Context context;
    private String caseId;
    private final AllEligibleCouples allEligibleCouples;
    private AllAlerts allAlerts;
    private final AllTimelineEvents allTimelineEvents;
    private PrettyTime prettyTime;

    public EligibleCoupleDetailController(Context context, String caseId, AllEligibleCouples allEligibleCouples, AllAlerts allAlerts,
                                          AllTimelineEvents allTimelineEvents) {
        this.context = context;
        this.caseId = caseId;
        this.allEligibleCouples = allEligibleCouples;
        this.allAlerts = allAlerts;
        this.allTimelineEvents = allTimelineEvents;
        this.prettyTime = new PrettyTime(DateUtil.today().toDate(), new Locale("short"));
    }

    public String get() {
        EligibleCouple eligibleCouple = allEligibleCouples.findByCaseID(caseId);
        List<List<ProfileTodo>> todosAndUrgentTodos = allAlerts.fetchAllActiveAlertsForCase(caseId);

        ECDetail ecContext = new ECDetail(caseId, eligibleCouple.village(), eligibleCouple.subCenter(), eligibleCouple.ecNumber(),
                eligibleCouple.isHighPriority(), null, eligibleCouple.photoPath(), new ArrayList<Child>(), new CoupleDetails(eligibleCouple.wifeName(),
                eligibleCouple.husbandName(), eligibleCouple.ecNumber(), eligibleCouple.isOutOfArea()),
                eligibleCouple.details()).
                addTodos(todosAndUrgentTodos.get(0)).
                addUrgentTodos(todosAndUrgentTodos.get(1)).
                addTimelineEvents(getEvents());

        return new Gson().toJson(ecContext);
    }

    public void markTodoAsCompleted(String caseId, String visitCode) {
        allAlerts.markAsCompleted(caseId, visitCode, LocalDate.now().toString());
    }

    public void startFormActivity(String formName, String entityId) {
        Intent intent = new Intent(context, FormActivity.class);
        intent.putExtra(FORM_NAME_PARAM, formName);
        intent.putExtra(ENTITY_ID_PARAM, entityId);
        context.startActivity(intent);
    }

    public void takePhoto() {
        Intent intent = new Intent(context, CameraLaunchActivity.class);
        intent.putExtra(AllConstants.TYPE, WOMAN_TYPE);
        intent.putExtra(ENTITY_ID, caseId);
        context.startActivity(intent);
    }

    private List<TimelineEvent> getEvents() {
        List<org.ei.drishti.domain.TimelineEvent> events = allTimelineEvents.forCase(caseId);
        List<TimelineEvent> timelineEvents = new ArrayList<TimelineEvent>();

        Collections.sort(events, new TimelineEventComparator());

        for (org.ei.drishti.domain.TimelineEvent event : events) {
            timelineEvents.add(new TimelineEvent(event.type(), event.title(), new String[]{event.detail1(), event.detail2()}, formatDate(event.referenceDate())));
        }

        return timelineEvents;
    }

    private String formatDate(LocalDate date) {
        List<Duration> durationComponents = prettyTime.calculatePreciseDuration(date.toDate());
        return prettyTime.format(durationComponents.subList(0, min(durationComponents.size(), 2))).replaceAll(" _", "");
    }
}
