package com.example.navigationview;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cursoradapter.widget.CursorAdapter;

import java.util.Locale;

public class CustomerPaymentAdapter extends CursorAdapter {

    public CustomerPaymentAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context)
                .inflate(R.layout.row_payment_customer, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView tvDate = view.findViewById(R.id.tvPayDate);
        TextView tvMode = view.findViewById(R.id.tvPayMode);
        TextView tvTotal = view.findViewById(R.id.tvTotalAmount);
        TextView tvPaid = view.findViewById(R.id.tvPaidAmount);
        TextView tvRemaining = view.findViewById(R.id.tvRemainingAmount);

        String date =
                cursor.getString(cursor.getColumnIndexOrThrow("payment_date"));
        String mode =
                cursor.getString(cursor.getColumnIndexOrThrow("payment_mode"));

        double total =
                cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
        double paid =
                cursor.getDouble(cursor.getColumnIndexOrThrow("paid_amount"));
        double remaining =
                cursor.getDouble(cursor.getColumnIndexOrThrow("remaining_amount"));

        tvDate.setText("Date : " + date);
        tvMode.setText("Mode : " + mode);

        tvTotal.setText(
                "Total : ₹" +
                        String.format(Locale.getDefault(), "%.2f", total)
        );

        tvPaid.setText(
                "Paid : ₹" +
                        String.format(Locale.getDefault(), "%.2f", paid)
        );

        tvRemaining.setText(
                "Remaining : ₹" +
                        String.format(Locale.getDefault(), "%.2f", remaining)
        );
    }
}