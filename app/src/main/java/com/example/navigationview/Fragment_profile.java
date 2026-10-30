package com.example.navigationview;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class Fragment_profile extends Fragment {

    TextView tvId, tvName, tvMobile, tvGender, tvEmail, tvFarmName;
    TextView tvNameTop, tvMobileTop;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ListenerRegistration listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        tvId = view.findViewById(R.id.tvId);
        tvName = view.findViewById(R.id.tvName);
        tvMobile = view.findViewById(R.id.tvMobile);
        tvGender = view.findViewById(R.id.tvGender);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvFarmName = view.findViewById(R.id.tvFarmName);

        tvNameTop = view.findViewById(R.id.tvNameTop);
        tvMobileTop = view.findViewById(R.id.tvMobileTop);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            loadAdminProfile();
        } else {
            Toast.makeText(getActivity(),
                    "Session expired",
                    Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadAdminProfile() {

        String uid = mAuth.getCurrentUser().getUid();

        listener = db.collection("Admins")
                .document(uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(getActivity(),
                                "Failed to load profile",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    Long adminIdLong = snapshot.getLong("Admin ID");
                    if (adminIdLong == null)
                        adminIdLong = snapshot.getLong("adminId");

                    String adminId = adminIdLong != null ?
                            String.valueOf(adminIdLong) : "--";

                    String name = snapshot.getString("name");
                    String mobile = snapshot.getString("mobile");
                    String gender = snapshot.getString("gender");
                    String email = snapshot.getString("email");
                    String farmName = snapshot.getString("farmName");

                    tvId.setText(adminId);
                    tvName.setText(name != null ? name : "--");
                    tvMobile.setText(mobile != null ? mobile : "--");
                    tvGender.setText(gender != null ? gender : "--");
                    tvEmail.setText(email != null ? email : "--");
                    tvFarmName.setText(farmName != null ? farmName : "--");

                    tvNameTop.setText(name != null ? name : "--");
                    tvMobileTop.setText(mobile != null ? mobile : "--");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }
}