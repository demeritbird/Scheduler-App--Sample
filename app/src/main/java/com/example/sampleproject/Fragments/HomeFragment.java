package com.example.sampleproject.Fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sampleproject.R;
import com.example.sampleproject.Models.Event;
import com.example.sampleproject.Models.EventAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;
    EventAdapter recyclerAdapter; // Create Object of the Adapter class
    DatabaseReference mbase; // Create object of the Firebase Realtime Database

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = root.findViewById(R.id.recycler1);
        TextView errorMsg = root.findViewById(R.id.tv_errormsg);

        ///// Recycler View ////

        Bundle resultIntent = getActivity().getIntent().getExtras();
        String id = resultIntent.getString("id", "1");
        String newUserName = resultIntent.getString("username", "no");
        initRecycler(root, id, errorMsg);
        ImageView userImage = root.findViewById(R.id.userImage);
        DatabaseReference firebase2 = FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference().child("members").child("member" + id).child("image").child("imageUri");
        firebase2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String yes = dataSnapshot.getValue().toString();
                if (!yes.equals("")) {
                    Uri imageUri = Uri.parse(yes);
                    Picasso.get().load(imageUri).placeholder(R.drawable.dembirdimage).into(userImage);
                    System.out.println("heereerer!~!!!!");
                } else {
                    System.out.println("no image");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        TextView username = root.findViewById(R.id.username_text);
        username.setText(newUserName);


        return root;
    }

    // TODO: move to helper function file, this file is only for view
    public void initRecycler(View root, String selId, TextView errorMsg) {
        // Create a instance of the database and get its reference
        mbase = FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference()
                .child("members").child("member" + selId).child("events");

        mbase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DataSnapshot deadlineString = snapshot.child("deadline");
                    Date deadlineDate = new Date(deadlineString.getValue().toString());

                    Date today = new Date();

                    Calendar todayCal = Calendar.getInstance();
                    todayCal.setTime(today);
                    todayCal.set(Calendar.HOUR, 0);
                    todayCal.set(Calendar.MINUTE, 0);
                    todayCal.set(Calendar.SECOND, -1);
                    Date newToday = todayCal.getTime();

                    long daysBetween = TimeUnit.DAYS.convert(deadlineDate.getTime() - newToday.getTime(), TimeUnit.MILLISECONDS);
                    if (snapshot.child("iscomplete").getValue().toString() == "true") {
                        snapshot.child("priority").getRef().setValue(999999 + Integer.parseInt(snapshot.child("daysleft").getValue().toString()));

                        if (newToday.getDay() == deadlineDate.getDay()) {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween - 1);
                        } else {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween);
                        }

                    } else {

                        if (newToday.getDay() == deadlineDate.getDay()) {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween - 1);
                            snapshot.child("priority").getRef().setValue((int) daysBetween -1 );
                        } else {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween);
                            snapshot.child("priority").getRef().setValue((int) daysBetween);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(String.valueOf(R.string.firebase_readError), "Failed to read value.", error.toException());
            }
        });
        Query queryBy = mbase.orderByChild("priority").startAt(0).limitToFirst(3);

        recyclerView.setNestedScrollingEnabled(false);
        // To display the Recycler view linearly
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // It is a class provide by the FirebaseUI to make a query in the database to fetch appropriate data
        FirebaseRecyclerOptions<Event> options = new FirebaseRecyclerOptions.Builder<Event>()
                .setQuery(queryBy, Event.class)
                .build();
        // Connecting object of required Adapter class to the Adapter class itself
        recyclerAdapter = new EventAdapter(options);
        // Connecting Adapter class with the Recycler view*/
        checkEmptyList(queryBy, errorMsg);

        recyclerView.setAdapter(recyclerAdapter);
    }

    public void updateRecycler(View root, String selId, TextView errorMsg) {

        mbase = FirebaseDatabase.getInstance(getResources().getString(R.string.firebase_link)).getReference()
                .child("members").child("member" + selId).child("events");

        mbase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DataSnapshot deadlineString = snapshot.child("deadline");
                    Date deadlineDate = new Date(deadlineString.getValue().toString());

                    Date today = new Date();

                    Calendar todayCal = Calendar.getInstance();
                    todayCal.setTime(today);
                    todayCal.set(Calendar.HOUR, 0);
                    todayCal.set(Calendar.MINUTE, 0);
                    todayCal.set(Calendar.SECOND, -1);
                    Date newToday = todayCal.getTime();

                    long daysBetween = TimeUnit.DAYS.convert(deadlineDate.getTime() - newToday.getTime(), TimeUnit.MILLISECONDS);

                    if (snapshot.child("iscomplete").getValue().toString() == "true") {
                        snapshot.child("priority").getRef().setValue(99999 + Integer.parseInt(snapshot.child("daysleft").getValue().toString()));

                        if (newToday.getDay() == deadlineDate.getDay()) {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween - 1);
                        } else {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween);
                        }

                    } else {

                        if (newToday.getDay() == deadlineDate.getDay()) {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween - 1);
                            snapshot.child("priority").getRef().setValue((int) daysBetween -1 );
                        } else {
                            snapshot.child("daysleft").getRef().setValue((int) daysBetween);
                            snapshot.child("priority").getRef().setValue((int) daysBetween);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(String.valueOf(R.string.firebase_readError), "Failed to read value.", error.toException());
            }
        });
        Query queryBy = mbase.orderByChild("priority").startAt(0).endAt(99999).limitToFirst(3);

        // To display the Recycler view linearly
        // It is a class provide by the FirebaseUI to make a query in the database to fetch appropriate data
        FirebaseRecyclerOptions<Event> options = new FirebaseRecyclerOptions.Builder<Event>()
                .setQuery(queryBy, Event.class)
                .build();
        // Connecting object of required Adapter class to the Adapter class itself
        recyclerAdapter.updateOptions(options);
        // Connecting Adapter class with the Recycler view*/
        checkEmptyList(queryBy, errorMsg);

        recyclerView.setAdapter(recyclerAdapter);
    }

    private void checkEmptyList(Query query, TextView errorMsg) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    errorMsg.setVisibility(View.VISIBLE);
                } else {
                    errorMsg.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
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