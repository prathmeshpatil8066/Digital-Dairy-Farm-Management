package com.example.navigationview;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_collection extends Fragment {

    EditText etCustomerId, etCustomerName, etQty, etFat, etSnf, etRate, etTotal;
    TextView tvDate;
    Button btnSubmit;
    ListView listRemainingCustomers;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    String adminUID;
    String todayDate, milkTime;

    String currentCustomerUID = "";
    String currentMilkType = "";

    double fatRate = 0;
    double snfRate = 0;

    boolean isValidData = false;
    boolean isSubscriptionActive = true;

    ListenerRegistration billsListener;
    Set<String> collectedUIDs = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.collection_fragment, container, false);

        etCustomerId = view.findViewById(R.id.customerId);
        etCustomerName = view.findViewById(R.id.customerName);
        etQty = view.findViewById(R.id.qty);
        etFat = view.findViewById(R.id.fat);
        etSnf = view.findViewById(R.id.snf);
        etRate = view.findViewById(R.id.rate);
        etTotal = view.findViewById(R.id.total);
        tvDate = view.findViewById(R.id.tvDate);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        listRemainingCustomers = view.findViewById(R.id.listRemainingCustomer);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) return view;

        adminUID = mAuth.getCurrentUser().getUid();

        setDateShift();
        checkSubscription();
        attachBillsListener();

        etCustomerId.addTextChangedListener(customerWatcher);
        etFat.addTextChangedListener(calcWatcher);
        etSnf.addTextChangedListener(calcWatcher);
        etQty.addTextChangedListener(calcWatcher);

        btnSubmit.setOnClickListener(v -> checkDuplicateAndSave());

        return view;
    }


    private void checkSubscription(){

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    Long end = doc.getLong("planEndDate");

                    if(end == null || System.currentTimeMillis() > end){
                        isSubscriptionActive = false;
                        btnSubmit.setEnabled(false);
                        btnSubmit.setAlpha(0.5f);
                    }
                });
    }


    private void setDateShift() {

        todayDate = new SimpleDateFormat("dd-MM-yyyy",
                Locale.getDefault()).format(new Date());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        milkTime = (hour < 12) ? "Morning" : "Evening";

        tvDate.setText(todayDate + " | " + milkTime);
    }


    private void attachBillsListener() {

        billsListener = db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("date", todayDate)
                .whereEqualTo("milkTime", milkTime)
                .addSnapshotListener((snapshot, e) -> {

                    collectedUIDs.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot bill : snapshot.getDocuments()) {
                            String uid = bill.getString("customerUID");
                            if (uid != null)
                                collectedUIDs.add(uid);
                        }
                    }

                    updateRemainingCustomers();
                });
    }


    private void updateRemainingCustomers() {

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<DocumentSnapshot> docs =
                            new ArrayList<>(snapshot.getDocuments());

                    docs.sort((d1, d2) -> {
                        Long id1 = d1.getLong("customerId");
                        Long id2 = d2.getLong("customerId");
                        if (id1 == null) id1 = 0L;
                        if (id2 == null) id2 = 0L;
                        return id1.compareTo(id2);
                    });

                    List<String> list = new ArrayList<>();

                    for (DocumentSnapshot doc : docs) {

                        String uid = doc.getId();

                        if (collectedUIDs.contains(uid))
                            continue;

                        Long id = doc.getLong("customerId");
                        String name = doc.getString("name");
                        String milkType = doc.getString("milkType");

                        if (id == null || name == null)
                            continue;

                        list.add(id + " - " + name +
                                " (" + (milkType != null ? milkType : "-") + ")");
                    }

                    listRemainingCustomers.setAdapter(
                            new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_list_item_1,
                                    list));
                });
    }


    private final TextWatcher customerWatcher = new TextWatcher() {

        @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
        @Override public void onTextChanged(CharSequence s,int a,int b,int c){}

        @Override
        public void afterTextChanged(Editable s) {

            currentCustomerUID = "";
            etCustomerName.setText("");

            String idStr = s.toString().trim();

            if (idStr.isEmpty()) return;

            try {
                int id = Integer.parseInt(idStr);

                db.collection("Admins")
                        .document(adminUID)
                        .collection("Customers")
                        .whereEqualTo("customerId", id)
                        .get()
                        .addOnSuccessListener(snapshot -> {

                            if (snapshot.isEmpty()) {
                                etCustomerName.setText("Not Found");
                                return;
                            }

                            DocumentSnapshot doc =
                                    snapshot.getDocuments().get(0);

                            currentCustomerUID = doc.getId();
                            currentMilkType = doc.getString("milkType");
                            etCustomerName.setText(doc.getString("name"));

                            loadRates();
                        });

            } catch (Exception e) {
                etCustomerName.setText("Invalid ID");
            }
        }
    };


    private void loadRates() {

        db.collection("Admins")
                .document(adminUID)
                .collection("RateChart")
                .document("rates")
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    if ("Cow".equalsIgnoreCase(currentMilkType)) {
                        fatRate = getDouble(doc, "Cow_fat");
                        snfRate = getDouble(doc, "Cow_snf");
                    } else {
                        fatRate = getDouble(doc, "Buffalo_fat");
                        snfRate = getDouble(doc, "Buffalo_snf");
                    }
                });
    }

    private double getDouble(DocumentSnapshot doc, String key){
        Double val = doc.getDouble(key);
        return val == null ? 0 : val;
    }


    private final TextWatcher calcWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
        @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
        @Override public void afterTextChanged(Editable s){ calculate(); }
    };

    private void calculate() {

        isValidData = false;

        // Clear old errors
        etFat.setError(null);
        etSnf.setError(null);

        String fatStr = etFat.getText().toString().trim();
        String snfStr = etSnf.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();

        if (fatStr.isEmpty() || snfStr.isEmpty() || qtyStr.isEmpty()) {
            etRate.setText("");
            etTotal.setText("");
            return;
        }

        if (currentMilkType == null || currentMilkType.isEmpty()) {
            etRate.setText("");
            etTotal.setText("");
            return;
        }

        try {

            double fat = Double.parseDouble(fatStr);
            double snf = Double.parseDouble(snfStr);
            double qty = Double.parseDouble(qtyStr);

            boolean valid = true;

            if ("Cow".equalsIgnoreCase(currentMilkType.trim())) {

                if (fat < 2.5 || fat > 5.5) {
                    etFat.setError("Cow FAT must be between 2.5 to 5.5");
                    valid = false;
                }

                if (snf < 8.0 || snf > 8.5) {
                    etSnf.setError("Cow SNF must be between 8.0 to 8.5");
                    valid = false;
                }
            }

            else if ("Buffalo".equalsIgnoreCase(currentMilkType.trim())) {

                if (fat < 5.5 || fat > 12.0) {
                    etFat.setError("Buffalo FAT must be between 5.5 to 12.0");
                    valid = false;
                }

                if (snf < 8.5 || snf > 9.0) {
                    etSnf.setError("Buffalo SNF must be between 8.5 to 9.0");
                    valid = false;
                }
            }

            if (!valid) {
                etRate.setText("");
                etTotal.setText("");
                isValidData = false;
                return;
            }

            double rate = (fat * fatRate) + (snf * snfRate);
            double total = qty * rate;

            etRate.setText(String.format(Locale.getDefault(), "%.2f", rate));
            etTotal.setText(String.format(Locale.getDefault(), "%.2f", total));

            isValidData = true;

        } catch (Exception e) {

            etRate.setText("");
            etTotal.setText("");
            isValidData = false;
        }
    }


    private void checkDuplicateAndSave() {

        if(!isSubscriptionActive){
            Toast.makeText(getContext(),
                    "Subscription Expired",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidData || currentCustomerUID.isEmpty()) {
            Toast.makeText(getContext(),
                    "Fill all fields correctly",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("customerUID", currentCustomerUID)
                .whereEqualTo("date", todayDate)
                .whereEqualTo("milkTime", milkTime)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {

                        Toast.makeText(getContext(),
                                "Already collected for this shift",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    saveBill();
                });
    }

    private void saveBill() {

        Map<String,Object> map = new HashMap<>();

        map.put("customerUID", currentCustomerUID);
        map.put("customerId", Integer.parseInt(etCustomerId.getText().toString()));
        map.put("customerName", etCustomerName.getText().toString());
        map.put("milkType", currentMilkType);
        map.put("date", todayDate);
        map.put("milkTime", milkTime);
        map.put("fat", Double.parseDouble(etFat.getText().toString()));
        map.put("snf", Double.parseDouble(etSnf.getText().toString()));
        map.put("qty", Double.parseDouble(etQty.getText().toString()));
        map.put("rate", Double.parseDouble(etRate.getText().toString()));
        map.put("totalAmount", Double.parseDouble(etTotal.getText().toString()));

        db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .add(map)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(getContext(),
                            "Collection Saved",
                            Toast.LENGTH_SHORT).show();

                    clearFields();
                });
    }

    private void clearFields(){
        etCustomerId.setText("");
        etCustomerName.setText("");
        etQty.setText("");
        etFat.setText("");
        etSnf.setText("");
        etRate.setText("");
        etTotal.setText("");
        isValidData = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (billsListener != null)
            billsListener.remove();
    }
}