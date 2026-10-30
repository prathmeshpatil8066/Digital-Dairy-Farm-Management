package com.example.navigationview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MilkDataAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<BillModel> list;

    public MilkDataAdapter(Context context,
                           ArrayList<BillModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position,
                        View convertView,
                        ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.row_today_receipt,
                            parent,
                            false);
        }

        BillModel model = list.get(position);

        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvShift = convertView.findViewById(R.id.tvShift);
        TextView tvQty = convertView.findViewById(R.id.tvQty);
        TextView tvRate = convertView.findViewById(R.id.tvRate);
        TextView tvTotal = convertView.findViewById(R.id.tvTotal);

        tvDate.setText(model.getDate() != null ?
                model.getDate() : "-");

        tvShift.setText(model.getMilkTime() != null ?
                model.getMilkTime() : "-");

        tvQty.setText(String.format(
                Locale.getDefault(),
                "%.2f L",
                model.getQty()
        ));

        tvRate.setText(String.format(
                Locale.getDefault(),
                "₹ %.2f",
                model.getRate()
        ));

        tvTotal.setText(String.format(
                Locale.getDefault(),
                "₹ %.2f",
                model.getTotalAmount()
        ));

        return convertView;
    }
}