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
        // 1. Use Google Calendar API client to fetch events.
        // 2. Map the API response to a list of CalendarEvent objects.
        //    - For each event, set the source to CalendarEvent.Source.GOOGLE
        // 3. Add the events to the 'events' list.
        // 4. Notify listeners that the data has changed.
    }

    // This method will fetch events from Outlook Calendar
    public void fetchOutlookCalendarEvents() {
        // 1. Use Microsoft Graph API client to fetch events.
        // 2. Map the API response to a list of CalendarEvent objects.
        //    - For each event, set the source to CalendarEvent.Source.OUTLOOK
        // 3. Add the events to the 'events' list.
        // 4. Notify listeners that the data has changed.
    }

    // TODO add methods for updating/adding/deleting events


    // This method returns the merged events
    public List<CalendarEvent> getCalendarEvents() {
        return calendarEvents;
    }
}