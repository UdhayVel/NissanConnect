package com.mappls.app.navigation.demo.car.surface;

import android.Manifest;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.car.app.CarContext;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.mappls.app.navigation.demo.HomeActivity;
import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.ThreadUtils;
import com.mappls.app.navigation.demo.fragment.HomeFragment;
import com.mappls.sdk.direction.ui.DirectionFragment;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.Mappls;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.MapplsMapOptions;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.constants.MapplsConstants;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.location.LocationComponent;
import com.mappls.sdk.maps.location.LocationComponentActivationOptions;
import com.mappls.sdk.maps.location.LocationComponentOptions;
import com.mappls.sdk.maps.location.engine.LocationEngine;
import com.mappls.sdk.maps.location.engine.LocationEngineCallback;
import com.mappls.sdk.maps.location.engine.LocationEngineRequest;
import com.mappls.sdk.maps.location.engine.LocationEngineResult;
import com.mappls.sdk.maps.location.modes.CameraMode;
import com.mappls.sdk.maps.location.modes.RenderMode;
import com.mappls.sdk.maps.location.permissions.PermissionsManager;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.Place;
import com.mappls.sdk.services.api.PlaceResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCode;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCodeManager;

import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class CarMapContainer implements DefaultLifecycleObserver, LocationEngineCallback<LocationEngineResult> {

    private CarContext carContext;
    public static MapView mapViewInstance;
    public static MapplsMap mapplsMap;
    public static Integer surfaceWidth;
    public static Integer surfaceHeight;
    private Animator scaleAnimator;

    private LocationEngine locationEngine;

    private NavApplication app;

    public static final String LOG_TAG = "CarMapContainer";
    public static final float DOUBLE_CLICK_FACTOR = 2.0F;

    public CarMapContainer(CarContext carContext, Lifecycle lifecycle) {
        this.carContext = carContext;
        lifecycle.addObserver(this);
    }

    public NavApplication getMyApplication() {
        return ((NavApplication) carContext.getApplicationContext());
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Mappls.getInstance(carContext);

        try {
            app = getMyApplication();
        } catch (Exception e) {
            //ignore
        }


        ThreadUtils.runOnMainThread(() -> {
            mapViewInstance = createMapViewInstance();
            CarContextUtils.windowManager(carContext).addView(mapViewInstance, getWindowManagerLayoutParams());
            mapViewInstance.onStart();

            mapViewInstance.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull MapplsMap map) {
                    mapplsMap = map;

                    map.setStyle(new Style.Builder().fromJson("https://demotiles.maplibre.org/style.json"));

                    map.getStyle(style -> {
                        try {
                            enableLocationComponent(style);
                            mapplsMap.enableTraffic(true);
                            mapplsMap.setMaxZoomPreference(22);
                            mapplsMap.setMinZoomPreference(4);

                            setCompassDrawable();
                        } catch (SecurityException e) {
                            Log.e(LOG_TAG, "Location permission not granted", e);
                        }
                    });
                }

                @Override
                public void onMapError(int errorCode, @Nullable String errorMessage) {
                    Log.e(LOG_TAG, "Map error: " + errorMessage);
                }
            });
        });
    }


    public void getReverseGeoCode(LatLng latLng) {
        MapplsReverseGeoCode reverseGeoCode = MapplsReverseGeoCode.builder()
                .setLocation(latLng.getLatitude(), latLng.getLongitude())
                .build();
        MapplsReverseGeoCodeManager.newInstance(reverseGeoCode).call(new OnResponseCallback<PlaceResponse>() {
            @Override
            public void onSuccess(PlaceResponse placeResponse) {
                if(placeResponse != null) {
                    List<Place> placesList = placeResponse.getPlaces();
                    Place place = placesList.get(0);

                    ELocation eLocation = new ELocation();
                    eLocation.entryLongitude = latLng.getLongitude();
                    eLocation.longitude = latLng.getLongitude();
                    eLocation.entryLatitude = latLng.getLatitude();
                    eLocation.latitude = latLng.getLatitude();
                    eLocation.placeName = place.getFormattedAddress();

//                    if(mapplsMap != null) {
//                        mapplsMap.addMarker(new MarkerOptions().title(eLocation.placeName).position(new LatLng(place.getLat(), place.getLng())));
//                    }

                    eLocation.placeAddress = carContext.getString(R.string.point_on_map);
                    app.setELocation(eLocation);
                }
            }

            @Override
            public void onError(int i, String s) {
            }
        });
    }


    public void setCompassDrawable() {
        mapViewInstance.getCompassView().setBackgroundResource(R.drawable.compass_background);
        assert mapplsMap.getUiSettings() != null;
        mapplsMap.getUiSettings().setCompassImage(Objects.requireNonNull(ContextCompat.getDrawable(carContext, R.drawable.compass_north_up)));
        int padding = dpToPx(carContext, 8);
        int elevation = dpToPx(carContext,8);
        mapViewInstance.getCompassView().setPadding(padding, padding, padding, padding);
        ViewCompat.setElevation(mapViewInstance.getCompassView(), elevation);
        mapplsMap.getUiSettings().setCompassMargins(dpToPx(carContext,20), dpToPx(carContext,100), dpToPx(carContext,20), dpToPx(carContext,20));
    }

    public static int dpToPx(CarContext carContext, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                carContext.getResources().getDisplayMetrics()
        );
    }


    public void scrollBy(Float x, Float y) {
        mapplsMap.scrollBy(-x, -y);
    }

    private void doubleClickZoomWithAnimation(PointF zoomFocalPoint, boolean isZoomIn) {
        cancelCurrentAnimator(scaleAnimator);
        int currentZoom = mapplsMap != null ? mapplsMap.getPrefetchZoomDelta() : 0;

        if (currentZoom != 0) {
            scaleAnimator = createScaleAnimator(
                    currentZoom,
                    isZoomIn ? 1.0 : -1.0,
                    zoomFocalPoint
            );
            scaleAnimator.start();
        }
    }

    private Animator createScaleAnimator(
            int currentZoom,
            double zoomAddition,
            PointF animationFocalPoint
    ) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) currentZoom, (float) (currentZoom + zoomAddition));
        animator.setDuration(MapplsConstants.ANIMATION_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            if (animationFocalPoint != null && mapplsMap != null) {
                float animatedValue = (Float) animation.getAnimatedValue();
//                mapplsMap.setMaxZoomPreference(animatedValue);
            }
        });

        return animator;
    }


    private void cancelCurrentAnimator(Animator animator) {
        if (animator != null && animator.isStarted()) {
            animator.cancel();
        }
    }

    public void onScale(float focusX, float focusY, float scaleFactor) {
        if (scaleFactor == DOUBLE_CLICK_FACTOR) {
            doubleClickZoomWithAnimation(new PointF(focusX, focusY), true);
            return;
        }
        if (scaleFactor == -DOUBLE_CLICK_FACTOR) {
            doubleClickZoomWithAnimation(new PointF(focusX, focusY), false);
            return;
        }

        if (mapplsMap != null) {
            int currentZoomLevel = mapplsMap.getPrefetchZoomDelta();
            if (currentZoomLevel != 0) {
                double zoomAdditional = (Math.log(scaleFactor) / Math.log(Math.PI / 2)) * MapplsConstants.ZOOM_RATE;
                mapplsMap.setPrefetchZoomDelta((int) (currentZoomLevel + zoomAdditional));
            }
        }
    }


    private MapView createMapViewInstance() {
        MapplsMapOptions options = MapplsMapOptions.createFromAttributes(carContext);
        options.textureMode(true);

        MapView mapView = new MapView(carContext, options);
        mapView.setLayerType(View.LAYER_TYPE_HARDWARE, new Paint());
        return mapView;
    }

    private WindowManager.LayoutParams getWindowManagerLayoutParams() {
        return new WindowManager.LayoutParams(
                surfaceWidth != null ? surfaceWidth : WindowManager.LayoutParams.MATCH_PARENT,
                surfaceHeight != null ? surfaceHeight : WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.RGBX_8888
        );
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void enableLocationComponent(@NonNull Style style) {
        if (PermissionsManager.areLocationPermissionsGranted(carContext)) {
            LocationComponentOptions options = LocationComponentOptions.builder(carContext)
                    .trackingGesturesManagement(true)
                    .accuracyAlpha(0f)
                    .accuracyColor(ContextCompat.getColor(carContext, R.color.accuracy_green))
                    .build();

            LocationComponent locationComponent = mapplsMap.getLocationComponent();

            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(carContext, style)
                    .locationComponentOptions(options)
                    .build();

            locationComponent.activateLocationComponent(activationOptions);
            locationComponent.setLocationComponentEnabled(true);

            locationEngine = locationComponent.getLocationEngine();
            LocationEngineRequest request = new LocationEngineRequest.Builder(1000)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(100)
                    .build();

            if (locationEngine != null) {
                locationEngine.requestLocationUpdates(request, this, Looper.getMainLooper());
            }

            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    @Override
    public void onSuccess(@Nullable LocationEngineResult locationEngineResult) {
        Log.e("locationn::", String.valueOf(locationEngineResult));
        if(locationEngineResult != null) {
            Location location = locationEngineResult.getLastLocation();
            Timber.i("onLocationChanged");
            try {
                if (location == null || location.getLatitude() <= 0)
                    return;

                mapplsMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16), 500);

                getReverseGeoCode(new LatLng(location.getLatitude(), location.getLongitude()));

                NavigationContext.getNavigationContext().setCurrentLocation(location);
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
        Log.e("locationnException::", exception.toString());
    }
    public void setSurfaceSize(int surfaceWidth, int surfaceHeight) {
        Log.v(LOG_TAG, "setSurfaceSize: " + surfaceWidth + ", " + surfaceHeight);

        if (!Integer.valueOf(surfaceWidth).equals(this.surfaceWidth) ||
                !Integer.valueOf(surfaceHeight).equals(this.surfaceHeight)) {

            this.surfaceWidth = surfaceWidth;
            this.surfaceHeight = surfaceHeight;

            if (mapViewInstance != null) {
                CarContextUtils.windowManager(carContext).updateViewLayout(
                        mapViewInstance,
                        getWindowManagerLayoutParams()
                );
            }
        }
    }
}
