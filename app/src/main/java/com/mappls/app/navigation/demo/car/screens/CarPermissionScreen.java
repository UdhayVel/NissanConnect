package com.mappls.app.navigation.demo.car.screens;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;

import java.util.List;
import java.util.concurrent.Executor;

public class CarPermissionScreen extends Screen {

    public static final String LOG_TAG = "CarPermissionScreen";

    public CarPermissionScreen(CarContext carContext) {
        super(carContext);
    }

    @Override
    public Template onGetTemplate() {
        Log.v(LOG_TAG, "CarPermissionScreen.onGetTemplate");
        String message = "No Permission added";
        MessageTemplate.Builder templateBuilder = new MessageTemplate.Builder(message);

        templateBuilder.setTitle(getCarContext().getString(R.string.app_name))
                .setIcon(new CarIcon.Builder(
                        IconCompat.createWithResource(getCarContext(), R.drawable.ic_launcher_foreground)
                ).build());

        // The condition for AAOS was commented out in Kotlin, so we omit it here as well.
        templateBuilder.addAction(
                new Action.Builder()
                        .setBackgroundColor(CarColor.BLUE)
                        .setTitle("Open Location")
                        .setOnClickListener(ParkedOnlyOnClickListener.create(this::clickedFixPermissionAA))
                        .build()
        );

        templateBuilder.addAction(
                new Action.Builder()
                        .setBackgroundColor(CarColor.DEFAULT)
                        .setTitle("Close")
                        .setOnClickListener(() -> getCarContext().finishCarApp())
                        .build()
        );

        return templateBuilder.build();
    }

    private void clickedFixPermissionAA() {
        // If this function is called, you're standing still, Android Auto has already checked that.
        // Check if permission is already given
        if (ContextCompat.checkSelfPermission(
                getCarContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Not granted
            // Open screen on phone
            Intent intent = new Intent(getCarContext(), PhonePermissionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                getCarContext().startActivity(intent);
                CarToast.makeText(getCarContext(), "Phone open", CarToast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                CarToast.makeText(getCarContext(), "Phone open manual", CarToast.LENGTH_LONG).show();
            }
        } else {
            // Granted
            CarToast.makeText(getCarContext(), "Permission Granted", CarToast.LENGTH_LONG).show();
            // Close this screen
            CarContextUtils.screenManager(getCarContext()).popTo("ROOT");
        }
    }
}
