package com.synclytic.app.presenter;

import com.synclytic.app.model.CalendarEvent;
import com.synclytic.app.model.CalendarModel;
import com.synclytic.app.view.CalendarView;
import com.synclytic.app.view.CalendarWidgetView;
import java.util.List;

public class CalendarPresenter {
    private CalendarModel calendarModel;
    private CalendarView calendarView;

    private CalendarWidgetView calendarWidgetView;

    public CalendarPresenter(CalendarView calendarView, CalendarWidgetView calendarWidgetView) {
        this.calendarView = calendarView;
        this.calendarModel = new CalendarModel();
        this.calendarWidgetView = calendarWidgetView;
    }

    // This method fetches the data and updates the view
    public void fetchCalendarData() {
        // Fetch events from both Google and Outlook calendars
        calendarModel.fetchGoogleCalendarEvents();
        calendarModel.fetchOutlookCalendarEvents();

        // Get the merged events
        List<CalendarEvent> calendarEvents = calendarModel.getCalendarEvents();

        // Pass the fetched events to the view
        calendarView.showCalendarEvents(calendarEvents);
    }

    public void showCalendarEvents(List<CalendarEvent> calendarEvents) {
        calendarView.showCalendarEvents(calendarEvents);
        calendarWidgetView.showCalendarEvents(calendarEvents);
    }
}