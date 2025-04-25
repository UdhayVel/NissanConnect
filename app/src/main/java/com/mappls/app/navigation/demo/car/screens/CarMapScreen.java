package com.mappls.app.navigation.demo.car.screens;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.screens.interfaceclasses.SelectLocationCallBack;
import com.mappls.app.navigation.demo.car.screens.models.LocationList;
import com.mappls.app.navigation.demo.car.screens.models.SelectedLocation;
import com.mappls.app.navigation.demo.car.screens.models.UiState;
import com.mappls.app.navigation.demo.car.surface.CarMapRenderer;
import com.mappls.sdk.maps.geometry.LatLng;

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
    SelectedLocation selectedLocation = new SelectedLocation();
    private boolean showListView = true;

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

    @OptIn(markerClass = ExperimentalCarApi.class)
    @NonNull
    @Override
    public Template onGetTemplate() {

        /// Fav Lists of Locations
        List<LocationList> locationLists = new ArrayList<>();
        locationLists.add(new LocationList("Mahindra Research Valley", "Dist. 3.4 km",
                new LatLng(12.719272539838009, 80.01369888214302)));
        locationLists.add(new LocationList("Renault Nissan", "Dist. 1 km",
                new LatLng(12.73747702155609, 80.00497228905022)));

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
//                        .addText(distanceText)
                        .setBrowsable(true)
                        .setOnClickListener(() -> {

                            selectedLocation.setName(station.getName());
                            selectedLocation.setDescription(station.getDistance());
                            selectedLocation.setLatLng(station.getLatLng());

                            carMapRenderer.setSelectedLocation(station);

                            showNavIcon = true;
                            showListView = false;
                            invalidate();
                        })
                        .addText(station.getDistance())
                        .build();

                items.addItem(row);
            }
        }

        ListTemplate favListTemplate =
                new ListTemplate.Builder().setHeaderAction(new Action.Builder().setIcon(
                                new CarIcon.Builder(IconCompat.createWithResource(carContext,
                                        R.mipmap.ic_launcher)).build()).build())
                        .setSingleList(items.build()).build();

        MessageTemplate navigationMessageTemplate =
                new MessageTemplate.Builder(selectedLocation.getName()+"\n"+ selectedLocation.getDescription())
                        .setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext,
                                R.drawable.mappls_nearby_location_marker)).build())
                .addAction(
                        new Action.Builder()
                                .setTitle("Navigate")
                                .setBackgroundColor(CarColor.YELLOW)
                                .setOnClickListener(() -> {
                                    carMapRenderer.startNavigationCars();

                                    CarNavigationScreen carNavigationScreen =
                                            new CarNavigationScreen(getCarContext(), carMapRenderer);
                                    CarContextUtils.screenManager(carContext).push(carNavigationScreen);
                                })
                                .build()
                )
                .addAction(
                        new Action.Builder()
                                .setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext,
                                        R.drawable.mappls_category_ic_baseline_arrow_back)).build())
                                .setOnClickListener(() -> {
                                    showListView = true;
                                    invalidate();
                                })
                                .build()
                )// optional icon
                .build();


        /// Map Template
        MapWithContentTemplate.Builder mapTemplate = new MapWithContentTemplate.Builder();
        mapTemplate.setActionStrip(buildActionStrip().build());
        mapTemplate.setContentTemplate(showListView ? favListTemplate : navigationMessageTemplate);
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
        int drawable = R.drawable.ic_logout;

        Action.Builder navigation = new Action.Builder();
        navigation.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, drawable)).build());
        navigation.setOnClickListener(() -> {
            carContext.finishCarApp();
        });
        actionStripBuilder.addAction(navigation.build());


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

    @Override
    public void setSelectedLocation(LocationList location) {
        carMapRenderer.setSelectedLocation(location);
    }
}
