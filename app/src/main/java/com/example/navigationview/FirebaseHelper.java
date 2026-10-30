package com.example.navigationview;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    public static DatabaseReference getUsersRef() {
        return FirebaseDatabase.getInstance().getReference("Users");
    }

    public static DatabaseReference getCustomersRef() {
        return FirebaseDatabase.getInstance().getReference("Customers");
    }

    public static DatabaseReference getBillsRef() {
        return FirebaseDatabase.getInstance().getReference("Bills");
    }

    public static DatabaseReference getPaymentsRef() {
        return FirebaseDatabase.getInstance().getReference("Payments");
    }

    public static DatabaseReference getRateRef() {
        return FirebaseDatabase.getInstance().getReference("RateChart");
    }
}