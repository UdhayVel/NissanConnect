package com.mappls.app.navigation.demo.car.screens;

import static com.mappls.app.navigation.demo.utils.utils.checkUnit;
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
import androidx.car.app.model.Distance;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.Step;
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
    double distanceInKM = 0.0;
    double nextDist = 0.0;
    String eta = "";
    int remainingDurationInSec = 0;
    String nextStep = "";
    int iconId = 0;
    int whiteColor;

    protected CarNavigationScreen(@NonNull CarContext carContext, @NonNull CarMapRenderer carMapRenderer) {
        super(carContext);
        this.carContext = carContext;
        this.carMapRenderer = carMapRenderer;

        whiteColor = ContextCompat.getColor(carContext, R.color.white);

        CarMapRenderer.mapContainer.getLiveDataAdviseInfo().observe(this, adviseInfo -> {
            eta = adviseInfo.getEta();
            distanceInKM = convertIntoKM(adviseInfo.getLeftDistance());
            nextDist = adviseInfo.getDistanceToNextAdvise();
            remainingDurationInSec = adviseInfo.getLeftTime();
            nextStep = adviseInfo.getText();
            iconId = getDrawableResId((int) adviseInfo.getManeuverID(), carContext);

            Timber.tag("text").d(adviseInfo.getText() + "");
            Timber.tag("shortText").d(adviseInfo.getShortText() + "");
            Timber.tag("nextInsInfo").d(adviseInfo.getNextInstructionInfo() + "");
            Timber.tag("etaInSec").d(adviseInfo.getEtaInSecond() + "");
            Timber.tag("disFromRoute").d(adviseInfo.getDistanceFromRoute() + "");
            Timber.tag("info").d(adviseInfo.getInfo() + "");
            Timber.tag("leftTim").d(adviseInfo.getLeftTime() + "");
            Timber.tag("leftStep").d(adviseInfo.getLeftTimeStep() + "");
            Timber.tag("loc").d(adviseInfo.getLocation() + "");
            Timber.tag("nextIns").d(adviseInfo.getNextInstructionText() + "");
            Timber.tag("nextDis").d(adviseInfo.getDistanceToNextAdvise() + "");
            Timber.tag("leftDis").d(adviseInfo.getLeftDistance() + "");
            Timber.tag("navigationScreenAI").d(eta + " / " + distanceInKM + " / " + remainingDurationInSec + " / " + iconId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Timber.tag("convertSecsIntoHr::").d(Duration.ofSeconds(adviseInfo.getLeftTime()) +
                        "");
            }
            invalidate();
        });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.tag("ZonedTime").d("%s", ZonedDateTime.now().plusMinutes(remainingDurationInSec));
        }

        Maneuver currentManeuver = new Maneuver.Builder(Maneuver.TYPE_KEEP_LEFT)
                .setIcon(new CarIcon.Builder(IconCompat
                        .createWithResource(carContext, iconId == 0 ?
                                R.drawable.ic_cancel_black_24dp : iconId)).setTint(CarColor.createCustom(whiteColor, whiteColor)).build())  // optional
                .build();
//
//        Maneuver nextManeuver = new Maneuver.Builder(iconId == 0 ?
//                Maneuver.TYPE_UNKNOWN : iconId)
//                .build();

        Step currentStep = new Step.Builder(nextStep)
                .setManeuver(currentManeuver)
                .setRoad(nextStep == null ? "" : nextStep)
                .build();


        RoutingInfo.Builder routingInfo =
                new RoutingInfo.Builder().setCurrentStep(currentStep,
                        Distance.create(nextDist, Distance.UNIT_METERS));

        /// Travel Estimation Info
        TravelEstimate.Builder travelEstimate = null;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            travelEstimate = new TravelEstimate.Builder(
                    Distance.create(distanceInKM, checkUnit(distanceInKM) ? Distance.UNIT_METERS : Distance.UNIT_KILOMETERS),
                    ZonedDateTime.now().plusSeconds(remainingDurationInSec)
            );
//            travelEstimate.setTripText(new CarText.Builder(nextStep == null ? "" : nextStep).build());
            travelEstimate.setTripIcon(new CarIcon.Builder(IconCompat
                    .createWithResource(carContext, R.drawable.ic_cancel_black_24dp).setTint(whiteColor)).build());
            travelEstimate.setRemainingTime(Duration.ofSeconds(remainingDurationInSec));
            travelEstimate.setRemainingTimeColor(CarColor.GREEN);
            travelEstimate.setRemainingDistanceColor(CarColor.RED);
        }

        /// Navigation Template
        NavigationTemplate.Builder navigationTemplate = new NavigationTemplate.Builder();
        navigationTemplate.setPanModeListener(isInPanMode -> {

        });
        navigationTemplate.setActionStrip(buildActionStrip().build());
        navigationTemplate.setNavigationInfo(routingInfo.build());
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
