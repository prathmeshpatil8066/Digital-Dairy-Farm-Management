package com.example.navigationview;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import static android.content.Context.MODE_PRIVATE;

public class Fragment_profile_customer extends Fragment {

    TextView tvTopMobile, tvTopName;
    TextView tvId, tvName, tvMobile, tvEmail, tvMilkType;
    TextView tvDairyFarm, tvAdminName, tvAdminId;

    String customerUID, adminUID;
    FirebaseFirestore db;

    ListenerRegistration customerListener;
    ListenerRegistration adminListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.profile_customer_fragment,
                container,
                false);

        tvTopMobile = view.findViewById(R.id.tvTopMobile);
        tvTopName   = view.findViewById(R.id.tvTopName);

        tvId        = view.findViewById(R.id.tvId);
        tvName      = view.findViewById(R.id.tvName);
        tvMobile    = view.findViewById(R.id.tvMobile);
        tvEmail     = view.findViewById(R.id.tvEmail);
        tvMilkType  = view.findViewById(R.id.tvMilkType);

        tvDairyFarm = view.findViewById(R.id.tvDairyFarm);
        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAdminId   = view.findViewById(R.id.tvAdminId);

        SharedPreferences sp =
                requireActivity().getSharedPreferences(
                        "customer_session",
                        MODE_PRIVATE);

        customerUID = sp.getString("customer_uid", null);
        adminUID    = sp.getString("admin_uid", null);

        if (customerUID == null || adminUID == null) {
            Toast.makeText(getActivity(),
                    "Session expired",
                    Toast.LENGTH_SHORT).show();
            return view;
        }

        db = FirebaseFirestore.getInstance();

        attachRealtimeCustomerProfile();
        attachRealtimeAdminProfile();

        return view;
    }

    private void attachRealtimeCustomerProfile() {

        customerListener = db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .document(customerUID)
                .addSnapshotListener((doc, error) -> {

                    if (error != null) {
                        Toast.makeText(getActivity(),
                                "Customer load failed",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (doc == null || !doc.exists()) {
                        return;
                    }

                    String name = doc.getString("name");
                    String mobile = doc.getString("mobile");
                    String milkType = doc.getString("milkType");
                    Long customerId = doc.getLong("customerId");

                    String email = FirebaseAuth.getInstance()
                            .getCurrentUser() != null ?
                            FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getEmail()
                            : "-";

                    tvTopName.setText(name != null ? name : "-");
                    tvTopMobile.setText(mobile != null ? mobile : "-");

                    tvId.setText(customerId != null ?
                            String.valueOf(customerId) : "-");

                    tvName.setText(name != null ? name : "-");
                    tvMobile.setText(mobile != null ? mobile : "-");
                    tvEmail.setText(email != null ? email : "-");
                    tvMilkType.setText(milkType != null ? milkType : "-");
                });
    }

    private void attachRealtimeAdminProfile() {

        adminListener = db.collection("Admins")
                .document(adminUID)
                .addSnapshotListener((doc, error) -> {

                    if (error != null || doc == null || !doc.exists())
                        return;

                    String adminName = doc.getString("name");
                    String farmName = doc.getString("farmName");

                    Long adminId = doc.getLong("Admin ID");
                    if (adminId == null)
                        adminId = doc.getLong("adminId");

                    tvAdminName.setText(adminName != null ? adminName : "-");
                    tvDairyFarm.setText(farmName != null ? farmName : "-");
                    tvAdminId.setText(adminId != null ?
                            String.valueOf(adminId) : "-");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (customerListener != null) {
            customerListener.remove();
            customerListener = null;
        }

        if (adminListener != null) {
            adminListener.remove();
            adminListener = null;
        }
    }
}