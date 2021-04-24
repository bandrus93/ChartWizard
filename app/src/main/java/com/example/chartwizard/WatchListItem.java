package com.example.chartwizard;

public class WatchListItem {
    private int mImageResource;
    private String mAssetPrice;
    private final String mAssetSymbol;

    public WatchListItem(int imageResource, String assetPrice, String assetSymbol) {
        mImageResource = imageResource;
        mAssetPrice = assetPrice;
        mAssetSymbol = assetSymbol;
    }

    public int getImageResource() {
        return mImageResource;
    }

    public void setImageResource(int i) {
        mImageResource = i;
    }

    public String getAssetPrice() {
        return mAssetPrice;
    }

    public void setAssetPrice(String p) {
        mAssetPrice = p;
    }

    public String getAssetSymbol() {
        return mAssetSymbol;
    }
}
