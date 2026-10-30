package com.example.navigationview;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class Fragment_totalcustomer extends Fragment {

    TextView tvTotalCustomers, tvActiveCustomers;
    EditText etSearchCustomer;
    ListView listCustomers;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    String adminUID;

    ArrayList<HashMap<String,String>> customerList = new ArrayList<>();
    ArrayList<HashMap<String,String>> fullList = new ArrayList<>();

    ListenerRegistration customerListener;
    ListenerRegistration billListener;

    HashSet<String> activeSet = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.totalcustomer_fragment,
                container,
                false
        );

        tvTotalCustomers = view.findViewById(R.id.tvTotalCustomers);
        tvActiveCustomers = view.findViewById(R.id.tvActiveCustomers);
        etSearchCustomer = view.findViewById(R.id.etSearchCustomer);
        listCustomers = view.findViewById(R.id.listCustomers);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null)
            return view;

        adminUID = mAuth.getCurrentUser().getUid();

        attachRealtimeListeners();

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override
            public void afterTextChanged(Editable s) {
                filterLocal(s.toString().trim());
            }
        });

        return view;
    }

    private void attachRealtimeListeners(){

        customerListener = db.collection("Admins")
                .document(adminUID)
                .collection("Customers")
                .addSnapshotListener((snapshot, e) -> {

                    fullList.clear();

                    if(snapshot == null) return;

                    List<DocumentSnapshot> temp =
                            new ArrayList<>(snapshot.getDocuments());

                    temp.sort((d1,d2)->{
                        Long id1 = d1.getLong("customerId");
                        Long id2 = d2.getLong("customerId");
                        if(id1==null) id1=0L;
                        if(id2==null) id2=0L;
                        return id1.compareTo(id2);
                    });

                    for(DocumentSnapshot doc : temp){

                        Long idLong = doc.getLong("customerId");
                        if(idLong==null) continue;

                        HashMap<String,String> map = new HashMap<>();
                        map.put("id", String.valueOf(idLong));
                        map.put("name", doc.getString("name"));
                        map.put("milk_type", doc.getString("milkType"));

                        fullList.add(map);
                    }

                    filterLocal(etSearchCustomer.getText().toString());
                });

        billListener = db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .addSnapshotListener((snapshot, e) -> {

                    activeSet.clear();

                    if(snapshot!=null){
                        for(DocumentSnapshot bill : snapshot){
                            String uid = bill.getString("customerUID");
                            if(uid!=null)
                                activeSet.add(uid);
                        }
                    }

                    tvActiveCustomers.setText(
                            String.valueOf(activeSet.size()));
                });
    }

    private void filterLocal(String keyword){

        customerList.clear();

        for(HashMap<String,String> map : fullList){

            String name = map.get("name");
            String id = map.get("id");

            if(keyword.isEmpty()){
                customerList.add(map);
            }
            else{
                boolean matchName = name!=null &&
                        name.toLowerCase()
                                .contains(keyword.toLowerCase());

                boolean matchId = id!=null &&
                        id.contains(keyword);

                if(matchName || matchId)
                    customerList.add(map);
            }
        }

        tvTotalCustomers.setText(
                String.valueOf(customerList.size()));

        setAdapter();
    }

    private void setAdapter(){

        SimpleAdapter adapter =
                new SimpleAdapter(
                        requireContext(),
                        customerList,
                        R.layout.row_customer,
                        new String[]{"id","name","milk_type"},
                        new int[]{
                                R.id.tvCustomerId,
                                R.id.tvCustomerName,
                                R.id.tvMilkType
                        }
                );

        listCustomers.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(customerListener!=null)
            customerListener.remove();

        if(billListener!=null)
            billListener.remove();
    }
}