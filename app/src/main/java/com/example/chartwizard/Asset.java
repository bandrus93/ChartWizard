package com.example.chartwizard;

import androidx.collection.ArrayMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

public class Asset implements Serializable {

    public static ArrayList<Asset> assetList = new ArrayList<>();
    private static double tradeFee = 0.005;

    private String symbol;
    private double priceCeiling, priceFloor, tradePrice, tradeAmount, stoploss, interestRate, takeProfit, currentPrice, currentVolume;
    private Double[] supportLevels, resistanceLevels;
    private double[][] candles;
    public ArrayMap<String, String> priceLevels = new ArrayMap<String, String>();
    public int interestPeriod;

    public static int findAsset(String sym) {
        int position = 0;
        for (int i = 0; i < assetList.size(); i++) {
            if (assetList.get(i).getSymbol().equals(sym)) {
                position = i;
                break;
            }
        }
        return position;
    }

    public void setSymbol(String s) {
        symbol = s;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setPriceCeiling(double c) {
        priceCeiling = c;
    }

    public double getPriceCeiling() {
        return priceCeiling;
    }

    public void setPriceFloor(double f) {
        priceFloor = f;
    }

    public double getPriceFloor() {
        return priceFloor;
    }

    public void setTradePrice(double t) {
        tradePrice = t;
    }

    public void setTradeAmount(double a) {
        tradeAmount = a;
    }

    public double getEven(String tt) {
        double bEP;
        if ("short".equals(tt)) {
            bEP = ((tradeAmount * tradePrice) * (1 - tradeFee)) / (tradeAmount + (interestRate * interestPeriod));
        } else {
            bEP = (tradeAmount * tradePrice * (1 + tradeFee)) / (tradeAmount - (tradeAmount * tradeFee));
        }
        return bEP;
    }

    public void setStoploss(double s) {
        stoploss = s;
    }

    public double getStoploss() {
        return stoploss;
    }

    public void setInterestRate(double i) {
        interestRate = i;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setTakeProfit(double mPP) {
        takeProfit = mPP;
    }

    public double getTakeProfit() {
        return takeProfit;
    }

    public void setSupportLevels(Double[] sL) {
        supportLevels = sL;
    }

    public Double[] getSupportLevels() {
        return supportLevels;
    }

    public void setResistanceLevels(Double[] rL) {
        resistanceLevels = rL;
    }

    public Double[] getResistanceLevels() {
        return resistanceLevels;
    }

    public void setCandles(double[][] c) {
        candles = c;
    }

    public Double getCandles(int index1, int index2) {
        return candles[index1][index2];
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double p) {
        currentPrice = p;
    }

    public double getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(double v) {
        currentVolume = v;
    }
    public static String formatUSD(double f) {
        Float value = Float.parseFloat(Double.toString(f));
        return String.format(Locale.US,"$%3.2f", value);
    }
}
