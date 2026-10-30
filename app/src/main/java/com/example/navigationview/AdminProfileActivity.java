package com.example.navigationview;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminProfileActivity extends AppCompatActivity {

    TextView tvName,tvFarm,tvCustomers,tvPlan,tvStart,tvEnd,tvRemaining;

    FirebaseFirestore db;

    String adminUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        tvName=findViewById(R.id.tvName);
        tvFarm=findViewById(R.id.tvFarm);
        tvCustomers=findViewById(R.id.tvCustomers);
        tvPlan=findViewById(R.id.tvPlan);
        tvStart=findViewById(R.id.tvStart);
        tvEnd=findViewById(R.id.tvEnd);
        tvRemaining=findViewById(R.id.tvRemaining);

        adminUID=getIntent().getStringExtra("adminUID");

        db=FirebaseFirestore.getInstance();

        loadAdmin();
    }

    private void loadAdmin(){

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    if(doc.exists()){

                        tvName.setText(doc.getString("name"));
                        tvFarm.setText(doc.getString("farmName"));

                        tvPlan.setText(
                                "Plan : "+doc.getString("planName"));

                        long start=doc.getLong("planStartDate");
                        long end=doc.getLong("planEndDate");

                        SimpleDateFormat sdf =
                                new SimpleDateFormat("dd MMM yyyy",
                                        Locale.getDefault());

                        tvStart.setText(
                                "Start : "+sdf.format(new Date(start)));

                        tvEnd.setText(
                                "End : "+sdf.format(new Date(end)));

                        long now=System.currentTimeMillis();

                        long remaining =
                                (end-now)/(1000*60*60*24);

                        tvRemaining.setText(
                                "Remaining Days : "+remaining);

                    }

                });


        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .get()
                .addOnSuccessListener(q -> {

                    tvCustomers.setText(
                            "Customers : "+q.size());

                });

    }
}