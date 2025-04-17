package com.mappls.app.navigation.demo.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mappls.app.navigation.demo.HomeActivity;
import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.NavigationActivity;
import com.mappls.app.navigation.demo.NavigationUiActivity;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.viewmodel.RouteViewModel;
import com.mappls.sdk.direction.ui.DirectionCallback;
import com.mappls.sdk.direction.ui.DirectionFragment;
import com.mappls.sdk.direction.ui.model.DirectionOptions;
import com.mappls.sdk.direction.ui.model.DirectionPoint;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.camera.CameraMapplsPinUpdateFactory;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.navigation.NavLocation;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.navigation.data.WayPoint;
import com.mappls.sdk.navigation.model.NavigationResponse;
import com.mappls.sdk.navigation.ui.navigation.MapplsNavigationViewHelper;
import com.mappls.sdk.navigation.util.ErrorType;
import com.mappls.sdk.plugins.places.autocomplete.model.MapplsFavoritePlace;
import com.mappls.sdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mappls.sdk.plugins.places.autocomplete.ui.PlaceAutocompleteFragment;
import com.mappls.sdk.plugins.places.autocomplete.ui.PlaceSelectionListener;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.Place;
import com.mappls.sdk.services.api.PlaceResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.directions.DirectionsCriteria;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCode;
import com.mappls.sdk.services.api.reversegeocode.MapplsReverseGeoCodeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class HomeFragment extends Fragment {

    private BottomSheetBehavior sheetBehavior;
    private TextView textViewPutRouteName;
    private RouteViewModel viewModel;
    private EditText searchEditText;
    private String fromLocation;
    private ELocation eLocation;
    private NavApplication app;
    private DirectionPoint currentLocationPoint;


    public HomeFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(RouteViewModel.class);
        app = getMyApplication();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    public NavApplication getMyApplication() {

        if (getActivity() != null)
            return ((NavApplication) getActivity().getApplication());
        else
            return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.edit_text_search);
        LinearLayout layoutBottomSheet = view.findViewById(R.id.bottom_sheet);

        Button textViewGetRoute = view.findViewById(R.id.text_view_get_route);
        textViewPutRouteName = view.findViewById(R.id.text_view_route_name);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);

        view.findViewById(R.id.search_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = NavigationContext.getNavigationContext().getCurrentLocation();
                if (location == null) {
                    Toast.makeText(v.getContext(), "Please wait before getting current location", Toast.LENGTH_SHORT).show();
                    return;
                }
                PlaceOptions placeOptions = PlaceOptions.builder()
                        .saveHistory(true)
                        .userAddedLocationEnable(true)
                        .historyCount(5)
                        .enableTextSearch(true)
                        .location(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
                        .backgroundColor(ContextCompat.getColor(getContext(), R.color.cardview_dark_background))
                        .build(PlaceOptions.MODE_CARDS);

                PlaceAutocompleteFragment fragment = PlaceAutocompleteFragment.newInstance(placeOptions);
                fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(ELocation eLocation) {
                        if (eLocation != null) {
                            searchEditText.setText(eLocation.placeName);
                            showInfoOnLongClick(eLocation);
                            app.setELocation(eLocation);
                            if (getActivity() != null && ((HomeActivity) getActivity()).mapplsMap != null) {
                                if (eLocation.latitude != null && eLocation.longitude != null) {
                                    LatLng selectedLocationLatlng = new LatLng(eLocation.latitude, eLocation.longitude);
                                    ((HomeActivity) getActivity()).mapplsMap.addMarker(new MarkerOptions().position(selectedLocationLatlng));
                                    ((HomeActivity) getActivity()).mapplsMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocationLatlng, 14));
                                } else {
                                    ((HomeActivity) getActivity()).mapplsMap.addMarker(new MarkerOptions().mapplsPin(eLocation.getMapplsPin()));
                                    ((HomeActivity) getActivity()).mapplsMap.animateCamera(CameraMapplsPinUpdateFactory.newMapplsPinZoom(eLocation.getMapplsPin(), 14));
                                }
                            }
                        } else {
                            showHideBottomSheet(false);
                        }
                        ((HomeActivity) getActivity()).popToBackStack(fragment);
                    }

                    @Override
                    public void onFavoritePlaceSelected(MapplsFavoritePlace mapplsFavoritePlace) {

                    }


                    @Override
                    public void onCancel() {

                        ((HomeActivity) getActivity()).popToBackStack(fragment);
                    }

                    @Override
                    public void requestForCurrentLocation() {

                    }
                });
                ((HomeActivity) getActivity()).addFragment(fragment, true);
            }
        });


        textViewGetRoute.setOnClickListener(this::onClick2);

    }


    private void getDirections(ELocation eLocation) {
        if (getActivity() == null)
            return;
        try {
            Location location = ((HomeActivity) getActivity()).getMapboxMap().getLocationComponent().getLastKnownLocation();

            if (location != null) {
                if (fromLocation != null)
//                    ((HomeActivity) getActivity()).navigateTo(RouteFragment.newInstance(eLocation, fromLocation), true);
//                    ((HomeActivity) getActivity()).navigateTo(DirectionWidgetFragment.newInstance(eLocation, fromLocation), true);
                    openDirectionWidget(eLocation);
                else {
                    Toast.makeText(getActivity(), R.string.current_location_not_available, Toast.LENGTH_SHORT).show();
                    getReverseGeoCode(location.getLatitude(), location.getLongitude());
                }
            } else {
                Toast.makeText(getActivity(), R.string.current_location_not_available, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void openDirectionWidget(ELocation eLocation) {
        showHideBottomSheet(false);
        List<String> annotations = new ArrayList<String>();
        annotations.add(DirectionsCriteria.ANNOTATION_CONGESTION);
        annotations.add(DirectionsCriteria.ANNOTATION_NODES);
        annotations.add(DirectionsCriteria.ANNOTATION_DURATION);
        annotations.add(DirectionsCriteria.ANNOTATION_BASE_DURATION);
        annotations.add(DirectionsCriteria.ANNOTATION_SPEED_LIMIT);
        DirectionOptions.Builder optionsBuilder = DirectionOptions.builder();
        if(eLocation.longitude != null && eLocation.latitude != null) {
            optionsBuilder.destination(DirectionPoint.setDirection(Point.fromLngLat(eLocation.longitude, eLocation.latitude), eLocation.placeName, eLocation.placeAddress));
        } else {
            optionsBuilder.destination(DirectionPoint.setDirection(eLocation.mapplsPin, eLocation.placeName, eLocation.placeAddress));
        }
        optionsBuilder.annotation(annotations);
        optionsBuilder.showAlternative(true);
        optionsBuilder.resource(DirectionsCriteria.RESOURCE_ROUTE_ETA)
                .steps(true);
        optionsBuilder.showDefaultMap(false);


        DirectionFragment directionFragment = DirectionFragment.newInstance(optionsBuilder.build());
        ((HomeActivity) getActivity()).addFragment(directionFragment, true);
        directionFragment.provideMapView((( HomeActivity)getActivity()).getMapView());

        directionFragment.setDirectionCallback(new DirectionCallback() {
            @Override
            public void onCancel() {
//                if (getActivity() != null)
//                    getActivity().finish();
                ((HomeActivity) getActivity()).popToBackStack(directionFragment);
            }

            @Override
            public void onStartNavigation(DirectionPoint directionPoint, DirectionPoint directionPoint1,
                                          List<DirectionPoint> list, DirectionsResponse directionsResponse,
                                          int i) {
                currentLocationPoint = directionPoint;
                viewModel.setSelectedIndex(i);
                viewModel.setTrip(directionsResponse);
                app.setTrip(viewModel.getTrip());

                ELocation eLocationDestination = new ELocation();
                eLocationDestination.placeName = directionPoint1.getPlaceName();
                eLocationDestination.placeAddress = directionPoint1.getPlaceAddress();
                eLocationDestination.latitude = directionPoint1.getLatitude();
                eLocationDestination.longitude = directionPoint1.getLongitude();
                eLocationDestination.mapplsPin = directionPoint1.getMapplsPin();
                viewModel.seteLocation(eLocationDestination);
                List<ELocation> eLocations = new ArrayList<>();
                for(DirectionPoint viaPointsDirection: list) {
                    ELocation eLocationViaPoint = new ELocation();
                    eLocationViaPoint.placeName = viaPointsDirection.getPlaceName();
                    eLocationViaPoint.placeAddress = viaPointsDirection.getPlaceAddress();
                    eLocationViaPoint.latitude = viaPointsDirection.getLatitude();
                    eLocationViaPoint.longitude = viaPointsDirection.getLongitude();
                    eLocationViaPoint.mapplsPin = viaPointsDirection.getMapplsPin();

                    eLocations.add(eLocationViaPoint);
                }
                viewModel.seteLocations(eLocations);

//                eLocationDestination.entryLatitude = directionPoint1.getMapplsPin();
                Toast.makeText(getActivity(), "On Navigation Start", Toast.LENGTH_SHORT).show();
                Dialog dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.custom_dialog_layout_button);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.setCancelable(true);
                dialog.getWindow().getAttributes().windowAnimations = R.style.animation;

                LinearLayout customUi = dialog.findViewById(R.id.linear_layout_navigation_custom);
                LinearLayout navigationUi = dialog.findViewById(R.id.linear_layout_navigation_Ui);

                customUi.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        dialog.dismiss();
//                        Toast.makeText(MainActivity.this, "okay clicked", Toast.LENGTH_SHORT).show();
                        startCustomNavigation();

                    }
                });

                navigationUi.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        dialog.dismiss();
//                        Toast.makeText(MainActivity.this, "Cancel clicked", Toast.LENGTH_SHORT).show();
                        startNavigationUi();
                    }
                });

                dialog.show();
            }
        });
    }

    private void startCustomNavigation() {
        Toast.makeText(getActivity(), "Custom Navigation Ui", Toast.LENGTH_SHORT).show();
        if (getActivity() == null)
            return;
        NavLocation location = ((HomeActivity) getActivity()).getUserLocation();
        if (location == null)
            return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {

                NavigationResponse result =  navigationBackgroundOperation();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //UI Thread work here
                        if (getActivity() == null)
                            return;
//                        hideProgress();
                        if (result != null && result.getError() != null) {
                            Timber.d(result.toString());

                            Toast.makeText(app, result.getError().errorMessage + "", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        getActivity().startActivity(new Intent(getActivity(), NavigationActivity.class));
                    }
                });
            }
        });
    }
    private void startNavigationUi(){
        Toast.makeText(getActivity(), "Navigation Ui", Toast.LENGTH_SHORT).show();
        if (getActivity() == null)
            return;
        NavLocation location = ((HomeActivity) getActivity()).getUserLocation();
        if (location == null)
            return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {

                NavigationResponse result =  navigationBackgroundOperation();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //UI Thread work here
                        if (getActivity() == null)
                            return;
//                        hideProgress();
                        if (result != null && result.getError() != null) {
                            Timber.d(result.toString());

                            Toast.makeText(app, result.getError().errorMessage + "", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        getActivity().startActivity(new Intent(getActivity(), NavigationUiActivity.class));
                    }
                });
            }
        });


    }
    private NavigationResponse  navigationBackgroundOperation(){
        try {

            LatLng currentLocation = null;
            NavLocation location = ((HomeActivity) requireActivity()).getUserLocation();
            if (location != null)
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            NavLocation navLocation = new NavLocation("navigation");

            Point position = Point.fromLngLat(currentLocationPoint.getLatitude(),currentLocationPoint.getLongitude());
            LatLng point = new LatLng(position.latitude(), position.longitude());
            navLocation.setLongitude(point.getLongitude());
            navLocation.setLatitude(point.getLatitude());
            app.setStartNavigationLocation(navLocation);
            if (currentLocation == null)
                return new NavigationResponse(ErrorType.UNKNOWN_ERROR, null);

            List<WayPoint> wayPoints;
            if (viewModel.geteLocations() == null) {
                wayPoints = new ArrayList<>();
            } else {
                if (viewModel.geteLocations().size() > 0) {
                    wayPoints = new ArrayList<>();
                    for (ELocation eLocation : viewModel.geteLocations()) {
                        wayPoints.add(getNavigationGeoPoint(eLocation));

                    }
                } else {
                    wayPoints = new ArrayList<>();
                }
            }

            MapplsNavigationViewHelper.getInstance().setStartLocation(navLocation);
            MapplsNavigationViewHelper.getInstance().setDestination(app.getELocation());
            MapplsNavigationViewHelper.getInstance().setWayPoints(wayPoints);
            return MapplsNavigationHelper.getInstance().startNavigation(app.getTrip(), viewModel.getSelectedIndex(), currentLocation,
                    getNavigationGeoPoint(viewModel.geteLocation()), wayPoints);

        } catch (Exception e) {
            Timber.e(e);
            return new NavigationResponse(ErrorType.UNKNOWN_ERROR, null);
        }
    }
    public WayPoint getNavigationGeoPoint(ELocation eLocation) {
        try {
            if (eLocation.entryLatitude != null && eLocation.entryLongitude != null && eLocation.entryLatitude > 0 && eLocation.entryLongitude > 0)
                return new WayPoint(eLocation.entryLatitude, eLocation.entryLongitude, eLocation.placeName, eLocation.placeName);
            else if(eLocation.latitude != null && eLocation.longitude != null) {
                return new WayPoint(eLocation.latitude, eLocation.longitude, eLocation.placeName, eLocation.placeName);
            } else {
                return new WayPoint(eLocation.getMapplsPin(), eLocation.placeName, eLocation.placeName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new WayPoint(0, 0, null, null);
        }
    }
    private WayPoint getWayPoints(ELocation eLocation) {
        try {
            return new WayPoint(eLocation.latitude, eLocation.longitude, eLocation.placeName, eLocation.placeName);
        } catch (Exception e) {
            return new WayPoint(0, 0, null, null);
        }
    }
    private void showHideBottomSheet(boolean showHide) {
        if (showHide) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }


    public void getReverseGeoCode(Double latitude, Double longitude) {

        MapplsReverseGeoCode reverseGeoCode = MapplsReverseGeoCode.builder()
                .setLocation(latitude, longitude)
                .build();
        MapplsReverseGeoCodeManager.newInstance(reverseGeoCode).call(new OnResponseCallback<PlaceResponse>() {
            @Override
            public void onSuccess(PlaceResponse placeResponse) {
                if (placeResponse != null) {
                    List<Place> placesList = placeResponse.getPlaces();
                    Place place = placesList.get(0);
                    fromLocation = place.getFormattedAddress();

                }
            }

            @Override
            public void onError(int i, String s) {
                Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showInfoOnLongClick(ELocation eLocation) {
        this.eLocation = eLocation;
        textViewPutRouteName.setText(eLocation.placeName);
        showHideBottomSheet(true);
    }

    private void onClick2(View v) {
        getDirections(eLocation);

    }

    public interface TextSearchListener {
        void showProgress();

        void hideProgress();
    }


}
