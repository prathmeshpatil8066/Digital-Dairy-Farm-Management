package com.example.navigationview;

public class BillModel {

    public boolean isHeader = false;
    private String headerTitle;
    private String headerSummary;

    private String date;
    private String milkTime;
    private double fat;
    private double snf;
    private double rate;
    private double qty;
    private double totalAmount;

    public BillModel() {
    }

    public BillModel(String date,
                     String milkTime,
                     double fat,
                     double snf,
                     double rate,
                     double qty,
                     double totalAmount) {

        this.date = date;
        this.milkTime = milkTime;
        this.fat = fat;
        this.snf = snf;
        this.rate = rate;
        this.qty = qty;
        this.totalAmount = totalAmount;
        this.isHeader = false;
    }

    public BillModel(String headerTitle,
                     String headerSummary) {

        this.headerTitle = headerTitle;
        this.headerSummary = headerSummary;
        this.isHeader = true;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public String getHeaderSummary() {
        return headerSummary;
    }

    public String getDate() {
        return date;
    }

    public String getMilkTime() {
        return milkTime;
    }

    public double getFat() {
        return fat;
    }

    public double getSnf() {
        return snf;
    }

    public double getRate() {
        return rate;
    }

    public double getQty() {
        return qty;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}