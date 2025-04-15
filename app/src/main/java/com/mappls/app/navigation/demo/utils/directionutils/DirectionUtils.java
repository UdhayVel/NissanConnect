package com.mappls.app.navigation.demo.utils.directionutils;

import android.location.Location;
import android.util.Log;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;

import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.R;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.Place;
import com.mappls.sdk.services.api.PlaceResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCode;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCodeManager;

import java.util.List;

import timber.log.Timber;

public class DirectionUtils {


    public static void getDirections(CarContext context, MapplsMap mapplsMap, ELocation eLocation, String fromLocation) {
        if (context == null)
            return;
        try {
            Location location = mapplsMap.getLocationComponent().getLastKnownLocation();

            if (location != null) {
                if (fromLocation != null) {
//                    openDirectionWidget(eLocation);
                } else {
                    CarToast.makeText(context, R.string.current_location_not_available, CarToast.LENGTH_SHORT).show();
                    getReverseGeoCode(context, new LatLng(location.getLatitude(), location.getLongitude()));
                }
            } else {
                CarToast.makeText(context, R.string.current_location_not_available, CarToast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    public static void getReverseGeoCode(CarContext carContext, LatLng latLng) {
        Log.e("LatLng:: ", latLng.getLatitude() + "");
        MapplsReverseGeoCode reverseGeoCode = MapplsReverseGeoCode.builder()
                .setLocation(latLng.getLatitude(), latLng.getLongitude()).build();
        MapplsReverseGeoCodeManager.newInstance(reverseGeoCode).call(new OnResponseCallback<PlaceResponse>() {
            @Override
            public void onSuccess(PlaceResponse placeResponse) {
                if (placeResponse != null) {
                    List<Place> placesList = placeResponse.getPlaces();
                    Place place = placesList.get(0);

                    ELocation eLocation = new ELocation();
                    eLocation.entryLongitude = latLng.getLongitude();
                    eLocation.longitude = latLng.getLongitude();
                    eLocation.entryLatitude = latLng.getLatitude();
                    eLocation.latitude = latLng.getLatitude();
                    eLocation.placeName = place.getFormattedAddress();

                    eLocation.placeAddress = carContext.getString(R.string.point_on_map);
                    Log.e("LatLng onSuccess:: ", eLocation.entryLatitude + "");
                    NavApplication app = ((NavApplication) carContext.getApplicationContext());
                    app.setELocation(eLocation);
                    Log.e("ELocation:: ", app.getELocation().entryLatitude + "");
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e("LatLng OnError:: ", s + "");
                CarToast.makeText(carContext, s, CarToast.LENGTH_LONG).show();
            }
        });
    }
}
