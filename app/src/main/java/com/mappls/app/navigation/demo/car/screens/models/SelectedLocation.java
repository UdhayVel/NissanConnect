package com.mappls.app.navigation.demo.car.screens.models;

import com.mappls.sdk.maps.geometry.LatLng;

public class SelectedLocation {

    String name;
    String description;
    LatLng latLng;

    public SelectedLocation() {
        this.name = "";
        this.description = "";
        this.latLng = new LatLng();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}
