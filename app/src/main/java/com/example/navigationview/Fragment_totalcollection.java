package com.example.navigationview;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_totalcollection extends Fragment {

    RadioGroup rgMilkType, rgShift;
    TextView tvSelectDate;
    TextView tvResultQty, tvResultAmount;
    Button btnSubmit;
    View cardResult;
    ListView listBills;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String adminUID;

    String selectedDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.totalcollection_fragment,
                container,
                false
        );

        rgMilkType = view.findViewById(R.id.rgMilkType);
        rgShift = view.findViewById(R.id.radioGroupShift);
        tvSelectDate = view.findViewById(R.id.tvSelectDate);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        tvResultQty = view.findViewById(R.id.tvResultQty);
        tvResultAmount = view.findViewById(R.id.tvResultAmount);
        cardResult = view.findViewById(R.id.cardResult);
        listBills = view.findViewById(R.id.listBills);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null)
            adminUID = mAuth.getCurrentUser().getUid();

        tvSelectDate.setOnClickListener(v -> pickDate());
        btnSubmit.setOnClickListener(v -> loadSummary());

        return view;
    }



    private void pickDate() {

        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {

                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth);

                    selectedDate = new SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale.getDefault()
                    ).format(c.getTime());

                    tvSelectDate.setText(selectedDate);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }


    private void loadSummary() {

        if (rgMilkType.getCheckedRadioButtonId() == -1 ||
                rgShift.getCheckedRadioButtonId() == -1 ||
                selectedDate.isEmpty()) {

            Toast.makeText(getActivity(),
                    "Select milk type, shift & date",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbMilk =
                requireView().findViewById(rgMilkType.getCheckedRadioButtonId());

        RadioButton rbShift =
                requireView().findViewById(rgShift.getCheckedRadioButtonId());

        String selectedMilkType = rbMilk.getText().toString();
        String selectedShift = rbShift.getText().toString();

        db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("milkTime", selectedShift)
                .whereEqualTo("milkType", selectedMilkType)
                .get()
                .addOnSuccessListener(snapshot -> {

                    double totalQty = 0;
                    double totalAmount = 0;
                    int count = 0;

                    ArrayList<String> list = new ArrayList<>();

                    for (DocumentSnapshot bill : snapshot.getDocuments()) {

                        Long customerId = bill.getLong("customerId");
                        String customerName = bill.getString("customerName");

                        Double qty = bill.getDouble("qty");
                        Double amount = bill.getDouble("totalAmount");
                        Double fat = bill.getDouble("fat");
                        Double snf = bill.getDouble("snf");
                        Double rate = bill.getDouble("rate");

                        if (qty == null) qty = 0.0;
                        if (amount == null) amount = 0.0;
                        if (fat == null) fat = 0.0;
                        if (snf == null) snf = 0.0;
                        if (rate == null) rate = 0.0;

                        totalQty += qty;
                        totalAmount += amount;
                        count++;

                        list.add(
                                "ID: " + (customerId != null ? customerId : "-") +
                                        "\nName: " + (customerName != null ? customerName : "-") +
                                        "\nMilk Type: " + selectedMilkType +
                                        "\nFAT: " + fat +
                                        " | SNF: " + snf +
                                        "\nQty: " + qty + " L" +
                                        "\nRate: ₹" + rate +
                                        "\nTotal: ₹" + amount
                        );
                    }

                    if (count == 0) {
                        cardResult.setVisibility(View.GONE);
                        listBills.setAdapter(null);
                        Toast.makeText(getActivity(),
                                "No collection found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    cardResult.setVisibility(View.VISIBLE);

                    tvResultQty.setText("Total Milk : " +
                            String.format(Locale.getDefault(),"%.2f L", totalQty));

                    tvResultAmount.setText("Total Amount : ₹" +
                            String.format(Locale.getDefault(),"%.2f", totalAmount));

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_list_item_1,
                                    list
                            );

                    listBills.setAdapter(adapter);

                    setListViewHeightBasedOnChildren(listBills);
                });
    }


    private void setListViewHeightBasedOnChildren(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;

        for (int i = 0; i < listAdapter.getCount(); i++) {

            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );

            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight +
                (listView.getDividerHeight() * (listAdapter.getCount() - 1));

        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}