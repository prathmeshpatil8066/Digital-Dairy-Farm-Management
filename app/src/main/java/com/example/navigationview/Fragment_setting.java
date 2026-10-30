package com.example.navigationview;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class Fragment_setting extends Fragment {

    TextView tvAdminName, tvAdminMobile, tvAdminGender;
    Button btnEditProfile, btnUpdatePassword;
    EditText etDeleteCustomerId;
    Button btnDeleteCustomer;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    String adminUID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.setting_fragment, container, false);

        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAdminMobile = view.findViewById(R.id.tvAdminMobile);
        tvAdminGender = view.findViewById(R.id.tvAdminGender);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword);

        etDeleteCustomerId = view.findViewById(R.id.etDeleteCustomerId);
        btnDeleteCustomer = view.findViewById(R.id.btnDeleteCustomer);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getActivity(),
                    "Session expired",
                    Toast.LENGTH_SHORT).show();
            return view;
        }

        adminUID = mAuth.getCurrentUser().getUid();

        loadAdminProfile();

        btnEditProfile.setOnClickListener(v -> showEditDialog());
        btnUpdatePassword.setOnClickListener(v -> sendPasswordReset());
        btnDeleteCustomer.setOnClickListener(v -> confirmDeleteCustomer());

        return view;
    }

    private void loadAdminProfile() {

        db.collection("Admins")
                .document(adminUID)
                .addSnapshotListener((document, error) -> {

                    if (document == null || !document.exists()) return;

                    String name = document.getString("name");
                    String mobile = document.getString("mobile");
                    String gender = document.getString("gender");

                    tvAdminName.setText("Name : " + (name != null ? name : "--"));
                    tvAdminMobile.setText("Mobile : " + (mobile != null ? mobile : "--"));
                    tvAdminGender.setText("Gender : " + (gender != null ? gender : "--"));
                });
    }

    private void showEditDialog() {

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40,20,40,10);

        EditText etName = new EditText(getContext());
        etName.setHint("Enter Name");

        EditText etMobile = new EditText(getContext());
        etMobile.setHint("Enter Mobile");
        etMobile.setInputType(InputType.TYPE_CLASS_PHONE);

        layout.addView(etName);
        layout.addView(etMobile);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {

                    if(etName.getText().toString().trim().isEmpty()){
                        Toast.makeText(getActivity(),"Name required",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String,Object> map = new HashMap<>();
                    map.put("name", etName.getText().toString().trim());
                    map.put("mobile", etMobile.getText().toString().trim());

                    db.collection("Admins")
                            .document(adminUID)
                            .update(map)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(getActivity(),
                                            "Profile Updated",
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordReset() {

        String email = mAuth.getCurrentUser().getEmail();

        if(email == null) return;

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getActivity(),
                                "Password reset email sent",
                                Toast.LENGTH_LONG).show());
    }

    private void confirmDeleteCustomer() {

        String id = etDeleteCustomerId.getText().toString().trim();

        if (id.isEmpty()) {
            Toast.makeText(getActivity(),
                    "Enter Customer ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Customer")
                .setMessage("This will permanently delete customer, bills & payments.")
                .setPositiveButton("Delete", (dialog, which) ->
                        deleteCustomer(id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCustomer(String customerIdStr) {

        ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage("Deleting customer...");
        dialog.setCancelable(false);
        dialog.show();

        int customerId;

        try {
            customerId = Integer.parseInt(customerIdStr);
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(getActivity(),
                    "Invalid Customer ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        dialog.dismiss();
                        Toast.makeText(getActivity(),
                                "Customer Not Found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String customerUID = doc.getId();

                        doc.getReference().delete();

                        deleteSubCollection("Bills", customerUID);

                        deleteSubCollection("Payments", customerUID);
                    }

                    dialog.dismiss();

                    Toast.makeText(getActivity(),
                            "Customer Deleted Successfully",
                            Toast.LENGTH_LONG).show();

                    etDeleteCustomerId.setText("");
                })
                .addOnFailureListener(e -> dialog.dismiss());
    }

    private void deleteSubCollection(String collectionName, String customerUID){

        db.collection("Admins")
                .document(adminUID)
                .collection(collectionName)
                .whereEqualTo("customerUID", customerUID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }
}