package com.synclytic.app.view;

import com.synclytic.app.model.CalendarEvent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import java.util.*;

public class CalendarWidgetView extends AppWidgetProvider implements CalendarView {
    @Override
    public void showCalendarEvents(List<CalendarEvent> calendarEvents) {
        // 1. Convert the List<CalendarEvent> to an array of strings (if needed)
        String[] eventStrings = new String[calendarEvents.size()];
        for (int ni = 0; ni < calendarEvents.size(); ni++) {
            eventStrings[ni] = calendarEvents.get(ni).getTitle(); // Example: Extract the event title
        }
        // 2. Update the widget UI with the calendar events (using eventStrings or calendarEvents)
        // ... your code to update the widget UI ...
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // ... your existing code to update the widget UI (e.g., updateWidget(context, views)) ...

        // Get an instance of CalendarWidgetView (which is 'this' in this context)
        CalendarWidgetView calendarWidgetView = this;

        // Store a reference to the instance in Shared Preferences
        SharedPreferences prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("widget_view_key",
                "widget_view_instance"); // Store a simple string for now
        editor.apply();
    }

    public void updateWidget(Context context, RemoteViews views) {
        // Update the widget's view with calendar events
    }

    // TODO add or revise this code later to show RSVP status
    //if (event.getRsvpStatus() == CalendarEvent.RsvpStatus.TENTATIVE) {
        // Set paint to draw a dotted line or with transparency
    //} else if (event.getRsvpStatus() == CalendarEvent.RsvpStatus.NEEDS_ACTION) {
        // Set paint to draw only an outline
    //}

}