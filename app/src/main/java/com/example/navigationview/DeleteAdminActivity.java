package com.example.navigationview;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeleteAdminActivity extends AppCompatActivity {

    EditText etAdminId;
    TextView tvAdminName,tvStartDate;
    Button btnDelete;

    FirebaseFirestore db;

    String adminUID="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_admin);

        etAdminId=findViewById(R.id.etAdminId);
        tvAdminName=findViewById(R.id.tvAdminName);
        tvStartDate=findViewById(R.id.tvStartDate);
        btnDelete=findViewById(R.id.btnDeleteAdmin);

        db=FirebaseFirestore.getInstance();

        etAdminId.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count){}

            @Override
            public void afterTextChanged(Editable s){

                if(s.length()>0){

                    int id=Integer.parseInt(s.toString());

                    loadAdmin(id);

                }

            }
        });


        btnDelete.setOnClickListener(v->confirmDelete());

    }


    private void loadAdmin(int id){

        db.collection("Admins")
                .whereEqualTo("Admin ID",id)
                .get()
                .addOnSuccessListener(query->{

                    if(!query.isEmpty()){

                        var doc=query.getDocuments().get(0);

                        adminUID=doc.getId();

                        tvAdminName.setText(
                                "Name : "+doc.getString("name"));

                        long start=doc.getLong("planStartDate");

                        SimpleDateFormat sdf=
                                new SimpleDateFormat(
                                        "dd MMM yyyy",
                                        Locale.getDefault());

                        tvStartDate.setText(
                                "Start : "+sdf.format(new Date(start)));

                    }

                });

    }



    private void confirmDelete(){

        new AlertDialog.Builder(this)

                .setTitle("Delete Admin")

                .setMessage("Are you sure you want to delete this admin?")

                .setPositiveButton("Yes",(d,w)->deleteAdmin())

                .setNegativeButton("Cancel",null)

                .show();

    }


    private void deleteAdmin(){

        if(adminUID.isEmpty()){

            Toast.makeText(this,
                    "Admin not found",
                    Toast.LENGTH_LONG).show();

            return;
        }

        db.collection("Admins")
                .document(adminUID)
                .delete()
                .addOnSuccessListener(v->{

                    Toast.makeText(this,
                            "Admin Deleted",
                            Toast.LENGTH_LONG).show();

                    finish();

                });

    }

}