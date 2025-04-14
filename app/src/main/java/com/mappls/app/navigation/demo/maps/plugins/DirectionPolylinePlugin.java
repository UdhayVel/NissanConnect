package com.mappls.app.navigation.demo.maps.plugins;

import static com.mappls.sdk.maps.style.expressions.Expression.get;
import static com.mappls.sdk.maps.style.expressions.Expression.interpolate;
import static com.mappls.sdk.maps.style.expressions.Expression.literal;
import static com.mappls.sdk.maps.style.expressions.Expression.match;
import static com.mappls.sdk.maps.style.expressions.Expression.stop;
import static com.mappls.sdk.maps.style.expressions.Expression.string;
import static com.mappls.sdk.maps.style.expressions.Expression.toColor;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.iconAnchor;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.iconImage;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.iconOffset;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.iconPitchAlignment;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.lineCap;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.lineColor;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.lineJoin;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.lineOffset;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.lineOpacity;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.lineWidth;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.symbolPlacement;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.symbolSortKey;
import static com.mappls.sdk.maps.style.layers.PropertyFactory.visibility;
import static com.mappls.sdk.maps.utils.BitmapUtils.getBitmapFromDrawable;
import static com.mappls.sdk.maps.utils.ColorUtils.colorToRgbaString;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.mappls.app.navigation.demo.R;
import com.mappls.sdk.geojson.Feature;
import com.mappls.sdk.geojson.FeatureCollection;
import com.mappls.sdk.geojson.LineString;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.Style;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.style.expressions.Expression;
import com.mappls.sdk.maps.style.layers.Layer;
import com.mappls.sdk.maps.style.layers.LineLayer;
import com.mappls.sdk.maps.style.layers.Property;
import com.mappls.sdk.maps.style.layers.PropertyFactory;
import com.mappls.sdk.maps.style.layers.SymbolLayer;
import com.mappls.sdk.maps.style.sources.GeoJsonSource;
import com.mappls.sdk.maps.style.sources.Source;
import com.mappls.sdk.services.api.directions.DirectionsCriteria;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;
import com.mappls.sdk.services.api.directions.models.RouteLeg;
import com.mappls.sdk.services.utils.CongestionDelayInfo;
import com.mappls.sdk.services.utils.Constants;
import com.mappls.sdk.services.utils.MapplsUtils;
import com.mappls.sdk.turf.TurfConstants;
import com.mappls.sdk.turf.TurfMeasurement;
import com.mappls.sdk.turf.TurfMisc;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import timber.log.Timber;


public final class DirectionPolylinePlugin implements MapView.OnDidFinishLoadingStyleListener, MapplsMap.OnMapClickListener {
    private final static String CONSTANT_START = "start_marker";
    private final static String CONSTANT_END = "end_marker";
    private final static String CONSTANT_START_POINT = "start_point_marker";
    private final static String CONSTANT_END_POINT = "end_point_marker";
    private final static String CONSTANT_WAYPOINTS = "via_points";
    private final static String CONSTANT_TRACK_POINTS = "track_points";
    private static final String FILTER_TEXT = "direction_type";
    private static final String POSITION_TEXT = "position_text";
    private static final String CONGESTION_LAYER = "congestion_layer";
    private final static String SEGMENT_SORT_KEY_DELAY = "segment-delay_marker-sort-key";
    private final static String CONSTANT_SEGMENT_DELAY = "segment-delay_marker";

    List<Point> curvedPathStart = new ArrayList<>();
    List<Point> curvedPathEnd = new ArrayList<>();
    boolean bearingVisibility = false;
    float bearing = 0f;
    LatLng position;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Handler traversedHandler = new Handler(Looper.getMainLooper());
    private MapplsMap mapplsMap;
    private MapView mMapView;
    private List<String> layerIds;
    private boolean enabled = false;
    private LineString traversed;
    Runnable updateTraversedPolylineStateRunnable = new Runnable() {
        @Override
        public void run() {
            updateTraversedPolylineStates();
        }
    };
    private List<LineString> trips;
    private LatLng startLatLng = null;
    private LatLng endLatLng = null;
    private List<LatLng> wayPoints;
    private List<DirectionsRoute> directionsRouteList = null;
    private int selected = 0;
    private OnNewRouteSelectedListener onNewRouteSelectedListener;
    private boolean enableCongestion = false;
    private String profile = DirectionsCriteria.PROFILE_DRIVING;
    Runnable updatePolylineStateRunnable = new Runnable() {
        @Override
        public void run() {
            updateSelectedPolylineStates();
        }
    };


    /**
     * Create a directions plugin.
     *
     * @param mapView   the MapView to apply the directions plugin to
     * @param mapplsMap the MapboxMap to apply directions plugin with
     */
    public DirectionPolylinePlugin(@NonNull MapView mapView, @NonNull MapplsMap mapplsMap) {
        this.mapplsMap = mapplsMap;
        this.mMapView = mapView;
        updateState();
        updateTraversedState();
        mapView.addOnDidFinishLoadingStyleListener(this);
        mapplsMap.addOnMapClickListener(this);
    }

    public boolean isEnableCongestion() {
        return enableCongestion;
    }

    public void setEnableCongestion(boolean enableCongestion) {
        this.enableCongestion = enableCongestion;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setCurrentLocation(Location location) {
        if (trips != null && trips.size() > 0 && trips.get(0) != null) {
            Point point = trips.get(0).coordinates().get(0);
            if (point.latitude() != location.getLatitude() || point.longitude() != location.getLongitude()) {
                traversed = TurfMisc.lineSlice(point, Point.fromLngLat(location.getLongitude(), location.getLatitude()), trips.get(0));
                updateTraversedPolylineStatus();
            }
        }
    }

    /**
     * Returns true if the directions plugin is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            updateState();
        }
    }

    //Alternative route click listener
    public void setOnNewRouteSelectedListener(OnNewRouteSelectedListener onNewRouteSelectedListener) {
        this.onNewRouteSelectedListener = onNewRouteSelectedListener;
    }

    public void removePolylineClickListener() {
        this.onNewRouteSelectedListener = null;
    }

    public void showSelectedOnly(boolean showSelected) {
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                for (String layerID : layerIds) {
                    Layer layer = style.getLayer(layerID);
                    if (layer != null) {
                        if (layer.getId().equalsIgnoreCase("route_alternate") || layer.getId().equalsIgnoreCase("route_alternate_case")) {
                            layer.setProperties(visibility(showSelected ? Property.NONE : Property.VISIBLE));
                            layer.setProperties(lineOpacity(showSelected ? 0.0f : 1.0f));
                        }
                    }
                }
            }
        });

    }

    /**
     * Toggles the directions plugin state.
     * <p>
     * If the directions plugin wasn't initialised yet, directions source and layers will be added to the current map style.
     * Else visibility will be toggled based on the current state.
     * </p>
     */
    public void toggle() {
        enabled = !enabled;
        updateState();
    }


    @Override
    public void onDidFinishLoadingStyle() {
        updateState();
        updateTraversedState();
        if (isEnabled())
            updatePolylineStatus();
    }


    private void updateTraversedState() {
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                Source source = style.getSource(DirectionPolylineData.TRAVERSED_SOURCE_ID);
                if (source == null) {
                    initialiseTraversed(style);
                    return;
                }
            }
        });


    }

    private void initialiseTraversed(Style style) {
        if (style.getSource(DirectionPolylineData.TRAVERSED_SOURCE_ID) == null) {
            GeoJsonSource traversedSource = new GeoJsonSource(DirectionPolylineData.TRAVERSED_SOURCE_ID);
            style.addSource(traversedSource);
        }

        addTraversedPathLayer(style);
    }

    /**
     * Update the state of the directions plugin.
     */
    private void updateState() {
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                Source source = style.getSourceAs(DirectionPolylineData.SOURCE_ID);
                if (source == null) {
                    initialise(style);
                    return;
                }
                setVisibility(enabled, style);
            }
        });
    }

    /**
     * Initialise the directions source and layers.
     *
     * @param style
     */
    private void initialise(Style style) {
        layerIds = new ArrayList<>();
        addDirectionPolylineSource(style);
        addDirectionsLayer(style);
        addImagesToMap(style);
    }

    /**
     * Adds directions source to the map.
     *
     * @param style
     */
    private void addDirectionPolylineSource(Style style) {
        GeoJsonSource trafficSource = new GeoJsonSource(DirectionPolylineData.SOURCE_ID);
        style.addSource(trafficSource);
    }

    /**
     * Adds directions layers to the map.
     *
     * @param style
     */
    private void addDirectionsLayer(Style style) {
        try {
            addSelectedPathLayer(style);
            addAlternatePathLayer(style);
            addDottedLayer(style);
            addAlternateDottedLayer(style);
            addCongestionLayer(style);
            addLinePointSymbolLayer(style);
            addDurationSegmentLayer(style);
            addSymbolLayer(style);
        } catch (Exception exception) {
            Timber.e("Unable to attach directions Layers to current style.");
        }
    }

    private void addDurationSegmentLayer(Style style) {
        SymbolLayer symbolLayer = new SymbolLayer(DirectionsSegmentSymbolLayer.BASE_LAYER_ID, DirectionPolylineData.SOURCE_ID)
                .withProperties(iconImage("{icon}"),
                        iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT),
                        symbolSortKey(get(SEGMENT_SORT_KEY_DELAY)),
                        iconOffset(new Float[]{0f, 2.0f}));
        symbolLayer.withFilter(DirectionsSegmentSymbolLayer.FILTER);
        symbolLayer.setSourceLayer(DirectionPolylineData.SOURCE_LAYER);
        style.addLayer(symbolLayer);
        layerIds.add(symbolLayer.getId());
    }

    private void addLinePointSymbolLayer(Style style) {
        SymbolLayer symbolLayer = new SymbolLayer(DirectionPointSymbolLayer.BASE_LAYER_ID, DirectionPolylineData.SOURCE_ID)
                .withProperties(iconImage("{icon}"),
                        iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                        iconAllowOverlap(true),
//                        iconSize(1.1f),
                        iconIgnorePlacement(false));
        symbolLayer.withFilter(DirectionPointSymbolLayer.FILTER);
        symbolLayer.setSourceLayer(DirectionPolylineData.SOURCE_LAYER);
        style.addLayer(symbolLayer);
        layerIds.add(symbolLayer.getId());
    }


    private void addDottedLayer(Style style) {
        Expression symbolSpacing = Expression.interpolate(
                Expression.exponential(1f), Expression.zoom(),
                Expression.stop(1, 1f),
                Expression.stop(2, 1f),
                Expression.stop(3, 1f),
                Expression.stop(4, 1f),
                Expression.stop(5, 1f),
                Expression.stop(6, 2f),
                Expression.stop(7, 3f),
                Expression.stop(8, 4f),
                Expression.stop(9, 5f),
                Expression.stop(10, 6f),
                Expression.stop(22, 30f)
        );

        SymbolLayer symbolLayer = new SymbolLayer(SelectedDottedRoute.BASE_LAYER_ID, DirectionPolylineData.SOURCE_ID)
                .withProperties(
                        iconImage(SelectedDottedRoute.ICON_IMAGE),
                        iconIgnorePlacement(true),
                        symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
                        PropertyFactory.symbolSpacing(symbolSpacing),
                        iconAllowOverlap(true)
                )
                .withFilter(SelectedDottedRoute.FILTER);
        String lastAddedLayerId = layerIds.get(layerIds.size() - 1);
        if (lastAddedLayerId != null && style.getLayer(lastAddedLayerId) != null) {
            style.addLayerAbove(symbolLayer, lastAddedLayerId);
        } else {
            style.addLayer(symbolLayer);
        }
        layerIds.add(symbolLayer.getId());
    }

    private void addAlternateDottedLayer(Style style) {
        Expression symbolSpacing = Expression.interpolate(
                Expression.exponential(1f), Expression.zoom(),
                Expression.stop(1, 1f),
                Expression.stop(2, 1f),
                Expression.stop(3, 1f),
                Expression.stop(4, 1f),
                Expression.stop(5, 1f),
                Expression.stop(6, 2f),
                Expression.stop(7, 3f),
                Expression.stop(8, 4f),
                Expression.stop(9, 5f),
                Expression.stop(10, 6f),
                Expression.stop(22, 30f)
        );

        SymbolLayer symbolLayer = new SymbolLayer(AlternateDottedRoute.BASE_LAYER_ID, DirectionPolylineData.SOURCE_ID)
                .withProperties(
                        iconImage(AlternateDottedRoute.ICON_IMAGE),
                        iconIgnorePlacement(true),
                        symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
                        PropertyFactory.symbolSpacing(symbolSpacing),
                        iconAllowOverlap(true)
                )
                .withFilter(AlternateDottedRoute.FILTER);
        String lastAddedLayerId = layerIds.get(layerIds.size() - 1);
        if (lastAddedLayerId != null && style.getLayer(lastAddedLayerId) != null) {
            style.addLayerAbove(symbolLayer, lastAddedLayerId);
        } else {
            style.addLayer(symbolLayer);
        }
        layerIds.add(symbolLayer.getId());
    }

    private void addCongestionLayer(Style style) {
        Expression FUNCTION_LINE_WIDTH = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 4f),
                Expression.stop(10f, 6.5f),
                Expression.stop(13f, 7f),
                Expression.stop(16f, 9f),
                Expression.stop(19f, 14f),
                Expression.stop(22f, 18f)
        );

        LineLayer congestionLayer = new LineLayer(CONGESTION_LAYER, DirectionPolylineData.SOURCE_ID)
                .withProperties(
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineWidth(FUNCTION_LINE_WIDTH),
                        lineColor(
                                match(
                                        get("congestion"),
                                        toColor(literal(colorToRgbaString(Color.TRANSPARENT))),
                                        stop(
                                                "moderate",
                                                literal(colorToRgbaString(Color.parseColor("#ff8c1a")))
                                        ),
                                        stop(
                                                "heavy",
                                                literal(colorToRgbaString(Color.parseColor("#981b25")))
                                        ),
                                        stop(
                                                "severe",
                                                literal(colorToRgbaString(Color.parseColor("#8b0000")))
                                        )
                                )
                        )
                );

        if (style.getSource(CONGESTION_LAYER) == null) {
            style.addLayerAbove(congestionLayer, SelectedRoute.BASE_LAYER_ID);
        }

    }

    private void addSymbolLayer(Style style) {
        SymbolLayer symbolLayer = new SymbolLayer(DirectionsSymbolLayer.BASE_LAYER_ID, DirectionPolylineData.SOURCE_ID)
                .withProperties(
                        iconImage(get("icon")),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(false));
        symbolLayer.withFilter(DirectionsSymbolLayer.FILTER);
        symbolLayer.setSourceLayer(DirectionPolylineData.SOURCE_LAYER);
        style.addLayer(symbolLayer);
        layerIds.add(symbolLayer.getId());
    }


    private void addTraversedPathLayer(Style style) {
        LineLayer selectedLineLayer = DirectionsLayer.getTraversedLineLayer(
                TraversedRoute.BASE_LAYER_ID,
                TraversedRoute.ZOOM_LEVEL,
                TraversedRoute.FILTER,
                TraversedRoute.FUNCTION_LINE_COLOR,
                TraversedRoute.FUNCTION_LINE_WIDTH,
                TraversedRoute.FUNCTION_LINE_OFFSET
        );
        LineLayer selectedLineLayerCase = DirectionsLayer.getTraversedLineLayer(
                TraversedRoute.CASE_LAYER_ID,
                TraversedRoute.ZOOM_LEVEL,
                TraversedRoute.FILTER,
                TraversedRoute.FUNCTION_LINE_COLOR_CASE,
                TraversedRoute.FUNCTION_LINE_WIDTH_CASE,
                TraversedRoute.FUNCTION_LINE_OFFSET,
                TraversedRoute.FUNCTION_LINE_OPACITY_CASE
        );
        // #TODO https://github.com/mapbox/mapbox-plugins-android/issues/14
        addTrafficLayersToMap(selectedLineLayerCase, selectedLineLayer, "highway_name", style);
    }


    /**
     * Add local layer to the map.
     *
     * @param style
     */
    private void addSelectedPathLayer(Style style) {
        LineLayer selectedLineLayer = DirectionsLayer.getLineLayer(
                SelectedRoute.BASE_LAYER_ID,
                SelectedRoute.ZOOM_LEVEL,
                SelectedRoute.FILTER,
                SelectedRoute.FUNCTION_LINE_COLOR,
                SelectedRoute.FUNCTION_LINE_WIDTH,
                SelectedRoute.FUNCTION_LINE_OFFSET
        );
        LineLayer selectedLineLayerCase = DirectionsLayer.getLineLayer(
                SelectedRoute.LOCAL_CASE_LAYER_ID,
                SelectedRoute.ZOOM_LEVEL,
                SelectedRoute.FILTER,
                SelectedRoute.FUNCTION_LINE_COLOR_CASE,
                SelectedRoute.FUNCTION_LINE_WIDTH_CASE,
                SelectedRoute.FUNCTION_LINE_OFFSET,
                SelectedRoute.FUNCTION_LINE_OPACITY_CASE
        );
        // #TODO https://github.com/mapbox/mapbox-plugins-android/issues/14
        addTrafficLayersToMap(selectedLineLayerCase, selectedLineLayer, "highway_name", style);
    }

    /**
     * Add alternate layer to the map.
     *
     * @param style
     */
    private void addAlternatePathLayer(Style style) {
        LineLayer alternateLineLayer = DirectionsLayer.getLineLayer(
                AlternateRoute.BASE_LAYER_ID,
                AlternateRoute.ZOOM_LEVEL,
                AlternateRoute.FILTER,
                AlternateRoute.FUNCTION_LINE_COLOR,
                AlternateRoute.FUNCTION_LINE_WIDTH,
                AlternateRoute.FUNCTION_LINE_OFFSET
        );
        LineLayer alternateLineLayerCase = DirectionsLayer.getLineLayer(
                AlternateRoute.CASE_LAYER_ID,
                AlternateRoute.ZOOM_LEVEL,
                AlternateRoute.FILTER,
                AlternateRoute.FUNCTION_LINE_COLOR_CASE,
                AlternateRoute.FUNCTION_LINE_WIDTH_CASE,
                AlternateRoute.FUNCTION_LINE_OFFSET
        );
        addTrafficLayersToMap(alternateLineLayerCase, alternateLineLayer, getLastAddedLayerId(), style);
    }

    private void addAdvicesSymbolLayer(Style style) {
        SymbolLayer symbolLayer = new SymbolLayer(DirectionsSymbolLayer.BASE_ADVICES_LAYER_ID, DirectionPolylineData.SOURCE_ID).withProperties(iconImage(CONSTANT_TRACK_POINTS));
        symbolLayer.withFilter(DirectionsSymbolLayer.FILTER_ADVICES);
        symbolLayer.setSourceLayer(DirectionPolylineData.SOURCE_LAYER);
        style.addLayerAbove(symbolLayer, getLastAddedLayerId());
        layerIds.add(symbolLayer.getId());
    }

    /**
     * Returns the last added layer id.
     *
     * @return the id of the last added layer
     */
    private String getLastAddedLayerId() {
        return layerIds.get(layerIds.size() - 1);
    }

    /**
     * Add Layer to the map and track the id.
     *
     * @param layer        the layer to be added to the map
     * @param idBelowLayer the id of the layer above
     * @param style
     */
    private void addTrafficLayersToMap(Layer layerCase, Layer layer, String idBelowLayer, Style style) {
        if (style.getLayer(idBelowLayer) != null)
            style.addLayerBelow(layerCase, idBelowLayer);
        else
            style.addLayer(layerCase);
        style.addLayerAbove(layer, layerCase.getId());
        layerIds.add(layerCase.getId());
        layerIds.add(layer.getId());
    }

    /**
     * Add Layer to the map and track the id.
     *
     * @param layer        the layer to be added to the map
     * @param idAboveLayer the id of the layer below
     * @param style
     */
    private void addAboveTrafficLayersToMap(Layer layerCase, Layer layer, String idAboveLayer, Style style) {
        if (style.getLayer(idAboveLayer) != null)
            style.addLayerAbove(layerCase, idAboveLayer);
        else
            style.addLayer(layerCase);
        style.addLayerAbove(layer, layerCase.getId());
        layerIds.add(layerCase.getId());
        layerIds.add(layer.getId());
    }

    /**
     * Toggles the visibility of the directions layers.
     *
     * @param visible true for visible, false for none
     * @param style
     */
    private void setVisibility(boolean visible, Style style) {
        if (mapplsMap == null)
            return;
        if (layerIds == null || layerIds.size() <= 0)
            return;
        List<Layer> layers = style.getLayers();
        if (layers == null || layers.size() <= 0)
            return;
        for (Layer layer : layers) {
            if (layerIds.contains(layer.getId())) {
                layer.setProperties(visibility(visible ? Property.VISIBLE : Property.NONE));
            }
        }
        setBearingLayerVisibility(bearingVisibility, style);
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                setVisibility(true, style);
            }
        });
        updatePolylineStatus();
    }

    public void setTrips(List<LineString> trips, LatLng startLatLng, LatLng endLatLng, List<LatLng> wayPoints, List<DirectionsRoute> directionsRouteList) {
        this.startLatLng = startLatLng;
        this.endLatLng = endLatLng;
        this.wayPoints = wayPoints;
        this.trips = trips;
        this.directionsRouteList = directionsRouteList;
        this.selected = 0;
        updatePolylineStatus();
        updateTraversedPolylineStatus();
    }

    public void setTrips(List<LineString> trips, LatLng startLatLng, LatLng endLatLng, List<LatLng> wayPoints, List<DirectionsRoute> directionsRouteList, int selected) {
        this.startLatLng = startLatLng;
        this.endLatLng = endLatLng;
        this.wayPoints = wayPoints;
        this.trips = trips;
        this.directionsRouteList = directionsRouteList;
        this.selected = selected;
        updatePolylineStatus();
        updateTraversedPolylineStatus();
    }

    public void updatePolyline(List<LineString> trips, List<DirectionsRoute> directionsRouteList, int selected) {
        this.trips = trips;
        this.directionsRouteList = directionsRouteList;
        this.selected = selected;
        updatePolylineStatus();
        updateTraversedPolylineStatus();
    }

    private synchronized void updateSelectedPolylineStates() {
        if (trips == null)
            return;
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                setVisibility(true, style);
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        List<Feature> features = new ArrayList<>(trips.size());
                        for (int i = 0; i < trips.size(); i++) {
                            LineString lineString = trips.get(i);
                            Feature feature = Feature.fromGeometry(lineString);
                            if (profile.equalsIgnoreCase(DirectionsCriteria.PROFILE_WALKING)) {
                                feature.addStringProperty(FILTER_TEXT, (selected == i) ? "selected-walking" : "alternate-walking");
                            } else {
                                feature.addStringProperty(FILTER_TEXT, (selected == i) ? "selected" : "alternate");
                            }
                            feature.addNumberProperty(POSITION_TEXT, i);
                            features.add(feature);
                        }
                        if (trips.size() > selected) {
                            LineString lineString = trips.get(selected);
                            if (lineString != null) {
                                List<Point> points = lineString.coordinates();
                                if (points.size() > 2) {
                                    Feature startPolylineFeature = Feature.fromGeometry(points.get(0));
                                    startPolylineFeature.addStringProperty("icon", CONSTANT_START_POINT);
                                    startPolylineFeature.addStringProperty(FILTER_TEXT, "line-point-marker");
                                    features.add(startPolylineFeature);

                                    Feature endPolylineFeature = Feature.fromGeometry(points.get(points.size() - 1));
                                    endPolylineFeature.addStringProperty("icon", CONSTANT_END_POINT);
                                    endPolylineFeature.addStringProperty(FILTER_TEXT, "line-point-marker");
                                    features.add(endPolylineFeature);
                                }
                            }
                        }
                        if (position != null) {
                            Feature bearingFeature = Feature.fromGeometry(
                                    Point.fromLngLat(position.getLongitude(), position.getLatitude()));
                            bearingFeature.addStringProperty("icon", DirectionsSymbolLayer.ICON_BEARING_IMAGE);
                            bearingFeature.addStringProperty(FILTER_TEXT, "bearing");
                            features.add(bearingFeature);
                        }
                        if (endLatLng != null) {
                            Feature endFeature = Feature.fromGeometry(
                                    Point.fromLngLat(endLatLng.getLongitude(), endLatLng.getLatitude()));
                            endFeature.addStringProperty("icon", CONSTANT_END);
                            endFeature.addStringProperty(FILTER_TEXT, "marker");
                            features.add(appendIdInFeature(endFeature, "end-marker"));
                        }
                        /**
                         *  ended
                         */
                        if (wayPoints != null) {
                            int i = 0;
                            for (LatLng latLng : wayPoints) {
                                Feature viaFeature = Feature.fromGeometry(
                                        Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
                                viaFeature.addStringProperty("icon", CONSTANT_WAYPOINTS);
                                viaFeature.addStringProperty(FILTER_TEXT, "marker");
                                features.add(appendIdInFeature(viaFeature, "via-marker-" + (i++)));
                            }
                        }

      /*  Layer locationLayer = mapplsMap.getLayer(DirectionsSymbolLayer.BASE_BEARING_LAYER_ID);
        if (locationLayer != null) {
            locationLayer.setProperties(
                    PropertyFactory.iconRotate(bearing)
            );
        }
*/


                        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
                        mMapView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (style.isFullyLoaded()) {
                                    GeoJsonSource source = style.getSourceAs(DirectionPolylineData.SOURCE_ID);
                                    if (source != null) {
                                        source.setGeoJson(featureCollection);
                                    }
                                }
                            }
                        });

                        addCongestion(features, style);
                    }
                });

            }
        });
    }

    /**
     * addCurvedLineStringFeatures for adding all parameter in features
     *
     * @param curvedPath
     * @param filterText
     * @param features
     */
    private void addCurvedLineStringFeatures(List<Point> curvedPath, String filterText, List<Feature> features) {
        List<LineString> curvedLineString = Collections.singletonList(LineString.fromLngLats(curvedPath));

        for (int i = 0; i < curvedLineString.size(); i++) {
            LineString lineString = curvedLineString.get(i);
            Feature feature = Feature.fromGeometry(lineString);
            feature.addStringProperty(FILTER_TEXT, filterText);
            feature.addStringProperty("curve-icon", CurrentToPolylineStartDottedRoute.ICON_IMAGE);
            feature.addNumberProperty(POSITION_TEXT, i);
            features.add(feature);
        }
    }

    private void addCongestion(List<Feature> features, Style style) {
        if (enableCongestion) {
            if (directionsRouteList != null && trips != null && !directionsRouteList.isEmpty() && !trips.isEmpty()) {
                if (selected >= 0 && selected < directionsRouteList.size() && selected < trips.size()) {
                    List<Feature> congestionList = createCongestionFeature(directionsRouteList.get(selected), trips.get(selected));
                    features.addAll(congestionList);
                }
                FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
                mMapView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (style.isFullyLoaded()) {
                            GeoJsonSource source = style.getSourceAs(DirectionPolylineData.SOURCE_ID);
                            if (source != null) {
                                source.setGeoJson(featureCollection);
                            }
                        }
                    }
                });
                if (selected >= 0 && selected < directionsRouteList.size() && selected < trips.size()) {
                    DirectionsRoute directionsRoute = directionsRouteList.get(selected);
                    showDurationSegments(features, directionsRoute);
                }
            }
        }
    }

    private void showDurationSegments(List<Feature> features, DirectionsRoute directionsRoute) {
        if(directionsRoute == null) {
            return;
        }
        Point startPoint = LineString.fromPolyline(directionsRoute.geometry(), Constants.PRECISION_6).coordinates().get(0);
        List<CongestionDelayInfo> congestionSegments = MapplsUtils.getCongestionDelayInfoFromRoute(directionsRoute, startPoint, 50.0, 1);

        int i = 0;
        addImage(congestionSegments);
        for (CongestionDelayInfo congestionSegment :
                congestionSegments) {
//            List<Point> points = congestionSegment.getPoints();
//            if (points.size() > 0) {

            Feature feature = Feature.fromGeometry(congestionSegment.getPoint());
            feature.addStringProperty("icon", CONSTANT_SEGMENT_DELAY + i);
            feature.addStringProperty(FILTER_TEXT, "segment");
            feature.addNumberProperty(SEGMENT_SORT_KEY_DELAY, 0 - congestionSegment.getDelayDuration());
            features.add(appendIdInFeature(feature, "segment-delay-" + i));
            i++;
            features.add(feature);
//            }
        }
        mMapView.post(new Runnable() {
            @Override
            public void run() {
                mapplsMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        setVisibility(true, style);
                        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
                        GeoJsonSource source = style.getSourceAs(DirectionPolylineData.SOURCE_ID);
                        if (source != null) {
                            source.setGeoJson(featureCollection);
                        }
                    }
                });
            }
        });
    }


    private void addImage(List<CongestionDelayInfo> congestionSegments) {
        HashMap<String, Bitmap> imagesMap = new HashMap<>();

        for (int i = 0; i < congestionSegments.size(); i++) {
            LinearLayout constraintLayout = (LinearLayout) LayoutInflater.from(mMapView.getContext()).inflate(R.layout.direction_segment_delay, null);
            TextView textView = constraintLayout.findViewById(R.id.direction_delay_text);
            textView.setText("+" + congestionSegments.get(i).getDelayDuration() + " min");
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            constraintLayout.measure(measureSpec, measureSpec);

            Bitmap bitmap = SymbolGenerator.generate(constraintLayout);
            imagesMap.put(CONSTANT_SEGMENT_DELAY + i, bitmap);

        }
        mMapView.post(new Runnable() {
            @Override
            public void run() {
                mapplsMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        style.addImagesAsync(imagesMap);
                    }
                });
            }
        });
    }

    /**
     * Utility class to generate Bitmaps for Symbol.
     * <p>
     * Bitmaps can be added to the map with
     * </p>
     */
    private static class SymbolGenerator {

        /**
         * Generate a Bitmap from an Android SDK View.
         *
         * @param view the View to be drawn to a Bitmap
         * @return the generated bitmap
         */
        static Bitmap generate(@NonNull View view) {


            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(measureSpec, measureSpec);

            int measuredWidth = view.getMeasuredWidth();
            int measuredHeight = view.getMeasuredHeight();

            view.layout(0, 0, measuredWidth, measuredHeight);
            Bitmap bitmap1 = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inBitmap = bitmap1;
//            BitmapFactory.decodeByteArray(bitmap1.getNinePatchChunk(), 0, view.getHeight(), options);
            options.inSampleSize = calculateInSampleSize(options, measuredWidth, measuredHeight);
//            options.inBitmap = bitmap1;
            Bitmap bitmap = options.inBitmap;
            bitmap.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        }
    }


    /**
     * Calculate the sample size of the image
     *
     * @param options   BitmapFactory options
     * @param reqWidth  required width
     * @param reqHeight required height
     * @return calculated sample size
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    private synchronized void updateTraversedPolylineStates() {
        mapplsMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                if (traversed == null) {
                    FeatureCollection featureCollection = FeatureCollection.fromFeatures(new ArrayList<>());

                    GeoJsonSource source = style.getSourceAs(DirectionPolylineData.TRAVERSED_SOURCE_ID);
                    if (source != null) {
                        source.setGeoJson(featureCollection);
                    }
                } else {
                    Feature feature = Feature.fromGeometry(traversed);
                    feature.addStringProperty(FILTER_TEXT, "traversed");
                    GeoJsonSource source = style.getSourceAs(DirectionPolylineData.TRAVERSED_SOURCE_ID);
                    if (source != null) {
                        source.setGeoJson(feature);
                    }
                }
            }
        });
    }

    private List<Feature> createCongestionFeature(DirectionsRoute directionsRoute, LineString lineString) {
        List<Feature> features = new ArrayList<>();
        if (directionsRoute.legs() != null) {
            RouteLeg routeLeg = directionsRoute.legs().get(0);
            if (routeLeg != null && routeLeg.annotation() != null && routeLeg.annotation().congestion() != null) {
                List<String> congestion = routeLeg.annotation().congestion();
                for (int i = 0; i < congestion.size(); i++) {

                    if (congestion.size() + 1 <= lineString.coordinates().size()) {
                        String currentCongestion = congestion.get(i);

                        Point thisPoint = lineString.coordinates().get(i);
                        Point nextPoint = lineString.coordinates().get(i + 1);

                        List<Point> line = new ArrayList<>();
                        line.add(thisPoint);
                        line.add(nextPoint);

                        LineString newLineString = LineString.fromLngLats(line);
                        Feature feature = Feature.fromGeometry(newLineString);
                        feature.addStringProperty("congestion", currentCongestion);
                        features.add(feature);
                    }
                }
            }
        }

        return features;
    }


    private void addImagesToMap(Style style) {
        try {
            Bitmap startIcon = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.destination));
            Bitmap endIcon = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.destination));
            Bitmap viaPointsIcon = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.destination));
            Bitmap trackPointsIcon = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.destination));
            Bitmap selectedDottedImage = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.ic_walking_selected));
            Bitmap alternateDottedImage = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.ic_walking_alternate));
            Bitmap startPoint = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.ic_route_start_point));
            Bitmap endPoint = getBitmapFromDrawable(mMapView.getContext().getResources().getDrawable(R.drawable.ic_route_end_point));
            style.addImage(CONSTANT_START, startIcon);
            style.addImage(CONSTANT_END, endIcon);
            style.addImage(CONSTANT_WAYPOINTS, viaPointsIcon);
            style.addImage(CONSTANT_TRACK_POINTS, trackPointsIcon);
            style.addImage(SelectedDottedRoute.ICON_IMAGE, selectedDottedImage);
            style.addImage(AlternateDottedRoute.ICON_IMAGE, alternateDottedImage);
            style.addImage(CONSTANT_START_POINT, startPoint);
            style.addImage(CONSTANT_END_POINT, endPoint);

        } catch (Exception e) {
            //ignore this never comes if app is in focus (native crash fixes)
        }
    }

    private void updatePolylineStatus() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(updatePolylineStateRunnable, 100);
    }

    private void updateTraversedPolylineStatus() {
        traversedHandler.removeCallbacksAndMessages(null);
        traversedHandler.postDelayed(updateTraversedPolylineStateRunnable, 10);
    }

    public void removeAllData() {
        if (mapplsMap.getStyle() != null && mapplsMap.getStyle().isFullyLoaded()) {
            GeoJsonSource source = mapplsMap.getStyle().getSourceAs(DirectionPolylineData.SOURCE_ID);
            if (source != null) {
                source.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            }
            GeoJsonSource traversedSource = mapplsMap.getStyle().getSourceAs(DirectionPolylineData.TRAVERSED_SOURCE_ID);
            if (traversedSource != null) {
                traversedSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            }
        }
    }

    public void setBearingLayerVisibility(boolean visible, Style style) {
        Layer layer = style.getLayer(DirectionsSymbolLayer.BASE_BEARING_LAYER_ID);
        if (layer != null) {
            layer.setProperties(PropertyFactory.visibility(visible ? Property.VISIBLE : Property.NONE));
        }
        this.bearingVisibility = visible;
    }

    public void setBearingIcon(float bearing, LatLng position) {
        this.bearing = bearing;
        this.position = position;
        updatePolylineStatus();
    }

    private Feature appendIdInFeature(Feature feature, String id) {
        try {
            JSONObject jsonObject = new JSONObject(feature.toJson());
            jsonObject.put("id", id);
            return Feature.fromJson(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {
        if (directionsRouteList != null) {
            HashMap<LineString, DirectionsRoute> routeLineStrings = new HashMap<>();
            for (DirectionsRoute directionsRoute : directionsRouteList) {
                if (directionsRoute.geometry() != null) {
                    routeLineStrings.put(LineString.fromPolyline(directionsRoute.geometry(), Constants.PRECISION_6), directionsRoute);
                }
            }
            findClickedRoute(mapplsMap, latLng, routeLineStrings, directionsRouteList);
        }
        return false;
    }

    private void findClickedRoute(MapplsMap map, @NonNull LatLng point, HashMap<LineString, DirectionsRoute> routeLineStrings,
                                  List<DirectionsRoute> directionsRoutes) {

        HashMap<Double, DirectionsRoute> routeDistancesAwayFromClick = new HashMap<>();
        Point clickPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());

        PointF screenLocationClick = map.getProjection().toScreenLocation(point);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                calculateClickDistances(map, routeDistancesAwayFromClick, clickPoint, screenLocationClick, routeLineStrings);

                List<Double> distancesAwayFromClick = new ArrayList<>(routeDistancesAwayFromClick.keySet());
                Collections.sort(distancesAwayFromClick);

                if (distancesAwayFromClick.size() == 0) {
                    return;
                }

                double shortestDistance = distancesAwayFromClick.get(0);

                if (shortestDistance < 100) {

                    DirectionsRoute clickedRoute = routeDistancesAwayFromClick.get(shortestDistance);
                    int newPrimaryRouteIndex = directionsRoutes.indexOf(clickedRoute);
                    selected = newPrimaryRouteIndex;
                    mMapView.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePolylineStatus();

                            if (onNewRouteSelectedListener != null) {
                                onNewRouteSelectedListener.onNewRouteSelected(newPrimaryRouteIndex, clickedRoute);
                            }
                        }
                    });
                }
            }
        });

    }

    /**
     * We modified this method to return screen Pixel distance instead of distance between two locations on map
     */
    private void calculateClickDistances(MapplsMap map, HashMap<Double, DirectionsRoute> routeDistancesAwayFromClick,
                                         Point clickPoint, PointF clickScreenLocation, HashMap<LineString, DirectionsRoute> routeLineStrings) {
        for (LineString lineString : routeLineStrings.keySet()) {
            Point pointOnLine = findPointOnLine(clickPoint, lineString);
            if (pointOnLine == null) {
                return;
            }

            mMapView.post(new Runnable() {
                @Override
                public void run() {
                    PointF pointScreenLocation = map.getProjection().toScreenLocation(new LatLng(pointOnLine.latitude(), pointOnLine.longitude()));

                    double pixelDistance = Math.hypot((clickScreenLocation.x - pointScreenLocation.x), (clickScreenLocation.y - pointScreenLocation.y));
                    Timber.d("Distance is equal 2  --->  " + pixelDistance);

                    routeDistancesAwayFromClick.put(pixelDistance, routeLineStrings.get(lineString));
                }
            });


        }
    }

    private Point findPointOnLine(Point clickPoint, LineString lineString) {
        List<Point> linePoints = lineString.coordinates();
        Feature feature = TurfMisc.nearestPointOnLine(clickPoint, linePoints);
        return (Point) feature.geometry();
    }

    private static class DirectionsLayer {
        static LineLayer getLineLayer(String lineLayerId, float minZoom, Expression statement,
                                      Expression lineColor, Expression lineWidth, Expression lineOffset) {
            return getLineLayer(lineLayerId, minZoom, statement, lineColor, lineWidth, lineOffset, null);
        }

        static LineLayer getLineLayer(String lineLayerId, float minZoom, Expression statement,
                                      Expression lineColorExpression, Expression lineWidthExpression,
                                      Expression lineOffsetExpression, Expression lineOpacityExpression) {
            LineLayer lineLayer = new LineLayer(lineLayerId, DirectionPolylineData.SOURCE_ID);
            lineLayer.setSourceLayer(DirectionPolylineData.SOURCE_LAYER);
            lineLayer.setProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                    lineColor(lineColorExpression),
                    lineWidth(lineWidthExpression),
                    lineOffset(lineOffsetExpression)
            );
            if (lineOpacityExpression != null) {
                lineLayer.setProperties(lineOpacity(lineOpacityExpression));
            }
            lineLayer.setFilter(statement);
            lineLayer.setMinZoom(minZoom);
            return lineLayer;
        }


        static LineLayer getTraversedLineLayer(String lineLayerId, float minZoom, Expression statement,
                                               Expression lineColor, Expression lineWidth, Expression lineOffset) {
            return getTraversedLineLayer(lineLayerId, minZoom, statement, lineColor, lineWidth, lineOffset, null);
        }

        static LineLayer getTraversedLineLayer(String lineLayerId, float minZoom, Expression statement,
                                               Expression lineColorExpression, Expression lineWidthExpression,
                                               Expression lineOffsetExpression, Expression lineOpacityExpression) {
            LineLayer lineLayer = new LineLayer(lineLayerId, DirectionPolylineData.TRAVERSED_SOURCE_ID);
            lineLayer.setSourceLayer(DirectionPolylineData.SOURCE_LAYER);
            lineLayer.setProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                    lineColor(lineColorExpression),
                    lineWidth(lineWidthExpression),
                    lineOffset(lineOffsetExpression)
            );
            if (lineOpacityExpression != null) {
                lineLayer.setProperties(lineOpacity(lineOpacityExpression));
            }
            lineLayer.setFilter(statement);
            lineLayer.setMinZoom(minZoom);
            return lineLayer;
        }
    }

    private static class DirectionsFunction {
        static Expression getLineColorFunction(@ColorInt int selected, @ColorInt int alternate) {
            return match(get(FILTER_TEXT), toColor(literal(colorToRgbaString(Color.TRANSPARENT))),
                    stop("selected", toColor(literal(colorToRgbaString(selected)))),
                    stop("alternate", toColor(literal(colorToRgbaString(alternate)))),
                    Expression.stop("traversed", toColor(literal(colorToRgbaString(alternate)))));
        }

        private static Expression getIconImageFunction() {
            return string(get(literal("icon")));
        }
    }

    private static class DirectionPolylineData {
        private static final String SOURCE_ID = "directions";
        private static final String SOURCE_LAYER = "directions";
        private static final String TRAVERSED_SOURCE_ID = "traversed_directions";
    }

    private static class DirectionType {
        static final Expression FUNCTION_LINE_COLOR = DirectionsFunction.getLineColorFunction(DirectionsColor.BASE_SELECTED_ROUTE,
                DirectionsColor.BASE_ALTERNATE_ROUTE);
        static final Expression FUNCTION_LINE_COLOR_CASE = DirectionsFunction.getLineColorFunction(
                DirectionsColor.CASE_SELECTED_ROUTE, DirectionsColor.CASE_ALTERNATE_ROUTE);
    }

    private static class SelectedDottedRoute extends DirectionType {
        private static final String BASE_LAYER_ID = "route_selected_dotted";
        private static final String ICON_IMAGE = "selected_dotted_route_image";

        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("selected-walking", true)
        );
    }

    private static class AlternateDottedRoute extends DirectionType {
        private static final String BASE_LAYER_ID = "route_alternate_dotted";
        private static final String ICON_IMAGE = "alternate_dotted_route_image";

        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("alternate-walking", true)
        );
    }

    private static class CurrentToPolylineStartDottedRoute extends DirectionType {
        private static final String BASE_LAYER_ID = "current_to_polyline_curved_line";
        private static final String ICON_IMAGE = "curved-line-image";

        private static final String SOURCE_ID = "curved_line";
        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("current-to-polyline", true)
        );
    }

    private static class SelectedRoute extends DirectionType {
        static final Expression FUNCTION_LINE_WIDTH = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 4f),
                Expression.stop(10f, 6.5f),
                Expression.stop(13f, 7f),
                Expression.stop(16f, 9f),
                Expression.stop(19f, 14f),
                Expression.stop(22f, 18f)
        );
        static final Expression FUNCTION_LINE_WIDTH_CASE = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 5f),
                Expression.stop(10f, 7.5f),
                Expression.stop(13f, 8.5f),
                Expression.stop(16f, 10.5f),
                Expression.stop(19f, 16f),
                Expression.stop(22f, 27f)
        );
        static final Expression FUNCTION_LINE_OFFSET = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                stop(14, 0f),
                stop(20, 0f)
        );
        static final Expression FUNCTION_LINE_OPACITY_CASE = interpolate(Expression.exponential(1.0f), Expression.zoom(),
                stop(15, 1.0f),
                stop(16, 1.0f)
        );
        private static final String BASE_LAYER_ID = "route_selected";
        private static final String LOCAL_CASE_LAYER_ID = "route_selected_case";
        private static final float ZOOM_LEVEL = 0.0f;
        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("selected", true)
        );
    }

    private static class TraversedRoute extends DirectionType {
        static final Expression FUNCTION_LINE_WIDTH = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 4f),
                Expression.stop(10f, 6.5f),
                Expression.stop(13f, 7f),
                Expression.stop(16f, 9f),
                Expression.stop(19f, 14f),
                Expression.stop(22f, 18f)
        );
        static final Expression FUNCTION_LINE_WIDTH_CASE = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 5f),
                Expression.stop(10f, 7.5f),
                Expression.stop(13f, 8.5f),
                Expression.stop(16f, 10.5f),
                Expression.stop(19f, 16f),
                Expression.stop(22f, 27f)
        );
        static final Expression FUNCTION_LINE_OFFSET = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(7, 0f),
                Expression.stop(9, 0f),
                Expression.stop(11, 0f),
                Expression.stop(18, 0f),
                Expression.stop(20, 0f)
        );
        static final Expression FUNCTION_LINE_OPACITY_CASE = interpolate(Expression.exponential(1.0f), Expression.zoom(),
                Expression.stop(15, 1.0f),
                Expression.stop(16, 1.0f)
        );
        private static final String BASE_LAYER_ID = "route_traversed";
        private static final String CASE_LAYER_ID = "route_traversed_case";
        private static final float ZOOM_LEVEL = 0.0f;
        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("traversed", true)
        );
    }

    private static class AlternateRoute extends DirectionType {
        static final Expression FUNCTION_LINE_WIDTH = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 4f),
                Expression.stop(10f, 6.5f),
                Expression.stop(13f, 7f),
                Expression.stop(16f, 9f),
                Expression.stop(19f, 14f),
                Expression.stop(22f, 18f)
        );
        static final Expression FUNCTION_LINE_WIDTH_CASE = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                Expression.stop(4f, 5f),
                Expression.stop(10f, 7.5f),
                Expression.stop(13f, 8.5f),
                Expression.stop(16f, 10.5f),
                Expression.stop(19f, 16f),
                Expression.stop(22f, 27f)
        );
        static final Expression FUNCTION_LINE_OFFSET = interpolate(Expression.exponential(1.5f), Expression.zoom(),
                stop(7, 0f),
                stop(9, 0f),
                stop(11, 0f),
                stop(18, 0f),
                stop(20, 0f)
        );
        static final Expression FUNCTION_LINE_OPACITY_CASE = interpolate(Expression.exponential(1.0f), Expression.zoom(),
                stop(15, 1.0f),
                stop(16, 1.0f)
        );
        private static final String BASE_LAYER_ID = "route_alternate";
        private static final String CASE_LAYER_ID = "route_alternate_case";
        private static final float ZOOM_LEVEL = 0.0f;
        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("alternate", true)
        );
    }


    private static class DirectionPointSymbolLayer extends DirectionType {
        private static final String BASE_LAYER_ID = "directions-line-point-layer";

        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("line-point-marker", true)
        );
    }


    private static class DirectionsSegmentSymbolLayer extends DirectionType {
        private static final String BASE_LAYER_ID = "com.mappls.sdk.directions.directions-congestion-segment-marker-layer";
        private static final String ICON_IMAGE = "com.mappls.sdk.directions.directions-congestion-segment-marker-image";
        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("segment", true)
        );
    }

    private static class DirectionsSymbolLayer extends DirectionType {
        private static final String BASE_LAYER_ID = "directions-marker-layer";
        private static final String ICON_IMAGE = "directions-marker-image";
        private static final String BASE_ADVICES_LAYER_ID = "directions-marker-advices-layer";
        private static final Expression FILTER = match(
                get(FILTER_TEXT), literal(false),
                stop("marker", true)
        );/*match(
                get(FILTER_TEXT), literal(false),
                stop("selected", true)
        );*/
        private static final Expression FILTER_ADVICES = match(
                get(FILTER_TEXT), literal(false),
                stop("advices", true)
        );
        private static final String BASE_BEARING_LAYER_ID = "directions-marker-bearing-layer";
        private static final String ICON_BEARING_IMAGE = "directions-marker-bearing-image";
    }

    private static class DirectionsColor {
        private static final int BASE_SELECTED_ROUTE = Color.parseColor("#07b9fc"); // for selected color
        private static final int CASE_SELECTED_ROUTE = Color.parseColor("#000000");// for boundary selected color
        private static final int BASE_ALTERNATE_ROUTE = Color.parseColor("#a1bbd2"); // for alternate route color
        private static final int CASE_ALTERNATE_ROUTE = Color.parseColor("#000000"); // for alternate boundary color
    }

    public interface OnNewRouteSelectedListener {
        void onNewRouteSelected(int index, DirectionsRoute directionsRoute);
    }
}
