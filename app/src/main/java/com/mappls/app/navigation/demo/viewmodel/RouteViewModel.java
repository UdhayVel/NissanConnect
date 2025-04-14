package com.mappls.app.navigation.demo.viewmodel;


import androidx.lifecycle.ViewModel;

import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;

import java.util.List;

public class RouteViewModel extends ViewModel {

    private ELocation eLocation;
    private DirectionsResponse trip;
    private List<ELocation> eLocations;
    private String fromLocation;
    private int selectedIndex = 0;

    public ELocation geteLocation() {
        return eLocation;
    }

    public void seteLocation(ELocation eLocation) {
        this.eLocation = eLocation;
    }

    public DirectionsResponse getTrip() {
        return trip;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public void setTrip(DirectionsResponse trip) {
        this.trip = trip;
    }

    public List<ELocation> geteLocations() {
        return eLocations;
    }

    public void seteLocations(List<ELocation> eLocations) {
        this.eLocations = eLocations;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }
}
