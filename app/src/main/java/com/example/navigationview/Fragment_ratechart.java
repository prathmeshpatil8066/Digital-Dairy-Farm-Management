package com.example.navigationview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Fragment_ratechart extends Fragment {

    EditText etCowFatRate, etCowSnfRate;
    EditText etBuffaloFatRate, etBuffaloSnfRate;
    Button btnSaveRates;

    TextView tvPdfStatus;
    Button btnUploadPdf, btnViewPdf, btnDeletePdf;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseStorage storage;

    String adminUID;
    String pdfUrl = null;

    ActivityResultLauncher<String> pdfPicker;
    ListenerRegistration pdfListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ratechart_fragment, container, false);

        etCowFatRate = view.findViewById(R.id.etCowRate);
        etCowSnfRate = view.findViewById(R.id.etCowSnfRate);
        etBuffaloFatRate = view.findViewById(R.id.etBuffaloRate);
        etBuffaloSnfRate = view.findViewById(R.id.etBuffaloSnfRate);

        btnSaveRates = view.findViewById(R.id.btnSaveRates);

        tvPdfStatus = view.findViewById(R.id.txtStatus);
        btnUploadPdf = view.findViewById(R.id.btnUpload);
        btnViewPdf = view.findViewById(R.id.btnView);
        btnDeletePdf = view.findViewById(R.id.btnDelete);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Login expired", Toast.LENGTH_LONG).show();
            return view;
        }

        adminUID = mAuth.getCurrentUser().getUid();

        initPdfPicker();
        loadRates();
        listenPdfStatus();

        btnSaveRates.setOnClickListener(v -> saveRates());
        btnUploadPdf.setOnClickListener(v -> pdfPicker.launch("application/pdf"));
        btnViewPdf.setOnClickListener(v -> viewPdf());
        btnDeletePdf.setOnClickListener(v -> deletePdf());

        return view;
    }

    private void loadRates() {

        db.collection("Admins")
                .document(adminUID)
                .collection("RateChart")
                .document("rates")
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    setValue(etCowFatRate, doc.getDouble("Cow_fat"));
                    setValue(etCowSnfRate, doc.getDouble("Cow_snf"));
                    setValue(etBuffaloFatRate, doc.getDouble("Buffalo_fat"));
                    setValue(etBuffaloSnfRate, doc.getDouble("Buffalo_snf"));
                });
    }

    private void setValue(EditText et, Double value) {
        if (value != null)
            et.setText(String.format(Locale.getDefault(), "%.2f", value));
    }

    private void saveRates() {

        try {

            double cowFat = Double.parseDouble(etCowFatRate.getText().toString());
            double cowSnf = Double.parseDouble(etCowSnfRate.getText().toString());
            double buffaloFat = Double.parseDouble(etBuffaloFatRate.getText().toString());
            double buffaloSnf = Double.parseDouble(etBuffaloSnfRate.getText().toString());

            Map<String,Object> map = new HashMap<>();
            map.put("Cow_fat", cowFat);
            map.put("Cow_snf", cowSnf);
            map.put("Buffalo_fat", buffaloFat);
            map.put("Buffalo_snf", buffaloSnf);

            db.collection("Admins")
                    .document(adminUID)
                    .collection("RateChart")
                    .document("rates")
                    .set(map, SetOptions.merge());

            Toast.makeText(getContext(),
                    "Rates Saved Successfully",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e){
            Toast.makeText(getContext(),
                    "Invalid rate values",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void initPdfPicker() {

        pdfPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null)
                        uploadPdf(uri);
                });
    }

    private void uploadPdf(Uri uri) {

        btnUploadPdf.setEnabled(false);
        tvPdfStatus.setText("Uploading PDF...");

        StorageReference pdfRef = storage.getReference()
                .child("rateCharts")
                .child(adminUID + "_ratechart.pdf");

        pdfRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {

                    pdfRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {

                                pdfUrl = downloadUri.toString();

                                Map<String,Object> map = new HashMap<>();
                                map.put("pdfUrl", pdfUrl);

                                db.collection("Admins")
                                        .document(adminUID)
                                        .collection("RateChart")
                                        .document("rates")
                                        .set(map, SetOptions.merge())
                                        .addOnSuccessListener(unused -> {

                                            btnUploadPdf.setEnabled(true);
                                            tvPdfStatus.setText("PDF Uploaded Successfully");

                                            Toast.makeText(getContext(),
                                                    "PDF Uploaded",
                                                    Toast.LENGTH_SHORT).show();
                                        });

                            });

                })
                .addOnFailureListener(e -> {
                    btnUploadPdf.setEnabled(true);
                    tvPdfStatus.setText("Upload Failed");
                    Toast.makeText(getContext(),
                            "Upload Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void listenPdfStatus() {

        pdfListener = db.collection("Admins")
                .document(adminUID)
                .collection("RateChart")
                .document("rates")
                .addSnapshotListener((snapshot, error) -> {

                    if (snapshot == null || !snapshot.exists()) {
                        tvPdfStatus.setText("No PDF Uploaded");
                        btnViewPdf.setEnabled(false);
                        btnDeletePdf.setEnabled(false);
                        return;
                    }

                    pdfUrl = snapshot.getString("pdfUrl");

                    if (pdfUrl != null) {
                        tvPdfStatus.setText("Rate Chart PDF Available");
                        btnViewPdf.setEnabled(true);
                        btnDeletePdf.setEnabled(true);
                    } else {
                        tvPdfStatus.setText("No PDF Uploaded");
                        btnViewPdf.setEnabled(false);
                        btnDeletePdf.setEnabled(false);
                    }
                });
    }

    private void viewPdf() {

        if (pdfUrl == null) {
            Toast.makeText(getContext(),
                    "No PDF Found",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(pdfUrl), "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (Exception e){
            Toast.makeText(getContext(),
                    "No app found to open PDF",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePdf() {

        StorageReference pdfRef = storage.getReference()
                .child("rateCharts")
                .child(adminUID + "_ratechart.pdf");

        pdfRef.delete()
                .addOnSuccessListener(unused -> {

                    db.collection("Admins")
                            .document(adminUID)
                            .collection("RateChart")
                            .document("rates")
                            .update("pdfUrl", FieldValue.delete());

                    Toast.makeText(getContext(),
                            "PDF Deleted",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "File not found in storage",
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (pdfListener != null) {
            pdfListener.remove();
            pdfListener = null;
        }
    }
}