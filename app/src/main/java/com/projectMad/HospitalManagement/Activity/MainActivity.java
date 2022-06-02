package com.projectMad.HospitalManagement.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.projectMad.HospitalManagement.Adapter.AppointmentAdapter;
import com.projectMad.HospitalManagement.Model.Appointment;
import com.projectMad.HospitalManagement.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private TextView mNavHeaderName,mNavHeaderEmail,mNavHeaderType;
    private CircleImageView nav_header_user_image;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference reference, userRef;
    private RecyclerView recyclerViewId;

    private FloatingActionButton fab;
    private TextView postIdeaTextView;
    private ProgressBar progress_circular;

    private AppointmentAdapter appointmentAdapter;
   private List<Appointment> appointmentList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Hospital System");

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        fab = findViewById(R.id.fab);
        postIdeaTextView = findViewById(R.id.postIdeaTextView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        /*if (savedInstanceState ==null) {
            navigationView.setCheckedItem(R.id.nav_home);
        }*/

        navigationView.setNavigationItemSelectedListener(this);


        reference = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                navigationView =findViewById(R.id.nav_view);
                Menu nav_Menu = navigationView.getMenu();

                String type = snapshot.child("type").getValue().toString();
                if (type.equals("Admin")){
                    nav_Menu.findItem(R.id.nav_allUsers).setVisible(true);
                    nav_Menu.findItem(R.id.nav_patients).setVisible(true);
                    nav_Menu.findItem(R.id.nav_doctors).setVisible(true);
                    nav_Menu.findItem(R.id.nav_appointments).setTitle("All Appointments");

                    readAllBookingAppointments();

                }else if (type.equals("patient")){
                    fab.setVisibility(View.VISIBLE);
                    postIdeaTextView.setVisibility(View.VISIBLE);

                    readMyBookings();
                }
                else {
                    nav_Menu.findItem(R.id.nav_appointments).setTitle("Patient Appointments");
                    readAssignedAppointments();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        userRef = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());


        mNavHeaderName =navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_name);
        mNavHeaderEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_email);
        nav_header_user_image = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_image);
        mNavHeaderType = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_type);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String name = dataSnapshot.child("name").getValue().toString();
                    mNavHeaderName.setText(name);

                    String email = dataSnapshot.child("email").getValue().toString();
                    mNavHeaderEmail.setText(email);

                    String type = dataSnapshot.child("type").getValue().toString();
                    mNavHeaderType.setText("Type: "+type);

                    String image = dataSnapshot.child("profilepictureurl").getValue(String.class);
                    Glide.with(getApplication()).load(image).into(nav_header_user_image);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MakeAppointmentActivity.class);
                startActivity(intent);
            }
        });

        progress_circular = findViewById(R.id.progress_circular);
        recyclerViewId = findViewById(R.id.recyclerViewId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        //recyclerView.setHasFixedSize(true);
        recyclerViewId.setLayoutManager(layoutManager);

        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(MainActivity.this, appointmentList);
        recyclerViewId.setAdapter(appointmentAdapter);


    }
    /*private void readIdeasAssignedToOriginatorsOrPartners(){
        String userid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("business ideas");
        Query query = reference.orderByChild("assignedto").equalTo(userid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot snapshot :dataSnapshot.getChildren()){
                    Post post = snapshot.getValue(Post.class);
                    postList.add(post);

                }
                postAdapter.notifyDataSetChanged();
                progress_circular.setVisibility(View.GONE);

                if (postList.isEmpty()){
                    Toast.makeText(MainActivity.this, "Nothing to show", Toast.LENGTH_SHORT).show();
                    progress_circular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }*/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()){

            case R.id.nav_dashboard:
                Intent myFeedIntent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(myFeedIntent);
                break;

           /* case R.id.nav_feed:
                Intent myFeedIntent = new Intent(MainActivity.this, MyQuestionsActivity.class);
                startActivity(myFeedIntent);
                break;*/

            case R.id.nav_profile:
                SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
                editor.putString("profileid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                editor.apply();
                Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
                break;


            case R.id.nav_notifications:
                Intent notificationsIntent = new Intent(MainActivity.this, NotificationsActivity.class);
                startActivity(notificationsIntent);
                break;


            case R.id.nav_logout:
                FirebaseAuth.getInstance().signOut();
                Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                finish();
                break;

            case R.id.nav_allUsers:
                Intent usersIntent = new Intent(MainActivity.this, AllAppUsersActivity.class);
                startActivity(usersIntent);
                break;

            case R.id.nav_patients:
                Intent originatorsIntent = new Intent(MainActivity.this, PatientsActivity.class);
                startActivity(originatorsIntent);
                break;

            case R.id.nav_doctors:
                Intent investorsIntent = new Intent(MainActivity.this, DoctorsActivity.class);
                startActivity(investorsIntent);
                break;

            case R.id.nav_about:
                Intent intent2 = new Intent(MainActivity.this, AboutAppActivity.class);
                startActivity(intent2);
                break;

            /*case R.id.nav_rate:
                launchMarket();
                break;
            case R.id.nav_share:
                shareIt();
                break;*/
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void readMyBookings(){
        String userid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("appointments");
        Query query = reference.orderByChild("publisher").equalTo(userid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                appointmentList.clear();
                for (DataSnapshot snapshot :dataSnapshot.getChildren()){
                    Appointment appointment = snapshot.getValue(Appointment.class);
                    appointmentList.add(appointment);

                }
                appointmentAdapter.notifyDataSetChanged();
                progress_circular.setVisibility(View.GONE);

                if (appointmentList.isEmpty()){
                    Toast.makeText(MainActivity.this, "No appointments to show.", Toast.LENGTH_SHORT).show();
                    progress_circular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void readAllBookingAppointments() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("appointments");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                appointmentList.clear();
                for (DataSnapshot snapshot :dataSnapshot.getChildren()){
                    Appointment appointment = snapshot.getValue(Appointment.class);
                    appointmentList.add(appointment);

                }
                appointmentAdapter.notifyDataSetChanged();
                progress_circular.setVisibility(View.GONE);

                if (appointmentList.isEmpty()){
                    Toast.makeText(MainActivity.this, "No appointments to show", Toast.LENGTH_SHORT).show();
                    progress_circular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void readAssignedAppointments(){
        String userid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("appointments");
        Query query = reference.orderByChild("assignedto").equalTo(userid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                appointmentList.clear();
                for (DataSnapshot snapshot :dataSnapshot.getChildren()){
                    Appointment appointment = snapshot.getValue(Appointment.class);
                    appointmentList.add(appointment);

                }
                appointmentAdapter.notifyDataSetChanged();
                progress_circular.setVisibility(View.GONE);

                if (appointmentList.isEmpty()){
                    Toast.makeText(MainActivity.this, "No appointments to show", Toast.LENGTH_SHORT).show();
                    progress_circular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this)
                .setTitle("m-Doctor app")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}