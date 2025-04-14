package com.mappls.app.navigation.demo.fragment;

import static android.view.View.GONE;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.visibility;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.NavigationActivity;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.adapter.NavigationPagerAdapter;
import com.mappls.app.navigation.demo.maps.plugins.BearingIconPlugin;
import com.mappls.app.navigation.demo.maps.plugins.DirectionPolylinePlugin;
import com.mappls.app.navigation.demo.maps.plugins.MapEventsPlugin;
import com.mappls.app.navigation.demo.maps.plugins.RouteArrowPlugin;
import com.mappls.app.navigation.demo.model.Stop;
import com.mappls.app.navigation.demo.utils.NavigationLocationEngine;
import com.mappls.app.navigation.demo.views.LockableBottomSheetBehavior;
import com.mappls.app.navigation.demo.views.RecenterButton;
import com.mappls.sdk.geojson.LineString;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.geojson.utils.PolylineUtils;
import com.mappls.sdk.gestures.MoveGestureDetector;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.camera.CameraPosition;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.geometry.LatLngBounds;
import com.mappls.sdk.maps.location.LocationComponent;
import com.mappls.sdk.maps.location.engine.LocationEngineProvider;
import com.mappls.sdk.maps.location.modes.RenderMode;
import com.mappls.sdk.maps.style.layers.Property;
import com.mappls.sdk.maps.utils.MathUtils;
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.navigation.NavLocation;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.navigation.NavigationFormatter;
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
import com.mappls.sdk.navigation.iface.OnSpeedLimitListener;
import com.mappls.sdk.navigation.model.AdviseInfo;
import com.mappls.sdk.navigation.model.Junction;
import com.mappls.sdk.navigation.notifications.NavigationNotification;
import com.mappls.sdk.navigation.routing.NavigationStep;
import com.mappls.sdk.navigation.util.GPSInfo;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;
import com.mappls.sdk.services.api.directions.models.LegStep;
import com.mappls.sdk.services.api.directions.models.RouteClasses;
import com.mappls.sdk.services.api.directions.models.RouteLeg;
import com.mappls.sdk.services.api.event.route.model.ReportDetails;
import com.mappls.sdk.services.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;


public class NavigationFragment extends Fragment implements
        View.OnClickListener,
        MapplsMap.OnMoveListener,
        LocationChangedListener,
        INavigationListener,
        OnMapReadyCallback, INavigation {

    private static final int BOTTOMSHEET_PADDING_MULTIPLIER = 4;
    private static final int WAYNAME_PADDING_MULTIPLIER = 2;

    String lastImageSetFor = null;
    Handler gpsHandler = new Handler();
    private boolean isItSaveForFragmentTransaction = true;
    private boolean mFragmentTransactionSave = true;
    private NavApplication app;
    private int currentPageLocation = 1;
    private GPSInfo gpsInfo;
    private AnimationSet fadeInSlowOut;
    private MapplsMap mapplsMap;
    //map related
    private BroadcastReceiver mBroadcastReceiver;
    private LocationComponent locationPlugin;
    private DirectionPolylinePlugin directionPolylinePlugin;
    private RouteArrowPlugin routeArrowPlugin;
    private MapEventsPlugin mapEventsPlugin;
    private BearingIconPlugin bearingIconPlugin;
    private NavigationCamera camera;
    //Views
    private BottomSheetBehavior mBottomSheetBehavior;
    private TextView warningTextView;
    private TextView tvRouteClassDetail;
    Runnable gpsRunnable = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null)
                return;

            if (gpsInfo != null && !gpsInfo.fixed) {
                if (warningTextView != null)
                    warningTextView.setBackgroundColor(getResources().getColor(R.color.red));
            } else if (gpsInfo != null && gpsInfo.usedSatellites < 3) {
                if (warningTextView != null)
                    warningTextView.setBackgroundColor(getResources().getColor(R.color.common_gray));
            } else {
                dismissSnackBar();
            }
        }
    };
    private TextView otherInfoTextView;
    private RecenterButton mFollowMeButton;
    private View nextInstructionContainer;
    private ViewPager navigationStripViewPager;
    private ImageView nextInstructionImageView;
    private TextView soundChipText;
    private FloatingActionButton soundFab;
    private TextView tvEta, tvDistanceLeft, tvDurationLeft;
    private ImageView ivRouteOverview, ivNavigationStop;
    private FloatingActionButton settingFloatingActionButton;
    private ImageView junctionViewImageView;


    public NavigationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcastReceiver = new MyLocalBroadcastReceiver();
        app = getMyApplication();


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        if (getActivity() instanceof NavigationActivity) {
            ((NavigationActivity) getActivity()).getMapView().getMapAsync(this);
        }
        MapplsNavigationHelper.getInstance().addNavigationListener(this);

        onRouteProgress(MapplsNavigationHelper.getInstance().getAdviseInfo());

        MapplsNavigationHelper.getInstance().setOnSpeedLimitListener(new OnSpeedLimitListener() {
            @Override
            public void onSpeedChanged(double speed, boolean overSpeed) {

//                Toast.makeText(getContext(),String.valueOf(speed),Toast.LENGTH_LONG).show();
                Timber.tag("speeds").d(String.valueOf(speed));
                if (settingFloatingActionButton != null) {
                    settingFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(overSpeed ? Color.RED : Color.GREEN));
                }
            }
        });
        MapplsNavigationHelper.getInstance().setJunctionViewEnabled(true);

        MapplsNavigationHelper.getInstance().setNavigationEventLoadedListener(new NavigationEventLoadedListener() {
            @Override
            public void onNavigationEventsLoaded(List<ReportDetails> events) {

                Timber.d(new Gson().toJson(events));
                if (MapplsNavigationHelper.getInstance().getEvents() != null && MapplsNavigationHelper.getInstance().getEvents().size() > 0 && mapEventsPlugin != null) {
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
                    lastImageSetFor = null;
                    junctionViewImageView.setVisibility(View.INVISIBLE);
                    return;
                } else {
                    Timber.tag("JunctionView").d("Junction View approaching %s", point.getLeftDistance());
                    if (point.bitmap != null) {
                        junctionViewImageView.setImageBitmap(point.bitmap);
                    }
                    junctionViewImageView.setVisibility(View.VISIBLE);
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

    public void openNavigationSummaryDialog() {
        FragmentManager fragmentManager = (requireActivity()).getSupportFragmentManager();

        if (fragmentManager.findFragmentByTag(RouteSummaryDialogFragment.class.getSimpleName()) == null) {
            RouteSummaryDialogFragment routeSummaryDialogFragment = new RouteSummaryDialogFragment();
            Timber.e("openNavigationSummaryDialog");
            routeSummaryDialogFragment.show(fragmentManager, RouteSummaryDialogFragment.class.getSimpleName());
        }
    }

    private void openClassesDetailDialog() {
        FragmentManager fragmentManager = (requireActivity()).getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(ClassesDetailDialogFragment.class.getSimpleName()) == null) {
            ClassesDetailDialogFragment classesDetailDialogFragment = new ClassesDetailDialogFragment();
            classesDetailDialogFragment.show(fragmentManager, ClassesDetailDialogFragment.class.getSimpleName());
        }
    }


    private void drawPolyLine() {
        if (getActivity() == null)
            return;
        ArrayList<Point> points = new ArrayList<>();
        String locations = MapplsNavigationHelper.getInstance().getCurrentRoute().geometry();

        Timber.e(MapplsNavigationHelper.getInstance().getCurrentRoute().toJson());
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

        }
    }


    @Override
    public void onDestroyView() {
        if (getActivity() == null)
            return;


        if (directionPolylinePlugin != null)
            directionPolylinePlugin.removeAllData();

//        camera.updateCameraTrackingLocation(false);

        if (locationPlugin != null) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationPlugin.setLocationComponentEnabled(false);
        }
        camera = null;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationModeNavigation(false);
            locationPlugin = null;
        }
        MapplsNavigationHelper.getInstance().removeNavigationListener(this);
        if (mapplsMap != null) {
            mapplsMap.removeOnMoveListener(this);
            mapplsMap.removeAnnotations();
        }


        super.onDestroyView();

    }

    public void onBackPressed() {
        MapplsNavigationHelper.getInstance().stopNavigation();
//        openNavigationSummaryDialog();
    }

    //
    @Override
    public void onDetach() {
        if (directionPolylinePlugin != null)
            directionPolylinePlugin.setEnabled(true);
        super.onDetach();
    }


    public NavApplication getMyApplication() {

        if (getActivity() != null)
            return ((NavApplication) getActivity().getApplication());
        else
            return null;
    }

    @Override
    public void onClick(View view) {

        if (getActivity() == null)
            return;

        switch (view.getId()) {

            case R.id.follow_button:

                nextInstructionContainer.setVisibility(View.VISIBLE);

                setNavigationPadding(true);
                followMe(true);
                break;

            case R.id.sound_btn:

                toggleMute();
                break;

            case R.id.reset_bounds_button:
                if (camera != null)
                    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
                List<NavigationStep> routeDirectionIfs = MapplsNavigationHelper.getInstance().getNavigationSteps();


                ArrayList<LatLng> points = new ArrayList<>();


                for (NavigationStep routeDirectionInfo : routeDirectionIfs) {
                    NavLocation navLocation = routeDirectionInfo.getNavLocation();
                    if (navLocation != null)
                        points.add(new LatLng(navLocation.getLatitude(), navLocation.getLongitude()));
                }
                if (points.size() > 1) {
                    LatLngBounds bounds = new LatLngBounds.Builder().includes(points).build();

                    mapplsMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
                }

                break;

            case R.id.image_route_overview:
                if (getActivity() == null)
                    return;

                if (MapplsNavigationHelper.getInstance().getCurrentRoute() == null) {
                    return;
                }
                setNavigationPadding(false);
                if (camera != null)
                    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);


                mapplsMap.setCameraPosition(new CameraPosition.Builder().bearing(0.0).tilt(0.0).build());

                if (mFollowMeButton.getVisibility() != View.VISIBLE)
                    mFollowMeButton.setVisibility(View.VISIBLE);

                ivRouteOverview.setVisibility(GONE);
                ivNavigationStop.setVisibility(GONE);

                points = new ArrayList<>();

                List<Point> locations = PolylineUtils.decode(MapplsNavigationHelper.getInstance().getCurrentRoute().geometry(), Constants.PRECISION_6);
                if (locations != null && locations.size() > 0) {
                    for (Point _location : locations) {
                        if (_location != null)
                            points.add(new LatLng(_location.latitude(), _location.longitude()));
                    }

                    if (points.size() > 1) {
                        LatLngBounds bounds = new LatLngBounds.Builder().includes(points).build();
                        mapplsMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20, 320, 20, 280), 2000);
                    }
                }
                break;
            case R.id.image_navigation_stop:
                if (getActivity() == null)
                    return;
                MapplsNavigationHelper.getInstance().stopNavigation();

        }
    }

    float getLocationAngle(NavLocation currentLocation) {
        try {
            List<NavLocation> routeNodes = NavigationContext.getNavigationContext().getCalculatedRoute().getPath();

            return (float) MathUtils.wrap(currentLocation
                            .bearingTo(routeNodes.get((routeNodes.indexOf(currentLocation) + 1))),
                    0, 360);
        } catch (Exception e) {
            return 0;
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onMapReady(MapplsMap map) {

        try {
            Timber.e("onMapReady");
            if (getActivity() == null)
                return;
            mapplsMap = map;
            map.getUiSettings().setLogoMargins(0, 0, 0, 250);
            map.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    mapplsMap.removeAnnotations();
                    locationPlugin = mapplsMap.getLocationComponent();
                    locationPlugin.setRenderMode(RenderMode.GPS);

                    directionPolylinePlugin = ((NavigationActivity) getActivity()).getDirectionPolylinePlugin();
                    bearingIconPlugin = ((NavigationActivity) getActivity()).getBearingIconPlugin();
                    routeArrowPlugin = ((NavigationActivity) getActivity()).getRouteArrowPlugin();
                    mapEventsPlugin = ((NavigationActivity) getActivity()).getMapEventPlugin();
                    List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();
                    AdviseInfo adviseInfo = MapplsNavigationHelper.getInstance().getAdviseInfo();
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


                    map.addOnMoveListener(NavigationFragment.this);

                    if (camera != null) {
                        camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
                        if(adviseInfo != null) {
                            adviseInfo.setLocation(NavigationLocationProvider.convertLocation(getLocationForNavigation(), app));
                            onRouteProgress(MapplsNavigationHelper.getInstance().getAdviseInfo());
                        }
                    }


                    if (getActivity() != null && ((NavigationActivity) getActivity()).getMapView() != null) {
                        ((NavigationActivity) getActivity()).getMapView().setOnTouchListener((view1, motionEvent) -> {

                            if (!mBottomSheetBehavior.isHideable()) {
                                followMe(false);

                            }

                            return view1.onTouchEvent(motionEvent);
                        });
                    }

                    if (ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationModeNavigation(true);
                    }
                    setNavigationPadding(true);
                    drawPolyLine();
                    initCamera();

                    showRouteClassesDetailToast();

                    style.getLayer("mappls-location-shadow-layer").setProperties(visibility(Property.NONE));
                }
            });


        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        setNavigationPadding(true);
        followMe(true);

    }

    @Override
    public void onMapError(int i, String s) {
        Timber.e(i + "------" + s);

    }


    @Override
    public void onStart() {
        super.onStart();
        isItSaveForFragmentTransaction = true;
        if (getActivity() != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(NavigationNotification.NAVIGATION_STOP_NAVIGATION_SERVICE_ACTION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(NavigationNotification.NAVIGATION_STOP_NAVIGATION_SERVICE_ACTION));
            }


    }


    @Override
    public void onPause() {
        isItSaveForFragmentTransaction = false;
        super.onPause();
        try {
            if (getActivity() != null)
                getActivity().unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            //ignore
        }

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {

    }

    @Override
    public void onMove(@NonNull MoveGestureDetector moveGestureDetector) {

    }

    @Override
    public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {
        ivRouteOverview.setVisibility(View.VISIBLE);
        ivNavigationStop.setVisibility(View.VISIBLE);
        if (!mBottomSheetBehavior.isHideable()) {
            followMe(false);
        }
    }

    /**
     * Initializes the {@link NavigationCamera} that will be used to follow
     * the {@link Location} from navigation service
     */
    private void initCamera() {
        camera = new NavigationCamera(mapplsMap);
        camera.addProgressChangeListener(this);
        camera.start(null);
        // for camera move according to marker move
        camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    }


    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void locationModeNavigation(boolean enable) {

        try {
            if (mapplsMap == null)
                return;

            if (enable) {


                 MapplsNavigationHelper.getInstance().addLocationChangeListener(this);

                Location location = getLocationForNavigation();
                if (locationPlugin != null && !((NavigationActivity) getActivity()).getMapView().isDestroyed())
                    locationPlugin.forceLocationUpdate(location);

                mapplsMap.getLocationComponent().setLocationEngine(new NavigationLocationEngine());

                followMe(true);

            } else {
                mapplsMap.getLocationComponent().setLocationEngine(LocationEngineProvider.getBestLocationEngine(getActivity()));
                MapplsNavigationHelper.getInstance().removeLocationChangeListener(this);
                CameraPosition.Builder builder = new CameraPosition.Builder().bearing(0).tilt(0);

                mapplsMap.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));


            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isItSaveForFragmentTransaction = false;
        super.onPause();
        try {
            if (getActivity() != null)
                getActivity().unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            //ignore
        }
    }

    public Location getLocationForNavigation() {

        if (getActivity() == null)
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
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                Location location = locationPlugin != null ? locationPlugin.getLastKnownLocation() : null;
                return location != null ? location : NavigationLocationProvider.revertLocation(NavigationContext.getNavigationContext().getLocationProvider().getFirstTimeRunDefaultLocation(), app);
            }
        } catch (Exception e) {
            Timber.e(e);
            return NavigationLocationProvider.revertLocation(NavigationContext.getNavigationContext().getLocationProvider().getFirstTimeRunDefaultLocation(), getContext());
        }
    }

    public synchronized void followMe(boolean followButton) {

        if (getActivity() == null)
            return;


        if (!followButton) {
            if (mFollowMeButton.getVisibility() != View.VISIBLE)
                mFollowMeButton.show();
        } else {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = locationPlugin.getLastKnownLocation();
                if (location != null) {
                    CameraPosition.Builder builder = new CameraPosition.Builder().tilt(45).zoom(16).target(new LatLng(location.getLatitude(), location.getLongitude()));
                    mapplsMap.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                }
            }


            if (mFollowMeButton.getVisibility() == View.VISIBLE) {
                mFollowMeButton.hide();
            }
            if (bearingIconPlugin != null)
                bearingIconPlugin.setBearingLayerVisibility(false);

        }

        if (camera != null && followButton != camera.isTrackingEnabled())
            camera.updateCameraTrackingMode(followButton ? NavigationCamera.NAVIGATION_TRACKING_MODE_GPS : NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);


    }

    /**
     * Sets up mute UI event.
     * <p>
     * Shows chip with "Muted" text.
     * Changes sound {@link FloatingActionButton}
     * {@link Drawable} to denote sound is off.
     * <p>
     * Sets private state variable to true (muted)
     */
    private void mute() {

        setSoundChipText(getString(R.string.muted));
        showSoundChip();
        soundFabOff();
        MapplsNavigationHelper.getInstance().setMute(true);
    }

    /**
     * Sets up unmuted UI event.
     * <p>
     * Shows chip with "Unmuted" text.
     * Changes sound {@link FloatingActionButton}
     * {@link Drawable} to denote sound is on.
     * <p>
     * Sets private state variable to false (unmuted)
     */
    private void unmute() {

        setSoundChipText(getString(R.string.unmuted));
        showSoundChip();
        soundFabOn();
        MapplsNavigationHelper.getInstance().setMute(false);
    }

    /**
     * Changes sound {@link FloatingActionButton}
     * {@link Drawable} to denote sound is off.
     */
    private void soundFabOff() {
        soundFab.setImageResource(R.drawable.ic_sound_off);
    }

    /**
     * Changes sound {@link FloatingActionButton}
     * {@link Drawable} to denote sound is on.
     */
    private void soundFabOn() {
        soundFab.setImageResource(R.drawable.ic_sound_on);
    }

    /**
     * Sets {@link TextView} inside of chip view.
     *
     * @param text to be displayed in chip view ("Muted"/"Umuted")
     */
    private void setSoundChipText(String text) {
        soundChipText.setText(text);
    }

    /**
     * Shows and then hides the sound chip using {@link AnimationSet}
     */
    private void showSoundChip() {
        soundChipText.startAnimation(fadeInSlowOut);
    }

    /**
     * Initializes all animations needed to show / hide views.
     */
    private void initAnimations() {

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(300);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartOffset(1000);
        fadeOut.setDuration(1000);

        fadeInSlowOut = new AnimationSet(false);
        fadeInSlowOut.addAnimation(fadeIn);
        fadeInSlowOut.addAnimation(fadeOut);
    }

    public void toggleMute() {
        if (MapplsNavigationHelper.getInstance().isMute()) {
            unmute();
        } else {
            mute();
        }
    }


    //navigation listener

    void showSnackBar(final String text) {
        if (warningTextView != null) {
            warningTextView.setText(text);
            warningTextView.setAlpha(1);
        }
    }

    void dismissSnackBar() {
        if (warningTextView != null) {
            warningTextView.setAlpha(0);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        gpsHandler.removeCallbacksAndMessages(null);
        if (getActivity() != null && location != null && mapplsMap != null && mFragmentTransactionSave && !((NavigationActivity) getActivity()).getMapView().isDestroyed()) {

//            if (locationPlugin != null)
//                locationPlugin.forceLocationUpdate(location);
            dismissSnackBar();
        }

    }

    @Override
    public void onGPSConnectionChanged(boolean gpsRestored) {

        if (gpsRestored) {
            showSnackBar(getString(R.string.gps_connection_restored));
            dismissSnackBar();
        } else {
            showSnackBar(getString(R.string.gps_connection_lost));
        }
    }

    @Override
    public void onSatelliteInfoChanged(GPSInfo gpsInfo) {

        this.gpsInfo = gpsInfo;
        gpsHandler.postDelayed(gpsRunnable, 2000);


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
    public void onRerouteFailed(int error, String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onEvent(@Nullable NavEvent navEvent) {

    }

    @Override
    public void onETARefreshed(String geometry) {

    }

    @Override
    public void onNewRoute(String geometry/*boolean isNewRoute, final List<NavLocation> routeNodes*/) {


        if (getActivity() == null)
            return;

        Timber.e("onNewRoute");
        getActivity().runOnUiThread(() -> {
            if (getActivity() == null)
                return;

            showRouteClassesDetailToast();
            setAdapter();
            otherInfoTextView.setVisibility(GONE);

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
                tvEta.setTextColor(ContextCompat.getColor(
                        getContext(),
                        color
                ));
            }

            drawPolyLine();


        });


    }

    private int getCongestionPercentage(List<String> congestionText, int index) {
//        val congestion= directionRoute?.legs()?.get(0)?.annotation()?.congestion()

        if (congestionText == null || congestionText.isEmpty())
            return R.color.navigation_eta_text_color_with_out_traffic;

        int heavy = 0;
        int low = 0;
        int congestionPercentage = 0;

        List<String> congestion;
        if (congestionText != null && congestionText.isEmpty()) {
            congestion = new ArrayList<>();
        } else {
            if (index < congestionText.size()) {
                congestion = congestionText.subList(index, congestionText.size());
            } else {
                congestion = new ArrayList<>();
            }
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
    public void onNavigationCancelled() {
        openNavigationSummaryDialog();
        Timber.e("onNavigationCancelled");


    }

    @Override
    public void onNavigationFinished() {
        Timber.e("onRouteFinished");

        Toast.makeText(app, "Reached to destination", Toast.LENGTH_SHORT).show();
        if (getActivity() != null && isItSaveForFragmentTransaction) {
            openNavigationSummaryDialog();
            dismissSnackBar();
        }

    }



    private double[] calculateDefaultPadding(MapView mapView) {
        int defaultTopPadding = calculateTopPaddingWithoutWayname(mapView);
        Resources resources = mapView.getContext().getResources();
        int waynameLayoutHeight = (int) resources.getDimension(R.dimen.mappls_wayname_view_height);
        double topPadding = defaultTopPadding - (waynameLayoutHeight * WAYNAME_PADDING_MULTIPLIER);
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
        return mapViewHeight - (bottomSheetHeight * BOTTOMSHEET_PADDING_MULTIPLIER);
    }

    @Override
    public void onWayPointReached(WayPoint wayPoint) {
        Toast.makeText(getContext(), "Reached to " + wayPoint.getSpokenName(), Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onRouteProgress(AdviseInfo adviseInfo) {
        if (getActivity() != null && adviseInfo != null) {


            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
//                    if (directionPolylinePlugin != null) {
//                        directionPolylinePlugin.setCurrentLocation(adviseInfo.getLocation());
//                    }
                }
            });

            Timber.tag("Test").e("Advise Info----" + adviseInfo.toString());
            if (adviseInfo.isRouteBeingRecalculated() && !adviseInfo.isOnRoute()) {
                otherInfoTextView.setVisibility(View.VISIBLE);
                nextInstructionContainer.setVisibility(GONE);
                return;
            }


            otherInfoTextView.setVisibility(GONE);


            if (navigationStripViewPager.getAdapter() == null || !(navigationStripViewPager.getAdapter() instanceof NavigationPagerAdapter))
                setAdapter();
            if (camera != null && camera.isTrackingEnabled())
                navigationStripViewPager.setCurrentItem(adviseInfo.getPosition());
            if (navigationStripViewPager.getAdapter() != null) {
                ((NavigationPagerAdapter) navigationStripViewPager.getAdapter()).setDistance(adviseInfo.getDistanceToNextAdvise());
                ((NavigationPagerAdapter) navigationStripViewPager.getAdapter()).setSelectedPosition(adviseInfo.getPosition());
            }


            if (adviseInfo.isOnRoute()) {
                otherInfoTextView.setVisibility(GONE);
                nextInstructionContainer.setVisibility(View.VISIBLE);
            }
            List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();

            nextInstructionContainer.setVisibility(View.VISIBLE);
            if (adviseInfo.getPosition() == navigationStripViewPager.getCurrentItem() && adviseArrayList.size() - 1 > adviseInfo.getPosition()) {

                //show next to next instruction icon
                NavigationStep routeDirectionInfo = adviseArrayList.get(adviseInfo.getPosition() + 1);
                LegStep legStep = (LegStep) routeDirectionInfo.getExtraInfo();
                if (legStep != null) {
                    nextInstructionImageView.setImageResource(getDrawableResId(routeDirectionInfo.getManeuverID()));
                } else {
                    nextInstructionContainer.setVisibility(GONE);
                }
            } else {
                nextInstructionContainer.setVisibility(GONE);
            }




            int color = getCongestionPercentage(MapplsNavigationHelper.getInstance().getCurrentRoute().legs().get(0).annotation().congestion(),
                    MapplsNavigationHelper.getInstance().getNodeIndex());
            tvEta.setTextColor(ContextCompat.getColor(
                    getContext(),
                    color
            ));
            tvDistanceLeft.setText(NavigationFormatter.getFormattedDistance(adviseInfo.getLeftDistance(), app));
            tvDurationLeft.setText(NavigationFormatter.getFormattedDuration(adviseInfo.getLeftTime(), app));
            tvEta.setText(String.format("%s ETA", adviseInfo.getEta()));

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

            currentPageLocation = adviseInfo.getPosition();

            if (camera != null && camera.isTrackingEnabled()) {
                camera.onRouteProgress(adviseInfo);
            } else {
                if (locationPlugin != null)
                    locationPlugin.forceLocationUpdate(adviseInfo.getLocation());
            }
        }

    }

    int getDrawableResId(int maneuverId) {
        return getResources().getIdentifier("ic_step_" + maneuverId, "drawable", getActivity().getPackageName());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    public void setNavigationPadding(boolean navigation) {
        if (mapplsMap == null)
            return;

        if (navigation) {

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mapplsMap.moveCamera(CameraUpdateFactory.paddingTo(calculateDefaultPadding(((NavigationActivity) getActivity()).getMapView()))
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


    @SuppressLint("MissingPermission")
    void initViews(View view) {
        initAnimations();
        soundChipText = view.findViewById(R.id.sound_text);
        soundFab = view.findViewById(R.id.sound_btn);
        tvRouteClassDetail = view.findViewById(R.id.class_detail_text);
        soundFab.setOnClickListener(this);

        mFollowMeButton = view.findViewById(R.id.follow_button);
        mFollowMeButton.setOnClickListener(this);

        view.findViewById(R.id.reset_bounds_button).setOnClickListener(this);

        warningTextView = view.findViewById(R.id.warning_text_view);

        otherInfoTextView = view.findViewById(R.id.other_info_text_view);
        ImageButton imageViewLeft = view.findViewById(R.id.navigation_strip_left_image_button);
        ImageButton imageViewRight = view.findViewById(R.id.navigation_strip_right_image_button);
        navigationStripViewPager = view.findViewById(R.id.navigation_info_layout_new);

        nextInstructionImageView = view.findViewById(R.id.next_instruction_image_view);
        nextInstructionContainer = view.findViewById(R.id.next_advise_container);


        tvEta = view.findViewById(R.id.text_view_reach_eta);
        tvDistanceLeft = view.findViewById(R.id.text_view_total_distance_left);
        tvDurationLeft = view.findViewById(R.id.text_view_total_time_left);

        ivRouteOverview = view.findViewById(R.id.image_route_overview);
        ivNavigationStop = view.findViewById(R.id.image_navigation_stop);

        ivRouteOverview.setOnClickListener(this);
        ivNavigationStop.setOnClickListener(this);
        junctionViewImageView = view.findViewById(R.id.junction_view_image_view);
        settingFloatingActionButton = view.findViewById(R.id.settings_button);
        settingFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MapplsNavigationHelper.getInstance().getCurrentRoute() != null) {
                    if (MapplsNavigationHelper.getInstance().getCurrentRoute().routeClasses() != null) {
                        openClassesDetailDialog();
                    } else {
                        Toast.makeText(requireContext(), "This route does not contain any Classes", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Please wait No Route Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
        imageViewLeft.setOnClickListener(v -> nextPreviousButtonPressed(true));
        imageViewRight.setOnClickListener(v -> nextPreviousButtonPressed(false));

        mFollowMeButton.setOnClickListener(v -> {
            navigationStripViewPager.setCurrentItem(currentPageLocation);
            nextInstructionContainer.setVisibility(View.VISIBLE);
            ivRouteOverview.setVisibility(View.VISIBLE);
            ivNavigationStop.setVisibility(View.VISIBLE);
            setNavigationPadding(true);
            followMe(true);
        });


        View mBottomSheetContainer = view.findViewById(R.id.options_recycler_view_container);

        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetContainer);
        mBottomSheetBehavior.setHideable(false);
        ((LockableBottomSheetBehavior) mBottomSheetBehavior).setLocked(true);
    }

    void setAdapter() {
        if (getActivity() == null)
            return;
        List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();


        Stop stop = new Stop();
        stop.setName("END STOP");

        NavigationPagerAdapter adapter = new NavigationPagerAdapter(getActivity(), adviseArrayList, stop);
        navigationStripViewPager.setAdapter(adapter);
        adapter.setSelectedPosition(1);
        navigationStripViewPager.setCurrentItem(1);


    }

    private void showRouteClassesDetailToast() {
        if (MapplsNavigationHelper.getInstance().getCurrentRoute() != null && MapplsNavigationHelper.getInstance().getCurrentRoute().routeClasses() != null) {
            RouteClasses routeClasses = MapplsNavigationHelper.getInstance().getCurrentRoute().routeClasses();
            List<String> classesList = new ArrayList<>();

            if (routeClasses.toll() != null && routeClasses.toll() == 1) {
                classesList.add("Toll");
            }
            if (routeClasses.ferry() != null && routeClasses.ferry() == 1) {
                classesList.add("Ferry");
            }
            if (routeClasses.ferry() != null && routeClasses.ferry() == 1) {
                classesList.add("Tunnel");
            }
            if (routeClasses.motorway() != null && routeClasses.motorway() == 1) {
                classesList.add("Motorway");
            }
            if (routeClasses.restricted() != null && routeClasses.restricted() == 1) {
                classesList.add("Restricted");
            }

            if (classesList.size() > 0) {
                StringBuilder builder = new StringBuilder("This route contains ");
                if (classesList.size() == 1) {
                    builder.append(classesList.get(0));
                } else {
                    for (int i = 0; i < classesList.size(); i++) {
                        if (i == classesList.size() - 1) {
                            builder.append(" & ");
                            builder.append(classesList.get(i));
                        } else if (i == 0) {
                            builder.append(classesList.get(i));
                        } else {
                            builder.append(", ");
                            builder.append(classesList.get(i));
                        }
                    }
                }
                tvRouteClassDetail.setText(builder.toString());
            } else {
                tvRouteClassDetail.setText("This route does not contains any classes");
            }
        } else {
            tvRouteClassDetail.setText("This route does not contains any classes");
        }
        tvRouteClassDetail.startAnimation(fadeInSlowOut);
    }

    private void nextPreviousButtonPressed(boolean isLeft) {
        nextInstructionContainer.setVisibility(GONE);
        if (camera != null)
            camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);


        if (mFollowMeButton.getVisibility() != View.VISIBLE)
            mFollowMeButton.setVisibility(View.VISIBLE);

        if (isLeft) {

            if (navigationStripViewPager.getCurrentItem() > currentPageLocation) {
                navigationStripViewPager.setCurrentItem((navigationStripViewPager.getCurrentItem()) - 1);
//                if (bearingIconPlugin != null)
//                    bearingIconPlugin.setBearingLayerVisibility(true);
                setNavigationPadding(false);
                fixPreviewNavigationMarker();
            }

        } else {
            List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();
            if (navigationStripViewPager.getCurrentItem() < adviseArrayList.size()) {
                navigationStripViewPager.setCurrentItem((navigationStripViewPager.getCurrentItem()) + 1);
                if (bearingIconPlugin != null)
                    bearingIconPlugin.setBearingLayerVisibility(true);
                setNavigationPadding(false);
                fixPreviewNavigationMarker();
            }
        }

    }

    private void fixPreviewNavigationMarker() {
        try {
            if (mapplsMap != null) {
                if (camera != null)
                    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);

            }
            List<NavigationStep> adviseArrayList = MapplsNavigationHelper.getInstance().getNavigationSteps();
            NavigationStep currentRouteDirectionInfo = adviseArrayList.get(navigationStripViewPager.getCurrentItem());

            NavLocation location = currentRouteDirectionInfo.getNavLocation();

            if (mapplsMap != null && location != null) {
                LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition.Builder builder = new CameraPosition.Builder()
                        .target(position)
                        .zoom(mapplsMap.getMaxZoomLevel())
                        .tilt(0);
                mapplsMap.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), 2000);


                NavLocation location1 = adviseArrayList.get(navigationStripViewPager.getCurrentItem()).getNavLocation();

                if (location1 != null) {
                    float angle = getLocationAngle(location1);
                    if (bearingIconPlugin != null) {
                        bearingIconPlugin.setBearingIcon(angle,
                                new LatLng(location1.getLatitude(), location1.getLongitude()));
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void setProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {

    }

    @Override
    public void removeProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {

    }

    class MyLocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mFragmentTransactionSave && getActivity() != null)
                getActivity().onBackPressed();

        }
    }

}