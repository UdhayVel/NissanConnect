package com.mappls.app.navigation.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mappls.app.navigation.demo.fragment.HomeFragment;
import com.mappls.app.navigation.demo.fragment.NavigationFragment;
import com.mappls.app.navigation.demo.maps.plugins.BearingIconPlugin;
import com.mappls.app.navigation.demo.maps.plugins.DirectionPolylinePlugin;
import com.mappls.sdk.direction.ui.DirectionFragment;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
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
import com.mappls.sdk.maps.location.permissions.PermissionsListener;
import com.mappls.sdk.maps.location.permissions.PermissionsManager;
import com.mappls.sdk.navigation.NavLocation;
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


public class HomeActivity extends BaseActivity implements MapplsMap.OnMapLongClickListener,
        FragmentManager.OnBackStackChangedListener,
        LocationEngineCallback<LocationEngineResult>,
        PermissionsListener {


    public static int DEFAULT_PADDING;
    public static int DEFAULT_BOTTOM_PADDING;
    public MapplsMap mapplsMap;
    boolean isVisible = false;
    Handler backStackHandler = new Handler();
    //    bearing plugin
    BearingIconPlugin _bearingIconPlugin;
    private NavApplication app;
    private LocationEngine locationEngine;
    private PermissionsManager permissionsManager;
    //location layer plugin
    private DirectionPolylinePlugin directionPolylinePlugin;
    private boolean firstFix;
    private Fragment currentFragment;
    final int PERMISSION_REQUEST_CODE =112;
    Runnable backStackRunnable = () -> {
        try {
            onBackStackChangedWithDelay();
        } catch (Exception e) {
            Timber.e(e);
        }
    };

    @SuppressLint("RestrictedApi")
    private void setupUI() {
        FloatingActionButton floatingActionButton = findViewById(R.id.move_to_current_location);
        floatingActionButton.setVisibility(View.VISIBLE);
    }

    public NavApplication getMyApplication() {
        return ((NavApplication) getApplication());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("1")){
//                getNotificationPermission();
                checkPermission();
            }
        }
        try {
            app = getMyApplication();
            DEFAULT_PADDING = (int) getResources().getDimension(R.dimen.default_map_padding);
            DEFAULT_BOTTOM_PADDING = (int) getResources().getDimension(R.dimen.default_map_bottom_padding);

            getSupportFragmentManager().addOnBackStackChangedListener(this);
            setupUI();
        } catch (Exception e) {
            //ignore
        }

        this.navigateTo(new HomeFragment(), true);


    }




    @Override
    public void onSuccess(LocationEngineResult locationEngineResult) {
        if(locationEngineResult != null) {
            Location location = locationEngineResult.getLastLocation();
            Timber.i("onLocationChanged");
            try {
                if (location == null || location.getLatitude() <= 0)
                    return;

//                Fragment fragment = getSupportFragmentManager().findFragmentByTag(RouteFragment.class.getSimpleName());
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(DirectionFragment.class.getSimpleName());
                if (!firstFix && (fragment == null || !fragment.isVisible())) {
                    mapplsMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16), 500);
                    firstFix = true;
                    if (currentFragment != null) {

                        ((HomeFragment) currentFragment).getReverseGeoCode(location.getLatitude(), location.getLongitude());
                    }
                }
                NavigationContext.getNavigationContext().setCurrentLocation(location);
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }

    public DirectionPolylinePlugin getDirectionPolylinePlugin() {
        return directionPolylinePlugin;
    }


    public MapView getMapView() {
        return mapView;
    }

    public NavLocation getUserLocation() {
        if ( NavigationContext.getNavigationContext().getCurrentLocation() != null) {
            NavLocation loc = new NavLocation("router");
            loc.setLatitude( NavigationContext.getNavigationContext().getCurrentLocation().getLatitude());
            loc.setLongitude( NavigationContext.getNavigationContext().getCurrentLocation().getLongitude());
            return loc;
        } else {
            return null;
        }
    }

    public void clearPOIs() {
        try {
            if (mapplsMap == null)
                return;
            mapplsMap.removeAnnotations();
            if (directionPolylinePlugin != null)
                directionPolylinePlugin.removeAllData();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
        Toast.makeText(this, "Long clicked", Toast.LENGTH_SHORT).show();
        if (fragmentOnTopOfStack() instanceof HomeFragment) {
            if (latLng.getLatitude() > 0) {
                getReverseGeoCode(latLng);
            }
        }
        return false;
    }

    Fragment fragmentOnTopOfStack() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        if (index >= 0) {
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            String tag = backEntry.getName();
            return getSupportFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    Fragment getFragmentOnTopOfBackStack() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        if (index >= 0) {
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            String tag = backEntry.getName();
            Timber.i(tag, " fragment Tag");
            return getSupportFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    private void onBackStackChangedWithDelay() {
        currentFragment = getFragmentOnTopOfBackStack();
//        if (!((currentFragment instanceof RouteFragment) || (currentFragment instanceof NavigationFragment)) && directionPolylinePlugin != null) {
        if (!((currentFragment instanceof NavigationFragment)) && directionPolylinePlugin != null) {
            directionPolylinePlugin.removeAllData();
            directionPolylinePlugin.setEnabled(false);
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }

    }


    @Override
    public void onBackStackChanged() {
        backStackHandler.removeCallbacksAndMessages(null);
        backStackHandler.postDelayed(backStackRunnable, 100);
    }


    public void getReverseGeoCode(LatLng latLng) {

        showProgress();
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

                    if(mapplsMap != null) {
                        mapplsMap.addMarker(new MarkerOptions().position(new LatLng(place.getLat(), place.getLng())));
                    }

                    eLocation.placeAddress = getString(R.string.point_on_map);
                    app.setELocation(eLocation);
                    if (currentFragment != null) {
                        try {
                            ((HomeFragment) currentFragment).showInfoOnLongClick(eLocation);
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
                hideProgress();
            }

            @Override
            public void onError(int i, String s) {

                hideProgress();
                Toast.makeText(HomeActivity.this, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onMapError(int i, String s) {
        Timber.d("map loading failed with code " + i + " and message " + s);
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(Style style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            LocationComponentOptions options = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .accuracyAlpha(0f)
                    .accuracyColor(ContextCompat.getColor(this, R.color.accuracy_green))
                    .build();

            // Get an instance of the component
            LocationComponent locationComponent = mapplsMap.getLocationComponent();

            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
                    .locationComponentOptions(options)
                    .build();
            // Activate with options
            locationComponent.activateLocationComponent(activationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);
            locationEngine = locationComponent.getLocationEngine();
            LocationEngineRequest request = new LocationEngineRequest.Builder(1000)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(100).build();
            assert locationEngine != null;
            locationEngine.requestLocationUpdates(request, this, getMainLooper());
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);


        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    public MapplsMap getMapboxMap() {
        if (mapplsMap != null)
            return mapplsMap;
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            if(mapplsMap != null) {
                mapplsMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });
            }
        }
        else{
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }
    public void getNotificationPermission(){
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }catch (Exception e){

        }
    }
    public  void checkNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
////                sendNotification(this);
//            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->{
//                Snackbar.make(
//                                findViewById(R.id.parent_layout),
//                                "The user denied the notifications ):",
//                                Snackbar.LENGTH_LONG
//                        )
//                        .setAction("Settings") {
//                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    val uri:Uri =
//                            Uri.fromParts("com.onesilisondiode.geeksforgeeks", packageName, null)
//                    intent.data = uri
//                    startActivity(intent)
//                }
//                    .show()
//            }
//            else ->{
//                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//            }
//        }
    }
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void checkPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED&&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
                if(mapplsMap != null) {
                    mapplsMap.getStyle(new Style.OnStyleLoaded() {
                        @Override
                        public void onStyleLoaded(@NonNull Style style) {
                            enableLocationComponent(style);
                        }
                    });
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS,}, 1);
            }
            }
        }catch(Exception e){}
    }
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//            backStackHandler.postDelayed(backStackRunnable, 100);
            if(mapplsMap != null) {
                mapplsMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });
            }
        } else {

                checkPermission();

        }

    }



    @Override
    public void onMapReady(MapplsMap map) {
        if (map == null)
            return;
        this.mapplsMap = map;
//        mapplsMap.getUiSettings().setLogoMargins(30,0,0,100);
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                mapplsMap.enableTraffic(true);
                directionPolylinePlugin = new DirectionPolylinePlugin(mapView, map);
                enableLocationComponent(style);

                _bearingIconPlugin = new BearingIconPlugin(mapView, mapplsMap);
                mapplsMap.setMaxZoomPreference(18.5);
                mapplsMap.setMinZoomPreference(4);

                mapplsMap.addOnMapLongClickListener(HomeActivity.this);
                setCompassDrawable();
            }
        });



    }

    public void setCompassDrawable() {
        mapView.getCompassView().setBackgroundResource(R.drawable.compass_background);
        assert mapplsMap.getUiSettings() != null;
        mapplsMap.getUiSettings().setCompassImage(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.compass_north_up)));
        int padding = dpToPx(8);
        int elevation = dpToPx(8);
        mapView.getCompassView().setPadding(padding, padding, padding, padding);
        ViewCompat.setElevation(mapView.getCompassView(), elevation);
        mapplsMap.getUiSettings().setCompassMargins(dpToPx(20), dpToPx(100), dpToPx(20), dpToPx(20));
    }

    public int dpToPx(final float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
