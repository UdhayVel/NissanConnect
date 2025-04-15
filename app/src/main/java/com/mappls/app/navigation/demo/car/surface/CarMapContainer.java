package com.mappls.app.navigation.demo.car.surface;

import android.Manifest;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.extensions.ThreadUtils;
import com.mappls.app.navigation.demo.car.utils.PolylineDecoder;
import com.mappls.app.navigation.demo.maps.plugins.BearingIconPlugin;
import com.mappls.app.navigation.demo.maps.plugins.DirectionPolylinePlugin;
import com.mappls.app.navigation.demo.maps.plugins.RouteArrowPlugin;
import com.mappls.app.navigation.demo.utils.NavigationLocationEngine;
import com.mappls.app.navigation.demo.utils.directionutils.DirectionUtils;
import com.mappls.sdk.geojson.LineString;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.Mappls;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.MapplsMapOptions;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.annotations.Marker;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.annotations.Polyline;
import com.mappls.sdk.maps.annotations.PolylineOptions;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.constants.MapplsConstants;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.geometry.LatLngBounds;
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
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.services.api.ApiResponse;
import com.mappls.sdk.services.api.directions.DirectionsCriteria;
import com.mappls.sdk.services.api.directions.MapplsDirectionManager;
import com.mappls.sdk.services.api.directions.MapplsDirections;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class CarMapContainer implements DefaultLifecycleObserver, LocationEngineCallback<LocationEngineResult>, MapplsMap.OnMapLongClickListener {

    private CarContext carContext;
    public static MapView mapViewInstance;
    public static MapplsMap mapplsMap;
    public static Marker secondaryMarker;
    public boolean firstFix = false;
    public static Integer surfaceWidth;
    public static Integer surfaceHeight;
    private Animator scaleAnimator;

    private LocationEngine locationEngine;

    private NavApplication app;
    private Polyline routePolyline;

    public static final String LOG_TAG = "CarMapContainer";
    public static final float DOUBLE_CLICK_FACTOR = 2.0F;

    public static DirectionPolylinePlugin directionPolylinePlugin;
    //    public static MapEventsPlugin mapEventsPlugin;
    public static RouteArrowPlugin routeArrowPlugin;
    public static BearingIconPlugin bearingIconPlugin;
    private LocationComponent locationPlugin;
    private LatLng currentLatLng;

    private NavigationLocationEngine navigationLocationEngine;


    private final MutableLiveData<Boolean> liveDataCarScreenUIObserver = new MutableLiveData<>();


    public CarMapContainer(CarContext carContext, Lifecycle lifecycle) {
        this.carContext = carContext;
        lifecycle.addObserver(this);
    }

    public NavApplication getMyApplication() {
        return ((NavApplication) carContext.getApplicationContext());
    }


    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);

        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(this);
        }
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Mappls.getInstance(carContext);
        navigationLocationEngine = new NavigationLocationEngine();

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
                    Log.e("Mapps", mapplsMap.toString());
                    map.getStyle(style -> {
                        try {
                            mapplsMap.enableTraffic(true);
                            directionPolylinePlugin = new DirectionPolylinePlugin(mapViewInstance, map);
                            enableLocationComponent(style);

                            bearingIconPlugin = new BearingIconPlugin(mapViewInstance, mapplsMap);
                            routeArrowPlugin = new RouteArrowPlugin(mapViewInstance, mapplsMap);
                            mapplsMap.setMaxZoomPreference(18.5);
                            mapplsMap.setMinZoomPreference(4);

                            mapplsMap.addOnMapLongClickListener(CarMapContainer.this);
                            setCompassDrawable();

                            setupCarUI(mapplsMap != null);

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

    private void setupCarUI(boolean isMapAvail) {
        liveDataCarScreenUIObserver.postValue(isMapAvail);
    }
    public LiveData<Boolean> getCarsUIUpdate() {
        return liveDataCarScreenUIObserver;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
    }


    public void setCompassDrawable() {
        mapViewInstance.getCompassView().setBackgroundResource(R.drawable.compass_background);
        assert mapplsMap.getUiSettings() != null;
        mapplsMap.getUiSettings()
                .setCompassImage(Objects.requireNonNull(ContextCompat.getDrawable(carContext, R.drawable.compass_north_up)));
        int padding = dpToPx(8);
        int elevation = dpToPx(8);
        mapViewInstance.getCompassView().setPadding(padding, padding, padding, padding);
        ViewCompat.setElevation(mapViewInstance.getCompassView(), elevation);
        mapplsMap.getUiSettings().setCompassMargins(dpToPx(20), dpToPx(100), dpToPx(20), dpToPx(20));
    }


    public int dpToPx(final float dp) {
        return (int) (dp * app.getResources().getDisplayMetrics().density);
    }


    public void scrollBy(Float x, Float y) {
        mapplsMap.scrollBy(-x, -y);
    }

    private void doubleClickZoomWithAnimation(PointF zoomFocalPoint, boolean isZoomIn) {
        cancelCurrentAnimator(scaleAnimator);
        int currentZoom = mapplsMap != null ? mapplsMap.getPrefetchZoomDelta() : 0;

        if (currentZoom != 0) {
            scaleAnimator = createScaleAnimator(currentZoom, isZoomIn ? 1.0 : -1.0, zoomFocalPoint);
            scaleAnimator.start();
        }
    }

    private Animator createScaleAnimator(int currentZoom, double zoomAddition, PointF animationFocalPoint) {
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
        return new WindowManager.LayoutParams(surfaceWidth != null ? surfaceWidth : WindowManager.LayoutParams.MATCH_PARENT, surfaceHeight != null ? surfaceHeight : WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, PixelFormat.RGBX_8888);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void enableLocationComponent(@NonNull Style style) {
        if (PermissionsManager.areLocationPermissionsGranted(carContext)) {
            LocationComponentOptions options = LocationComponentOptions.builder(carContext)
                    .trackingGesturesManagement(true)
                    .accuracyAlpha(0f)
                    .accuracyColor(ContextCompat.getColor(carContext, R.color.colorAccent)).build();

            locationPlugin = mapplsMap.getLocationComponent();

            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions
                    .builder(carContext, style)
                    .locationComponentOptions(options)
//                    .locationEngine(navigationLocationEngine)
                    .build();

            locationPlugin.activateLocationComponent(activationOptions);
            locationPlugin.setLocationComponentEnabled(true);

            locationEngine = locationPlugin.getLocationEngine();
            LocationEngineRequest request = new LocationEngineRequest.Builder(10 * 1000).setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY).setFastestInterval(100).build();

            if (locationEngine != null) {
                locationEngine.requestLocationUpdates(request, this, Looper.getMainLooper());
            }

            locationPlugin.setCameraMode(CameraMode.TRACKING);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
        }
    }

    @Override
    public void onSuccess(@Nullable LocationEngineResult locationEngineResult) {
        Log.e("locationn::", String.valueOf(locationEngineResult));
        if (locationEngineResult != null) {
            Location location = locationEngineResult.getLastLocation();
            Timber.i("onLocationChanged");
            try {
                if (location == null || location.getLatitude() <= 0) return;

                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (!firstFix) {
                    firstFix = true;
                    mapplsMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16), 500);
                }
//                if (routePolyline != null) {
//                    mapplsMap.removePolyline(routePolyline);
//                }
//                routePolyline = mapplsMap.addPolyline(new PolylineOptions().add(sourceLatLng).add(destLatLng).color(Color.parseColor("#3bb2d0")).width(2));

//                DirectionPoint originPoint = DirectionPoint.setDirection();
//                DirectionPoint destPoint̉ = DirectionPoint.setDirection();
//
//                DirectionOptions directionOptions = DirectionOptions.builder().alongRouteBuffer(200)
//                        .origin(originPoint)
//                        .destination(destPoint̉)
//                        .searchAlongRoute(true)
//                        .resource(DirectionsCriteria.RESOURCE_DISTANCE_TRAFFIC)
//                        .showAlternative(true)
//                        .profile(DirectionsCriteria.PROFILE_DRIVING)
//                        .overview(DirectionsCriteria.OVERVIEW_FULL).build();

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

        if (!Integer.valueOf(surfaceWidth).equals(this.surfaceWidth) || !Integer.valueOf(surfaceHeight).equals(this.surfaceHeight)) {

            this.surfaceWidth = surfaceWidth;
            this.surfaceHeight = surfaceHeight;

            if (mapViewInstance != null) {
                CarContextUtils.windowManager(carContext).updateViewLayout(mapViewInstance, getWindowManagerLayoutParams());
            }
        }
    }

    public void addMarker(LatLng latLng) {
//        LatLng selectedLocationLatLng = new LatLng(eLocation.latitude, eLocation.longitude);
        if(currentLatLng == null){
            return;
        }
        if (secondaryMarker != null) {
            mapplsMap.removeMarker(secondaryMarker);
        }
        secondaryMarker = mapplsMap.addMarker(new MarkerOptions().position(latLng));
        mapplsMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
        if (latLng.getLatitude() > 0) {
            DirectionUtils.getReverseGeoCode(carContext, latLng);
        }

        if (currentLatLng.getLatitude() > 0) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {

//                    List<String> annotations = new ArrayList<String>();
//                    annotations.add(DirectionsCriteria.ANNOTATION_CONGESTION);
//                    annotations.add(DirectionsCriteria.ANNOTATION_NODES);
//                    annotations.add(DirectionsCriteria.ANNOTATION_DURATION);
//                    annotations.add(DirectionsCriteria.ANNOTATION_BASE_DURATION);
//                    annotations.add(DirectionsCriteria.ANNOTATION_SPEED_LIMIT);
//                    DirectionOptions.Builder optionsBuilder = DirectionOptions.builder();
//                    if(eLocation.longitude != null && eLocation.latitude != null) {
//                        optionsBuilder.destination(DirectionPoint.setDirection(Point.fromLngLat(eLocation.longitude, eLocation.latitude), eLocation.placeName, eLocation.placeAddress));
//                    } else {
//                        optionsBuilder.destination(DirectionPoint.setDirection(eLocation.mapplsPin, eLocation.placeName, eLocation.placeAddress));
//                    }
//                    optionsBuilder.annotation(annotations);
//                    optionsBuilder.showAlternative(true);
//                    optionsBuilder.resource(DirectionsCriteria.RESOURCE_ROUTE_ETA)
//                            .steps(true);
//                    optionsBuilder.showDefaultMap(false);


                    MapplsDirections directions = MapplsDirections.builder()
                            .origin(Point.fromLngLat(currentLatLng.getLongitude(), currentLatLng.getLatitude()))
                            .steps(true)
                            .resource(DirectionsCriteria.RESOURCE_ROUTE_ETA)
                            .profile(DirectionsCriteria.PROFILE_DRIVING)
                            .overview(DirectionsCriteria.OVERVIEW_FULL)
                            .destination(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()))
                            .build();

                    ApiResponse<DirectionsResponse> response = MapplsDirectionManager
                            .newInstance(directions)
                            .executeCall();

                    if (response != null && response.getResponse() != null) {
                        DirectionsResponse directionsResponse = response.getResponse();

                        // Now post back to main thread to update UI or map
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.e("DirectionsResponse:: ", directionsResponse.toString());

                            if (!directionsResponse.routes().isEmpty()) {
                                DirectionsRoute route = directionsResponse.routes().get(0);
                                // Decode the polyline
                                List<LatLng> latLNGs = PolylineDecoder.decodePolyline(route.geometry());
                                Log.e("routes:: ", latLNGs.toString());

                                if(routePolyline != null) {
                                    mapplsMap.removePolyline(routePolyline);
                                }
                                routePolyline = mapplsMap.addPolyline(new PolylineOptions().addAll(latLNGs).color(Color.parseColor("#3bb2d0")).width(8));
                                if (latLNGs.size() > 1) {
                                    LatLngBounds bounds = new LatLngBounds.Builder().includes(latLNGs).build();

                                    mapplsMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
                                }
                            }
                            // TODO: plot polyline or update UI here
                        });

                    } else {
                        Log.e("DirectionsNull ", "Response was null or empty.");
                    }

                } catch (IOException e) {
                    Log.e("DirectionsCatch", "Error fetching directions", e);
                }
            });

        }

    }


    private void drawPolyLine() {
        Log.e("carContext:: ", carContext + "");
        if (carContext == null)
            return;
        ArrayList<Point> points = new ArrayList<>();
        String locations = MapplsNavigationHelper.getInstance().getCurrentRoute().geometry();

        Log.e("locationnsss:: ", "" + MapplsNavigationHelper.getInstance().getCurrentRoute().geometry());
        if (directionPolylinePlugin != null) {
            LatLng latLng = null;
            if (app.getELocation() != null && app.getELocation().latitude != null && app.getELocation().longitude != null) {
                latLng = new LatLng(app.getELocation().latitude, app.getELocation().longitude);
            }
            List<LineString> listOfPoint = new ArrayList<>();
            listOfPoint.add(LineString.fromPolyline(locations, 6));
            List<LatLng> wayPoints = new ArrayList<>();
            for (int i = 0; i < app.getTrip().waypoints().size() - 1; i++) {
                if (i != 0) {
                    Point point = app.getTrip().waypoints().get(i).location();
                    wayPoints.add(new LatLng(point.latitude(), point.longitude()));
                }
            }


            List<DirectionsRoute> directionsRoutes = new ArrayList<>();
            directionsRoutes.add(MapplsNavigationHelper.getInstance().getCurrentRoute());
            directionPolylinePlugin.setTrips(listOfPoint, null, latLng, wayPoints, directionsRoutes); // need to add way point
            directionPolylinePlugin.setEnabled(true);

            if (points.size() > 1) {
                LatLngBounds bounds = new LatLngBounds.Builder().includes(wayPoints).build();

                mapplsMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
            }


        }
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
        CarToast.makeText(carContext, "Long clicked", CarToast.LENGTH_SHORT).show();
        addMarker(latLng);
        return false;
    }

    public MapplsMap getMapplsMap() {

        return mapplsMap;
    }
}
