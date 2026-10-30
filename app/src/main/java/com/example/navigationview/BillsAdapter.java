package com.example.navigationview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class BillsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_BILL = 1;

    private ArrayList<BillModel> list;

    public BillsAdapter(ArrayList<BillModel> list) {
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isHeader() ? TYPE_HEADER : TYPE_BILL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {

            View view = inflater.inflate(
                    R.layout.row_statement_header,
                    parent,
                    false);

            return new HeaderViewHolder(view);

        } else {

            View view = inflater.inflate(
                    R.layout.row_bill_item,
                    parent,
                    false);

            return new BillViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        BillModel model = list.get(position);

        if (holder instanceof HeaderViewHolder) {

            HeaderViewHolder h = (HeaderViewHolder) holder;

            h.tvRange.setText(model.getHeaderTitle());
            h.tvSummary.setText(model.getHeaderSummary());

        } else {

            BillViewHolder b = (BillViewHolder) holder;

            // DATE + SHIFT same line
            b.tvDate.setText(
                    model.getDate() + " (" + model.getMilkTime() + ")"
            );

            b.tvFat.setText(String.format(
                    Locale.getDefault(),
                    "FAT: %.1f",
                    model.getFat()
            ));

            b.tvSnf.setText(String.format(
                    Locale.getDefault(),
                    "SNF: %.1f",
                    model.getSnf()
            ));

            b.tvRate.setText(String.format(
                    Locale.getDefault(),
                    "Rate: ₹ %.2f",
                    model.getRate()
            ));

            b.tvQty.setText(String.format(
                    Locale.getDefault(),
                    "Qty: %.2f L",
                    model.getQty()
            ));

            b.tvAmount.setText(String.format(
                    Locale.getDefault(),
                    "Total: ₹ %.2f",
                    model.getTotalAmount()
            ));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }



    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView tvRange, tvSummary;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRange = itemView.findViewById(R.id.tvRange);
            tvSummary = itemView.findViewById(R.id.tvSummary);
        }
    }


    static class BillViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate, tvFat, tvSnf, tvRate, tvQty, tvAmount;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDate = itemView.findViewById(R.id.tvDate);
            tvFat = itemView.findViewById(R.id.tvFat);
            tvSnf = itemView.findViewById(R.id.tvSnf);
            tvRate = itemView.findViewById(R.id.tvRate);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}