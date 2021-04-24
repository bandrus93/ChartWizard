package com.example.chartwizard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.tinder.scarlet.Scarlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class ChartService extends Service {

    private HandlerThread serviceThread, failSafe;
    private Looper serviceLooper, errorLooper;
    private ServiceHandler serviceHandler;
    public static FailSafe errorHandler;
    public static WebSocket liveFeed;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private boolean running = false;
    private ArrayList<ScheduledExecutorService> executing;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManager;
    private NotificationChannel channel;
    public String notifTitle, notifContent;
    public int notifId = 0;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            channel = new NotificationChannel("TRADE_ALERT_CHANNEL", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {
            if (msg.what > 1) {
                try {
                    updateCandles();
                } catch (NoSuchAlgorithmException e) {
                    Message eMsg = errorHandler.obtainMessage();
                    eMsg.what = 0;
                    errorHandler.sendMessage(eMsg);
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    Message eMsg = errorHandler.obtainMessage();
                    eMsg.what = 1;
                    errorHandler.sendMessage(eMsg);
                    e.printStackTrace();
                } catch (Exception e) {
                    Message eMsg = errorHandler.obtainMessage();
                    eMsg.what = 2;
                    eMsg.obj = e.getMessage();
                    errorHandler.sendMessage(eMsg);
                    e.printStackTrace();
                }
                if (msg.arg2 >= 0) {
                    updateSubscriptions("subscribe", msg.arg2);
                }
                if (!executing.isEmpty()) {
                    for (int i = 0; i < executing.size(); i++) {
                        ScheduledExecutorService eTask = executing.get(i);
                        eTask.shutdown();
                    }
                }
                switch (msg.what) {
                    case 4:
                        ScheduledExecutorService bullExecutor = Executors.newScheduledThreadPool(1);
                        Task bullSeek = new Task("ENTER_LONG");
                        ScheduledFuture<?> seek1 = bullExecutor.scheduleAtFixedRate(bullSeek, 0, 60, TimeUnit.SECONDS);
                        executing.add(bullExecutor);
                        break;
                    case 3:
                        ScheduledExecutorService bearExecutor = Executors.newScheduledThreadPool(1);
                        Task bearSeek = new Task("ENTER_SHORT");
                        ScheduledFuture<?> seek2 = bearExecutor.scheduleAtFixedRate(bearSeek, 0, 60, TimeUnit.SECONDS);
                        executing.add(bearExecutor);
                        break;
                    case 2:
                        ScheduledExecutorService masterExecutor = Executors.newScheduledThreadPool(2);
                        Task bullMaster = new Task("ENTER_LONG");
                        Task bearMaster = new Task("ENTER_SHORT");
                        ScheduledFuture<?> seek = masterExecutor.scheduleAtFixedRate(bullMaster, 0, 60, TimeUnit.SECONDS);
                        ScheduledFuture<?> seek0 = masterExecutor.scheduleAtFixedRate(bearMaster, 0, 60, TimeUnit.SECONDS);
                        executing.add(masterExecutor);
                        break;
                }
            }
            else if (msg.what == 1) {
                for (int i = 0; i < executing.size(); i++) {
                    ScheduledExecutorService eTask = executing.get(i);
                    eTask.shutdown();
                }
                ScheduledExecutorService longExecutor = Executors.newScheduledThreadPool(1);
                Task bullExit = new Task("EXIT_LONG");
                ScheduledFuture<?> exit = longExecutor.scheduleAtFixedRate(bullExit, 0, 60, TimeUnit.SECONDS);
                executing.add(longExecutor);
            } else {
                for (int i = 0; i < executing.size(); i++) {
                    ScheduledExecutorService eTask = executing.get(i);
                    eTask.shutdown();
                }
                ScheduledExecutorService shortExecutor = Executors.newScheduledThreadPool(1);
                Task bearExit = new Task("EXIT_SHORT");
                ScheduledFuture<?> exit1 = shortExecutor.scheduleAtFixedRate(bearExit, 0, 60, TimeUnit.SECONDS);
                executing.add(shortExecutor);
            }
        }
    }

    public final class FailSafe extends Handler {
        public FailSafe(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String error;
            switch (msg.what) {
                case 0:
                    error = "Signature error: Algorithm does not exist";
                    break;
                case 1:
                    error = "Signature error: Invalid key";
                    break;
                case 2:
                    error = (String) msg.obj;
                    break;
                case 3:
                    error = "Task error: An error occurred during task execution";
                    break;
                default:
                    error = "Unknown error: An unexpected error occurred";
            }
            if (MainActivity.active) {
                Snackbar.make(MainActivity.viewRoot, error, BaseTransientBottomBar.LENGTH_INDEFINITE);
            }
            else if (ActivePosition.active) {
                Snackbar.make(ActivePosition.viewRoot, error, BaseTransientBottomBar.LENGTH_INDEFINITE);
            } else {
                notifTitle = "Warning: Fail-safe initiated!";
                notifContent = error;
                notificationManager.notify(notifId, builder.build());
                notifId++;
            }
        }
    }

    @Override
    public void onCreate() {
        serviceThread = new HandlerThread("ServiceStartArgs") {
            @Override
            public void run() {
                Looper.prepare();
                serviceLooper = Looper.myLooper();
                serviceHandler = new ServiceHandler(serviceLooper);
                Looper.loop();
            }
        };
        serviceThread.setPriority(THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        failSafe = new HandlerThread("FailSafeArgs") {
            @Override
            public void run() {
                Looper.prepare();
                errorLooper = Looper.myLooper();
                errorHandler = new FailSafe(errorLooper);
                Looper.loop();
            }
        };
        failSafe.setPriority(THREAD_PRIORITY_BACKGROUND);
        failSafe.start();
        executing = new ArrayList<>();
        builder = new NotificationCompat.Builder(this, "TRADE_ALERT_CHANNEL")
                .setSmallIcon(R.drawable.ic_baseline_insights_24)
                .setContentTitle(notifTitle)
                .setContentText(notifContent)
                .setAutoCancel(true);
        notificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        String assetSymbol = extras.getString("ASSET_SYMBOL");
        String serviceMode = extras.getString("SERVICE_MODE");
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        if ("".equals(assetSymbol)) {
            msg.arg2 = -1;
        } else {
            msg.arg2 = Asset.findAsset(assetSymbol);
        }
        switch (serviceMode) {
            case "bull seek":
                msg.what = 4;
                break;
            case "bear seek":
                msg.what = 3;
                break;
            case "seek all":
                msg.what = 2;
                break;
            case "bull exit":
                msg.what = 1;
                break;
            case "bear exit":
                msg.what = 0;
                break;
        }
        serviceHandler.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDestroy() {
        serviceThread.quitSafely();
        for (int i = 0; i < Asset.assetList.size(); i++) {
            updateSubscriptions("unsubscribe", i);
        }
        running = false;
        if (executing != null) {
            for (int i = 0; i < executing.size(); i++) {
                ScheduledExecutorService eTask = executing.get(i);
                eTask.shutdown();
            }
        }
        failSafe.quitSafely();
        notifId = 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public synchronized void updateCandles() throws NoSuchAlgorithmException, InvalidKeyException, Exception {
        String intervalStart = "";
        String intervalEnd = "";
        String timeStamp = "";
        //API credentials have been redacted for security reasons
        String secret = "redacted";
        String cbAccessKey = "redacted";
        String cbAccessPassphrase = "redacted";
        String requestPath;
        String method = "GET";
        String preHash;
        String granularity = "21600";

        Request getServerTime = new Request.Builder()
                .url("https://api.pro.coinbase.com/time")
                .get()
                .build();
        okhttp3.Call timeSync = httpClient.newCall(getServerTime);
        okhttp3.Response response = timeSync.execute();
        if (!response.isSuccessful()) {
            Message eMsg = errorHandler.obtainMessage();
            String e = R.string.error_header + response.toString();
            eMsg.what = 2;
            eMsg.obj = e;
            errorHandler.sendMessage(eMsg);
        } else {
            ServerTime gdaxTime = gson.fromJson(response.body().string(), ServerTime.class);
            timeStamp = gdaxTime.getEpochTime().toString();
            String[] interval = gdaxTime.getInterval().split(",");
            intervalStart = interval[0];
            intervalEnd = interval[1];
        }
        for (int i = 0; i < Asset.assetList.size(); i++) {
            Asset selectedAsset = Asset.assetList.get(i);
            if (!selectedAsset.priceLevels.isEmpty()) {
                selectedAsset.priceLevels.clear();
            }
            String symbol = selectedAsset.getSymbol();
            String productId = symbol + "-USD";
            requestPath = "/products/" + productId + "/candles";
            preHash = timeStamp + method + requestPath;
            String signature = generateSig(secret, preHash);
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("api.pro.coinbase.com")
                    .addPathSegments(requestPath)
                    .addQueryParameter("start", intervalStart)
                    .addQueryParameter("end", intervalEnd)
                    .addQueryParameter("granularity", granularity)
                    .build();
            Request getHistoricRates = new Request.Builder()
                    .url(url)
                    .addHeader("CB-ACCESS-KEY", cbAccessKey)
                    .addHeader("CB-ACCESS-PASSPHRASE", cbAccessPassphrase)
                    .addHeader("CB-ACCESS-SIGN", signature)
                    .addHeader("CB-ACCESS-TIMESTAMP", timeStamp)
                    .get()
                    .build();
            Call call = httpClient.newCall(getHistoricRates);
            if (i > 0) {
                call.wait(1010);
            }
            Response serverResponse = call.execute();
            if (!serverResponse.isSuccessful()) {
                Message eMsg = errorHandler.obtainMessage();
                String e = R.string.error_header + serverResponse.toString();
                eMsg.what = 2;
                eMsg.obj = e;
                errorHandler.sendMessage(eMsg);
            } else {
                String rawData = serverResponse.body().string();
                String[] arrayData = rawData.split("\\[]");
                int n = 0;
                double[][] candles = new double[3][3];
                for(int j = 2; j < 7; j++) {
                    if(j % 2 == 0) {
                        String[] dataValue = arrayData[j].split(",");
                        candles[n][0] = Double.parseDouble(dataValue[3]);
                        candles[n][1] = Double.parseDouble(dataValue[4]);
                        candles[n][2] = Double.parseDouble(dataValue[5]);
                        n++;
                    }
                }
                selectedAsset.setCandles(candles);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String generateSig(String secret, String preHash) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] decodedSecret = Base64.getDecoder().decode(secret);
        SecretKeySpec keyspec = new SecretKeySpec(decodedSecret, "HmacSHA256");
        Mac sha256 = Mac.getInstance("HmacSHA256");
        sha256.init(keyspec);
        return Base64.getEncoder().encodeToString(sha256.doFinal(preHash.getBytes()));
    }

    public void updateSubscriptions(String type, int position) {
        if (running) {
            String pair = (Asset.assetList.get(position).getSymbol()) + "-USD";
            String msg = type + ',' + pair;
            liveFeed.send(msg);
            if ("unsubscribe".equals(type)) {
                Asset.assetList.remove(position);
                if (Asset.assetList.size() == 0) {
                    liveFeed.close(1000, null);
                }
            }
        } else {
            new Thread() {
                @Override
                public void run() {
                    running = true;
                    Looper.prepare();
                    Looper socketLooper = Looper.myLooper();
                    Looper.loop();
                    Request request = new Request.Builder()
                            .url("wss://ws-feed.pro.coinbase.com")
                            .build();
                    LiveFeedListener listener = new LiveFeedListener(socketLooper);
                    liveFeed = httpClient.newWebSocket(request, listener);
                }
            }.start();
        }
    }

    public class Task implements Runnable {
        private final String method;
        private int n = 1;
        private int m = 1;

        public Task(String method) {
            this.method = method;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            try {
                switch (method) {
                    case "ENTER_LONG":
                        for (int i = 0; i < Asset.assetList.size(); i++) {
                            Asset selectedAsset = Asset.assetList.get(i);
                            double currentPrice = selectedAsset.getCurrentPrice();
                            double currentVolume = selectedAsset.getCurrentVolume();
                            double prevClose = selectedAsset.getCandles(2, 1);
                            double currentMoment = ((currentPrice - prevClose) / n) * currentVolume;
                            double priceFloor = selectedAsset.getPriceFloor();
                            Double[] supportLevels = selectedAsset.getSupportLevels();
                            Double[] resistanceLevels = selectedAsset.getResistanceLevels();
                            double initOpen = selectedAsset.getCandles(0, 0);
                            double subOpen = selectedAsset.getCandles(1, 0);
                            double prevOpen = selectedAsset.getCandles(2, 0);
                            double prevVolume = selectedAsset.getCandles(2, 2);
                            double prevMoment = ((prevClose - prevOpen) / 360) * prevVolume;
                            if (currentPrice - prevClose > 0 && prevOpen - prevClose < 0 && prevOpen - subOpen < 0 && subOpen - initOpen < 0) {
                                notifTitle = "Buy " + selectedAsset.getSymbol();
                                notifContent = "Bottom reversal detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            }
                            else if (currentPrice - prevClose > 0 && prevOpen - prevClose < 0) {
                                for (Double supportLevel : supportLevels) {
                                    double posTolerance = supportLevel * 1.005;
                                    double negTolerance = supportLevel * .995;
                                    if (prevClose > negTolerance && prevClose < posTolerance) {
                                        notifTitle = "Buy " + selectedAsset.getSymbol();
                                        notifContent = "Bottom reversal at support detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    } else if (prevClose < supportLevel && Math.abs(prevMoment) <= currentMoment) {
                                        notifTitle = "Buy " + selectedAsset.getSymbol();
                                        notifContent = "Strong reversal below support detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    }
                                }
                            }
                            else if (prevClose - prevOpen > 0 && currentPrice - prevClose > 0 && currentMoment > prevMoment) {
                                for (Double resistanceLevel : resistanceLevels) {
                                    if (currentPrice > resistanceLevel) {
                                        notifTitle = "Buy " + selectedAsset.getSymbol();
                                        notifContent = "Accelerating positive momentum above resistance detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    }
                                }
                                if (prevClose < priceFloor) {
                                    notifTitle = "Buy " + selectedAsset.getSymbol();
                                    notifContent = "Accelerating positive momentum below floor detected";
                                    notificationManager.notify(notifId, builder.build());
                                    notifId++;
                                }
                            }
                        }
                        n++;
                        if (n > 360) {
                            updateCandles();
                            n = 1;
                        }
                        break;
                    case "ENTER_SHORT":
                        for (int i = 0; i < Asset.assetList.size(); i++) {
                            Asset selectedAsset = Asset.assetList.get(i);
                            double currentPrice = selectedAsset.getCurrentPrice();
                            double currentVolume = selectedAsset.getCurrentVolume();
                            double prevClose = selectedAsset.getCandles(2, 1);
                            double currentMoment = ((currentPrice - prevClose) / m) * currentVolume;
                            double priceCeiling = selectedAsset.getPriceCeiling();
                            Double[] supportLevels = selectedAsset.getSupportLevels();
                            Double[] resistanceLevels = selectedAsset.getResistanceLevels();
                            double initOpen = selectedAsset.getCandles(0, 0);
                            double subOpen = selectedAsset.getCandles(1, 0);
                            double prevOpen = selectedAsset.getCandles(2, 0);
                            double prevVolume = selectedAsset.getCandles(2, 2);
                            double prevMoment = ((prevClose - prevOpen) / 360) * prevVolume;
                            if (currentPrice - prevClose < 0 && prevOpen - prevClose > 0 && prevOpen - subOpen > 0 && subOpen - initOpen > 0) {
                                notifTitle = "Sell " + selectedAsset.getSymbol();
                                notifContent = "Top reversal detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            }
                            else if (currentPrice - prevClose < 0 && prevOpen - prevClose > 0) {
                                for (Double resistanceLevel : resistanceLevels) {
                                    double posTolerance = resistanceLevel * 1.005;
                                    double negTolerance = resistanceLevel * .995;
                                    if (prevClose > negTolerance && prevClose < posTolerance) {
                                        notifTitle = "Sell " + selectedAsset.getSymbol();
                                        notifContent = "Top reversal at resistance detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    } else if (prevClose > resistanceLevel && Math.abs(currentMoment) >= prevMoment) {
                                        notifTitle = "Sell " + selectedAsset.getSymbol();
                                        notifContent = "Strong reversal above resistance detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    }
                                }
                            }
                            else if (prevClose - prevOpen < 0 && currentPrice - prevClose < 0 && currentMoment < prevMoment) {
                                for (Double supportLevel : supportLevels) {
                                    if (currentPrice < supportLevel) {
                                        notifTitle = "Sell " + selectedAsset.getSymbol();
                                        notifContent = "Accelerating negative momentum below support detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    }
                                }
                                if (prevClose > priceCeiling) {
                                    notifTitle = "Sell " + selectedAsset.getSymbol();
                                    notifContent = "Accelerating negative momentum above ceiling detected";
                                    notificationManager.notify(notifId, builder.build());
                                    notifId++;
                                }
                            }
                        }
                        m++;
                        if (m > 360) {
                            updateCandles();
                            m = 1;
                        }
                        break;
                    default:
                        Asset activeAsset = ActivePosition.activeToken;
                        double currentPrice = activeAsset.getCurrentPrice();
                        double stoploss = activeAsset.getStoploss();
                        double currentVolume = activeAsset.getCurrentVolume();
                        double prevClose = activeAsset.getCandles(2, 1);
                        double currentMoment = ((currentPrice - prevClose) / n) * currentVolume;
                        double priceFloor = activeAsset.getPriceFloor();
                        double priceCeiling = activeAsset.getPriceCeiling();
                        Double[] supportLevels = activeAsset.getSupportLevels();
                        Double[] resistanceLevels = activeAsset.getResistanceLevels();
                        double initOpen = activeAsset.getCandles(0, 0);
                        double subOpen = activeAsset.getCandles(1, 0);
                        double prevOpen = activeAsset.getCandles(2, 0);
                        double prevVolume = activeAsset.getCandles(2, 2);
                        double prevMoment = ((prevClose - prevOpen) / 360) * prevVolume;
                        if ("EXIT_LONG".equals(method)) {
                            if (currentPrice < stoploss) {
                                notifTitle = "Sell " + activeAsset.getSymbol();
                                notifContent = activeAsset.getSymbol() + " has fallen below your stoploss level";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            } else if (currentPrice - prevClose < 0 && prevOpen - prevClose > 0 && prevOpen - subOpen > 0 && subOpen - initOpen > 0) {
                                notifTitle = "Sell " + activeAsset.getSymbol();
                                notifContent = "Top reversal detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            } else if (currentPrice - prevClose < 0 && prevOpen - prevClose > 0) {
                                for (Double resistanceLevel : resistanceLevels) {
                                    double posTolerance = resistanceLevel * 1.005;
                                    double negTolerance = resistanceLevel * .995;
                                    if (prevClose > negTolerance && prevClose < posTolerance) {
                                        notifTitle = "Sell " + activeAsset.getSymbol();
                                        notifContent = "Top reversal at resistance detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    } else if (prevClose > resistanceLevel && Math.abs(currentMoment) >= prevMoment) {
                                        notifTitle = "Sell " + activeAsset.getSymbol();
                                        notifContent = "Strong reversal above resistance detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    }
                                }
                            } else if (Math.abs(currentMoment) >= prevMoment && ((prevClose - priceFloor) / (priceCeiling - priceFloor)) > 0.8) {
                                notifTitle = "Sell " + activeAsset.getSymbol();
                                notifContent = "Overvalued correction detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            } else if (prevClose - prevOpen < 0 && currentPrice - prevClose < 0) {
                                notifTitle = "Sell " + activeAsset.getSymbol();
                                notifContent = "Sustained negative momentum detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            }
                        } else {
                            if (currentPrice > stoploss) {
                                notifTitle = "Buy " + activeAsset.getSymbol();
                                notifContent = activeAsset.getSymbol() + " has risen above your stoploss level";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            } else if (currentPrice - prevClose > 0 && prevOpen - prevClose < 0 && prevOpen - subOpen < 0 && subOpen - initOpen < 0) {
                                notifTitle = "Buy " + activeAsset.getSymbol();
                                notifContent = "Bottom reversal detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            } else if (currentPrice - prevClose > 0 && prevOpen - prevClose < 0) {
                                for (Double supportLevel : supportLevels) {
                                    double posTolerance = supportLevel * 1.005;
                                    double negTolerance = supportLevel * .995;
                                    if (prevClose > negTolerance && prevClose < posTolerance) {
                                        notifTitle = "Buy " + activeAsset.getSymbol();
                                        notifContent = "Bottom reversal at support detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    } else if (prevClose < supportLevel && Math.abs(currentMoment) >= prevMoment) {
                                        notifTitle = "Buy " + activeAsset.getSymbol();
                                        notifContent = "Strong reversal below support detected";
                                        notificationManager.notify(notifId, builder.build());
                                        notifId++;
                                        break;
                                    }
                                }
                            } else if (currentMoment >= Math.abs(prevMoment) && ((prevClose - priceFloor) / (priceCeiling - priceFloor)) < 0.2) {
                                notifTitle = "Buy " + activeAsset.getSymbol();
                                notifContent = "Undervalued correction detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            } else if (prevClose - prevOpen > 0 && currentPrice - prevClose > 0) {
                                notifTitle = "Buy " + activeAsset.getSymbol();
                                notifContent = "Sustained positive momentum detected";
                                notificationManager.notify(notifId, builder.build());
                                notifId++;
                            }
                        }
                }
            } catch (Exception e) {
                Message eMsg = errorHandler.obtainMessage();
                eMsg.what = 3;
                errorHandler.sendMessage(eMsg);
                e.printStackTrace();
            }
        }
    }
}
