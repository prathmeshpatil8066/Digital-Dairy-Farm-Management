package com.example.navigationview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SuperAdminDashboard extends AppCompatActivity {

    TextView tvTotalAdmins;
    Button btnAddAdmin,btnManageAdmins,btnDeleteAdmin,btnLogout;

    FirebaseFirestore db;
    FirebaseAuth auth;

    SharedPreferences adminSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_admin_dashboard);

        tvTotalAdmins=findViewById(R.id.tvTotalAdmins);

        btnAddAdmin=findViewById(R.id.btnAddAdmin);
        btnManageAdmins=findViewById(R.id.btnManageAdmins);
        btnDeleteAdmin=findViewById(R.id.btnDeleteAdmin);
        btnLogout=findViewById(R.id.btnLogout);

        db=FirebaseFirestore.getInstance();
        auth=FirebaseAuth.getInstance();

        adminSp=getSharedPreferences("ADMIN_SESSION",MODE_PRIVATE);

        loadAdminsRealtime();

        btnAddAdmin.setOnClickListener(v->{

            startActivity(new Intent(
                    SuperAdminDashboard.this,
                    AddAdminActivity.class));

        });


        btnManageAdmins.setOnClickListener(v->{

            startActivity(new Intent(
                    SuperAdminDashboard.this,
                    ManageAdminActivity.class));

        });


        btnDeleteAdmin.setOnClickListener(v->{

            startActivity(new Intent(
                    SuperAdminDashboard.this,
                    DeleteAdminActivity.class));

        });


        btnLogout.setOnClickListener(v->logout());

    }


    // REALTIME REFRESH
    private void loadAdminsRealtime(){

        db.collection("Admins")
                .whereEqualTo("role","admin")
                .addSnapshotListener((value,error)->{

                    if(value==null) return;

                    int count=value.size();

                    tvTotalAdmins.setText(String.valueOf(count));

                });

    }



    private void logout(){

        auth.signOut();

        adminSp.edit().clear().apply();

        Intent i=new Intent(
                SuperAdminDashboard.this,
                AdminLogin.class);

        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(i);
    }


    // BACK BLOCK
    @Override
    public void onBackPressed(){

    }

}