package com.mappls.app.navigation.demo.car.screens;

import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapTemplate;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.screens.interfaceclasses.SelectLocationCallBack;
import com.mappls.app.navigation.demo.car.screens.models.LocationList;
import com.mappls.app.navigation.demo.car.screens.models.UiState;
import com.mappls.app.navigation.demo.car.surface.CarMapRenderer;
import com.mappls.sdk.maps.geometry.LatLng;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarMapScreen extends Screen implements SelectLocationCallBack {
    CarContext carContext;
    CarMapRenderer carMapRenderer;
    boolean showNavIcon = false;

    UiState uiState = new UiState(
            true,
            ""
    );

    public CarMapScreen(@NonNull CarContext carContext, CarMapRenderer carMapRenderer) {
        super(carContext);
        this.carContext = carContext;
        this.carMapRenderer = carMapRenderer;
        if (CarMapRenderer.mapContainer != null)
            CarMapRenderer.mapContainer.getCarsUIUpdate().observe(this, value -> {
                uiState.setLoading(false);
                invalidate();
            });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {

        /// Fav Lists of Locations
        List<LocationList> locationLists = new ArrayList<>();
        locationLists.add(new LocationList("Renault Nissan", "Dist. 33.4 km", new LatLng(12.737430948595877, 80.00507178028703)));
        locationLists.add(new LocationList("Infosys", "Dist. 34.4 km", new LatLng(12.733380697239431, 80.0092495398074)));

        /// Text Span for Lists
        Distance distance = Distance.create(33400, Distance.UNIT_METERS);
        SpannableString distanceText = new SpannableString("33.4 km away");
        distanceText.setSpan(DistanceSpan.create(distance), 0, distanceText.length(), 0);

        ItemList.Builder items = new ItemList.Builder();
        /// Item List Builder
        if (uiState.isLoading()) {
            Row row = new Row.Builder()
                    .setTitle("Loading...")
                    .addText("Please wait")
                    .build();

            items.addItem(row);
        } else {
            for (LocationList station : locationLists) {
                Row row = new Row.Builder()
                        .setTitle(station.getName())
                        .addText(distanceText)
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                            carMapRenderer.setSelectedLocation(station);
                            showNavIcon = true;
                            invalidate();
                        })
                        .addText(station.getDistance())
                        .build();

                items.addItem(row);
            }
        }

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
        navigationTemplate.setActionStrip(buildActionStrip().build());
        if (travelEstimate != null) {
            navigationTemplate.setDestinationTravelEstimate(travelEstimate);
        }
        if (carContext.getCarAppApiLevel() >= 2) {
            navigationTemplate.setMapActionStrip(buildMapActionStrip(carMapRenderer).build());
        }


        /// Map Template
        MapTemplate.Builder mapTemplate = new MapTemplate.Builder();
        mapTemplate.setActionStrip(buildActionStrip().build());
        mapTemplate.setHeader(new Header.Builder()
                .setTitle("Favourite Locations")
                .setStartHeaderAction(
                        new Action.Builder()
                                .setIcon(new CarIcon.Builder(
                                        IconCompat.createWithResource(carContext, R.drawable.ic_location_on_black_24dp))
                                        .setTint(CarColor.createCustom(Color.WHITE, 0))
                                        .build())
                                .build())
                .build());
        mapTemplate.setItemList(items.build());
        if (carContext.getCarAppApiLevel() >= 2) {
            mapTemplate.setMapController(new MapController.Builder()
                    .setMapActionStrip(buildMapActionStrip(carMapRenderer)
                            .build())
                    .build());
        }

        return mapTemplate.build();
    }

    private ActionStrip.Builder buildActionStrip() {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
        actionStripBuilder.addAction(new Action.Builder()
                .setIcon(
                        new CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_search_grey_24dp))
                                .build())
                .setOnClickListener(() -> {
                })
                .build());

        actionStripBuilder.addAction(new Action.Builder()
                .setTitle("Exit") //There can be only 1 with a title
                .setOnClickListener(() -> {
                    carContext.finishCarApp();
                }).build());
        return actionStripBuilder;
    }

    private ActionStrip.Builder buildMapActionStrip(CarMapRenderer carMapRenderer) {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();

        actionStripBuilder.addAction(Action.PAN); // Needed to enable map interactivity! (pan and zoom gestures)

        // Navigation In Action
        if (showNavIcon) {
            Action.Builder navigation = new Action.Builder();
            navigation.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_navigation)).build());
            navigation.setOnClickListener(() -> {
//            carMapRenderer.startNavigationCars();
            });
            actionStripBuilder.addAction(navigation.build());
        }

        // Zoom In Action
        Action.Builder currentLocationBuilder = new Action.Builder();
        currentLocationBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.mappls_nearby_cureent_location_icon)).build());
        currentLocationBuilder.setOnClickListener(carMapRenderer::getCurrentLocation);
        actionStripBuilder.addAction(currentLocationBuilder.build());

//        // Zoom In Action
//        Action.Builder zoomInActionBuilder = new Action.Builder();
//        zoomInActionBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_add_black_24dp)).build());
//        zoomInActionBuilder.setOnClickListener(carMapRenderer::zoomInFromButton);
//        actionStripBuilder.addAction(zoomInActionBuilder.build());
//
//        // Zoom Out Action
//        Action.Builder zoomOutActionBuilder = new Action.Builder();
//        zoomOutActionBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_minus)).build());
//        zoomOutActionBuilder.setOnClickListener(carMapRenderer::zoomOutFromButton);
//        actionStripBuilder.addAction(zoomOutActionBuilder.build());

        return actionStripBuilder;
    }

    @Override
    public void setSelectedLocation(LocationList location) {
        carMapRenderer.setSelectedLocation(location);
    }
}
