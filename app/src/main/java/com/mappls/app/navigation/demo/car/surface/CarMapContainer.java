package com.mappls.app.navigation.demo.car.surface;

import android.Manifest;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.extensions.ThreadUtils;
import com.mappls.app.navigation.demo.maps.plugins.BearingIconPlugin;
import com.mappls.app.navigation.demo.maps.plugins.DirectionPolylinePlugin;
import com.mappls.app.navigation.demo.maps.plugins.MapEventsPlugin;
import com.mappls.app.navigation.demo.maps.plugins.RouteArrowPlugin;
import com.mappls.app.navigation.demo.utils.NavigationLocationEngine;
import com.mappls.app.navigation.demo.utils.directionutils.DirectionUtils;
import com.mappls.app.navigation.demo.viewmodel.RouteViewModel;
import com.mappls.sdk.direction.ui.model.DirectionPoint;
import com.mappls.sdk.geojson.LineString;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.gestures.MoveGestureDetector;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.Mappls;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.MapplsMapOptions;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.annotations.Marker;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.annotations.Polyline;
import com.mappls.sdk.maps.camera.CameraPosition;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.constants.MapplsConstants;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.geometry.LatLngBounds;
import com.mappls.sdk.maps.location.LocationComponent;
import com.mappls.sdk.maps.location.LocationComponentActivationOptions;
import com.mappls.sdk.maps.location.LocationComponentOptions;
import com.mappls.sdk.maps.location.engine.LocationEngine;
import com.mappls.sdk.maps.location.engine.LocationEngineCallback;
import com.mappls.sdk.maps.location.engine.LocationEngineProvider;
import com.mappls.sdk.maps.location.engine.LocationEngineRequest;
import com.mappls.sdk.maps.location.engine.LocationEngineResult;
import com.mappls.sdk.maps.location.modes.CameraMode;
import com.mappls.sdk.maps.location.modes.RenderMode;
import com.mappls.sdk.maps.location.permissions.PermissionsManager;
import com.mappls.sdk.navigation.AlternateRoute;
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.navigation.NavLocation;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.navigation.NavigationLocationProvider;
import com.mappls.sdk.navigation.camera.INavigation;
import com.mappls.sdk.navigation.camera.NavigationCamera;
import com.mappls.sdk.navigation.camera.ProgressChangeListener;
import com.mappls.sdk.navigation.data.WayPoint;
import com.mappls.sdk.navigation.events.NavEvent;
import com.mappls.sdk.navigation.iface.INavigationListener;
import com.mappls.sdk.navigation.iface.JunctionInfoChangedListener;
import com.mappls.sdk.navigation.iface.JunctionViewsLoadedListener;
import com.mappls.sdk.navigation.iface.LocationChangedListener;
import com.mappls.sdk.navigation.iface.NavigationEventListener;
import com.mappls.sdk.navigation.iface.NavigationEventLoadedListener;
import com.mappls.sdk.navigation.model.AdviseInfo;
import com.mappls.sdk.navigation.model.Junction;
import com.mappls.sdk.navigation.model.NavigationResponse;
import com.mappls.sdk.navigation.routing.NavigationStep;
import com.mappls.sdk.navigation.ui.navigation.MapplsNavigationViewHelper;
import com.mappls.sdk.navigation.util.ErrorType;
import com.mappls.sdk.navigation.util.GPSInfo;
import com.mappls.sdk.plugin.annotation.LineManager;
import com.mappls.sdk.plugin.annotation.LineOptions;
import com.mappls.sdk.services.api.ApiResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.directions.DirectionsCriteria;
import com.mappls.sdk.services.api.directions.MapplsDirectionManager;
import com.mappls.sdk.services.api.directions.MapplsDirections;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;
import com.mappls.sdk.services.api.directions.models.LegStep;
import com.mappls.sdk.services.api.directions.models.RouteLeg;
import com.mappls.sdk.services.api.event.route.model.ReportDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class CarMapContainer implements DefaultLifecycleObserver, LocationEngineCallback<LocationEngineResult>, MapplsMap.OnMapLongClickListener, INavigationListener, MapplsMap.OnMoveListener, LocationChangedListener, INavigation {

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
    public static LineManager lineManager;
        public static MapEventsPlugin mapEventsPlugin;
    public static RouteArrowPlugin routeArrowPlugin;
    public static BearingIconPlugin bearingIconPlugin;
    private LocationComponent locationPlugin;
    private LatLng currentLatLng;

    private NavigationLocationEngine navigationLocationEngine;

    /// For Navigation
    private RouteViewModel viewModel;
    private DirectionsResponse directionsResponse;
    private int routeIndex = -1;
    DirectionPoint destinationDirectionPoint;
    NavigationCamera camera;

    Handler gpsHandler = new Handler();

    private GPSInfo gpsInfo;
    Runnable gpsRunnable = new Runnable() {
        @Override
        public void run() {
            if (carContext == null)
                return;

            if (gpsInfo != null && !gpsInfo.fixed) {
//                if (warningTextView != null)
//                    warningTextView.setBackgroundColor(app.getResources().getColor(R.color.red));
            } else if (gpsInfo != null && gpsInfo.usedSatellites < 3) {
//                if (warningTextView != null)
//                    warningTextView.setBackgroundColor(app.getResources().getColor(R.color.common_gray));
            } else {
//                dismissSnackBar();
            }
        }
    };


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
        viewModel = new RouteViewModel();

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
                    map.getUiSettings().setLogoMargins(0, 0, 0, 250);
                    Log.e("Mapps", mapplsMap.toString());
                    map.getStyle(style -> {
                        try {
//                            mapplsMap.removeAnnotations();

                            enableLocationComponent(style);
                            mapplsMap.setMaxZoomPreference(18.5);
                            mapplsMap.setMinZoomPreference(4);
                            setCompassDrawable();

                            directionPolylinePlugin = new DirectionPolylinePlugin(mapViewInstance, map);
                            lineManager = new LineManager(mapViewInstance, mapplsMap, style); // For Polyline
                            bearingIconPlugin = new BearingIconPlugin(mapViewInstance, mapplsMap);
                            routeArrowPlugin = new RouteArrowPlugin(mapViewInstance, mapplsMap);
                            mapEventsPlugin = new MapEventsPlugin(mapViewInstance, mapplsMap);

//                            showRouteClassesDetailToast();


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

    private void initCamera() {
        camera = new NavigationCamera(mapplsMap);
        camera.addProgressChangeListener(CarMapContainer.this);
        camera.start(null);
        // for camera move according to marker move
        camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    }

    public void setNavigationPadding(boolean navigation) {
        if (mapplsMap == null)
            return;

        if (navigation) {

            if (app.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mapplsMap.moveCamera(CameraUpdateFactory.paddingTo(calculateDefaultPadding(mapViewInstance))
                );
            } else {
                mapplsMap.moveCamera(CameraUpdateFactory.paddingTo(
                                0, 250, 0, 0
                        )
                );
            }
        } else {
            mapplsMap.moveCamera(CameraUpdateFactory.paddingTo(
                            0, 0, 0, 0
                    )
            );
        }
    }



    private double[] calculateDefaultPadding(MapView mapView) {
        int defaultTopPadding = calculateTopPaddingWithoutWayname(mapView);
        Resources resources = mapView.getContext().getResources();
        int waynameLayoutHeight = (int) resources.getDimension(R.dimen.mappls_wayname_view_height);
        double topPadding = defaultTopPadding - (waynameLayoutHeight * 2);
        double leftPadding = 0;
//        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            leftPadding = 800;
//        }

        return new double[] {leftPadding, topPadding, 0.0, 0.0};
    }

    private int calculateTopPaddingWithoutWayname(MapView mapView) {
        Context context = mapView.getContext();
        Resources resources = context.getResources();
        int mapViewHeight = mapView.getHeight();
        int bottomSheetHeight = (int) resources.getDimension(R.dimen.mappls_summary_bottomsheet_height);
        return mapViewHeight - (bottomSheetHeight * 4);
    }



    public synchronized void followMe(boolean followButton) {

        if (carContext == null)
            return;


        if (!followButton) {
//            if (mFollowMeButton.getVisibility() != View.VISIBLE)
//                mFollowMeButton.show();
        } else {
            if (ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = locationPlugin.getLastKnownLocation();
                if (location != null) {
                    CameraPosition.Builder builder = new CameraPosition.Builder().tilt(45).zoom(16).target(new LatLng(location.getLatitude(), location.getLongitude()));
                    mapplsMap.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                }
            }


//            if (mFollowMeButton.getVisibility() == View.VISIBLE) {
//                mFollowMeButton.hide();
//            }
            if (bearingIconPlugin != null)
                bearingIconPlugin.setBearingLayerVisibility(false);

        }

        if (camera != null && followButton != camera.isTrackingEnabled())
            camera.updateCameraTrackingMode(followButton ? NavigationCamera.NAVIGATION_TRACKING_MODE_GPS : NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);


    }


    public Location getLocationForNavigation() {

        if (carContext == null)
            return null;
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        NavLocation navLocation = app.getStartNavigationLocation();
        if (navLocation != null) {
            loc.setLatitude(navLocation.getLatitude());
            loc.setLongitude(navLocation.getLongitude());
        }
        try {
            NavLocation firstNavLocation = MapplsNavigationHelper.getInstance().getFirstLocation();
            if (firstNavLocation != null && firstNavLocation.distanceTo(NavigationLocationProvider.convertLocation(loc, app)) < 10) {
                NavLocation secondNavLocation = MapplsNavigationHelper.getInstance().getSecondLocation();
                firstNavLocation.setBearing(firstNavLocation.bearingTo(secondNavLocation));
                return NavigationLocationProvider.revertLocation(firstNavLocation, app);
            } else {
                if (ActivityCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                Location location = locationPlugin != null ? locationPlugin.getLastKnownLocation() : null;
                return location != null ? location : NavigationLocationProvider.revertLocation(NavigationContext.getNavigationContext().getLocationProvider().getFirstTimeRunDefaultLocation(), app);
            }
        } catch (Exception e) {
            Timber.e(e);
            return NavigationLocationProvider.revertLocation(NavigationContext.getNavigationContext().getLocationProvider().getFirstTimeRunDefaultLocation(), carContext);
        }
    }
    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void locationModeNavigation(boolean enable) {

        try {
            if (mapplsMap == null)
                return;

            if (enable) {


                MapplsNavigationHelper.getInstance().addLocationChangeListener(CarMapContainer.this);

                Location location = getLocationForNavigation();
                if (locationPlugin != null && !mapViewInstance.isDestroyed())
                    locationPlugin.forceLocationUpdate(location);

                mapplsMap.getLocationComponent().setLocationEngine(new NavigationLocationEngine());

                followMe(true);

            } else {
                mapplsMap.getLocationComponent().setLocationEngine(LocationEngineProvider.getBestLocationEngine(carContext));
                MapplsNavigationHelper.getInstance().removeLocationChangeListener(this);
                CameraPosition.Builder builder = new CameraPosition.Builder().bearing(0).tilt(0);

                mapplsMap.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));


            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void setupNavigation() {
        /// Navigation related location engine and callbacks
        navigationLocationEngine = new NavigationLocationEngine();

        List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();
        AdviseInfo adviseInfo = MapplsNavigationHelper.getInstance().getAdviseInfo();
        Log.e("adviseInfo:: ", adviseInfo+"");
        if(adviseInfo != null) {
            int position = adviseInfo.getPosition() == 0 ? adviseInfo.getPosition() : adviseInfo.getPosition() - 1;
            NavigationStep currentRouteDirectionInfo = adviseArrayList.get(position);
            LegStep routeLeg = (LegStep) currentRouteDirectionInfo.getExtraInfo();


            LegStep nextRouteLeg = null;


            if (routeArrowPlugin != null) {
                routeArrowPlugin.addUpcomingManeuverArrow(routeLeg, nextRouteLeg);
            }
        }

        directionPolylinePlugin.setOnNewRouteSelectedListener(new DirectionPolylinePlugin.OnNewRouteSelectedListener() {
            @Override
            public void onNewRouteSelected(int index, DirectionsRoute directionsRoute) {
                MapplsNavigationHelper.getInstance().setRouteIndex(index);
            }
        });
        if (bearingIconPlugin != null) {
            bearingIconPlugin.setBearingLayerVisibility(false);
            bearingIconPlugin.setBearingIcon(0, null);
        }


        mapplsMap.addOnMoveListener(CarMapContainer.this);

        if (camera != null) {
            camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
            if(adviseInfo != null) {
                adviseInfo.setLocation(NavigationLocationProvider.convertLocation(getLocationForNavigation(), app));
                onRouteProgress(MapplsNavigationHelper.getInstance().getAdviseInfo());
            }
        }


        if (ActivityCompat.checkSelfPermission(app,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationModeNavigation(true);
        }
        setNavigationPadding(true);
        drawPolyLine();
        initCamera();

        locationPlugin.setRenderMode(RenderMode.GPS);


        MapplsNavigationHelper.getInstance().addNavigationListener(this);

        onRouteProgress(MapplsNavigationHelper.getInstance().getAdviseInfo());


        MapplsNavigationHelper.getInstance().setOnSpeedLimitListener((speed, overSpeed) -> {
            Timber.tag("speeds").d(String.valueOf(speed));
            Log.e("overSpeed",overSpeed ? "OverSpeeding": "Economy");
        });

        MapplsNavigationHelper.getInstance().setJunctionViewEnabled(true);

        MapplsNavigationHelper.getInstance().setNavigationEventLoadedListener(new NavigationEventLoadedListener() {
            @Override
            public void onNavigationEventsLoaded(List<ReportDetails> events) {

                Timber.d(new Gson().toJson(events));
                if (MapplsNavigationHelper.getInstance().getEvents() != null && !MapplsNavigationHelper.getInstance().getEvents().isEmpty() && mapEventsPlugin != null) {
                    mapEventsPlugin.setNavigationEvents(MapplsNavigationHelper.getInstance().getEvents());
                }
            }
        });

        MapplsNavigationHelper.getInstance().setJunctionVisualPromptBefore(200);

        MapplsNavigationHelper.getInstance().setJunctionViewsLoadedListener(new JunctionViewsLoadedListener() {
            @Override
            public void onJunctionViewsLoaded(List<Junction> junctions) {

            }
        });

        MapplsNavigationHelper.getInstance().setJunctionInfoChangedListener(new JunctionInfoChangedListener() {
            @Override
            public void junctionInfoChanged(Junction point) {
                if (point == null) {
                    Timber.tag("JunctionView").d("Junction point is null");
                    return;
                } else {
                    Timber.tag("JunctionView").d("Junction View approaching %s", point.getLeftDistance());
                }


            }
        });

        MapplsNavigationHelper.getInstance().setNavigationEventListener(new NavigationEventListener() {
            @Override
            public void onNavigationEvent(NavEvent navEvent) {
                if (navEvent != null)
                    Timber.d("Navigation Event approaching %s in %f", navEvent.getName(), navEvent.getDistanceLeft());
            }
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
        if (currentLatLng == null) {
            return;
        }
        if (secondaryMarker != null) {
            mapplsMap.removeMarker(secondaryMarker);
        }
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

                    Log.e("currentLatLng:: ", currentLatLng + " / "+ latLng);
                    MapplsDirections directions = MapplsDirections.builder()
                            .origin(Point.fromLngLat(currentLatLng.getLongitude(), currentLatLng.getLatitude()))
                            .steps(true)
                            .resource(DirectionsCriteria.RESOURCE_ROUTE_ETA)
                            .profile(DirectionsCriteria.PROFILE_DRIVING)
                            .alternatives(true)
                            .overview(DirectionsCriteria.OVERVIEW_FULL)
                            .destination(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()))
                            .build();

                    ApiResponse<DirectionsResponse> response = MapplsDirectionManager
                            .newInstance(directions)
                            .executeCall();

                    Log.e("responseresponse:: ", response.toString());

                    if (response != null && response.getResponse() != null) {
                        directionsResponse = response.getResponse();

                        // Now post back to main thread to update UI or map
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.e("DirectionsResponse:: ", directionsResponse.toString());

                            if (!directionsResponse.routes().isEmpty()) {
                                routeIndex = 0;
                                drawRoutes(directionsResponse.routes());

                                secondaryMarker = mapplsMap.addMarker(new MarkerOptions().position(latLng));
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

    private void drawRoutes(List<DirectionsRoute> routes) {

        if(lineManager != null) {
            lineManager.clearAll();
        }
        for (int i = routes.size() - 1; i >= 0; i--) {
            DirectionsRoute route = routes.get(i);

            // Convert the route geometry to a LineString (List of LatLng)
            List<Point> routePoints = LineString.fromPolyline(route.geometry(), 6).coordinates();
            List<LatLng> latLngPoints = new ArrayList<>();

            for (Point point : routePoints) {
                latLngPoints.add(new LatLng(point.latitude(), point.longitude()));
            }

            // Different color or width for primary vs alternate
            int routeColor = (i == 0) ? Color.parseColor("#3bb2d0") : Color.GRAY;

            LineOptions lineOptions = new LineOptions()
                    .points(latLngPoints)
                    .lineColor(colorIntToHex(routeColor))
                    .lineWidth(6f);

            // Use the manager to draw the annotation.
            lineManager.create(lineOptions);


            if (latLngPoints.size() > 1) {
                LatLngBounds bounds = new LatLngBounds.Builder().includes(latLngPoints).build();

                mapplsMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300, 30,30,
                        30));
            }
        }
    }


    public static String colorIntToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
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

    public NavLocation getUserLocation() {
        if (NavigationContext.getNavigationContext().getCurrentLocation() != null) {
            NavLocation loc = new NavLocation("router");
            loc.setLatitude(NavigationContext.getNavigationContext().getCurrentLocation().getLatitude());
            loc.setLongitude(NavigationContext.getNavigationContext().getCurrentLocation().getLongitude());
            return loc;
        } else {
            return null;
        }
    }


    private NavigationResponse navigationBackgroundOperation() {
        try {

            LatLng currentLocation = null;
            NavLocation location = getUserLocation();
            if (location != null)
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            Log.e("currentLocation:: ", currentLocation+"");
            NavLocation navLocation = new NavLocation("navigation");

            Log.e("navLocation:: ", navLocation.hasSpeed()+"");

            if (currentLocation != null) {
                Point position = Point.fromLngLat(currentLocation.getLatitude(), currentLocation.getLongitude());
                LatLng point = new LatLng(position.latitude(), position.longitude());
                navLocation.setLongitude(point.getLongitude());
                navLocation.setLatitude(point.getLatitude());
                app.setStartNavigationLocation(navLocation);
            } else {
                Log.e("NavigationResponse:: ", ErrorType.UNKNOWN_ERROR+"");
                return new NavigationResponse(ErrorType.UNKNOWN_ERROR, null);
            }

            List<WayPoint> wayPoints;
            if (viewModel.geteLocations() == null) {
                Log.e("getELocations:: ", viewModel.geteLocations()+"");
                wayPoints = new ArrayList<>();
            } else {
                Log.e("getELocations:: ", viewModel.geteLocations()+"");
                if (viewModel.geteLocations().size() > 0) {
                    wayPoints = new ArrayList<>();
                    for (ELocation eLocation : viewModel.geteLocations()) {
                        wayPoints.add(getNavigationGeoPoint(eLocation));

                    }
                } else {
                    wayPoints = new ArrayList<>();
                }
            }

            Log.e("wayPoints:: ", wayPoints+"");

            MapplsNavigationViewHelper.getInstance().setStartLocation(navLocation);
            MapplsNavigationViewHelper.getInstance().setDestination(app.getELocation());
            MapplsNavigationViewHelper.getInstance().setWayPoints(wayPoints);
            return MapplsNavigationHelper.getInstance().startNavigation(app.getTrip(),
                    viewModel.getSelectedIndex(), currentLocation,
                    getNavigationGeoPoint(viewModel.geteLocation()), wayPoints);
        } catch (Exception e) {
            Timber.e(e);
            return new NavigationResponse(ErrorType.UNKNOWN_ERROR, null);
        }
    }


    public WayPoint getNavigationGeoPoint(ELocation eLocation) {
        try {
            Log.e("ELocation:: ", eLocation+"");
            if (eLocation.entryLatitude != null && eLocation.entryLongitude != null &&
                    eLocation.entryLatitude > 0 && eLocation.entryLongitude > 0)
                return new WayPoint(eLocation.entryLatitude, eLocation.entryLongitude, eLocation.placeName, eLocation.placeName);
            else if (eLocation.latitude != null && eLocation.longitude != null) {
                return new WayPoint(eLocation.latitude, eLocation.longitude, eLocation.placeName, eLocation.placeName);
            } else {
                return new WayPoint(eLocation.getMapplsPin(), eLocation.placeName, eLocation.placeName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new WayPoint(0, 0, null, null);
        }
    }

    public void startNavigation() {

        /// how to get destination DirectionPoint
//        destinationDirectionPoint =
//                DirectionPoint.setDirection(com.mappls.sdk.geojson.Point.fromLngLat(0.0, 0.0), "", "");
//        List<DirectionPoint> wayPoints = new ArrayList<>();



//        ELocation eLocationDestination = new ELocation();
//        eLocationDestination.placeName = destinationDirectionPoint.getPlaceName();
//        eLocationDestination.placeAddress = destinationDirectionPoint.getPlaceAddress();
//        eLocationDestination.latitude = destinationDirectionPoint.getLatitude();
//        eLocationDestination.longitude = destinationDirectionPoint.getLongitude();
//        eLocationDestination.mapplsPin = destinationDirectionPoint.getMapplsPin();
//        viewModel.seteLocation(eLocationDestination);

        /// how to get waypoints
        /*List<ELocation> eLocations = new ArrayList<>();
        for (DirectionPoint viaPointsDirection : wayPoints) {
            ELocation eLocationViaPoint = new ELocation();
            eLocationViaPoint.placeName = viaPointsDirection.getPlaceName();
            eLocationViaPoint.placeAddress = viaPointsDirection.getPlaceAddress();
            eLocationViaPoint.latitude = viaPointsDirection.getLatitude();
            eLocationViaPoint.longitude = viaPointsDirection.getLongitude();
            eLocationViaPoint.mapplsPin = viaPointsDirection.getMapplsPin();

            eLocations.add(eLocationViaPoint);
        }
        viewModel.seteLocations(eLocations);*/

        viewModel.seteLocation(app.getELocation());
        viewModel.setSelectedIndex(routeIndex);
        viewModel.setTrip(directionsResponse);
        app.setTrip(viewModel.getTrip());


        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            Log.e("NavigationStarted:: ", "Start");
            NavigationResponse result = navigationBackgroundOperation();

            handler.post(() -> {
                //UI Thread work here
                if (carContext == null)
                    return;
//                        hideProgress();
                if (result != null && result.getError() != null) {
                    Timber.d(result.toString());

                    Log.e("NavigationError:: ", result.getError().errorMessage);

                    CarToast.makeText(carContext, result.getError().errorMessage, CarToast.LENGTH_SHORT).show();
                    return;
                }else{
                    setupNavigation();
                }
            });
        });
    }

    public void stopNavigtion() {
        MapplsNavigationHelper.getInstance().stopNavigation();
    }

    @Override
    public void onNavigationStarted() {
        Timber.e("onNavigationStarted");
    }

    @Override
    public void onReRoutingRequested() {
        Timber.e("onReRoutingRequested");
    }

    @Override
    public void onNewRoute(String message) {
        if (carContext == null)
            return;

        Timber.e("onNewRoute");

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (carContext == null)
                    return;

//                showRouteClassesDetailToast();
//                otherInfoTextView.setVisibility(GONE);

                if (mapplsMap != null)
                    mapplsMap.removeAnnotations();
                Location location = getLocationForNavigation();
                NavLocation navLocation = new NavLocation("Router");
                if (location != null) {
                    navLocation.setLatitude(location.getLatitude());
                    navLocation.setLongitude(location.getLongitude());
                }

                List<RouteLeg> routeLegs = MapplsNavigationHelper.getInstance().getCurrentRoute().legs();
                if (routeLegs != null && routeLegs.size() > 0 && routeLegs.get(0).annotation() != null) {

                    int color = getCongestionPercentage(routeLegs.get(0).annotation().congestion(),
                            MapplsNavigationHelper.getInstance().getNodeIndex());
//                    tvEta.setTextColor(ContextCompat.getColor(
//                            getContext(),
//                            color
//                    ));
                }

                drawPolyLine();
            }
        });
    }

    @Override
    public void onRouteProgress(@NonNull AdviseInfo adviseInfo) {

        if (carContext != null) {


            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
//                    if (directionPolylinePlugin != null) {
//                        directionPolylinePlugin.setCurrentLocation(adviseInfo.getLocation());
//                    }
                }
            });

            Timber.tag("Test").e("Advise Info----" + adviseInfo);
            if (adviseInfo != null && adviseInfo.isRouteBeingRecalculated() && !adviseInfo.isOnRoute()) {
//                otherInfoTextView.setVisibility(View.VISIBLE);
//                nextInstructionContainer.setVisibility(GONE);
                return;
            }


//            otherInfoTextView.setVisibility(GONE);


//            if (navigationStripViewPager.getAdapter() == null || !(navigationStripViewPager.getAdapter() instanceof NavigationPagerAdapter))
//                setAdapter();
//            if (camera != null && camera.isTrackingEnabled())
//                navigationStripViewPager.setCurrentItem(adviseInfo.getPosition());
//            if (navigationStripViewPager.getAdapter() != null) {
//                ((NavigationPagerAdapter) navigationStripViewPager.getAdapter()).setDistance(adviseInfo.getDistanceToNextAdvise());
//                ((NavigationPagerAdapter) navigationStripViewPager.getAdapter()).setSelectedPosition(adviseInfo.getPosition());
//            }


            if (adviseInfo.isOnRoute()) {
//                otherInfoTextView.setVisibility(GONE);
//                nextInstructionContainer.setVisibility(View.VISIBLE);
            }
            List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();

//            nextInstructionContainer.setVisibility(View.VISIBLE);
//            if (adviseInfo.getPosition() == navigationStripViewPager.getCurrentItem() && adviseArrayList.size() - 1 > adviseInfo.getPosition()) {

                //show next to next instruction icon
                NavigationStep routeDirectionInfo = adviseArrayList.get(adviseInfo.getPosition() + 1);
                LegStep legStep = (LegStep) routeDirectionInfo.getExtraInfo();
                if (legStep != null) {
//                    nextInstructionImageView.setImageResource(getDrawableResId(routeDirectionInfo.getManeuverID()));
                } else {
//                    nextInstructionContainer.setVisibility(GONE);
                }
//            } else {
//                nextInstructionContainer.setVisibility(GONE);
//            }




            int color = getCongestionPercentage(MapplsNavigationHelper.getInstance().getCurrentRoute().legs().get(0).annotation().congestion(),
                    MapplsNavigationHelper.getInstance().getNodeIndex());
//            tvEta.setTextColor(ContextCompat.getColor(
//                    getContext(),
//                    color
//            ));
//            tvDistanceLeft.setText(NavigationFormatter.getFormattedDistance(adviseInfo.getLeftDistance(), app));
//            tvDurationLeft.setText(NavigationFormatter.getFormattedDuration(adviseInfo.getLeftTime(), app));
//            tvEta.setText(String.format("%s ETA", adviseInfo.getEta()));

            int position = adviseInfo.getPosition() == 0 ? adviseInfo.getPosition() : adviseInfo.getPosition() - 1;
            NavigationStep currentRouteDirectionInfo = adviseArrayList.get(position);
            LegStep routeLeg = (LegStep) currentRouteDirectionInfo.getExtraInfo();


            LegStep nextRouteLeg=null;

            if (adviseArrayList.size() > position + 1) {
                nextRouteLeg = (LegStep) adviseArrayList.get(position + 1).getExtraInfo();

            }

            if (routeArrowPlugin != null) {
                routeArrowPlugin.addUpcomingManeuverArrow(routeLeg,nextRouteLeg);
            }

//            currentPageLocation = adviseInfo.getPosition();

            if (camera != null && camera.isTrackingEnabled()) {
                camera.onRouteProgress(adviseInfo);
            } else {
                if (locationPlugin != null)
                    locationPlugin.forceLocationUpdate(adviseInfo.getLocation());
            }
        }

    }

    private int getCongestionPercentage(List<String> congestionText, int index) {
//        val congestion= directionRoute?.legs()?.get(0)?.annotation()?.congestion()

        if (congestionText == null || congestionText.isEmpty())
            return R.color.navigation_eta_text_color_with_out_traffic;

        int heavy = 0;
        int low = 0;
        int congestionPercentage = 0;

        List<String> congestion;
        if (index < congestionText.size()) {
            congestion = congestionText.subList(index, congestionText.size());
        } else {
            congestion = new ArrayList<>();
        }
        for (int i = 0; i < congestion.size(); i++) {
            if (congestion.get(i).equals("heavy") || congestion.get(i).equals("moderate") || congestion.get(i).equals("severe")) {
                heavy++;
            } else {
                low++;
            }
        }

        if (!congestion.isEmpty()) {
            congestionPercentage = (heavy * 100 / congestion.size());
        } else {
            congestionPercentage = 1;
        }

        if (congestionPercentage <= 10) {
            return R.color.navigation_eta_text_color_with_out_traffic;
        } else if (congestionPercentage <= 25) {
            return R.color.navigation_eta_text_color_with_low_traffic;
        } else {
            return R.color.navigation_eta_text_color_with_traffic;
        }
    }


    @Override
    public void onETARefreshed(String s) {

    }

    @Override
    public void onNavigationCancelled() {
//        openNavigationSummaryDialog();
        Timber.e("onNavigationCancelled");

    }

    @Override
    public void onNavigationFinished() {
        Timber.e("onRouteFinished");

        CarToast.makeText(carContext, "Reached to destination", CarToast.LENGTH_SHORT).show();

//        if (carContext != null && isItSaveForFragmentTransaction) {
//            openNavigationSummaryDialog();
//            dismissSnackBar();
//        }
    }

    @Override
    public void onWayPointReached(WayPoint wayPoint) {

    }

    @Override
    public void onEvent(@Nullable NavEvent navEvent) {

    }

    @Override
    public void onAlternateRoutesUpdate(@Nullable List<AlternateRoute> directionsRoutes) {
        INavigationListener.super.onAlternateRoutesUpdate(directionsRoutes);
    }

    @Override
    public void onBetterRouteAvailable(List<DirectionsRoute> directionsRoutes) {
        INavigationListener.super.onBetterRouteAvailable(directionsRoutes);
    }

    @Override
    public void onRerouteFailed(int error, String message) {
        INavigationListener.super.onRerouteFailed(error, message);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {

    }

    @Override
    public void onMove(@NonNull MoveGestureDetector moveGestureDetector) {

    }

    @Override
    public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {
//        ivRouteOverview.setVisibility(View.VISIBLE);
//        ivNavigationStop.setVisibility(View.VISIBLE);
//        if (!mBottomSheetBehavior.isHideable()) {
            followMe(false);
//        }
    }

    @Override
    public void onLocationChanged(Location location) {
        gpsHandler.removeCallbacksAndMessages(null);
        if (carContext != null && location != null && mapplsMap != null && !(mapViewInstance.isDestroyed())) {
//            if (locationPlugin != null)
//                locationPlugin.forceLocationUpdate(location);
//            dismissSnackBar();
        }
    }

    @Override
    public void onGPSConnectionChanged(boolean gpsRestored) {
        if (gpsRestored) {
            Log.e("GPSConnection:: ", app.getString(R.string.gps_connection_restored));
        } else {
            Log.e("GPSConnection:: ", app.getString(R.string.gps_connection_lost));
        }
    }

    @Override
    public void onSatelliteInfoChanged(GPSInfo gpsInfo) {
        this.gpsInfo = gpsInfo;
        gpsHandler.postDelayed(gpsRunnable, 2000);

    }

    @Override
    public void setProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {

    }

    @Override
    public void removeProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {

    }
}
