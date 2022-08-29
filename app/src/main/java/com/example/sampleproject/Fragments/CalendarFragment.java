package com.example.sampleproject.Fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.sampleproject.Components.CustomCalendarView;
import com.example.sampleproject.Models.Event;
import com.example.sampleproject.Models.EventAdapter;
import com.example.sampleproject.R;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class CalendarFragment extends Fragment {
    public RecyclerView recyclerView;
    public static EventAdapter recyclerAdapter; // Create Object of the Adapter class
    DatabaseReference mbase; // Create object of the Firebase Realtime Database
    Date selectedDate = CustomCalendarView.dateSelected;
    Boolean isPrivate = false;
    Boolean isComplete = false;

    Button btnPrivate;
    Button btnPublic;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);
        recyclerView = root.findViewById(R.id.recycler2);


        initRecycler(root);


        //// Init Calendar View ////
        CustomCalendarView calendarView = ((CustomCalendarView) root.findViewById(R.id.calendar_view));
        calendarView.setEventHandler(new CustomCalendarView.EventHandler() {
            @Override
            public void onDayLongPress(Date date) {
                // show returned day
                DateFormat df = SimpleDateFormat.getDateInstance();
//                Toast.makeText(getContext(), df.format(date), Toast.LENGTH_SHORT).show();
                selectedDate = date;



                Calendar now = Calendar.getInstance();
                now.setTime(selectedDate);
                now.set(Calendar.HOUR, 0);
                now.set(Calendar.MINUTE, 0);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.HOUR_OF_DAY, 0);

                selectedDate = now.getTime();

                System.out.println(selectedDate);
                Toast.makeText(getContext(), selectedDate.toString() , Toast.LENGTH_SHORT).show();
                System.out.println("---asd");
                updateRecycler(root);
            }
        });

        //// Init GET firebase ////
        calendarView.invokeFirebaseEvent(calendarView);

        //// Init Components ////
        FloatingActionButton dialogButton = root.findViewById(R.id.fab);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogTheme);
                View bottomSheetView = inflater.inflate(R.layout.fragment_bottom_dialog, root.findViewById(R.id.bottomDialogContainer));
                EditText titleDialog = bottomSheetView.findViewById(R.id.title_dialog);
                EditText descriptionDialog = bottomSheetView.findViewById(R.id.description_dialog);

                String title = titleDialog.getText().toString();
                String description = descriptionDialog.getText().toString();
                String daysleft = "5";


                btnPrivate = bottomSheetView.findViewById(R.id.btn_private_sel);
                btnPublic = bottomSheetView.findViewById(R.id.btn_public_sel);

                btnPublic.setBackgroundColor(Color.RED);
                btnPrivate.setBackgroundColor(Color.GRAY);


                // Refactor Code here
                btnPrivacySettingOnClickListeners();
                postToFireBase(bottomSheetView, title, description, daysleft);

                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();
            }
        });

        return root;
    }


    private void btnPrivacySettingOnClickListeners() {
        btnPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPrivate = true;
                btnPrivate.setBackgroundColor(Color.YELLOW);
                btnPublic.setBackgroundColor(Color.GRAY);
            }
        });

        btnPublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPrivate = false;
                btnPublic.setBackgroundColor(Color.RED);
                btnPrivate.setBackgroundColor(Color.GRAY);
            }
        });
    }

    private void postToFireBase(View bottomSheetView, String title, String description, String daysleft) {
        bottomSheetView.findViewById(R.id.buttonDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference firebase = FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference().child("events");
                String uuid = UUID.randomUUID().toString();
                Event event = new Event(title, description, "deadline", daysleft, uuid, isPrivate, isComplete);
                firebase.child(uuid).child("title").setValue(event.getTitle());
                firebase.child(uuid).child("description").setValue(event.getDescription());
                firebase.child(uuid).child("deadline").setValue(event.getDeadline());
                firebase.child(uuid).child("daysleft").setValue(event.getDaysLeft());
                firebase.child(uuid).child("uid").setValue(event.getUid());

                Toast.makeText(getContext(), "added!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initRecycler(View root) {
        // Create a instance of the database and get its reference
        mbase = (DatabaseReference) FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference().child("events");
        Query query = mbase.orderByChild("deadline").equalTo(selectedDate.toString());

        FirebaseRecyclerOptions<Event> options
                = new FirebaseRecyclerOptions.Builder<Event>()
                .setQuery(query, Event.class)
                .build();

        // FIXME: must just be month, day year la, use the date formatter than we will roll from there

        recyclerView = root.findViewById(R.id.recycler2);
        recyclerView.setNestedScrollingEnabled(false);

        // To display the Recycler view linearly
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // It is a class provide by the FirebaseUI to make a query in the database to fetch appropriate data

        // Connecting object of required Adapter class to the Adapter class itself
        recyclerAdapter = new EventAdapter(options);
        // Connecting Adapter class with the Recycler view*/
        recyclerView.setAdapter(recyclerAdapter);
    }

    private void updateRecycler(View root) {
        // Create a instance of the database and get its reference
        mbase = (DatabaseReference) FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference().child("events");
        Query query = mbase.orderByChild("deadline").equalTo(selectedDate.toString());

        // FIXME: must just be month, day year la, use the date formatter than we will roll from there
        FirebaseRecyclerOptions<Event> options
                = new FirebaseRecyclerOptions.Builder<Event>()
                .setQuery(query, Event.class)
                .build();

        recyclerAdapter.updateOptions(options);
        recyclerView.setAdapter(recyclerAdapter);

    }

    // Function to tell the app to start getting from database on starting of the activity
    @Override
    public void onStart() {
        super.onStart();
        recyclerAdapter.startListening();
    }

    // Function to tell the app to stop getting data from database on stopping of the activity
    @Override
    public void onStop() {
        super.onStop();
        recyclerAdapter.stopListening();
    }


}