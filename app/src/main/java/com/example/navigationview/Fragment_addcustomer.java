package com.example.navigationview;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class Fragment_addcustomer extends Fragment {

    EditText name,email,password,mobile,upiId;
    RadioGroup rgMilkType;
    Button btnAddCustomer;
    ProgressBar progressBar;

    FirebaseAuth adminAuth;
    FirebaseAuth customerAuth;
    FirebaseFirestore db;

    String adminUID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.addcustomer_fragment, container, false);

        name = view.findViewById(R.id.name);
        email = view.findViewById(R.id.email);
        password = view.findViewById(R.id.password);
        mobile = view.findViewById(R.id.mobile);
        upiId = view.findViewById(R.id.upiId);
        rgMilkType = view.findViewById(R.id.rgMilkType);
        btnAddCustomer = view.findViewById(R.id.btnAddCustomer);
        progressBar = view.findViewById(R.id.progressBar);

        adminAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if(adminAuth.getCurrentUser()!=null)
            adminUID = adminAuth.getCurrentUser().getUid();

        initSecondaryAuth();

        btnAddCustomer.setOnClickListener(v -> checkSubscription());

        return view;
    }

    private void initSecondaryAuth(){

        FirebaseOptions options = FirebaseApp.getInstance().getOptions();

        FirebaseApp secondaryApp;

        try{
            secondaryApp = FirebaseApp.initializeApp(
                    requireContext(), options, "CUSTOMER_APP");
        }catch (Exception e){
            secondaryApp = FirebaseApp.getInstance("CUSTOMER_APP");
        }

        customerAuth = FirebaseAuth.getInstance(secondaryApp);
    }

    private void checkSubscription(){

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(doc -> {

                    Long end = doc.getLong("planEndDate");
                    Long maxCustomers = doc.getLong("maxCustomers");

                    if(end != null && System.currentTimeMillis() > end){

                        Toast.makeText(getContext(),
                                "Subscription Expired",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    int limit = maxCustomers == null ? 0 :
                            maxCustomers.intValue();

                    checkCustomerLimit(limit);
                });
    }

    private void checkCustomerLimit(int limit){

        db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int current = snapshot.size();

                    if(current >= limit){

                        Toast.makeText(getContext(),
                                "Customer limit reached",
                                Toast.LENGTH_LONG).show();

                        return;
                    }

                    createCustomer();
                });
    }

    private void createCustomer(){

        String n=name.getText().toString().trim();
        String e=email.getText().toString().trim();
        String p=password.getText().toString().trim();
        String m=mobile.getText().toString().trim();
        String upi=upiId.getText().toString().trim();

        int selectedId=rgMilkType.getCheckedRadioButtonId();

        if(TextUtils.isEmpty(n) ||
                TextUtils.isEmpty(e) ||
                TextUtils.isEmpty(p) ||
                selectedId==-1){

            Toast.makeText(getActivity(),
                    "Fill all required fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        RadioButton rb=requireView().findViewById(selectedId);
        String milkType=rb.getText().toString();

        customerAuth.createUserWithEmailAndPassword(e,p)
                .addOnSuccessListener(result -> {

                    String customerUID=result.getUser().getUid();

                    saveCustomer(customerUID,n,e,m,upi,milkType);

                    customerAuth.signOut();
                })
                .addOnFailureListener(e1 -> {

                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(getActivity(),
                            "Email already exists",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveCustomer(String customerUID,
                              String n,String e,String m,
                              String upi,String milkType){

        db.collection("Admins")
                .document(adminUID)
                .get()
                .addOnSuccessListener(adminDoc -> {

                    Long nextIdLong=adminDoc.getLong("nextCustomerId");
                    long nextId=nextIdLong==null?1:nextIdLong;

                    Map<String,Object> map=new HashMap<>();

                    map.put("customerId",nextId);
                    map.put("name",n);
                    map.put("email",e);
                    map.put("mobile",m);
                    map.put("milkType",milkType);
                    map.put("upiId",upi);
                    map.put("adminUid",adminUID);
                    map.put("createdAt",System.currentTimeMillis());

                    db.collection("Admins")
                            .document(adminUID)
                            .collection("Customers")
                            .document(customerUID)
                            .set(map);

                    db.collection("Admins")
                            .document(adminUID)
                            .update("nextCustomerId",nextId+1);

                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(getActivity(),
                            "Customer Added Successfully",
                            Toast.LENGTH_LONG).show();

                    clearFields();
                });
    }

    private void clearFields(){

        name.setText("");
        email.setText("");
        password.setText("");
        mobile.setText("");
        upiId.setText("");
        rgMilkType.clearCheck();
    }
}