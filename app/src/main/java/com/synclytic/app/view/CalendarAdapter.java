package com.synclytic.app.view; // Replace with your package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.synclytic.app.R;
import com.synclytic.app.model.CalendarEvent;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private List<CalendarEvent> calendarEvents;

    public CalendarAdapter(List<CalendarEvent> calendarEvents) {
        this.calendarEvents = calendarEvents;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calendar_event_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CalendarEvent event = calendarEvents.get(position);
        holder.eventTextView.setText(event.getTitle()); // Example: Display the event title
        // ... set other UI elements based on event properties (time, color, etc.) ...
    }

    @Override
    public int getItemCount() {
        return calendarEvents.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView eventTextView;
        // ... add other UI elements here ...

        public ViewHolder(View itemView) {
            super(itemView);
            eventTextView = itemView.findViewById(R.id.event_text_view); // Use your TextView ID
            // ... initialize other UI elements ...
        }
    }
}