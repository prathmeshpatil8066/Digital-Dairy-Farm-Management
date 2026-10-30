package com.example.navigationview;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ManageAdminActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    List<DocumentSnapshot> list = new ArrayList<>();

    FirebaseFirestore db;

    SuperAdminAdapter adapter;

    ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manage_admin);

        recyclerView = findViewById(R.id.adminRecycler);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        adapter = new SuperAdminAdapter(this, list);

        recyclerView.setAdapter(adapter);

        loadAdmins();
    }

    private void loadAdmins() {

        listener = db.collection("Admins")
                .whereEqualTo("role", "admin")
                .addSnapshotListener((snapshot, error) -> {

                    if (snapshot == null) return;

                    list.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        list.add(doc);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (listener != null)
            listener.remove();
    }
}