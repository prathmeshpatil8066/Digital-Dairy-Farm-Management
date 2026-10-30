package com.example.navigationview;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_home_customer extends Fragment {

    TextView tvTodayMilk, tvTodayAmount;
    BarChart barChart;

    FirebaseFirestore db;

    String customerUID;
    String adminUID;

    ListenerRegistration billsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.home_customer_fragment,
                container,
                false
        );

        tvTodayMilk = view.findViewById(R.id.tvTodayMilk);
        tvTodayAmount = view.findViewById(R.id.tvTodayAmount);
        barChart = view.findViewById(R.id.barChart);

        SharedPreferences sp =
                requireActivity().getSharedPreferences(
                        "customer_session",
                        0
                );

        customerUID = sp.getString("customer_uid", null);
        adminUID = sp.getString("admin_uid", null);

        if (customerUID == null || adminUID == null) {
            Toast.makeText(getActivity(),
                    "Session expired",
                    Toast.LENGTH_LONG).show();
            return view;
        }

        db = FirebaseFirestore.getInstance();

        attachRealtimeListener();

        return view;
    }

    private void attachRealtimeListener() {

        billsListener = db.collection("Admins")
                .document(adminUID)
                .collection("Bills")
                .whereEqualTo("customerUID", customerUID)
                .addSnapshotListener((snapshot, e) -> {

                    if (snapshot == null) return;

                    updateTodaySummary(snapshot);
                    updateChart(snapshot);
                });
    }

    private void updateTodaySummary(QuerySnapshot snapshot) {

        String todayDate = new SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
        ).format(Calendar.getInstance().getTime());

        double totalMilk = 0;
        double totalAmount = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            String date = doc.getString("date");

            if (!todayDate.equals(date))
                continue;

            totalMilk += getDouble(doc, "qty");
            totalAmount += getDouble(doc, "totalAmount");
        }

        tvTodayMilk.setText(
                String.format(Locale.getDefault(),
                        "%.2f L", totalMilk)
        );

        tvTodayAmount.setText(
                "₹ " + String.format(Locale.getDefault(),
                        "%.2f", totalAmount)
        );
    }

    private void updateChart(QuerySnapshot snapshot) {

        Map<String, Float> dateMilkMap =
                new TreeMap<>();

        for (DocumentSnapshot doc :
                snapshot.getDocuments()) {

            String date = doc.getString("date");
            Double qty = doc.getDouble("qty");

            if (date == null || qty == null)
                continue;

            float existing =
                    dateMilkMap.containsKey(date)
                            ? dateMilkMap.get(date)
                            : 0f;

            dateMilkMap.put(date,
                    existing + qty.floatValue());
        }

        List<String> allDates =
                new ArrayList<>(dateMilkMap.keySet());

        int start =
                Math.max(0,
                        allDates.size() - 7);

        ArrayList<BarEntry> entries =
                new ArrayList<>();

        ArrayList<String> labels =
                new ArrayList<>();

        int index = 0;

        for (int i = start;
             i < allDates.size();
             i++) {

            String date =
                    allDates.get(i);

            entries.add(
                    new BarEntry(index,
                            dateMilkMap.get(date))
            );

            labels.add(
                    date.substring(0, 5)
            );

            index++;
        }

        if (entries.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        BarDataSet dataSet =
                new BarDataSet(entries,
                        "Milk (Liters)");

        dataSet.setColor(
                Color.parseColor("#4CAF50")
        );

        BarData barData =
                new BarData(dataSet);

        barChart.setData(barData);
        barChart.setFitBars(true);

        XAxis xAxis =
                barChart.getXAxis();

        xAxis.setPosition(
                XAxis.XAxisPosition.BOTTOM);

        xAxis.setGranularity(1f);

        xAxis.setValueFormatter(
                new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int i = (int) value;
                        if (i >= 0 && i < labels.size())
                            return labels.get(i);
                        return "";
                    }
                });

        barChart.getAxisRight()
                .setEnabled(false);

        Description desc =
                new Description();
        desc.setText("");
        barChart.setDescription(desc);

        barChart.animateY(500);
        barChart.invalidate();
    }

    private double getDouble(DocumentSnapshot doc,
                             String key) {
        Double val = doc.getDouble(key);
        return val == null ? 0 : val;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (billsListener != null)
            billsListener.remove();
    }
}