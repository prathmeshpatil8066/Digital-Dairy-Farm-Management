package com.example.navigationview;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.razorpay.Checkout;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class Suscription extends Fragment {

    Button btnPlan1, btnPlan2, btnPlan3;
    TextView tvCurrentPlan, tvRemainingDays, tvStartDate, tvEndDate, tvPaymentHistory;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String adminUID;

    String selectedPlanId;
    String selectedPlanName;

    int selectedMaxCustomers;
    int selectedSetupAmount;
    int selectedYearlyAmount;
    int selectedFinalAmount;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_suscription, container, false);

        btnPlan1 = view.findViewById(R.id.btnPlan1);
        btnPlan2 = view.findViewById(R.id.btnPlan2);
        btnPlan3 = view.findViewById(R.id.btnPlan3);

        tvCurrentPlan = view.findViewById(R.id.tvCurrentPlan);
        tvRemainingDays = view.findViewById(R.id.tvRemainingDays);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        tvPaymentHistory = view.findViewById(R.id.tvPaymentHistory);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null)
            adminUID = mAuth.getCurrentUser().getUid();

        loadPlans();
        loadCurrentPlan();
        loadPaymentHistory();

        return view;
    }


    private void loadPlans() {

        db.collection("Plans")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<DocumentSnapshot> plans =
                            new ArrayList<>(snapshot.getDocuments());

                    if (plans.size() > 0)
                        setPlan(btnPlan1, plans.get(0));
                    if (plans.size() > 1)
                        setPlan(btnPlan2, plans.get(1));
                    if (plans.size() > 2)
                        setPlan(btnPlan3, plans.get(2));
                });
    }

    private void setPlan(Button button, DocumentSnapshot doc) {

        String title = doc.getString("title");
        String description = doc.getString("description");
        Long setup = doc.getLong("setupPrice");
        Long yearly = doc.getLong("YearlySupport");
        Long maxCust = doc.getLong("maxCustomers");

        if (title == null) return;

        int setupAmount = setup != null ? setup.intValue() : 0;
        int yearlyAmount = yearly != null ? yearly.intValue() : 0;
        int maxCustomers = maxCust != null ? maxCust.intValue() : 50;

        String text =
                title + "\n\n" +
                        (description != null ? description : "") +
                        "\n\nSetup ₹" + setupAmount +
                        " | Yearly ₹" + yearlyAmount;

        button.setText(text);

        button.setOnClickListener(v -> {

            selectedPlanId = doc.getId();
            selectedPlanName = title;
            selectedMaxCustomers = maxCustomers;
            selectedSetupAmount = setupAmount;
            selectedYearlyAmount = yearlyAmount;

            preparePayment();

        });
    }

    private void preparePayment() {

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean isSetupPaid = doc.getBoolean("isSetupPaid");

                    if (isSetupPaid == null || !isSetupPaid) {

                        selectedFinalAmount = selectedSetupAmount;

                    } else {

                        selectedFinalAmount = selectedYearlyAmount;
                    }

                    startPayment();
                });
    }


    private void startPayment() {

        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_S5ikuWG3lHifGg"); // 🔁 replace with live key in production

        try {
            JSONObject options = new JSONObject();
            options.put("name", "Digital Dairy Farm");
            options.put("description", selectedPlanName);
            options.put("currency", "INR");
            options.put("amount", selectedFinalAmount * 100);

            checkout.open(requireActivity(), options);

        } catch (Exception e) {
            Toast.makeText(getActivity(),
                    "Payment Error",
                    Toast.LENGTH_LONG).show();
        }
    }


    public void handlePaymentSuccess(String paymentId) {

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    Long existingEnd = doc.getLong("planEndDate");
                    Boolean isSetupPaid = doc.getBoolean("isSetupPaid");

                    long now = System.currentTimeMillis();
                    long oneYear = 365L * 24 * 60 * 60 * 1000;

                    long base = now;

                    if (existingEnd != null && existingEnd > now)
                        base = existingEnd;

                    Map<String, Object> update = new HashMap<>();
                    update.put("planId", selectedPlanId);
                    update.put("planName", selectedPlanName);
                    update.put("planStartDate", base);
                    update.put("planEndDate", base + oneYear);
                    update.put("maxCustomers", selectedMaxCustomers);
                    update.put("active", true);

                    if (isSetupPaid == null || !isSetupPaid)
                        update.put("isSetupPaid", true);

                    db.collection("Admins")
                            .document(adminUID)
                            .update(update);

                    savePaymentHistory(paymentId, now);

                    Toast.makeText(getActivity(),
                            "Payment Successful",
                            Toast.LENGTH_LONG).show();

                    loadCurrentPlan();
                    loadPaymentHistory();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshSubscription();
                    }
                });
    }

    public void handlePaymentFailure() {

        Toast.makeText(getActivity(),
                "Payment Failed",
                Toast.LENGTH_LONG).show();
    }


    private void savePaymentHistory(String paymentId, long time) {

        Map<String, Object> map = new HashMap<>();
        map.put("planName", selectedPlanName);
        map.put("amount", selectedFinalAmount);
        map.put("paymentId", paymentId);
        map.put("paymentDate", time);

        db.collection("Admins")
                .document(adminUID)
                .collection("SubscriptionPayments")
                .add(map);
    }


    private void loadCurrentPlan() {

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    String plan = doc.getString("planName");
                    Long start = doc.getLong("planStartDate");
                    Long end = doc.getLong("planEndDate");

                    if (plan == null || start == null || end == null) {
                        tvCurrentPlan.setText("No Active Plan");
                        tvRemainingDays.setText("-");
                        tvStartDate.setText("Start Date: -");
                        tvEndDate.setText("End Date: -");
                        return;
                    }

                    tvCurrentPlan.setText("Current Plan: " + plan);

                    long remaining =
                            (end - System.currentTimeMillis()) /
                                    (1000 * 60 * 60 * 24);

                    tvRemainingDays.setText(
                            "Remaining Days: " + Math.max(0, remaining));

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("dd MMM yyyy",
                                    Locale.getDefault());

                    tvStartDate.setText("Start Date: " +
                            sdf.format(new Date(start)));

                    tvEndDate.setText("End Date: " +
                            sdf.format(new Date(end)));
                });
    }


    private void loadPaymentHistory() {

        db.collection("Admins")
                .document(adminUID)
                .collection("SubscriptionPayments")
                .orderBy("paymentDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        tvPaymentHistory.setText("No Payments");
                        return;
                    }

                    StringBuilder history = new StringBuilder();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String plan = doc.getString("planName");
                        Long amount = doc.getLong("amount");
                        Long date = doc.getLong("paymentDate");

                        if (date == null) continue;

                        String formatted =
                                new SimpleDateFormat("dd MMM yyyy",
                                        Locale.getDefault())
                                        .format(new Date(date));

                        history.append(plan)
                                .append("\n₹").append(amount)
                                .append(" | ").append(formatted)
                                .append("\n\n");
                    }

                    tvPaymentHistory.setText(history.toString());
                });
    }
}