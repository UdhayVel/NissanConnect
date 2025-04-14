package com.mappls.app.navigation.demo.car;

import static com.mappls.app.navigation.demo.car.surface.CarMapContainer.LOG_TAG;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.screens.CarMapScreen;
import com.mappls.app.navigation.demo.car.screens.CarPermissionScreen;
import com.mappls.app.navigation.demo.car.surface.CarMapRenderer;

public class CarMapSession extends Session {

    private CarMapRenderer carMapRenderer;
    private Configuration carConfiguration;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Screen onCreateScreen(Intent intent) {
        carMapRenderer = new CarMapRenderer(getCarContext(), getLifecycle());
        CarToast.makeText(getCarContext(), "car map rendered", CarToast.LENGTH_LONG).show();
        CarMapScreen carMapScreen = new CarMapScreen(getCarContext(), carMapRenderer);
        CarContextUtils.screenManager(getCarContext()).push(carMapScreen);

        if (ContextCompat.checkSelfPermission(getCarContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "onCreateScreen: Location permission not granted");
            CarPermissionScreen carPermissionScreen = new CarPermissionScreen(getCarContext());
            CarContextUtils.screenManager(getCarContext()).push(carPermissionScreen);
            return carPermissionScreen;
        } else {
            return carMapScreen;
        }
    }

    private boolean hasStarted() {
        Lifecycle.State state = getLifecycle().getCurrentState();
        return state == Lifecycle.State.STARTED || state == Lifecycle.State.RESUMED;
    }

    @Override
    public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
        super.onCarConfigurationChanged(newConfiguration);
        Log.v(LOG_TAG, "onCarConfigurationChanged: old: " + carConfiguration + ", new: " + newConfiguration);
        carConfiguration = newConfiguration;
    }
}
