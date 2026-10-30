package com.example.navigationview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OpetionPage extends AppCompatActivity {

    Button btnAdminLogin, btnCustomerLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences adminSp =
                getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);

        SharedPreferences customerSp =
                getSharedPreferences("customer_session", MODE_PRIVATE);

        boolean isAdminLoggedIn =
                adminSp.getBoolean("isAdminLoggedIn", false);

        String adminUID =
                adminSp.getString("admin_uid", null);

        boolean isCustomerLoggedIn =
                customerSp.contains("customer_id");


        if (isAdminLoggedIn && adminUID != null) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        if (isCustomerLoggedIn) {

            Intent intent = new Intent(this, CustomerDashboard.class);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_opetion_page);

        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        btnCustomerLogin = findViewById(R.id.btnCustomerLogin);

        btnAdminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLogin.class))
        );

        btnCustomerLogin.setOnClickListener(v ->
                startActivity(new Intent(this, CustomerLogin.class))
        );
    }
}