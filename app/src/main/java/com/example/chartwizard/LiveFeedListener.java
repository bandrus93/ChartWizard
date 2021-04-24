package com.example.chartwizard;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class LiveFeedListener extends WebSocketListener {
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private WebSocket liveFeed;
    private final Looper socketLooper;
    private SocketHandler socketHandler;

    public LiveFeedListener(Looper looper) {
        socketLooper = looper;
    }

    @Override
    public void onOpen(WebSocket socket, Response response) {
        liveFeed = socket;
        socketHandler = new SocketHandler(socketLooper);
        String pair = (Asset.assetList.get(0).getSymbol()) + "-USD";
        LiveFeedMessage subscribe = new LiveFeedMessage();
        subscribe.type = "subscribe";
        subscribe.product_ids[0] = pair;
        subscribe.channels[0] = "level2";
        Message msg = socketHandler.obtainMessage();
        msg.arg1 = 0;
        msg.obj = subscribe;
        socketHandler.sendMessage(msg);
    }

    @Override
    public void onMessage(WebSocket liveFeed, String msg) {
        int msgArg = 1;
        LiveFeedMessage nSub = null;
        String[] split = msg.split(",");
        if ("subscribe".equals(split[0]) || "unsubscribe".equals(split[0])) {
            msgArg = 0;
            nSub = new LiveFeedMessage();
            nSub.type = split[0];
            nSub.product_ids[0] = split[1];
            nSub.channels[0] = "level2";
        }
        Message nMsg = socketHandler.obtainMessage();
        nMsg.arg1 = msgArg;
        if (msgArg == 1) {
            nMsg.obj = msg;
        } else {
            nMsg.obj = nSub;
        }
        socketHandler.sendMessage(nMsg);
    }

    @Override
    public void onClosing(WebSocket liveFeed, int code, String reason) {
        liveFeed.close(NORMAL_CLOSURE_STATUS, null);
    }

    @Override
    public void onFailure(WebSocket liveFeed, Throwable t, Response response) {
        Message eMsg = ChartService.errorHandler.obtainMessage();
        eMsg.what = 2;
        eMsg.obj = t.getMessage();
        ChartService.errorHandler.sendMessage(eMsg);
        t.printStackTrace();
    }

    private final class SocketHandler extends Handler {
        Gson gson = new Gson();

        public SocketHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String in;
            LiveFeedMessage out;
            Handler uiHandler = new Handler(Looper.getMainLooper());

            if (msg.arg1 == 0) {
                out = (LiveFeedMessage) msg.obj;
                String qMsg = encodeOutbound(out);
                liveFeed.send(qMsg);
            } else {
                in = (String) msg.obj;
                LiveFeedMessage qMsg = decodeInbound(in);
                switch (qMsg.type) {
                    case "error":
                        Message eMsg = ChartService.errorHandler.obtainMessage();
                        eMsg.what = 2;
                        eMsg.obj = qMsg.message;
                        ChartService.errorHandler.sendMessage(eMsg);
                        break;
                    case "snapshot":
                        String[] split = qMsg.product_id.split("-");
                        String sym = split[0];
                        int position = Asset.findAsset(sym);
                        double spread = Math.abs(qMsg.bids[0][0] - qMsg.asks[0][0]);
                        double currentPrice;
                        if (qMsg.bids[0][0] > qMsg.asks[0][0]) {
                            currentPrice = qMsg.asks[0][0] + (spread / 2);
                        } else {
                            currentPrice = qMsg.bids[0][0] + (spread / 2);
                        }
                        Asset.assetList.get(position).setCurrentPrice(currentPrice);
                        MainActivity.UpdateUI initialize = new MainActivity.UpdateUI(position, MainActivity.UpdateUI.ADD_ITEM);
                        uiHandler.post(initialize);
                        break;
                    case "l2update":
                        String[] nSplit = qMsg.product_id.split("-");
                        String nSym = nSplit[0];
                        int nPosition = Asset.findAsset(nSym);
                        Asset updateAsset = Asset.assetList.get(nPosition);
                        double price = 0.00;
                        if (updateAsset.priceLevels.isEmpty()) {
                            for (int n = 0; n < qMsg.changes.length; n++) {
                                if (!"0".equals(qMsg.changes[n][2])) {
                                    updateAsset.priceLevels.put(qMsg.changes[n][1], qMsg.changes[n][2]);
                                    if (price == 0.00) {
                                        price = Double.parseDouble(qMsg.changes[n][1]);
                                    }
                                }
                            }
                        } else {
                            for (int n = 0; n < qMsg.changes.length; n++) {
                                String key = qMsg.changes[n][1];
                                String value = qMsg.changes[n][2];
                                if (updateAsset.priceLevels.containsKey(key) && !"0".equals(value)) {
                                    int index = updateAsset.priceLevels.indexOfKey(key);
                                    updateAsset.priceLevels.setValueAt(index, value);
                                    if (price == 0.00) {
                                        price = Double.parseDouble(qMsg.changes[n][1]);
                                    }
                                }
                                else if (!updateAsset.priceLevels.containsKey(key) && !"0".equals(value)) {
                                    updateAsset.priceLevels.put(key, value);
                                    if (price == 0.00) {
                                        price = Double.parseDouble(qMsg.changes[n][1]);
                                    }
                                }
                            }
                        }
                        if (price != 0.00) {
                            updateAsset.setCurrentPrice(price);
                        }
                        double volume = 0.00;
                        for (String s : updateAsset.priceLevels.values()) {
                            volume = volume + Double.parseDouble(s);
                        }
                        updateAsset.setCurrentVolume(volume);
                        MainActivity.UpdateUI uiUpdate = new MainActivity.UpdateUI(nPosition, MainActivity.UpdateUI.UPDATE_ITEM);
                        uiHandler.post(uiUpdate);
                        break;
                }
            }
        }

        private String encodeOutbound(LiveFeedMessage msg) {
            return gson.toJson(msg);
        }

        private LiveFeedMessage decodeInbound(String msg) {
            return gson.fromJson(msg, LiveFeedMessage.class);
        }
    }
}