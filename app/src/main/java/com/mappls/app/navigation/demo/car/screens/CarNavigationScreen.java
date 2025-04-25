package com.mappls.app.navigation.demo.car.screens;

import static com.mappls.app.navigation.demo.utils.utils.convertIntoHrs;
import static com.mappls.app.navigation.demo.utils.utils.convertIntoKM;
import static com.mappls.app.navigation.demo.utils.utils.getDrawableResId;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Distance;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.surface.CarMapRenderer;

import java.time.Duration;
import java.time.ZonedDateTime;

import timber.log.Timber;

public class CarNavigationScreen extends Screen {

    CarContext carContext;
    CarMapRenderer carMapRenderer;
    double distance = 0.0;
    String eta = "";
    int remainingDuration = 0;
    String nextStep = "";
    int iconId = 0;

    protected CarNavigationScreen(@NonNull CarContext carContext, @NonNull CarMapRenderer carMapRenderer) {
        super(carContext);
        this.carContext = carContext;
        this.carMapRenderer = carMapRenderer;

        CarMapRenderer.mapContainer.getLiveDataAdviseInfo().observe(this, adviseInfo -> {
            eta = adviseInfo.getEta();
            distance = convertIntoKM(adviseInfo.getLeftDistance());
            remainingDuration = convertIntoHrs(adviseInfo.getLeftTime());
            nextStep = adviseInfo.getNextInstructionText();
            iconId = getDrawableResId((int) adviseInfo.getManeuverID(), carContext);

            Timber.tag("etaInSec").d(adviseInfo.getEtaInSecond() + "");
            Timber.tag("disFromRoute").d(adviseInfo.getDistanceFromRoute() + "");
            Timber.tag("info").d(adviseInfo.getInfo() + "");
            Timber.tag("leftTim").d(adviseInfo.getLeftTime() + "");
            Timber.tag("leftStep").d(adviseInfo.getLeftTimeStep() + "");
            Timber.tag("loc").d(adviseInfo.getLocation() + "");
            Timber.tag("nextIns").d(adviseInfo.getNextInstructionText() + "");
            Timber.tag("nextDis").d(adviseInfo.getDistanceToNextAdvise() + "");
            Timber.tag("leftDis").d(adviseInfo.getLeftDistance() + "");
            Timber.tag("navigationScreenAI").d(eta + " / " + distance + " / " + remainingDuration + " / " + iconId);
            invalidate();
        });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.tag("ZonedTime").d("%s", ZonedDateTime.now().plusMinutes(remainingDuration));
        }

        /// Travel Estimation Info
        TravelEstimate.Builder travelEstimate = null;
        int whiteColor = ContextCompat.getColor(carContext, R.color.white);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            travelEstimate = new TravelEstimate.Builder(
                    Distance.create(distance, Distance.UNIT_KILOMETERS),
                    ZonedDateTime.now().plusMinutes(remainingDuration)
            );
            travelEstimate.setTripText(new CarText.Builder(nextStep).build());
                travelEstimate.setTripIcon(new CarIcon.Builder(IconCompat
                        .createWithResource(carContext, iconId == 0?
                                R.drawable.ic_cancel_black_24dp : iconId)
                        .setTint(whiteColor)).build());
            travelEstimate.setRemainingTime(Duration.ofHours(remainingDuration));
            travelEstimate.setRemainingTimeColor(CarColor.GREEN);                    // Optional
        }

        /// Navigation Template
        NavigationTemplate.Builder navigationTemplate = new NavigationTemplate.Builder();
        navigationTemplate.setPanModeListener(isInPanMode -> {

        });
        navigationTemplate.setActionStrip(buildActionStrip().build());
        if (travelEstimate != null) {
            navigationTemplate.setDestinationTravelEstimate(travelEstimate.build());
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
        actionStripBuilder.addAction(new Action.Builder()
                .setTitle("Share")
                .setOnClickListener(carMapRenderer::shareLocation).build());


        return actionStripBuilder;
    }


    private ActionStrip.Builder buildMapActionStrip(CarMapRenderer carMapRenderer) {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();

        actionStripBuilder.addAction(Action.PAN); // Needed to enable map interactivity! (pan and zoom gestures)


        // Zoom In Action
        Action.Builder currentLocationBuilder = new Action.Builder();
        currentLocationBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.mappls_nearby_cureent_location_icon)).build());
        currentLocationBuilder.setOnClickListener(carMapRenderer::recenter);
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
