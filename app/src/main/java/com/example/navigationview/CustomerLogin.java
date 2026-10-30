package com.example.navigationview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerLogin extends AppCompatActivity {

    EditText etAdminId, etEmail, etPassword;
    Button btnLogin;
    TextView btnForgot;
    ProgressBar progressBar;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    SharedPreferences customerSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customerSp = getSharedPreferences("customer_session", MODE_PRIVATE);

        int savedId = customerSp.getInt("customer_id", -1);
        if (savedId != -1) {
            startActivity(new Intent(this, CustomerDashboard.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_customer_login);

        etAdminId = findViewById(R.id.etAdminId);
        etEmail = findViewById(R.id.customeremail);
        etPassword = findViewById(R.id.customerPassword);

        btnLogin = findViewById(R.id.customerlogin);
        btnForgot = findViewById(R.id.btnForgotPassword);
        progressBar = findViewById(R.id.loginProgress);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> loginCustomer());
        btnForgot.setOnClickListener(v -> resetPassword());
    }

    private void loginCustomer() {

        String adminIdStr = etAdminId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (adminIdStr.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        long adminId;
        try {
            adminId = Long.parseLong(adminIdStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Admin ID", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        showButton();
                        Toast.makeText(this,
                                "Wrong Email or Password",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null) {
                        showButton();
                        return;
                    }


                    String customerUID = user.getUid();

                    db.collection("Admins")
                            .whereEqualTo("Admin ID", adminId)
                            .get()
                            .addOnSuccessListener(adminSnapshot -> {

                                if (adminSnapshot.isEmpty()) {
                                    showButton();
                                    Toast.makeText(this,
                                            "Admin not found",
                                            Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    return;
                                }

                                String adminUID =
                                        adminSnapshot.getDocuments()
                                                .get(0)
                                                .getId();

                                db.collection("Admins")
                                        .document(adminUID)
                                        .collection("Customers")
                                        .document(customerUID)
                                        .get()
                                        .addOnSuccessListener(customerDoc -> {

                                            if (!customerDoc.exists()) {
                                                showButton();
                                                Toast.makeText(this,
                                                        "Access Denied",
                                                        Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                                return;
                                            }

                                            Long idLong =
                                                    customerDoc.getLong("customerId");

                                            if (idLong == null) {
                                                showButton();
                                                Toast.makeText(this,
                                                        "Customer ID missing",
                                                        Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                                return;
                                            }

                                            int customerId = idLong.intValue();

                                            customerSp.edit()
                                                    .putInt("customer_id", customerId)
                                                    .putString("admin_uid", adminUID)
                                                    .putString("customer_uid", customerUID)
                                                    .apply();

                                            Intent intent =
                                                    new Intent(this,
                                                            CustomerDashboard.class);

                                            intent.setFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            );

                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            showButton();
                                            Toast.makeText(this,
                                                    "Customer load error",
                                                    Toast.LENGTH_SHORT).show();
                                        });

                            })
                            .addOnFailureListener(e -> {
                                showButton();
                                Toast.makeText(this,
                                        "Admin load error",
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void resetPassword() {

        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this,
                    "Enter your email first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Password reset link sent to your email",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void showButton() {
        progressBar.setVisibility(View.GONE);
        btnLogin.setVisibility(View.VISIBLE);
    }
}