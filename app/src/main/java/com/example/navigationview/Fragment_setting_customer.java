package com.example.navigationview;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;

public class Fragment_setting_customer extends Fragment {

    EditText etOldPassword, etNewPassword, etConfirmPassword, etMobile;
    Button btnUpdatePassword, btnUpdateMobile, btnUpdateMilkType, btnLogout;
    Spinner spinnerMilkType;
    Switch switchDarkMode;

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    String customerUID;
    String adminUID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.setting_customer_fragment, container, false);

        etOldPassword = view.findViewById(R.id.etOldPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        etMobile = view.findViewById(R.id.etMobile);

        btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword);
        btnUpdateMobile = view.findViewById(R.id.btnUpdateMobile);
        btnUpdateMilkType = view.findViewById(R.id.btnUpdateMilkType);
        btnLogout = view.findViewById(R.id.btnLogout);

        spinnerMilkType = view.findViewById(R.id.spinnerMilkType);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (user == null) {
            logout();
            return view;
        }

        customerUID = user.getUid();

        SharedPreferences sp =
                requireActivity().getSharedPreferences("customer_session", MODE_PRIVATE);

        adminUID = sp.getString("admin_uid", null);

        if (adminUID == null) {
            Toast.makeText(getActivity(),"Session expired",Toast.LENGTH_LONG).show();
            logout();
            return view;
        }

        setupSpinner();
        loadCurrentUserData();
        setupDarkMode();

        btnUpdatePassword.setOnClickListener(v -> updatePassword());
        btnUpdateMobile.setOnClickListener(v -> updateMobile());
        btnUpdateMilkType.setOnClickListener(v -> updateMilkType());
        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    private void updatePassword() {

        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPass) ||
                TextUtils.isEmpty(newPass) ||
                TextUtils.isEmpty(confirmPass)) {

            Toast.makeText(getActivity(),
                    "All password fields required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(getActivity(),
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(getActivity(),
                    "Passwords do not match",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if(user.getEmail()==null){
            Toast.makeText(getActivity(),
                    "Email not found",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), oldPass);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        user.updatePassword(newPass)
                                .addOnSuccessListener(aVoid -> {

                                    Toast.makeText(getActivity(),
                                            "Password updated successfully",
                                            Toast.LENGTH_LONG).show();

                                    etOldPassword.setText("");
                                    etNewPassword.setText("");
                                    etConfirmPassword.setText("");
                                }))
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(),
                                "Old password incorrect",
                                Toast.LENGTH_LONG).show());
    }

    private void updateMobile() {

        String mobile = etMobile.getText().toString().trim();

        if (mobile.isEmpty()) {
            Toast.makeText(getActivity(),
                    "Enter mobile number",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mobile.length() < 10) {
            Toast.makeText(getActivity(),
                    "Enter valid mobile number",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .document(customerUID)
                .update("mobile", mobile)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getActivity(),
                                "Mobile updated successfully",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(),
                                "Update failed",
                                Toast.LENGTH_SHORT).show());
    }

    private void updateMilkType() {

        String milkType = spinnerMilkType.getSelectedItem().toString();

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .document(customerUID)
                .update("milkType", milkType)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getActivity(),
                                "Milk type updated",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(),
                                "Update failed",
                                Toast.LENGTH_SHORT).show());
    }

    private void loadCurrentUserData() {

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .document(customerUID)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if(snapshot.exists()){

                        String mobile = snapshot.getString("mobile");
                        String milkType = snapshot.getString("milkType");

                        if(mobile!=null)
                            etMobile.setText(mobile);

                        if(milkType!=null){
                            int pos = ((ArrayAdapter)spinnerMilkType.getAdapter())
                                    .getPosition(milkType);
                            spinnerMilkType.setSelection(pos);
                        }
                    }
                });
    }

    private void setupDarkMode() {

        SharedPreferences sp =
                requireActivity().getSharedPreferences("app_settings", MODE_PRIVATE);

        boolean isDark = sp.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDark);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {

            sp.edit().putBoolean("dark_mode", isChecked).apply();

            if (isChecked)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void logout() {

        SharedPreferences sp =
                requireActivity().getSharedPreferences("customer_session", MODE_PRIVATE);

        sp.edit().clear().apply();
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(getActivity(), OpetionPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setupSpinner() {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        Arrays.asList("Cow", "Buffalo"));

        spinnerMilkType.setAdapter(adapter);
    }
}