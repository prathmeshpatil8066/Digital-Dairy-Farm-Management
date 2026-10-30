package com.example.navigationview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class CustomerDashboard extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;

    SharedPreferences customerSp;
    String customerUID;
    String adminUID;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customerSp = getSharedPreferences("customer_session", MODE_PRIVATE);
        customerUID = customerSp.getString("customer_uid", null);
        adminUID = customerSp.getString("admin_uid", null);

        if (customerUID == null || adminUID == null) {
            startActivity(new Intent(this, OpetionPage.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_customer_dashboard);

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationview);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.openDrawer,
                R.string.closeDrawer
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        toggle.getDrawerArrowDrawable()
                .setColor(ContextCompat.getColor(this, android.R.color.white));


        setCustomerHeaderData();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framlayout, new Fragment_home_customer())
                .commit();

        // ================= BOTTOM NAV =================
        bottomNavigationView.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.home1) {
                selectedFragment = new Fragment_home_customer();
            }
            else if (item.getItemId() == R.id.todays) {
                selectedFragment = new Fragment_todaysrecipt();
            }
            else if (item.getItemId() == R.id.setting1) {
                selectedFragment = new Fragment_setting_customer();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

        // ================= DRAWER NAV =================
        navigationView.setNavigationItemSelectedListener(item -> {

            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.home1) {
                selectedFragment = new Fragment_home_customer();
            }
            else if (id == R.id.profile1) {
                selectedFragment = new Fragment_profile_customer();
            }
            else if (id == R.id.todaysrecipt) {
                selectedFragment = new Fragment_todaysrecipt();
            }
            else if (id == R.id.milkdata) {
                selectedFragment = new Fragment_milkdata();
            }
            else if (id == R.id.payment1) {
                selectedFragment = new Fragment_paymentinfo_customer();
            }
            else if (id == R.id.chart1) {
                selectedFragment = new Fragment_ratechart_customer();
            }
            else if (id == R.id.setting1) {
                selectedFragment = new Fragment_setting_customer();
            }
            else if (id == R.id.logout1) {

                customerSp.edit().clear().apply();
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(this, OpetionPage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // ================= HEADER DATA (Firestore) =================
    private void setCustomerHeaderData() {

        View headerView = navigationView.getHeaderView(0);

        TextView tvHeaderName =
                headerView.findViewById(R.id.tvHeaderName);

        TextView tvHeaderEmail =
                headerView.findViewById(R.id.tvHeaderEmail);

        String email = FirebaseAuth.getInstance()
                .getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "";

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .document(customerUID)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.exists()) {

                        String name = snapshot.getString("name");

                        tvHeaderName.setText(
                                name != null ? name : "Customer");

                        tvHeaderEmail.setText(email);
                    }
                });
    }

    private void loadFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framlayout, fragment)
                .addToBackStack(null)
                .commit();
    }
}