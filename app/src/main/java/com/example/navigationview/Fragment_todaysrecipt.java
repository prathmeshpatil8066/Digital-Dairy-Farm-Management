package com.example.navigationview;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

import static android.content.Context.MODE_PRIVATE;

public class Fragment_todaysrecipt extends Fragment {

    ListView listView;

    FirebaseFirestore db;
    String customerUid;
    String adminUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.todaysrecipt_fragment,
                container,
                false
        );

        listView = view.findViewById(R.id.listReceipts);

        SharedPreferences sp = requireActivity()
                .getSharedPreferences("customer_session", MODE_PRIVATE);

        customerUid = sp.getString("customer_uid", null);
        adminUid = sp.getString("admin_uid", null);

        if (customerUid == null || adminUid == null) {
            Toast.makeText(getActivity(),
                    "Session expired. Please login again.",
                    Toast.LENGTH_SHORT).show();
            return view;
        }

        db = FirebaseFirestore.getInstance();

        loadTodayReceipts();

        return view;
    }

    private void loadTodayReceipts() {

        String todayDate = new SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
        ).format(new Date());

        db.collection("Admins")
                .document(adminUid)
                .collection("Bills")
                .whereEqualTo("customerUID", customerUid)
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(snapshot -> {

                    ArrayList<String> receiptList = new ArrayList<>();

                    if (snapshot.isEmpty()) {

                        receiptList.add("No collection today");

                    } else {

                        for (DocumentSnapshot ds :
                                snapshot.getDocuments()) {

                            String milkTime = ds.getString("milkTime");

                            Double fat = ds.getDouble("fat");
                            Double snf = ds.getDouble("snf");
                            Double qty = ds.getDouble("qty");
                            Double rate = ds.getDouble("rate");
                            Double total = ds.getDouble("totalAmount");

                            if (fat == null) fat = 0.0;
                            if (snf == null) snf = 0.0;
                            if (qty == null) qty = 0.0;
                            if (rate == null) rate = 0.0;
                            if (total == null) total = 0.0;

                            String receipt =
                                    "Date : " + todayDate +
                                            "\nShift : " + milkTime +
                                            "\nFAT : " + fat +
                                            " | SNF : " + snf +
                                            "\nQty : " + qty + " L" +
                                            "\nRate : ₹" + rate +
                                            "\nTotal : ₹" + total;

                            receiptList.add(receipt);
                        }
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_list_item_1,
                                    receiptList);

                    listView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(),
                            "Failed to load today's data",
                            Toast.LENGTH_SHORT).show();
                });
    }
}