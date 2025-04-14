package com.mappls.app.navigation.demo;

import android.app.Application;
import android.content.res.Configuration;
import android.location.Location;

import androidx.annotation.NonNull;

import com.mappls.sdk.maps.Mappls;
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.navigation.NavLocation;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.services.account.MapplsAccountManager;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;

import timber.log.Timber;

public class NavApplication extends Application {


    ELocation eLocation = null;
    private NavLocation startNavigationLocation;
    private DirectionsResponse trip;

    @Override
    public void onCreate() {
        super.onCreate();
        NavigationContext.init(this);
        MapplsNavigationHelper.getInstance().init(this);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        MapplsAccountManager.getInstance().setRestAPIKey(getRestAPIKey());
        MapplsAccountManager.getInstance().setMapSDKKey(getMapSDKKey());
        MapplsAccountManager.getInstance().setAtlasClientId(getAtlasClientId());
        MapplsAccountManager.getInstance().setAtlasClientSecret(getAtlasClientSecret());

        MapplsNavigationHelper.getInstance().setNavigationActivityClass(NavigationActivity.class);
        MapplsNavigationHelper.getInstance().setJunctionViewEnabled(true);
        MapplsNavigationHelper.getInstance().setNavigationEventEnabled(true);

        Mappls.getInstance(this);

    }
    public String getAtlasClientId() {
        return "33OkryzDZsI8pxTHaVZjctjoxiLfbo76iA79fAX8V4wDX_6z0ypgxXtHgNrrfXgPeGZvs7rCmVk2mR59iNnNEg==";
    }

    public String getAtlasClientSecret() {
        return "lrFxI-iSEg_HUhtRITJXmiuhQ-_fgPFv85chr9a0nNlPzNplmtoBYulTR_SfmQEXeS1JuF0wEBO2arwuyxSKBB2v7PWUUQ7Z";
    }

    public String getMapSDKKey() {
        return "a960c854e4dbb51c06e994a9181d571e";
    }

    public String getRestAPIKey() {
        return "a960c854e4dbb51c06e994a9181d571e";
    }


    public ELocation getELocation() {
        return eLocation;
    }

    public void setELocation(ELocation eLocation) {
        this.eLocation = eLocation;
    }


    public NavLocation getStartNavigationLocation() {
        return startNavigationLocation;
    }

    public void setStartNavigationLocation(NavLocation startNavigationLocation) {
        this.startNavigationLocation = startNavigationLocation;
    }

    public DirectionsResponse getTrip() {
        return trip;
    }

    public void setTrip(DirectionsResponse trip) {
        this.trip = trip;
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        NavigationContext.terminate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        NavigationContext.getNavigationContext().onConfigurationChanged(newConfig);
    }
}
