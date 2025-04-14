package com.mappls.app.navigation.demo.car.extensions;


import android.view.WindowManager;

import androidx.car.app.CarContext;
import androidx.car.app.ScreenManager;


public class CarContextUtils {
    public static ScreenManager screenManager(CarContext carContext) {
        return (ScreenManager) carContext.getCarService(CarContext.SCREEN_SERVICE);
    }

    public static WindowManager windowManager(CarContext carContext) {
        return (WindowManager) carContext.getSystemService(CarContext.WINDOW_SERVICE) ;
    }

}