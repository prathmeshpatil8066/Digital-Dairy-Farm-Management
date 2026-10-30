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

public class AdminLogin extends AppCompatActivity {

    EditText email, password;
    Button loginBtn;
    ProgressBar progressBar;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    SharedPreferences adminSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_login);

        email = findViewById(R.id.adminemail);
        password = findViewById(R.id.adminPassword);
        loginBtn = findViewById(R.id.adminlogin);
        progressBar = findViewById(R.id.loginProgress);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        adminSp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);

        // 🔐 SESSION CHECK
        boolean isLoggedIn =
                adminSp.getBoolean("isAdminLoggedIn", false);

        String uid =
                adminSp.getString("admin_uid", null);

        if (isLoggedIn && uid != null) {

            checkRoleAndRedirect(uid);
            return;
        }

        loginBtn.setOnClickListener(v -> loginAdmin());
    }


    private void loginAdmin() {

        String e = email.getText().toString().trim();
        String p = password.getText().toString().trim();

        if (e.isEmpty()) {
            email.setError("Enter Email");
            return;
        }

        if (p.isEmpty()) {
            password.setError("Enter Password");
            return;
        }

        loginBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(e, p)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        showLoginButton();

                        Toast.makeText(this,
                                "Wrong Email or Password",
                                Toast.LENGTH_LONG).show();

                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null) {
                        showLoginButton();
                        return;
                    }

                    String uid = user.getUid();

                    checkRoleAndRedirect(uid);
                });
    }


    // 🔑 ROLE CHECK
    private void checkRoleAndRedirect(String uid) {

        db.collection("Admins")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {

                        mAuth.signOut();
                        showLoginButton();

                        Toast.makeText(this,
                                "Access Denied",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = doc.getString("role");

                    adminSp.edit()
                            .putBoolean("isAdminLoggedIn", true)
                            .putString("admin_uid", uid)
                            .apply();

                    Intent intent;

                    if (role != null && role.equals("superadmin")) {

                        intent = new Intent(
                                AdminLogin.this,
                                SuperAdminDashboard.class);

                    } else {

                        intent = new Intent(
                                AdminLogin.this,
                                MainActivity.class);
                    }

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    showLoginButton();

                    Toast.makeText(this,
                            "Firestore Error",
                            Toast.LENGTH_LONG).show();
                });
    }


    private void showLoginButton() {

        progressBar.setVisibility(View.GONE);
        loginBtn.setVisibility(View.VISIBLE);
    }
}