package com.example.navigationview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.razorpay.PaymentResultListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements PaymentResultListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;

    SharedPreferences adminSp;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    String adminUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adminSp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);

        boolean isLoggedIn = adminSp.getBoolean("isAdminLoggedIn", false);
        adminUID = adminSp.getString("admin_uid", null);

        if (!isLoggedIn || adminUID == null) {
            startActivity(new Intent(this, AdminLogin.class));
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    String role = doc.getString("role");

                    if (role != null && role.equals("superadmin")) {

                        startActivity(new Intent(
                                MainActivity.this,
                                SuperAdminDashboard.class));

                        finish();
                    }
                });

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationview);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        setupDrawer();
        setAdminHeaderData();
        checkSubscription();
    }
    private void setupDrawer() {

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
    }
    private void checkSubscription() {

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    Long trialStart = doc.getLong("trialStartDate");
                    Long planEnd = doc.getLong("planEndDate");

                    long currentTime = System.currentTimeMillis();

                    if (trialStart == null) {

                        long trialEnd = currentTime + (7L * 24 * 60 * 60 * 1000);

                        Map<String, Object> update = new HashMap<>();
                        update.put("trialStartDate", currentTime);
                        update.put("planName", "7 Days Trial");
                        update.put("planEndDate", trialEnd);
                        update.put("active", true);

                        db.collection("Admins")
                                .document(adminUID)
                                .update(update);

                        setupNavigation(false);
                        return;
                    }

                    if (planEnd != null && currentTime > planEnd) {
                        showExpiredDialog();
                        setupNavigation(true);
                    } else {
                        setupNavigation(false);
                    }
                });
    }
    private void setupNavigation(boolean expired) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framlayout, new Fragment_home())
                .commit();

        navigationView.setNavigationItemSelectedListener(item -> {

            handleNavigation(item.getItemId(), expired);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {

            if (expired &&
                    (item.getItemId() == R.id.collection ||
                            item.getItemId() == R.id.addcustomer)) {

                showExpiredDialog();
                return false;
            }

            Fragment fragment = null;

            if (item.getItemId() == R.id.home)
                fragment = new Fragment_home();

            else if (item.getItemId() == R.id.collection)
                fragment = new Fragment_collection();

            else if (item.getItemId() == R.id.addcustomer)
                fragment = new Fragment_addcustomer();

            if (fragment != null)
                loadFragment(fragment);

            return true;
        });
    }

    private void handleNavigation(int id, boolean expired) {

        if (id == R.id.home) loadFragment(new Fragment_home());

        else if (id == R.id.payment) loadFragment(new Fragment_payment());

        else if (id == R.id.profile) loadFragment(new Fragment_profile());

        else if (id == R.id.totalcollection) loadFragment(new Fragment_totalcollection());

        else if (id == R.id.totalcustomer) loadFragment(new Fragment_totalcustomer());

        else if (id == R.id.customerdetails) loadFragment(new Fragment_customerdetails());

        else if (id == R.id.paymentinfo) loadFragment(new Fragment_paymentinfo());

        else if (id == R.id.customersdata) loadFragment(new Fragment_admin_customers());

        else if (id == R.id.chart) loadFragment(new Fragment_ratechart());

        else if (id == R.id.setting) loadFragment(new Fragment_setting());

        else if (id == R.id.suscription) loadFragment(new Suscription());

        else if (id == R.id.logout) adminLogout();
    }
    private void showExpiredDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Subscription Expired")
                .setMessage("Please renew your plan.")
                .setPositiveButton("Go to Subscription",
                        (dialog, which) -> loadFragment(new Suscription()))
                .setCancelable(false)
                .show();
    }
    private void setAdminHeaderData() {

        navigationView.post(() -> {

            TextView tvHeaderName =
                    navigationView.getHeaderView(0)
                            .findViewById(R.id.tvHeaderName);

            TextView tvHeaderEmail =
                    navigationView.getHeaderView(0)
                            .findViewById(R.id.tvHeaderEmail);

            db.collection("Admins")
                    .document(adminUID)
                    .get()
                    .addOnSuccessListener(doc -> {

                        if (!doc.exists()) return;

                        tvHeaderName.setText(doc.getString("name"));
                        tvHeaderEmail.setText(doc.getString("email"));

                        String farm = doc.getString("farmName");

                        if (getSupportActionBar() != null)
                            getSupportActionBar().setTitle(
                                    farm != null ? farm : "Dairy Farm");
                    });
        });
    }
    public void refreshSubscription() {

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    Long planEnd = doc.getLong("planEndDate");

                    if (planEnd == null) return;

                    long currentTime = System.currentTimeMillis();

                    if (currentTime > planEnd) {

                        new AlertDialog.Builder(this)
                                .setTitle("Subscription Expired")
                                .setMessage("Please renew your plan.")
                                .setPositiveButton("Go to Subscription",
                                        (d, w) -> loadFragment(new Suscription()))
                                .setCancelable(false)
                                .show();
                    }
                });
    }
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {

        Fragment current =
                getSupportFragmentManager()
                        .findFragmentById(R.id.framlayout);

        if (current instanceof Suscription) {

            ((Suscription) current)
                    .handlePaymentSuccess(razorpayPaymentId);
        }
    }
    @Override
    public void onPaymentError(int code, String response) {

        Fragment current =
                getSupportFragmentManager()
                        .findFragmentById(R.id.framlayout);

        if (current instanceof Suscription) {

            ((Suscription) current)
                    .handlePaymentFailure();
        }
    }
    private void adminLogout() {

        mAuth.signOut();
        adminSp.edit().clear().apply();

        Intent intent = new Intent(this, AdminLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
    private void loadFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framlayout, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }
}