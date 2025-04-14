package com.mappls.app.navigation.demo.car.screens;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.screens.interfaceclasses.SelectLocationCallBack;
import com.mappls.app.navigation.demo.car.screens.models.LocationList;
import com.mappls.app.navigation.demo.car.surface.CarMapRenderer;

public class CarMapScreen extends Screen implements SelectLocationCallBack {
    CarContext carContext;
    CarMapRenderer carMapRenderer;

    public CarMapScreen(@NonNull CarContext carContext, CarMapRenderer carMapRenderer) {
        super(carContext);
        this.carContext = carContext;
        this.carMapRenderer = carMapRenderer;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        NavigationTemplate.Builder navigationTemplate = new NavigationTemplate.Builder();
        navigationTemplate.setActionStrip(buildActionStrip().build());
        if (carContext.getCarAppApiLevel() >= 2) {
            navigationTemplate.setMapActionStrip(buildMapActionStrip(carMapRenderer).build());
        }
        return navigationTemplate.build();
    }

    private ActionStrip.Builder buildActionStrip() {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
        actionStripBuilder.addAction(new Action.Builder().setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_search_grey_24dp)).build()).setOnClickListener(() -> {

        }).build());
        actionStripBuilder.addAction(new Action.Builder()
                //setTitle("Menu") //There can be only 1 with a title
                .setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_location_on_black_24dp)).build()).setOnClickListener(() -> {
                    CarContextUtils.screenManager(carContext).push(new LocationListScreen(carContext, this));
                }).build());
        return actionStripBuilder;
    }

    private ActionStrip.Builder buildMapActionStrip(CarMapRenderer carMapRenderer) {
        ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();

        actionStripBuilder.addAction(Action.PAN); // Needed to enable map interactivity! (pan and zoom gestures)

        // Zoom In Action
        Action.Builder zoomInActionBuilder = new Action.Builder();
        zoomInActionBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_add_black_24dp)).build());
        zoomInActionBuilder.setOnClickListener(carMapRenderer::zoomInFromButton);
        actionStripBuilder.addAction(zoomInActionBuilder.build());

        // Zoom Out Action
        Action.Builder zoomOutActionBuilder = new Action.Builder();
        zoomOutActionBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_arrow_back_black_24dp)).build());
        zoomOutActionBuilder.setOnClickListener(carMapRenderer::zoomOutFromButton);
        actionStripBuilder.addAction(zoomOutActionBuilder.build());

        return actionStripBuilder;
    }

    @Override
    public void setSelectedLocation(LocationList location) {
        carMapRenderer.setSelectedLocation(location);
    }
}
