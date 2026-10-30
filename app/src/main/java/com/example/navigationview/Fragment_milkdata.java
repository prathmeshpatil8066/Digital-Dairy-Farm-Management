package com.example.navigationview;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.*;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class Fragment_milkdata extends Fragment {

    RecyclerView rvMilkData;

    FirebaseFirestore db;

    String customerUID;
    String adminUID;

    ArrayList<BillModel> billList;
    BillsAdapter adapter;

    ListenerRegistration billsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.milkdata_fragment,
                container,
                false
        );

        rvMilkData = view.findViewById(R.id.listMilkData);

        SharedPreferences sp =
                requireActivity().getSharedPreferences(
                        "customer_session",
                        MODE_PRIVATE
                );

        customerUID = sp.getString("customer_uid", null);
        adminUID = sp.getString("admin_uid", null);

        if (customerUID == null || adminUID == null)
            return view;

        db = FirebaseFirestore.getInstance();

        billList = new ArrayList<>();

        rvMilkData.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new BillsAdapter(billList);
        rvMilkData.setAdapter(adapter);

        attachRealtimeListener();

        return view;
    }

    private void attachRealtimeListener() {

        billsListener = db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("customerUID", customerUID)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {

                    if (snapshot == null)
                        return;

                    billList.clear();

                    for (DocumentSnapshot bill : snapshot.getDocuments()) {

                        BillModel model = new BillModel(
                                bill.getString("date"),
                                bill.getString("milkTime"),
                                getDouble(bill, "fat"),
                                getDouble(bill, "snf"),
                                getDouble(bill, "rate"),
                                getDouble(bill, "qty"),
                                getDouble(bill, "totalAmount")
                        );

                        billList.add(model);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private double getDouble(DocumentSnapshot snap, String key) {
        Double val = snap.getDouble(key);
        return val == null ? 0 : val;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (billsListener != null)
            billsListener.remove();
    }
}