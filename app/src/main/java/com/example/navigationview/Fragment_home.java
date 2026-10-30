package com.example.navigationview;

import android.os.Bundle;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_home extends Fragment {

    TextView tvDate, tvMilkTime;
    TextView tvCowQty, tvBuffaloQty;
    TextView tvTotalMilk, tvTotalCustomers, tvTotalAmount;
    TextView tvLastUpdate, tvShiftInfo;
    ProgressBar pbCow, pbBuffalo;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String adminUID;

    ListenerRegistration billListener;

    String todayDate, milkTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        tvDate = view.findViewById(R.id.tvDate);
        tvMilkTime = view.findViewById(R.id.tvMilkTime);

        tvCowQty = view.findViewById(R.id.tvCowQty);
        tvBuffaloQty = view.findViewById(R.id.tvBuffaloQty);

        tvTotalMilk = view.findViewById(R.id.tvTotalMilk);
        tvTotalCustomers = view.findViewById(R.id.tvTotalCustomers);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        tvLastUpdate = view.findViewById(R.id.tvLastUpdate);
        tvShiftInfo = view.findViewById(R.id.tvShiftInfo);

        pbCow = view.findViewById(R.id.pbCow);
        pbBuffalo = view.findViewById(R.id.pbBuffalo);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null)
            return view;

        adminUID = mAuth.getCurrentUser().getUid();

        detectDateAndShift();
        loadTodaySummary();

        return view;
    }

    private void detectDateAndShift() {

        todayDate = new SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()).format(new Date());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        milkTime = (hour < 12) ? "Morning" : "Evening";

        tvDate.setText(todayDate);
        tvMilkTime.setText(milkTime);
        tvShiftInfo.setText("Shift : " + milkTime);
    }

    private void loadTodaySummary() {

        billListener = db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("date", todayDate)
                .whereEqualTo("milkTime", milkTime)
                .addSnapshotListener((snapshot, error) -> {

                    if (snapshot == null) return;

                    double cowQty = 0;
                    double buffaloQty = 0;
                    double totalAmount = 0;

                    Set<String> uniqueCustomers = new HashSet<>();

                    for (DocumentSnapshot bill : snapshot) {

                        String milkType = bill.getString("milkType");
                        Double qty = bill.getDouble("qty");
                        Double amount = bill.getDouble("totalAmount");
                        String uid = bill.getString("customerUID");

                        if (qty == null) qty = 0.0;
                        if (amount == null) amount = 0.0;

                        if ("Cow".equalsIgnoreCase(milkType))
                            cowQty += qty;
                        else if ("Buffalo".equalsIgnoreCase(milkType))
                            buffaloQty += qty;

                        totalAmount += amount;

                        if (uid != null)
                            uniqueCustomers.add(uid);
                    }

                    double totalMilk = cowQty + buffaloQty;

                    // UI UPDATE
                    tvCowQty.setText(String.format(
                            Locale.getDefault(),
                            "%.2f L", cowQty));

                    tvBuffaloQty.setText(String.format(
                            Locale.getDefault(),
                            "%.2f L", buffaloQty));

                    tvTotalMilk.setText("Total Milk : " +
                            String.format(Locale.getDefault(),
                                    "%.2f L", totalMilk));

                    tvTotalCustomers.setText("Customers : " +
                            uniqueCustomers.size());

                    tvTotalAmount.setText("Total Amount : ₹ " +
                            String.format(Locale.getDefault(),
                                    "%.2f", totalAmount));

                    // Progress %
                    if (totalMilk > 0) {

                        int cowPercent = (int)((cowQty / totalMilk) * 100);
                        int buffaloPercent = 100 - cowPercent;

                        pbCow.setProgress(cowPercent);
                        pbBuffalo.setProgress(buffaloPercent);
                    }

                    tvLastUpdate.setText("Updated at : " +
                            new SimpleDateFormat("hh:mm:ss a",
                                    Locale.getDefault()).format(new Date()));
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (billListener != null)
            billListener.remove();
    }
}