package com.example.chartwizard;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ServerTime {
    private String iso;
    private Double epoch;

    public Double getEpochTime() {
        return epoch;
    }

    public String getInterval() {
        String intervalStart, intervalEnd;
        Double factor = epoch * 1000;
        long epoch = factor.longValue();
        Calendar currentTime = Calendar.getInstance();
        currentTime.setTimeInMillis(epoch);
        DateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date date = currentTime.getTime();
        if (currentTime.get(Calendar.HOUR_OF_DAY) >= 2 && currentTime.get(Calendar.HOUR_OF_DAY) < 8) {
            String isoEnd = iso.format(date);
            currentTime.add(Calendar.DATE, -1);
            date = currentTime.getTime();
            String isoStart = iso.format(date);
            intervalStart = isoStart + "T08:00:00Z";
            intervalEnd = isoEnd + "T01:59:59Z";
        }
        else if (currentTime.get(Calendar.HOUR_OF_DAY) >= 8 && currentTime.get(Calendar.HOUR_OF_DAY) < 14) {
            String isoEnd = iso.format(date);
            currentTime.add(Calendar.DATE, -1);
            date = currentTime.getTime();
            String isoStart = iso.format(date);
            intervalStart = isoStart + "T14:00:00Z";
            intervalEnd = isoEnd + "T07:59:59Z";
        }
        else if (currentTime.get(Calendar.HOUR_OF_DAY) >= 14 && currentTime.get(Calendar.HOUR_OF_DAY) < 20) {
            String isoEnd = iso.format(date);
            currentTime.add(Calendar.DATE, -1);
            date = currentTime.getTime();
            String isoStart = iso.format(date);
            intervalStart = isoStart + "T20:00:00Z";
            intervalEnd = isoEnd + "T13:59:59Z";
        } else {
            String isoEnd = iso.format(date);
            intervalStart = isoEnd + "T02:00:00Z";
            intervalEnd = isoEnd + "T19:59:59Z";
        }
        return intervalStart + "," + intervalEnd;
    }
}
