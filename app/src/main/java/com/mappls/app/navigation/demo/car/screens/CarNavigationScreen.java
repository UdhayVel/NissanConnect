package com.mappls.app.navigation.demo.car.screens;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.surface.CarMapRenderer;

import java.time.ZonedDateTime;

public class CarNavigationScreen extends Screen {

    CarContext carContext;
    CarMapRenderer carMapRenderer;

    protected CarNavigationScreen(@NonNull CarContext carContext, @NonNull CarMapRenderer carMapRenderer) {
        super(carContext);
        this.carContext = carContext;
        this.carMapRenderer = carMapRenderer;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {

        /// Travel Estimation Info
        TravelEstimate travelEstimate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            travelEstimate = new TravelEstimate.Builder(
                    Distance.create(33.4, Distance.UNIT_KILOMETERS),
                    ZonedDateTime.now().plusMinutes(15)
            )
                    .setRemainingTimeColor(CarColor.GREEN)                    // Optional color
                    .build();
        }


        /// Navigation Template
        NavigationTemplate.Builder navigationTemplate = new NavigationTemplate.Builder();
        navigationTemplate.setPanModeListener(isInPanMode -> {

        });
        navigationTemplate.setActionStrip(buildActionStrip().build());
        if (travelEstimate != null) {
            navigationTemplate.setDestinationTravelEstimate(travelEstimate);
        }
        if (carContext.getCarAppApiLevel() >= 2) {
            navigationTemplate.setMapActionStrip(buildMapActionStrip(carMapRenderer).build());
        }


        return navigationTemplate.build();
    }


    private ActionStrip.Builder buildActionStrip() {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
        actionStripBuilder.addAction(new Action.Builder()
                .setIcon(new CarIcon.Builder(IconCompat
                        .createWithResource(carContext,
                                R.drawable.mappls_category_ic_baseline_arrow_back))
                        .build())
                .setOnClickListener(() -> {
            carMapRenderer.stopNavigationCars();
                    CarContextUtils.screenManager(carContext).pop();
        }).build());


        return actionStripBuilder;
    }


    private ActionStrip.Builder buildMapActionStrip(CarMapRenderer carMapRenderer) {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();

        actionStripBuilder.addAction(Action.PAN); // Needed to enable map interactivity! (pan and zoom gestures)


        // Zoom In Action
        Action.Builder currentLocationBuilder = new Action.Builder();
        currentLocationBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.mappls_nearby_cureent_location_icon)).build());
        currentLocationBuilder.setOnClickListener(carMapRenderer::getCurrentLocation);
        actionStripBuilder.addAction(currentLocationBuilder.build());

        // Zoom In Action
        Action.Builder zoomInActionBuilder = new Action.Builder();
        zoomInActionBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_add_black_24dp)).build());
        zoomInActionBuilder.setOnClickListener(carMapRenderer::zoomInFromButton);
        actionStripBuilder.addAction(zoomInActionBuilder.build());

        // Zoom Out Action
        Action.Builder zoomOutActionBuilder = new Action.Builder();
        zoomOutActionBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_minus)).build());
        zoomOutActionBuilder.setOnClickListener(carMapRenderer::zoomOutFromButton);
        actionStripBuilder.addAction(zoomOutActionBuilder.build());

        return actionStripBuilder;
    }

}
