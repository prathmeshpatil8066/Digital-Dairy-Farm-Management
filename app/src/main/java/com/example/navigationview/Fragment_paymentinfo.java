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

public class Fragment_paymentinfo extends Fragment {

    EditText etCustomerId;
    TextView tvCustomerName;
    Button btnGetPayment;
    ListView listPayments;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    String adminUID = "";
    String currentUID = "";

    ArrayList<String> paymentList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    ListenerRegistration paymentListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.paymentinfo_fragment,
                container,
                false
        );

        etCustomerId = view.findViewById(R.id.etCustomerId);
        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        btnGetPayment = view.findViewById(R.id.btnGetPayment);
        listPayments = view.findViewById(R.id.listPayments);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            adminUID = mAuth.getCurrentUser().getUid();
        }

        adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                paymentList);

        listPayments.setAdapter(adapter);

        etCustomerId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override
            public void afterTextChanged(Editable s) {

                String id = s.toString().trim();

                if(id.isEmpty()){
                    tvCustomerName.setText("Customer Name");
                    currentUID = "";
                    return;
                }

                try {
                    long customerId = Long.parseLong(id);

                    db.collection("Admins")
                            .document(adminUID)
                            .collection("Customers")
                            .whereEqualTo("customerId", customerId)
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                if(snapshot.isEmpty()){
                                    tvCustomerName.setText("Customer Not Found");
                                    currentUID = "";
                                    return;
                                }

                                DocumentSnapshot doc =
                                        snapshot.getDocuments().get(0);

                                currentUID = doc.getId();
                                tvCustomerName.setText(
                                        doc.getString("name"));
                            });

                } catch (Exception e){
                    tvCustomerName.setText("Invalid ID");
                    currentUID = "";
                }
            }
        });

        btnGetPayment.setOnClickListener(v -> {

            if(currentUID.isEmpty()){
                Toast.makeText(getActivity(),
                        "Invalid Customer ID",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            attachRealtimePayments();
        });

        return view;
    }

    private void attachRealtimePayments(){

        if(paymentListener != null)
            paymentListener.remove();

        paymentListener = db.collection("Admins")
                .document(adminUID)
                .collection("Payments")
                .whereEqualTo("customerUID", currentUID)
                .addSnapshotListener((snapshot, e) -> {

                    if(e != null){
                        Toast.makeText(getActivity(),
                                "Error: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    paymentList.clear();

                    if(snapshot == null || snapshot.isEmpty()){
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(),
                                "No Payment Data Found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> docs =
                            new ArrayList<>(snapshot.getDocuments());

                    docs.sort((a,b)->{

                        long t1 = getPaymentTimestamp(a);
                        long t2 = getPaymentTimestamp(b);

                        return Long.compare(t2, t1);
                    });

                    for(DocumentSnapshot pay : docs){

                        String startDate = pay.getString("startDate");
                        String endDate = pay.getString("endDate");

                        long paymentTime = getPaymentTimestamp(pay);

                        String formattedDate = new SimpleDateFormat(
                                "dd-MM-yyyy HH:mm",
                                Locale.getDefault())
                                .format(new Date(paymentTime));

                        Double total = pay.getDouble("slotBill");
                        Double paid = pay.getDouble("paidAmount");

                        if(total == null) total = 0.0;
                        if(paid == null) paid = 0.0;

                        String item =
                                "Period : "+startDate+" to "+endDate+
                                        "\nPayment Date : "+formattedDate+
                                        "\nTotal Bill : ₹"+
                                        String.format(Locale.getDefault(),"%.2f",total)+
                                        "\nPaid : ₹"+
                                        String.format(Locale.getDefault(),"%.2f",paid);

                        paymentList.add(item);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private long getPaymentTimestamp(DocumentSnapshot doc){

        Object obj = doc.get("paymentDate");

        if(obj instanceof Long){
            return (Long) obj;
        }

        if(obj instanceof String){
            try{
                return Long.parseLong((String)obj);
            }catch (Exception ignored){}
        }

        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(paymentListener != null)
            paymentListener.remove();
    }
}