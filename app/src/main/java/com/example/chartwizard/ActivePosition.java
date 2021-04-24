package com.example.chartwizard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class ActivePosition extends AppCompatActivity implements View.OnClickListener {
    private final Activity activePosition = this;

    public static Asset activeToken;
    public static CardView viewRoot;
    private static String tradeType;
    public static boolean active = false;

    private TextView activeSymbol, currentPrice, tradePrice, stoplossPrice, takeProfitPrice;
    private ImageView tradeIndicator, evenIndicator, stoplossIndicator, profitIndicator;
    private Button exitPosition;
    private int activeTokenPosition, resumeWatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_position);
        ConstraintLayout currentLayout = findViewById(R.id.activePosition);
        Intent startParams = getIntent();
        activeTokenPosition = Integer.parseInt(startParams.getStringExtra("LIST_POSITION"));
        activeToken = Asset.assetList.get(activeTokenPosition);
        viewRoot = findViewById(R.id.cardView);
        tradeType = startParams.getStringExtra("TRADE_TYPE");
        switch (startParams.getStringExtra("RESUME_CONDITION")) {
            case "seekLong":
                resumeWatch = 1;
                break;
            case "seekShort":
                resumeWatch = 2;
                break;
            case "seekAll":
                resumeWatch = 3;
                break;
        }
        activeSymbol = findViewById(R.id.activeSymbol);
        currentPrice = findViewById(R.id.currentPrice);
        tradePrice = findViewById(R.id.tradePrice);
        stoplossPrice = findViewById(R.id.stoplossPrice);
        takeProfitPrice = findViewById(R.id.takeProfitPrice);
        tradeIndicator = findViewById(R.id.tradeIndicator);
        evenIndicator = findViewById(R.id.evenIndicator);
        stoplossIndicator = findViewById(R.id.stoplossIndicator);
        profitIndicator = findViewById(R.id.profitIndicator);
        exitPosition = findViewById(R.id.exitPosition);
        exitPosition.setOnClickListener(this);
        if ("short".equals(tradeType)) {
            currentLayout.setBackgroundColor(getResources().getColor(R.color.bearRed));
            exitPosition.setBackgroundColor(getResources().getColor(R.color.bullGreen));
            exitPosition.setText(R.string.exit_short_text);
        }
        activeSymbol.setText(activeToken.getSymbol());
        double tokenPrice = activeToken.getCurrentPrice();
        double breakEven = activeToken.getEven(tradeType);
        double tokenStop = activeToken.getStoploss();
        double tokenProfit = activeToken.getTakeProfit();
        double prevClose = activeToken.getCandles(2, 1);
        currentPrice.setText(Asset.formatUSD(tokenPrice));
        if (tokenPrice > prevClose) {
            tradeIndicator.setImageResource(R.drawable.ic_bull_indicator);
        } else {
            tradeIndicator.setImageResource(R.drawable.ic_bear_indicator);
        }
        tradePrice.setText(Asset.formatUSD(breakEven));
        if (tokenPrice > breakEven) {
            evenIndicator.setImageResource(R.drawable.ic_bull_indicator);
        } else {
            evenIndicator.setImageResource(R.drawable.ic_bear_indicator);
        }
        stoplossPrice.setText(Asset.formatUSD(tokenStop));
        if (tokenPrice > tokenStop) {
            stoplossIndicator.setImageResource(R.drawable.ic_bull_indicator);
        } else {
            stoplossIndicator.setImageResource(R.drawable.ic_bear_indicator);
        }
        takeProfitPrice.setText(Asset.formatUSD(tokenProfit));
        if (tokenPrice > tokenProfit) {
            profitIndicator.setImageResource(R.drawable.ic_bull_indicator);
        } else {
            profitIndicator.setImageResource(R.drawable.ic_bear_indicator);
        }
        Intent intent = new Intent(this, ChartService.class);
        intent.putExtra("ASSET_SYMBOL", activeToken.getSymbol());
        if ("short".equals(tradeType)) {
            intent.putExtra("SERVICE_MODE", "bear exit");
        } else {
            intent.putExtra("SERVICE_MODE", "bull exit");
        }
        active = true;
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (resumeWatch) {
            case 1:
                intent.putExtra("SERVICE_MODE", "bull seek");
                break;
            case 2:
                intent.putExtra("SERVICE_MODE", "bear seek");
                break;
            case 3:
                intent.putExtra("SERVICE_MODE", "seek all");
                break;
        }
        startService(intent);
        activePosition.finish();
    }
}