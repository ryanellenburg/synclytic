package com.synclytic.app.model;

import java.util.Date;

public class CalendarEvent {
    public enum Source {
        GOOGLE, OUTLOOK
    }
    public enum RsvpStatus {
        ACCEPTED, TENTATIVE, NEEDS_ACTION
    }

    private String id;
    private String title;
    private Date startTime;
    private Date endTime;
    private boolean allDay;
    private String calendarId;
    private int color;
    private Source source;
    private RsvpStatus rsvpStatus; // Add this line

    // Constructor, getters, and setters
    public CalendarEvent(String id, String title, Date startTime, Date endTime,
                         boolean allDay, String calendarId, int color,
                         Source source, RsvpStatus rsvpStatus) { // Update constructor
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.allDay = allDay;
        this.calendarId = calendarId;
        this.color = color;
        this.source = source;
        this.rsvpStatus = rsvpStatus;
    }
    // TODO add getters and setters for all fields
}
