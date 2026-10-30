package com.example.navigationview;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_customerdetails extends Fragment {

    EditText etCustomerId;
    TextView tvCustomerName, tvMilkType;
    Button btnGetDetails;
    RecyclerView rvBills;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    String adminUID;
    String currentUID = "";

    ArrayList<BillModel> statementList = new ArrayList<>();
    BillsAdapter adapter;

    ListenerRegistration billsListener;

    SimpleDateFormat sdf =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.customerdetails_fragment, container, false);

        etCustomerId = view.findViewById(R.id.etCustomerId);
        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        tvMilkType = view.findViewById(R.id.tvMilkType);
        btnGetDetails = view.findViewById(R.id.btnGetDetails);
        rvBills = view.findViewById(R.id.rvBills);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null)
            adminUID = mAuth.getCurrentUser().getUid();

        rvBills.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new BillsAdapter(statementList);
        rvBills.setAdapter(adapter);

        etCustomerId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override
            public void afterTextChanged(Editable s) {

                String idStr = s.toString().trim();

                if(idStr.isEmpty()){
                    tvCustomerName.setText("Customer Name");
                    tvMilkType.setText("Milk Type");
                    currentUID = "";
                    return;
                }

                try {
                    long customerId = Long.parseLong(idStr);

                    db.collection("Admins")
                            .document(adminUID)
                            .collection("Customers")
                            .whereEqualTo("customerId", customerId)
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                if(snapshot.isEmpty()){
                                    tvCustomerName.setText("Customer Not Found");
                                    tvMilkType.setText("-");
                                    currentUID = "";
                                    return;
                                }

                                DocumentSnapshot doc = snapshot.getDocuments().get(0);

                                currentUID = doc.getId();
                                tvCustomerName.setText(doc.getString("name"));
                                tvMilkType.setText(doc.getString("milkType"));
                            });

                } catch (Exception e){
                    tvCustomerName.setText("Invalid ID");
                    tvMilkType.setText("-");
                    currentUID = "";
                }
            }
        });

        btnGetDetails.setOnClickListener(v -> {

            if(currentUID.isEmpty()){
                Toast.makeText(getActivity(),
                        "Invalid Customer ID",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            loadGroupedStatementsRealtime();
        });

        return view;
    }

    private void loadGroupedStatementsRealtime() {

        if(billsListener != null)
            billsListener.remove();

        statementList.clear();
        adapter.notifyDataSetChanged();

        billsListener = db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("customerUID", currentUID)
                .addSnapshotListener((snapshot, error) -> {

                    if(snapshot == null || snapshot.isEmpty()){
                        statementList.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(),
                                "No Bills Found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    TreeMap<Long, ArrayList<DocumentSnapshot>> groupedMap =
                            new TreeMap<>(Collections.reverseOrder());

                    for (DocumentSnapshot bill : snapshot) {

                        String dateStr = bill.getString("date");
                        if(dateStr == null) continue;

                        try {

                            Date date = sdf.parse(dateStr);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);

                            int day = cal.get(Calendar.DAY_OF_MONTH);
                            int month = cal.get(Calendar.MONTH);
                            int year = cal.get(Calendar.YEAR);

                            int startDay;

                            if (day <= 10) startDay = 1;
                            else if (day <= 20) startDay = 11;
                            else startDay = 21;

                            Calendar startCal = Calendar.getInstance();
                            startCal.set(year, month, startDay,0,0,0);
                            startCal.set(Calendar.MILLISECOND,0);

                            long slotKey = startCal.getTimeInMillis();

                            groupedMap.putIfAbsent(slotKey, new ArrayList<>());
                            groupedMap.get(slotKey).add(bill);

                        } catch (Exception ignored) {}
                    }

                    statementList.clear();

                    for (Long slotKey : groupedMap.keySet()) {

                        ArrayList<DocumentSnapshot> bills = groupedMap.get(slotKey);

                        bills.sort((a, b) -> {
                            try {
                                Date d1 = sdf.parse(a.getString("date"));
                                Date d2 = sdf.parse(b.getString("date"));
                                return d2.compareTo(d1);
                            } catch (Exception e) {
                                return 0;
                            }
                        });

                        Calendar startCal = Calendar.getInstance();
                        startCal.setTimeInMillis(slotKey);

                        Calendar endCal = (Calendar) startCal.clone();

                        int startDay = startCal.get(Calendar.DAY_OF_MONTH);

                        if (startDay == 1)
                            endCal.set(Calendar.DAY_OF_MONTH, 10);
                        else if (startDay == 11)
                            endCal.set(Calendar.DAY_OF_MONTH, 20);
                        else
                            endCal.set(Calendar.DAY_OF_MONTH,
                                    endCal.getActualMaximum(Calendar.DAY_OF_MONTH));

                        String range =
                                sdf.format(startCal.getTime()) +
                                        " to " +
                                        sdf.format(endCal.getTime());

                        double totalFat=0,totalSnf=0,totalRate=0,totalAmount=0;
                        int count=0;

                        for (DocumentSnapshot bill : bills) {

                            totalFat += getDouble(bill,"fat");
                            totalSnf += getDouble(bill,"snf");
                            totalRate += getDouble(bill,"rate");
                            totalAmount += getDouble(bill,"totalAmount");
                            count++;
                        }

                        String summary =
                                "Avg FAT: "+format(totalFat,count)+
                                        "\nAvg SNF: "+format(totalSnf,count)+
                                        "\nAvg Rate: ₹"+format(totalRate,count)+
                                        "\nTotal Amount: ₹"+
                                        String.format(Locale.getDefault(),"%.2f",totalAmount);

                        statementList.add(new BillModel(range,summary));

                        for (DocumentSnapshot bill : bills) {

                            statementList.add(new BillModel(
                                    bill.getString("date"),
                                    bill.getString("milkTime"),
                                    getDouble(bill,"fat"),
                                    getDouble(bill,"snf"),
                                    getDouble(bill,"rate"),
                                    getDouble(bill,"qty"),
                                    getDouble(bill,"totalAmount")
                            ));
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private String format(double total,int count){
        if(count==0) return "0";
        return String.format(Locale.getDefault(),"%.2f",total/count);
    }

    private double getDouble(DocumentSnapshot snap,String key){
        Double val = snap.getDouble(key);
        return val==null?0:val;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(billsListener!=null)
            billsListener.remove();
    }
}