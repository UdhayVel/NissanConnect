package com.mappls.app.navigation.demo.car.screens.models;

import androidx.annotation.NonNull;

import com.mappls.sdk.maps.geometry.LatLng;

public class LocationList{
    String name;
    String distance;
    LatLng latLng;
    public LocationList(@NonNull String name, @NonNull String distance, @NonNull LatLng latLng){
        this.latLng = latLng;
        this.distance = distance;
        this.name = name;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
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
