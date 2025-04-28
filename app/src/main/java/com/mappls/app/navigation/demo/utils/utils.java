package com.mappls.app.navigation.demo.utils;

import androidx.car.app.CarContext;

public class utils {

    public static double convertIntoKM(double meters) {
        double value = 0.0;

        if (meters != 0.0) {
            value = meters / 1000.0;
        }

        return value;
    }

    public static int convertIntoHrs(int mins) {
        int value = 0;

        if (mins != 0) {
            value = mins / 60;
        }

        return value;
    }

    public static String convertSecsIntoHrMinSec(int seconds) {
        int hr = 0;
        int mins = 0;
        int sec = 0;

        if (seconds != 0) {
            hr = seconds / 3600;

            int remainingHrs = seconds % 3600;
            mins = remainingHrs / 60;

            int remainingMins = remainingHrs % 60;
            sec = remainingMins;
        }

        return hr + "hr " + mins + "min " + sec +"sec";
    }

    public static int getDrawableResId(int maneuverId, CarContext carContext) {
        return carContext.getResources().getIdentifier("ic_step_" + maneuverId, "drawable", carContext.getPackageName());
    }
}
