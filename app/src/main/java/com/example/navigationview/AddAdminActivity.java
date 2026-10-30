package com.example.navigationview;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class AddAdminActivity extends AppCompatActivity {

    EditText etName,etEmail,etPassword,etMobile,etFarm;
    Spinner spGender;
    Button btnCreate;

    FirebaseAuth auth;
    FirebaseFirestore db;

    String[] genders={"Male","Female","Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_admin);

        etName=findViewById(R.id.etName);
        etEmail=findViewById(R.id.etEmail);
        etPassword=findViewById(R.id.etPassword);
        etMobile=findViewById(R.id.etMobile);
        etFarm=findViewById(R.id.etFarm);

        spGender=findViewById(R.id.spGender);
        btnCreate=findViewById(R.id.btnCreateAdmin);

        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();

        ArrayAdapter<String> adapter=
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        genders);

        spGender.setAdapter(adapter);

        btnCreate.setOnClickListener(v->createAdmin());

    }



    private void createAdmin(){

        String name=etName.getText().toString();
        String email=etEmail.getText().toString();
        String pass=etPassword.getText().toString();
        String mobile=etMobile.getText().toString();
        String farm=etFarm.getText().toString();
        String gender=spGender.getSelectedItem().toString();

        if(name.isEmpty()||email.isEmpty()||pass.isEmpty()){

            Toast.makeText(this,
                    "Fill all fields",
                    Toast.LENGTH_LONG).show();

            return;
        }

        auth.createUserWithEmailAndPassword(email,pass)
                .addOnSuccessListener(authResult->{

                    String uid=
                            authResult.getUser().getUid();

                    generateAdminId(uid,name,email,mobile,farm,gender);

                });

    }



    private void generateAdminId(String uid,String name,String email,
                                 String mobile,String farm,String gender){

        DocumentReference counterRef=
                db.collection("Counters")
                        .document("adminCounter");


        db.runTransaction(transaction->{

            DocumentSnapshot snapshot=
                    transaction.get(counterRef);

            long newId=1;

            if(snapshot.exists())
                newId=snapshot.getLong("lastId")+1;

            transaction.set(counterRef,
                    Collections.singletonMap("lastId",newId));

            return newId;

        }).addOnSuccessListener(adminId->{

            saveAdmin(uid,adminId,name,email,mobile,farm,gender);

        });

    }



    private void saveAdmin(String uid,long adminId,String name,
                           String email,String mobile,String farm,String gender){

        long start=System.currentTimeMillis();

        long end=start+(7L*24*60*60*1000);

        Map<String,Object> admin=new HashMap<>();

        admin.put("Admin ID",adminId);
        admin.put("name",name);
        admin.put("email",email);
        admin.put("mobile",mobile);
        admin.put("farmName",farm);
        admin.put("gender",gender);

        admin.put("role","admin");

        admin.put("trialStartDate",start);
        admin.put("planStartDate",start);
        admin.put("planEndDate",end);

        admin.put("planName","Trial Plan");
        admin.put("planId","trial");

        admin.put("active",true);
        admin.put("maxCustomers",50);

        admin.put("nextCustomerId",1);

        db.collection("Admins")
                .document(uid)
                .set(admin)
                .addOnSuccessListener(v->{

                    Toast.makeText(this,
                            "Admin Created",
                            Toast.LENGTH_LONG).show();

                    finish();

                });

    }

}