package com.jvmd.fraud.data;

public class TransactionData {

    private double amount;
    private double avgUserAmount;
    private double txFrequency;

    private double hourSin;
    private double hourCos;
    private double dayOfWeek;
    
    private double ipRiskScore;

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public double getAvgUserAmount() { return avgUserAmount; }
    public void setAvgUserAmount(double avgUserAmount) { this.avgUserAmount = avgUserAmount; }
    
    public double getTxFrequency() { return txFrequency; }
    public void setTxFrequency(double txFrequency) { this.txFrequency = txFrequency; }
    
    public double getHourSin() { return hourSin; }
    public void setHourSin(double hourSin) { this.hourSin = hourSin; }
    
    public double getHourCos() { return hourCos; }
    public void setHourCos(double hourCos) { this.hourCos = hourCos; }
    
    public double getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(double dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    
    public double getIpRiskScore() { return ipRiskScore; }
    public void setIpRiskScore(double ipRiskScore) { this.ipRiskScore = ipRiskScore; }
}
