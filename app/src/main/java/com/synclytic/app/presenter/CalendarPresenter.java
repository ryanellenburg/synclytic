package com.synclytic.app.presenter;

import com.synclytic.app.model.CalendarModel;
import com.synclytic.app.view.CalendarView;

public class CalendarPresenter {
    private CalendarModel calendarModel;
    private CalendarView calendarView;

    public CalendarPresenter(CalendarView view) {
        this.calendarView = view;
        this.calendarModel = new CalendarModel(); // Initialize the model
    }

    // This method fetches the data and updates the view
    public void fetchCalendarData() {
        // Fetch events from both Google and Outlook calendars
        calendarModel.fetchGoogleCalendarEvents();
        calendarModel.fetchOutlookCalendarEvents();

        // Get the merged events
        String[] calendarEvents = calendarModel.getCalendarEvents();

        // Pass the fetched events to the view
        calendarView.showCalendarEvents(calendarEvents);
    }
}