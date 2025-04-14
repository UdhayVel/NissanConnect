package com.mappls.app.navigation.demo.car_service.ui;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

import com.mappls.app.navigation.demo.car_service.HelloService;

/**
 * A screen that shows a simple "Hello World!" message.
 *
 * <p>See {@link HelloService} for the app's entry point to the car host.
 */
public class HelloWorldScreen extends Screen {
    public HelloWorldScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Row row = new Row.Builder().setTitle("Hello AndroidX!").build();
        return new PaneTemplate.Builder(new Pane.Builder().addRow(row).build())
                .setHeaderAction(Action.APP_ICON)
                .build();
    }
}