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

public class Fragment_paymentinfo_customer extends Fragment {

    TextView tvCustomerName, tvCustomerId, tvTotalPaid;
    ListView listCustomerPayments;

    FirebaseFirestore db;

    String customerUID;
    String adminUID;

    ArrayList<String> paymentList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    ListenerRegistration paymentListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.paymentinfo_customer_fragment,
                container,
                false
        );

        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        tvCustomerId = view.findViewById(R.id.tvCustomerId);
        tvTotalPaid = view.findViewById(R.id.tvTotalPaid);
        listCustomerPayments = view.findViewById(R.id.listCustomerPayments);

        SharedPreferences sp =
                requireActivity().getSharedPreferences(
                        "customer_session",
                        MODE_PRIVATE
                );

        customerUID = sp.getString("customer_uid", null);
        adminUID = sp.getString("admin_uid", null);

        if (customerUID == null || adminUID == null) {
            Toast.makeText(getActivity(),
                    "Session expired. Please login again.",
                    Toast.LENGTH_SHORT).show();
            return view;
        }

        db = FirebaseFirestore.getInstance();

        adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                paymentList);

        listCustomerPayments.setAdapter(adapter);

        loadCustomerInfo();
        attachRealtimePayments();

        return view;
    }

    private void loadCustomerInfo(){

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .document(customerUID)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if(snapshot.exists()){

                        String name = snapshot.getString("name");
                        Long customerId = snapshot.getLong("customerId");

                        tvCustomerName.setText(
                                name != null ? name : "Customer");

                        tvCustomerId.setText(
                                "Customer ID : " +
                                        (customerId != null ?
                                                customerId : "-"));
                    }
                });
    }

    private void attachRealtimePayments(){

        if(paymentListener != null)
            paymentListener.remove();

        paymentListener = db.collection("Admins")
                .document(adminUID)
                .collection("Payments")
                .whereEqualTo("customerUID", customerUID)
                .addSnapshotListener((snapshot, e) -> {

                    if(e != null){
                        Toast.makeText(getActivity(),
                                "Error loading payment history",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    paymentList.clear();
                    double grandTotalPaid = 0;

                    if(snapshot == null || snapshot.isEmpty()){
                        paymentList.add("No payment history found");
                        adapter.notifyDataSetChanged();
                        tvTotalPaid.setText("Total Paid Till Now : ₹0.00");
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
                        String mode = pay.getString("paymentMode");

                        long paymentTime = getPaymentTimestamp(pay);

                        String formattedDate = new SimpleDateFormat(
                                "dd-MM-yyyy HH:mm",
                                Locale.getDefault())
                                .format(new Date(paymentTime));

                        Double total = pay.getDouble("slotBill");
                        Double paid = pay.getDouble("paidAmount");

                        if(total == null) total = 0.0;
                        if(paid == null) paid = 0.0;

                        grandTotalPaid += paid;

                        String item =
                                "Period : " + startDate +
                                        " to " + endDate +
                                        "\nPayment Date : " + formattedDate +
                                        "\nTotal Bill : ₹" +
                                        String.format(Locale.getDefault(),"%.2f", total) +
                                        "\nPaid : ₹" +
                                        String.format(Locale.getDefault(),"%.2f", paid) +
                                        "\nMode : " + (mode != null ? mode : "-");

                        paymentList.add(item);
                    }

                    tvTotalPaid.setText(
                            "Total Paid Till Now : ₹" +
                                    String.format(Locale.getDefault(),
                                            "%.2f", grandTotalPaid));

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