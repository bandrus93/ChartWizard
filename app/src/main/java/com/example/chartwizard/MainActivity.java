package com.example.chartwizard;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    public static RecyclerView viewRoot;
    public static boolean active = false;

    private static ArrayList<WatchListItem> watchListArray;
    private RecyclerView watchList;
    private static WatchListAdapter listAdapter;
    private RecyclerView.LayoutManager listManager;
    public RadioGroup radioGroup;
    private RadioButton seekLong, seekShort, seekAll;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Spinner selectAsset;
    private EditText priceCeiling, priceFloor, supportLevel, supportLevel1, supportLevel2, resistanceLevel, resistanceLevel1, resistanceLevel2, tradePrice, tradeAmount, stoploss, interestRate, takeProfit;
    private CheckBox useSupport, useResistance;
    private ImageButton addSupport, addResistance, removeSupport1, removeSupport2, removeResistance1, removeResistance2;
    private Button addToList, cancel, killSwitch, openPosition, cancelOpen;
    private FloatingActionButton addNew;
    private static ProgressBar progressBar;
    private int checkedId;
    private String selectedAsset;
    private int supportCounter = 0;
    private int resistanceCounter = 0;
    private static boolean killEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        watchListArray = new ArrayList<>();
        watchList = findViewById(R.id.watchList);
        viewRoot = watchList;
        watchList.setHasFixedSize(true);
        listManager = new LinearLayoutManager(this);
        watchList.setLayoutManager(listManager);
        listAdapter = new WatchListAdapter(watchListArray);
        watchList.setAdapter(listAdapter);
        listAdapter.setOnItemClickListener(new WatchListAdapter.OnItemClickListener() {
            @Override
            public void onShowOptionsClick(int position, ImageView show, FloatingActionButton edit, FloatingActionButton delete, FloatingActionButton oLong, FloatingActionButton oShort) {
                if (edit.getVisibility() == View.GONE) {
                    oShort.setVisibility(View.VISIBLE);
                    oLong.setVisibility(View.VISIBLE);
                    delete.setVisibility(View.VISIBLE);
                    edit.setVisibility(View.VISIBLE);
                    show.setImageResource(R.drawable.ic_collapse);
                    MenuTransition expand = new MenuTransition("fadeIn", edit, delete, oLong, oShort);
                    expand.run();
                } else {
                    MenuTransition collapse = new MenuTransition("fadeOut", edit, delete, oLong, oShort);
                    collapse.run();
                    try {
                        Thread.sleep(525);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    edit.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    oLong.setVisibility(View.GONE);
                    oShort.setVisibility(View.GONE);
                    show.setImageResource(R.drawable.ic_baseline_more_vert);
                }
            }

            @Override
            public void onEditAssetClick(int position) {
                addNewAssetDialog(position);
            }

            @Override
            public void onDeleteAssetClick(int position) {
                String pair = Asset.assetList.get(position).getSymbol() + "-USD";
                String msg = "unsubscribe," + pair;
                ChartService.liveFeed.send(msg);
                Asset.assetList.remove(position);
                watchListArray.remove(position);
            }

            @Override
            public void onOpenLongClick(int position) {
                openPositionDialog(position, "long");
            }

            @Override
            public void onOpenShortClick(int position) {
                openPositionDialog(position, "short");
            }
        });
        radioGroup = findViewById(R.id.radioGroup);
        seekLong = findViewById(R.id.seekLong);
        seekShort = findViewById(R.id.seekShort);
        seekAll = findViewById(R.id.seekAll);
        checkedId = radioGroup.getCheckedRadioButtonId();
        progressBar = findViewById(R.id.progressBar);
        killSwitch = findViewById(R.id.killSwitch);
        killSwitch.setOnClickListener(this);
        addNew = findViewById(R.id.addNew);
        addNew.setOnClickListener(this);
        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }

    public void onClick(View button) {
        switch (button.getId()) {
            case R.id.addNew:
                addNewAssetDialog(null);
                break;
            case R.id.killSwitch:
                if (killEnabled) {
                    killConfirmDialog();
                }
                break;
        }
    }

    public void addNewAssetDialog(Integer position) {
        dialogBuilder = new AlertDialog.Builder(this);
        final View addNewPopupView = getLayoutInflater().inflate(R.layout.add_new_window, null);
        selectAsset = addNewPopupView.findViewById(R.id.selectAsset);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.assets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectAsset.setAdapter(adapter);
        priceCeiling = addNewPopupView.findViewById(R.id.priceCeiling);
        priceFloor = addNewPopupView.findViewById(R.id.priceFloor);
        supportLevel = addNewPopupView.findViewById(R.id.supportLevel);
        supportLevel1 = addNewPopupView.findViewById(R.id.supportLevel1);
        supportLevel2 = addNewPopupView.findViewById(R.id.supportLevel2);
        resistanceLevel = addNewPopupView.findViewById(R.id.resistanceLevel);
        resistanceLevel1 = addNewPopupView.findViewById(R.id.resistanceLevel1);
        resistanceLevel2 = addNewPopupView.findViewById(R.id.resistanceLevel2);
        useSupport = addNewPopupView.findViewById(R.id.useSupport);
        useResistance = addNewPopupView.findViewById(R.id.useResistance);
        addSupport = addNewPopupView.findViewById(R.id.addSupport);
        removeSupport1 = addNewPopupView.findViewById(R.id.removeSupport1);
        removeSupport2 = addNewPopupView.findViewById(R.id.removeSupport2);
        addResistance = addNewPopupView.findViewById(R.id.addResistance);
        removeResistance1 = addNewPopupView.findViewById(R.id.removeResistance1);
        removeResistance2 = addNewPopupView.findViewById(R.id.removeResistance2);
        addToList = addNewPopupView.findViewById(R.id.addToList);
        cancel = addNewPopupView.findViewById(R.id.cancel);
        if (position != null) {
            Asset selectedAsset = Asset.assetList.get(position);
            String assetSymbol = selectedAsset.getSymbol();
            String assetCeiling = String.valueOf(selectedAsset.getPriceCeiling());
            String assetFloor = String.valueOf(selectedAsset.getPriceFloor());
            Double[] assetSupport = selectedAsset.getSupportLevels();
            Double[] assetResistance = selectedAsset.getResistanceLevels();
            selectAsset.setPrompt(assetSymbol);
            selectAsset.setEnabled(false);
            priceCeiling.setText(assetCeiling);
            priceFloor.setText(assetFloor);
            for (Double aDouble : assetSupport) {
                String supportString = String.valueOf(aDouble);
                if (supportString.equals(assetFloor)) {
                    useSupport.setChecked(true);
                } else {
                    switch (supportCounter) {
                        case 0:
                            if (supportLevel.getText().toString().equals(supportString)) {
                                supportLevel1.setVisibility(View.VISIBLE);
                                removeSupport1.setVisibility(View.VISIBLE);
                                supportLevel1.setText(supportString);
                                supportCounter++;
                            } else {
                                supportLevel.setText(supportString);
                            }
                            break;
                        case 1:
                            supportLevel2.setVisibility(View.VISIBLE);
                            removeSupport2.setVisibility(View.VISIBLE);
                            supportLevel2.setText(supportString);
                            supportCounter++;
                            break;
                    }
                }
            }
            for (Double aDouble : assetResistance) {
                String resistanceString = String.valueOf(aDouble);
                if (resistanceString.equals(assetCeiling)) {
                    useResistance.setChecked(true);
                } else {
                    switch (resistanceCounter) {
                        case 0:
                            if (resistanceLevel.getText().toString().equals(resistanceString)) {
                                resistanceLevel1.setVisibility(View.VISIBLE);
                                removeResistance1.setVisibility(View.VISIBLE);
                                resistanceLevel1.setText(resistanceString);
                                resistanceCounter++;
                            } else {
                                resistanceLevel.setText(resistanceString);
                            }
                            break;
                        case 1:
                            resistanceLevel2.setVisibility(View.VISIBLE);
                            removeResistance2.setVisibility(View.VISIBLE);
                            resistanceLevel2.setText(resistanceString);
                            resistanceCounter++;
                            break;
                    }
                }
            }
            addToList.setText(R.string.save);
        }
        dialogBuilder.setView(addNewPopupView);
        dialog = dialogBuilder.create();
        dialog.show();

        selectAsset.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Object assetObject = adapterView.getItemAtPosition(i);
                selectedAsset = assetObject.toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        addSupport.setOnClickListener(addSupport -> {
            if (supportCounter == 0) {
                supportLevel1.setVisibility(View.VISIBLE);
                removeSupport1.setVisibility(View.VISIBLE);
                supportCounter++;
            }
            else if (supportCounter == 1) {
                supportLevel2.setVisibility(View.VISIBLE);
                removeSupport2.setVisibility(View.VISIBLE);
                supportCounter++;
            }
        });

        removeSupport1.setOnClickListener(removeSupport1 -> {
            supportLevel1.getText().clear();
            supportLevel1.setVisibility(View.GONE);
            removeSupport1.setVisibility(View.GONE);
            supportCounter--;
        });

        removeSupport2.setOnClickListener(removeSupport2 -> {
            supportLevel2.getText().clear();
            supportLevel2.setVisibility(View.GONE);
            removeSupport2.setVisibility(View.GONE);
            supportCounter--;
        });

        addResistance.setOnClickListener(addResistance -> {
            if (resistanceCounter == 0) {
                resistanceLevel1.setVisibility(View.VISIBLE);
                removeResistance1.setVisibility(View.VISIBLE);
                resistanceCounter++;
            }
            else if (resistanceCounter == 1) {
                resistanceLevel2.setVisibility(View.VISIBLE);
                removeResistance2.setVisibility(View.VISIBLE);
                resistanceCounter++;
            }
        });

        removeResistance1.setOnClickListener(removeResistance1 -> {
            resistanceLevel1.getText().clear();
            resistanceLevel1.setVisibility(View.GONE);
            removeResistance1.setVisibility(View.GONE);
            resistanceCounter--;
        });

        removeResistance2.setOnClickListener(removeResistance2 -> {
            resistanceLevel2.getText().clear();
            resistanceLevel2.setVisibility(View.GONE);
            removeResistance2.setVisibility(View.GONE);
            resistanceCounter--;
        });

        addToList.setOnClickListener(addToList -> {
            Asset asset;
            String symbol = null;
            if (position == null) {
                String[] split = selectedAsset.split("[()]");
                symbol = split[1];
            }
            double ceiling = Double.parseDouble(priceCeiling.getText().toString());
            double floor = Double.parseDouble(priceFloor.getText().toString());
            String sLevel1 = supportLevel1.getText().toString();
            String sLevel2 = supportLevel2.getText().toString();
            String rLevel1 = resistanceLevel1.getText().toString();
            String rLevel2 = resistanceLevel2.getText().toString();
            ArrayList<Double> support = new ArrayList<>();
            ArrayList<Double> resistance = new ArrayList<>();
            if (useSupport.isChecked()) {
                support.add(floor);
            }
            support.add(Double.parseDouble(supportLevel.getText().toString()));
            if (!"".equals(sLevel1)) {
                support.add(Double.parseDouble(sLevel1));
            }
            if (!"".equals(sLevel2)) {
                support.add(Double.parseDouble(sLevel2));
            }
            if (useResistance.isChecked()) {
                resistance.add(ceiling);
            }
            resistance.add(Double.parseDouble(resistanceLevel.getText().toString()));
            if (!"".equals(rLevel1)) {
                resistance.add(Double.parseDouble(rLevel1));
            }
            if (!"".equals(rLevel2)) {
                resistance.add(Double.parseDouble(rLevel2));
            }
            Double[] supportArray = new Double[support.size()];
            supportArray = support.toArray(supportArray);
            Double[] resistanceArray = new Double[resistance.size()];
            resistanceArray = resistance.toArray(resistanceArray);
            if (position == null) {
                asset = new Asset();
                asset.setSymbol(symbol);
            } else {
                asset = Asset.assetList.get(position);
            }
            asset.setPriceCeiling(ceiling);
            asset.setPriceFloor(floor);
            asset.setSupportLevels(supportArray);
            asset.setResistanceLevels(resistanceArray);
            if (position == null) {
                Asset.assetList.add(asset);
                engage(asset);
            }
            dialog.dismiss();
            progressBar.setVisibility(View.VISIBLE);
        });

        cancel.setOnClickListener(cancel -> dialog.dismiss());
    }

    public void engage(Asset asset) {
        Intent intent = new Intent(getApplicationContext(), ChartService.class);
        Bundle extras = new Bundle();
        extras.putString("ASSET_SYMBOL", asset.getSymbol());
        switch (checkedId) {
            case R.id.seekLong:
                extras.putString("SERVICE_MODE", "bull seek");
                break;
            case R.id.seekShort:
                extras.putString("SERVICE_MODE", "bear seek");
                break;
            case R.id.seekAll:
                extras.putString("SERVICE_MODE", "seek all");
                break;
        }
        intent.putExtras(extras);
        startService(intent);
        killSwitch.setVisibility(View.VISIBLE);
    }

    public void openPositionDialog(int position, String trade) {
        dialogBuilder = new AlertDialog.Builder(this);
        final View addNewPopupView = getLayoutInflater().inflate(R.layout.open_position_window, null);
        tradePrice = addNewPopupView.findViewById(R.id.purchasePrice);
        tradeAmount = addNewPopupView.findViewById(R.id.purchaseAmount);
        stoploss = addNewPopupView.findViewById(R.id.stoploss);
        interestRate = addNewPopupView.findViewById(R.id.interestRate);
        takeProfit = addNewPopupView.findViewById(R.id.takeProfit);
        openPosition = addNewPopupView.findViewById(R.id.openPosition);
        cancelOpen = addNewPopupView.findViewById(R.id.cancelOpen);
        if ("short".equals(trade)) {
            interestRate.setVisibility(View.VISIBLE);
            tradePrice.setHint(R.string.sale_price_hint);
            tradeAmount.setHint(R.string.sale_amount_hint);
            openPosition.setText(R.string.open_short_text);
            openPosition.setBackgroundColor(getResources().getColor(R.color.bearRed));
        }
        dialogBuilder.setView(addNewPopupView);
        dialog = dialogBuilder.create();
        dialog.show();

        openPosition.setOnClickListener(open -> {
            double price = Double.parseDouble(tradePrice.getText().toString());
            double amount = Double.parseDouble(tradeAmount.getText().toString());
            double stop = Double.parseDouble(stoploss.getText().toString());
            double interest;
            double profit = Double.parseDouble(takeProfit.getText().toString());
            Asset token = Asset.assetList.get(position);
            if ("short".equals(trade)) {
                interest = Double.parseDouble(interestRate.getText().toString());
                token.setInterestRate(interest);
            }
            token.setTradePrice(price);
            token.setTradeAmount(amount);
            token.setStoploss(stop);
            token.setTakeProfit(profit);
            execute(position, trade);
        });
    }

    public void execute(int position, String trade) {
        Intent intent = new Intent(this, ActivePosition.class);
        intent.putExtra("LIST_POSITION", position);
        intent.putExtra("TRADE_TYPE", trade);
        switch (checkedId) {
            case R.id.seekLong:
                intent.putExtra("RESUME_CONDITION", "seekLong");
                break;
            case R.id.seekShort:
                intent.putExtra("RESUME_CONDITION", "seekShort");
                break;
            case R.id.seekAll:
                intent.putExtra("RESUME_CONDITION", "seekAll");
                break;
        }
        startActivity(intent);
    }

    public void killConfirmDialog() {
        final Intent intent = new Intent(this, ChartService.class);
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.kill_confirm, (dialogInterface, i) -> {
          for (i = 0; i <= watchListArray.size(); i++) {
              watchListArray.remove(i);
          }
          stopService(intent);
          dialog.dismiss();
        });
        dialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialog.dismiss());
        dialogBuilder.setTitle(R.string.kill_confirm_title).setMessage(R.string.kill_confirm_message);
        dialog = dialogBuilder.create();
        dialog.show();
    }

    public static class MenuTransition implements Runnable {
        private final String transition;
        private final FloatingActionButton edit, delete, oLong, oShort;

        public MenuTransition(String transition, FloatingActionButton mEdit, FloatingActionButton mDelete, FloatingActionButton mLong, FloatingActionButton mShort) {
            this.transition = transition;
            edit = mEdit;
            delete = mDelete;
            oLong = mLong;
            oShort = mShort;
        }

        @Override
        public void run() {
            if ("fadeIn".equals(transition)) {
                oShort.animate().alpha(1f).setDuration(150);
                oLong.animate().alpha(1f).setStartDelay(125).setDuration(150);
                delete.animate().alpha(1f).setStartDelay(250).setDuration(150);
                edit.animate().alpha(1f).setStartDelay(375).setDuration(150);
            } else {
                edit.animate().alpha(0f).setDuration(150);
                delete.animate().alpha(0f).setStartDelay(125).setDuration(150);
                oLong.animate().alpha(0f).setStartDelay(250).setDuration(150);
                oShort.animate().alpha(0f).setStartDelay(375).setDuration(150);
            }
        }
    }

    public static class UpdateUI implements Runnable {
        public static final int ADD_ITEM = 0;
        public static final int UPDATE_ITEM = 1;

        private final int position, opcode;

        public UpdateUI(int uPosition, int uOpcode) {
            position = uPosition;
            opcode = uOpcode;
        }

        @Override
        public void run() {
            if (opcode == ADD_ITEM) {
                Asset asset = Asset.assetList.get(position);
                String ticker = asset.getSymbol();
                String price = Asset.formatUSD(asset.getCurrentPrice());
                int indicator;
                if (asset.getCurrentPrice() > asset.getCandles(2, 1)) {
                    indicator = R.drawable.ic_bull_indicator;
                } else {
                    indicator = R.drawable.ic_bear_indicator;
                }
                watchListArray.add(position, new WatchListItem(indicator, price, ticker));
                listAdapter.notifyItemInserted(position);
                if (position == 0) {
                    killEnabled = true;
                }
            } else {
                WatchListItem updateItem = watchListArray.get(position);
                Asset cAsset = Asset.assetList.get(position);
                String nPrice = Asset.formatUSD(cAsset.getCurrentPrice());
                int cIndicator;
                if (cAsset.getCurrentPrice() > cAsset.getCandles(2, 1)) {
                    cIndicator = R.drawable.ic_bull_indicator;
                } else {
                    cIndicator = R.drawable.ic_bear_indicator;
                }
                updateItem.setAssetPrice(nPrice);
                updateItem.setImageResource(cIndicator);
            }
            progressBar.setVisibility(View.GONE);
        }
    }
}