package com.example.sampleproject.Components;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.sampleproject.Helper.TimeHelper;
import com.example.sampleproject.Models.Event;
import com.example.sampleproject.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class CustomCalendarView extends LinearLayout {

    private final String TAG = getResources().getString(R.string.TAG_MESSAGE);
    private final int DAYS_COUNT =  Integer.parseInt(getResources().getString(R.string.noDaysInCalendar));
    private final String DATE_FORMAT =  getResources().getString(R.string.DATE_FORMAT);
    private String dateFormat;

    private final Calendar currentDate = Calendar.getInstance();
    private EventHandler eventHandler = null;

    private LinearLayout header;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private TextView txtDate;
    private GridView grid;

    public static Date dateSelected = new Date();

    CustomCalendarView cv = findViewById(R.id.calendar_view);

    public CustomCalendarView(Context context) {
        super(context);
    }

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initControl(context, attrs);
    }

    public CustomCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initControl(context, attrs);
    }

    private void initControl(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.calendar_view, this);

        loadDateFormat(attrs);
        assignUiElements();
        assignClickHandlers();

        Date today = new Date();

        Calendar nowCal = TimeHelper.setDateTimeToZero(today);
        Date todayChanged = nowCal.getTime();
        dateSelected = todayChanged;

        cv.invokeFirebaseEvent(cv);
    }

    private void loadDateFormat(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.CalendarView);

        try {
            dateFormat = typedArray.getString(R.styleable.CalendarView_dateFormat);
            if (dateFormat == null) {
                dateFormat = DATE_FORMAT;
            }
        } finally {
            typedArray.recycle();
        }
    }

    private void assignUiElements() {
        header = (LinearLayout) findViewById(R.id.calendar_header);
        btnPrev = findViewById(R.id.btn_calendar_prev);
        btnNext = findViewById(R.id.btn_calendar_next);
        txtDate = (TextView) findViewById(R.id.tv_calendar_date_display);
        grid = (GridView) findViewById(R.id.calendar_grid);
    }


    private void assignClickHandlers() {
        btnNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDate.add(Calendar.MONTH, 1);
                cv.invokeFirebaseEvent(cv);
            }
        });
        btnPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDate.add(Calendar.MONTH, -1);
                cv.invokeFirebaseEvent(cv);
            }
        });

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> view, View cell, int position, long id) {


                Date currentDate = (Date) view.getItemAtPosition(position);

                Calendar changedCal = TimeHelper.setDateTimeToZero(currentDate);
                Date changedDate = changedCal.getTime();
                dateSelected = changedDate;
                eventHandler.onDayLongPress(changedDate);

                cv.invokeFirebaseEvent(cv);


            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> view, View cell, int position, long id) {
                // handle long-press
                if (eventHandler == null)
                    return false;

                eventHandler.onDayLongPress((Date) view.getItemAtPosition(position));
                return true;
            }
        });
    }

    public void invokeFirebaseEvent(CustomCalendarView calendarView) {
        //// Receive Intents ////
        Bundle resultIntent = ((Activity) getContext()).getIntent().getExtras();
        String id = resultIntent.getString("id", "1");

        HashSet<Date> events = new HashSet<>();
        DatabaseReference firebase = FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference();
        DatabaseReference firebaseMemberEvents = firebase.child("members").child("member"+id).child("events");

        // Read from the database
        firebaseMemberEvents.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Event event = snapshot.getValue(Event.class);
                    Date addDate = new Date(event.getDeadline());
                    events.add(addDate);
                }
                calendarView.updateCalendar(events);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, String.valueOf(R.string.error_readFirebase), error.toException());
            }
        });
    }

    public void updateCalendar() {
        updateCalendar(null);
    }

    public void updateCalendar(HashSet<Date> events) {
        ArrayList<Date> cells = new ArrayList<>();
        Calendar calendar = (Calendar) currentDate.clone();

        // determine the cell for current month's beginning
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        // move calendar backwards to the beginning of the week
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell);

        // fill cells
        while (cells.size() < DAYS_COUNT) {
            cells.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }


        // update grid
        grid.setAdapter(new CalendarAdapter(getContext(), cells, events));

        // update title
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        txtDate.setText(simpleDateFormat.format(currentDate.getTime()));

        int color = R.color.main_blue;

        header.setBackgroundColor(getResources().getColor(color));
    }


    private class CalendarAdapter extends ArrayAdapter<Date> {

        private HashSet<Date> eventDays;
        private LayoutInflater inflater;

        public CalendarAdapter(Context context, ArrayList<Date> days, HashSet<Date> eventDays) {
            super(context, R.layout.control_calendar_view, days);
            this.eventDays = eventDays;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            // day in question
            Date date = getItem(position);
            int day = date.getDate();
            int month = date.getMonth();
            int year = date.getYear();

            Date today = new Date();

            // inflate item if it does not exist yet
            if (view == null)
                view = inflater.inflate(R.layout.control_calendar_view, parent, false);

            // if this day has an event, specify event image
            view.setBackgroundResource(0);
            if (eventDays != null) {
                for (Date eventDate : eventDays) {
                    if (eventDate.getDate() == day && eventDate.getMonth() == month && eventDate.getYear() == year) {
                        // mark this day for event
                        view.setBackgroundResource(R.drawable.event_date);
                        break;
                    }
                }
            }

            // clear styling
            ((TextView) view).setTypeface(null, Typeface.NORMAL);
            ((TextView) view).setTextColor(Color.BLACK);

            if (month != today.getMonth() || year != today.getYear()) {
                // if this day is outside current month, grey it out
                ((TextView) view).setTextColor(getResources().getColor(R.color.dark_grey_1));
            } else if (day == today.getDate()) {
                // if it is today, set it to blue/bold
                ((TextView) view).setTypeface(null, Typeface.BOLD);
                ((TextView) view).setTextColor(getResources().getColor(R.color.dark_blue_2));
            }

            // set text
            ((TextView) view).setText(String.valueOf(date.getDate()));

            return view;
        }
    }


    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public interface EventHandler {
        void onDayLongPress(Date date);
    }


}
