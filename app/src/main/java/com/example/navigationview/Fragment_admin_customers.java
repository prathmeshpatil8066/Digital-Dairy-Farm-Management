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

import java.util.*;

public class Fragment_admin_customers extends Fragment {

    EditText etSearch;
    ListView listCustomers;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    String adminUID;

    ArrayList<String> customerList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    List<DocumentSnapshot> allCustomers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.admin_customers_fragment, container, false);

        etSearch = view.findViewById(R.id.etSearchCustomer);
        listCustomers = view.findViewById(R.id.listCustomers);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getActivity(),
                    "Login expired",
                    Toast.LENGTH_LONG).show();
            return view;
        }

        adminUID = mAuth.getCurrentUser().getUid();

        adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                customerList
        );

        listCustomers.setAdapter(adapter);

        loadCustomers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(Editable s) {
                filterCustomers(s.toString().trim());
            }
        });

        return view;
    }

    private void loadCustomers() {

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        Toast.makeText(getActivity(),
                                "No customers found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> temp =
                            new ArrayList<>(snapshot.getDocuments());

                    temp.sort((d1, d2) -> {
                        Long id1 = d1.getLong("customerId");
                        Long id2 = d2.getLong("customerId");

                        if (id1 == null) id1 = 0L;
                        if (id2 == null) id2 = 0L;

                        return id1.compareTo(id2);
                    });

                    allCustomers = temp;
                    buildCustomerList(allCustomers);
                });
    }

    private void buildCustomerList(List<DocumentSnapshot> customers) {

        customerList.clear();
        adapter.notifyDataSetChanged();

        TreeMap<Long, String> sortedMap = new TreeMap<>();

        for (DocumentSnapshot doc : customers) {

            String uid = doc.getId();
            String name = doc.getString("name");
            String milkType = doc.getString("milkType");
            String mobile = doc.getString("mobile");
            Long customerId = doc.getLong("customerId");

            if (customerId == null) continue;

            calculateSummary(uid, customerId, name, milkType, mobile, sortedMap);
        }
    }

    private void calculateSummary(String uid,
                                  Long customerId,
                                  String name,
                                  String milkType,
                                  String mobile,
                                  TreeMap<Long, String> sortedMap) {

        final double[] totalCollection = {0};
        final double[] totalPaid = {0};

        db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("customerUID", uid)
                .get()
                .addOnSuccessListener(billSnap -> {

                    for (DocumentSnapshot bill : billSnap) {
                        Double amount = bill.getDouble("totalAmount");
                        if (amount != null)
                            totalCollection[0] += amount;
                    }

                    db.collection("Admins")
                            .document(adminUID)
                            .collection("Payments")
                            .whereEqualTo("customerUID", uid)
                            .get()
                            .addOnSuccessListener(paySnap -> {

                                for (DocumentSnapshot pay : paySnap) {
                                    Double paid = pay.getDouble("paidAmount");
                                    if (paid != null)
                                        totalPaid[0] += paid;
                                }

                                double outstanding =
                                        Math.max(0,
                                                totalCollection[0] - totalPaid[0]);

                                String display =
                                        "ID : " + customerId +
                                                "\nName : " + (name == null ? "-" : name) +
                                                "\nMilk : " + (milkType == null ? "-" : milkType) +
                                                "\nMobile : " + (mobile == null ? "-" : mobile) +
                                                "\nTotal Collection : ₹" +
                                                String.format(Locale.getDefault(),
                                                        "%.2f", totalCollection[0]) +
                                                "\nTotal Paid : ₹" +
                                                String.format(Locale.getDefault(),
                                                        "%.2f", totalPaid[0]) +
                                                "\nOutstanding : ₹" +
                                                String.format(Locale.getDefault(),
                                                        "%.2f", outstanding);

                                sortedMap.put(customerId, display);

                                customerList.clear();
                                customerList.addAll(sortedMap.values());
                                adapter.notifyDataSetChanged();
                            });
                });
    }

    private void filterCustomers(String keyword) {

        if (keyword.isEmpty()) {
            buildCustomerList(allCustomers);
            return;
        }

        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : allCustomers) {

            String name = doc.getString("name");
            Long id = doc.getLong("customerId");

            if ((name != null &&
                    name.toLowerCase().contains(keyword.toLowerCase()))
                    || (id != null &&
                    String.valueOf(id).contains(keyword))) {

                filtered.add(doc);
            }
        }

        buildCustomerList(filtered);
    }
}