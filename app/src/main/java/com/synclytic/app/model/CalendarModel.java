package com.synclytic.app.model;

import java.util.ArrayList;
import java.util.List;

public class CalendarModel {

    // Store the merged calendar events
    private List<CalendarEvent> calendarEvents;
    // TODO add listeners and observers here

    public CalendarModel() {
        calendarEvents = new ArrayList<>();
    }

    // This method will fetch events from Google Calendar
    public void fetchGoogleCalendarEvents() {
        // TODO the code for fetching is in MainActivity, need to connect MainActivity to this then to CalendarPresenter
    }

    // This method will fetch events from Outlook Calendar
    public void fetchOutlookCalendarEvents() {
        // TODO the code for fetching is in MainActivity, need to connect MainActivity to this then to CalendarPresenter
    }

    // TODO add methods for updating/adding/deleting events


    // This method returns the merged events
    public List<CalendarEvent> getCalendarEvents() {
        return calendarEvents;
    }
}