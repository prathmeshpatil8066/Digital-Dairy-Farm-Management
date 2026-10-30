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

public class Fragment_payment extends Fragment {

    EditText etCustomerId, etCash;
    TextView tvCustomerName,tvStartDate,tvEndDate;
    TextView tvSlotBill,tvTotalPayable;

    Button btnLoadSlot,btnSave;

    FirebaseFirestore db;
    FirebaseAuth auth;

    String adminUID="";
    String customerUID="";

    String startDate="",endDate="";

    double slotBill=0;
    double previousRemaining=0;
    double totalPayable=0;

    boolean paymentExists=false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view=inflater.inflate(R.layout.payment_fragment,container,false);

        db=FirebaseFirestore.getInstance();
        auth=FirebaseAuth.getInstance();
        adminUID=auth.getCurrentUser().getUid();

        bind(view);

        autoSlotDates();

        etCustomerId.addTextChangedListener(customerWatcher);

        btnLoadSlot.setOnClickListener(v -> loadSlotBill());

        btnSave.setOnClickListener(v -> savePayment());

        return view;
    }

    private void bind(View v){

        etCustomerId=v.findViewById(R.id.etCustomerId);
        etCash=v.findViewById(R.id.etPaidAmount);

        tvCustomerName=v.findViewById(R.id.tvCustomerName);
        tvStartDate=v.findViewById(R.id.tvStartDate);
        tvEndDate=v.findViewById(R.id.tvEndDate);

        tvSlotBill=v.findViewById(R.id.tvSlotBill);
        tvTotalPayable=v.findViewById(R.id.tvTotalPayable);

        btnLoadSlot=v.findViewById(R.id.btnLoadSlot);
        btnSave=v.findViewById(R.id.btnSave);
    }

    private void autoSlotDates(){

        Calendar today=Calendar.getInstance();
        int day=today.get(Calendar.DAY_OF_MONTH);

        Calendar start=Calendar.getInstance();
        Calendar end=Calendar.getInstance();

        if(day<=10){

            start.add(Calendar.MONTH,-1);
            end.add(Calendar.MONTH,-1);

            start.set(Calendar.DAY_OF_MONTH,21);
            end.set(Calendar.DAY_OF_MONTH,end.getActualMaximum(Calendar.DAY_OF_MONTH));

        }
        else if(day<=20){

            start.set(Calendar.DAY_OF_MONTH,1);
            end.set(Calendar.DAY_OF_MONTH,10);

        }
        else{

            start.set(Calendar.DAY_OF_MONTH,11);
            end.set(Calendar.DAY_OF_MONTH,20);
        }

        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-yyyy",Locale.getDefault());

        startDate=sdf.format(start.getTime());
        endDate=sdf.format(end.getTime());

        tvStartDate.setText("Start Date : "+startDate);
        tvEndDate.setText("End Date : "+endDate);
    }


    private final TextWatcher customerWatcher=new TextWatcher(){

        @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
        @Override public void onTextChanged(CharSequence s,int a,int b,int c){}

        @Override
        public void afterTextChanged(Editable s){

            if(s.toString().isEmpty()) return;

            int id=Integer.parseInt(s.toString());

            db.collection("Admins")
                    .document(adminUID)
                    .collection("Customers")
                    .whereEqualTo("customerId",id)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        if(snapshot.isEmpty()){
                            tvCustomerName.setText("Customer Not Found");
                            return;
                        }

                        DocumentSnapshot doc=snapshot.getDocuments().get(0);

                        customerUID=doc.getId();
                        tvCustomerName.setText(doc.getString("name"));

                        loadPreviousRemaining();
                    });
        }
    };


    private void loadPreviousRemaining(){

        db.collection("Admins")
                .document(adminUID)
                .collection("Payments")
                .whereEqualTo("customerUID",customerUID)
                .orderBy("paymentDate",Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if(snapshot.isEmpty()){
                        previousRemaining=0;
                    }
                    else{

                        previousRemaining=snapshot
                                .getDocuments()
                                .get(0)
                                .getDouble("remaining");
                    }
                });
    }


    private void loadSlotBill(){

        slotBill=0;
        paymentExists=false;

        db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("customerUID",customerUID)
                .get()
                .addOnSuccessListener(snapshot -> {

                    SimpleDateFormat sdf=
                            new SimpleDateFormat("dd-MM-yyyy",Locale.getDefault());

                    try{

                        Date start=sdf.parse(startDate);
                        Date end=sdf.parse(endDate);

                        for(DocumentSnapshot doc:snapshot){

                            String dateStr=doc.getString("date");
                            Double amt=doc.getDouble("totalAmount");

                            if(dateStr==null || amt==null) continue;

                            Date billDate=sdf.parse(dateStr);

                            if(!billDate.before(start) && !billDate.after(end)){
                                slotBill+=amt;
                            }
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    checkExistingPayment();
                });
    }


    private void checkExistingPayment(){

        db.collection("Admins")
                .document(adminUID)
                .collection("Payments")
                .whereEqualTo("customerUID",customerUID)
                .whereEqualTo("startDate",startDate)
                .whereEqualTo("endDate",endDate)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if(snapshot.isEmpty()){

                        totalPayable = slotBill + previousRemaining;

                    }else{

                        paymentExists=true;

                        totalPayable = snapshot
                                .getDocuments()
                                .get(0)
                                .getDouble("remaining");
                    }

                    tvSlotBill.setText("Slot Bill : ₹"+String.format("%.2f",slotBill));
                    tvTotalPayable.setText("Total Payable : ₹"+String.format("%.2f",totalPayable));
                });
    }


    private void savePayment(){

        if(paymentExists){

            Toast.makeText(getActivity(),
                    "Slot already settled",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if(etCash.getText().toString().isEmpty()){

            Toast.makeText(getActivity(),
                    "Enter payment",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double cash=Double.parseDouble(etCash.getText().toString());

        if(cash>totalPayable){

            Toast.makeText(getActivity(),
                    "Cash cannot exceed payable",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double remaining = totalPayable - cash;

        Map<String,Object> map=new HashMap<>();

        map.put("customerUID",customerUID);
        map.put("startDate",startDate);
        map.put("endDate",endDate);

        map.put("slotBill",slotBill);
        map.put("paidAmount",cash);
        map.put("remaining",remaining);

        map.put("paymentDate",System.currentTimeMillis());

        db.collection("Admins")
                .document(adminUID)
                .collection("Payments")
                .add(map)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(getActivity(),
                            "Payment Saved",
                            Toast.LENGTH_LONG).show();

                    totalPayable = remaining;

                    tvTotalPayable.setText("Total Payable : ₹"+String.format("%.2f",remaining));

                    etCash.setText("");
                });
    }
}