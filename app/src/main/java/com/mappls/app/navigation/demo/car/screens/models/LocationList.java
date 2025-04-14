package com.mappls.app.navigation.demo.car.screens.models;

import androidx.annotation.NonNull;

import com.mappls.sdk.maps.geometry.LatLng;

public class LocationList{
    String name;
    LatLng latLng;
    public LocationList(@NonNull String name, @NonNull LatLng latLng){
        this.latLng = latLng;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}
