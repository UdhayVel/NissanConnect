package com.mappls.app.navigation.demo.car.screens;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.core.graphics.drawable.IconCompat;

import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.screens.interfaceclasses.SelectLocationCallBack;
import com.mappls.app.navigation.demo.car.screens.models.LocationList;
import com.mappls.sdk.maps.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationListScreen extends Screen {
    SelectLocationCallBack callback;
    protected LocationListScreen(@NonNull CarContext carContext, SelectLocationCallBack callBack) {
        super(carContext);

        this.callback = callBack;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        List<LocationList> locationLists = new ArrayList<>();
        locationLists.add(new LocationList("Renault Nissan", "Dist. 33.4 km", new LatLng(12.737430948595877, 80.00507178028703)));
        locationLists.add(new LocationList("Infosys", "Dist. 34.4 km", new LatLng(12.733380697239431, 80.0092495398074)));

        ItemList.Builder itemListBuilder = new ItemList.Builder();

        for (LocationList station : locationLists) {
            Row row = new Row.Builder()
                    .setTitle(station.getName())
                    .addText(station.getDistance())
                    .setImage(new CarIcon.Builder(
                            IconCompat.createWithResource(getCarContext(), R.drawable.ic_location_on_black_24dp))
                            .build(), Row.IMAGE_TYPE_ICON)
                    .setOnClickListener(() -> {
                        callback.setSelectedLocation(station);
                        CarContextUtils.screenManager(getCarContext()).pop();
                    })
                    .build();

            itemListBuilder.addItem(row);
        }

        return new ListTemplate.Builder().setSingleList(itemListBuilder.build()).setHeaderAction(Action.BACK) .build();
    }
}

