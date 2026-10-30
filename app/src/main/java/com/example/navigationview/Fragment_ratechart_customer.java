package com.example.navigationview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

public class Fragment_ratechart_customer extends Fragment {

    TextView tvStatus;
    Button btnViewPdf;

    FirebaseFirestore db;

    String adminUID;
    String pdfUrl;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.ratechart_customer_fragment,
                container,
                false);

        tvStatus = view.findViewById(R.id.tvStatus);
        btnViewPdf = view.findViewById(R.id.btnViewPdf);

        btnViewPdf.setEnabled(false);

        db = FirebaseFirestore.getInstance();

        adminUID = requireActivity()
                .getSharedPreferences("customer_session", MODE_PRIVATE)
                .getString("admin_uid", null);

        if (adminUID == null) {
            tvStatus.setText("Session expired");
            return view;
        }

        loadRateChart();

        btnViewPdf.setOnClickListener(v -> openPdf());

        return view;
    }

    private void loadRateChart() {

        tvStatus.setText("Loading rate chart...");

        db.collection("Admins")
                .document(adminUID)
                .collection("RateChart")
                .document("rates")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null || snapshot == null || !snapshot.exists()) {
                        tvStatus.setText("❌ Rate chart not uploaded yet");
                        btnViewPdf.setEnabled(false);
                        return;
                    }

                    pdfUrl = snapshot.getString("pdfUrl");

                    if (pdfUrl == null) {
                        tvStatus.setText("❌ Rate chart not uploaded yet");
                        btnViewPdf.setEnabled(false);
                        return;
                    }

                    tvStatus.setText("📄 Rate chart available");
                    btnViewPdf.setEnabled(true);
                });
    }

    private void openPdf() {

        if (pdfUrl == null) {
            Toast.makeText(getContext(),
                    "No PDF found",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(pdfUrl));
            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    getActivity(),
                    "No PDF viewer found",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}