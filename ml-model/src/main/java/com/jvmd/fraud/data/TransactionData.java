package com.jvmd.fraud.data;

public class TransactionData {
    private double amount;
    private double hourOfDay;
    private double dayOfWeek;
    private double accountFromHash;
    private double accountToHash;
    private double transactionType;
    private double ipRiskScore;
    private double locationRisk;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(double hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public double getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(double dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public double getAccountFromHash() {
        return accountFromHash;
    }

    public void setAccountFromHash(double accountFromHash) {
        this.accountFromHash = accountFromHash;
    }

    public double getAccountToHash() {
        return accountToHash;
    }

    public void setAccountToHash(double accountToHash) {
        this.accountToHash = accountToHash;
    }

    public double getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(double transactionType) {
        this.transactionType = transactionType;
    }

    public double getIpRiskScore() {
        return ipRiskScore;
    }

    public void setIpRiskScore(double ipRiskScore) {
        this.ipRiskScore = ipRiskScore;
    }

    public double getLocationRisk() {
        return locationRisk;
    }

    public void setLocationRisk(double locationRisk) {
        this.locationRisk = locationRisk;
    }
}
