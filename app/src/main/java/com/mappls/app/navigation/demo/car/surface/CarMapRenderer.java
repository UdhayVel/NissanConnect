package com.mappls.app.navigation.demo.car.surface;

import android.app.Presentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.VirtualDisplay;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.mappls.app.navigation.demo.HomeActivity;
import com.mappls.app.navigation.demo.NavigationActivity;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.extensions.CarContextUtils;
import com.mappls.app.navigation.demo.car.extensions.ThreadUtils;
import com.mappls.app.navigation.demo.car.screens.interfaceclasses.SelectLocationCallBack;
import com.mappls.app.navigation.demo.car.screens.models.LocationList;
import com.mappls.sdk.direction.ui.model.DirectionPoint;
import com.mappls.sdk.maps.MapView;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.camera.CameraUpdate;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.navigation.NavLocation;
import com.mappls.sdk.navigation.NavigationContext;
import com.mappls.sdk.navigation.data.WayPoint;
import com.mappls.sdk.navigation.model.NavigationResponse;
import com.mappls.sdk.navigation.ui.navigation.MapplsNavigationViewHelper;
import com.mappls.sdk.navigation.util.ErrorType;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class CarMapRenderer implements SurfaceCallback, DefaultLifecycleObserver, ICarMapRenderer, SelectLocationCallBack {

    private static final String LOG_TAG = "CarMapRenderer";

    private final CarContext carContext;
    public static CarMapContainer mapContainer;

    private VirtualDisplay virtualDisplay;
    private Presentation presentation;
    private MapView navigationView;

    private final Paint osmPaint;

    private SurfaceContainer surfaceContainer;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private Rect lastKnownStableArea = new Rect();
    private Rect lastKnownVisibleArea = new Rect();

    @RequiresApi(api = Build.VERSION_CODES.M)
    public CarMapRenderer(CarContext carContext, Lifecycle serviceLifecycle) {
        this.carContext = carContext;
        this.mapContainer = new CarMapContainer(carContext, serviceLifecycle);

        this.osmPaint = new Paint();
        this.osmPaint.setColor(carContext.getColor(R.color.colorAccent));
        this.osmPaint.setTextAlign(Paint.Align.RIGHT);
        this.osmPaint.setTypeface(Typeface.DEFAULT);

        serviceLifecycle.addObserver(this);
    }

    public MapplsMap getMapplsMap(){
        if(mapContainer == null){
            return null;
        }
        return mapContainer.getMapplsMap();
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        try {
            AppManager appManager = carContext.getCarService(AppManager.class);
            appManager.setSurfaceCallback(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not set surface callback", e);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.v(LOG_TAG, "CarMapRenderer.onDestroy");
        surfaceContainer = null;
        uiHandler.removeCallbacksAndMessages(null);
        try {
            AppManager appManager = carContext.getCarService(AppManager.class);
            appManager.setSurfaceCallback(null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not remove surface callback", e);
        }
    }

    private boolean isSurfaceReady(SurfaceContainer surfaceContainer) {
        return surfaceContainer.getSurface() != null &&
                surfaceContainer.getDpi() != 0 &&
                surfaceContainer.getHeight() != 0 &&
                surfaceContainer.getWidth() != 0;
    }

    @Override
    public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
        Log.v(LOG_TAG, "CarMapRenderer.onSurfaceAvailable");

        this.surfaceContainer = surfaceContainer;

        if (!isSurfaceReady(surfaceContainer)) return;

        mapContainer.setSurfaceSize(surfaceContainer.getWidth(), surfaceContainer.getHeight());

        MapView mapView = mapContainer.mapViewInstance;
        if (mapView != null) {
            mapView.addOnDidBecomeIdleListener(this::drawOnSurface);
            mapView.addOnWillStartRenderingFrameListener(this::drawOnSurface);
        }

        ThreadUtils.runOnMainThread(this::drawOnSurface); // runOnMainThread Java equivalent
    }

    private void drawOnSurface() {
        MapView mapView = mapContainer.mapViewInstance;
        Surface surface = surfaceContainer != null ? surfaceContainer.getSurface() : null;
        if (mapView == null || surface == null || !surface.isValid()) return;

        Canvas canvas = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canvas = surface.lockHardwareCanvas();
        }
        drawMapOnCanvas(mapView, canvas);
        surface.unlockCanvasAndPost(canvas);
    }

    private void drawMapOnCanvas(MapView mapView, Canvas canvas) {
        View child = mapView.getChildAt(0);
        if (child instanceof TextureView) {
            Bitmap bitmap = ((TextureView) child).getBitmap();
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, 0f, 0f, null);
            }
        }

        float density = carContext.getResources().getDisplayMetrics().density;

        osmPaint.setTextSize(12 * density);
        canvas.drawText(
                carContext.getString(R.string.app_name),
                canvas.getWidth() - (12 * density),
                canvas.getHeight() - (4 * density),
                osmPaint
        );
    }

    @Override
    public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
        if (!visibleArea.equals(lastKnownVisibleArea)) {
            Log.v(LOG_TAG, "onVisibleAreaChanged left(" + visibleArea.left + ") top(" + visibleArea.top +
                    ") right(" + visibleArea.right + ") bottom(" + visibleArea.bottom + ")");
            lastKnownVisibleArea = visibleArea;
        }
    }

    @Override
    public void onStableAreaChanged(@NonNull Rect stableArea) {
        if (!stableArea.equals(lastKnownStableArea)) {
            Log.v(LOG_TAG, "onStableAreaChanged left(" + stableArea.left + ") top(" + stableArea.top +
                    ") right(" + stableArea.right + ") bottom(" + stableArea.bottom + ")");
            lastKnownStableArea = stableArea;
        }
    }

    @Override
    public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
        Log.v(LOG_TAG, "Surface destroyed");
        this.surfaceContainer = null;
        uiHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void zoomInFromButton() {
        float centerX = surfaceContainer != null ? surfaceContainer.getWidth() / 2f : -1f;
        float centerY = surfaceContainer != null ? surfaceContainer.getHeight() / 2f : -1f;
        onScale(centerX, centerY, CarMapContainer.DOUBLE_CLICK_FACTOR);
    }

    @Override
    public void zoomOutFromButton() {
        float centerX = surfaceContainer != null ? surfaceContainer.getWidth() / 2f : -1f;
        float centerY = surfaceContainer != null ? surfaceContainer.getHeight() / 2f : -1f;
        onScale(centerX, centerY, -CarMapContainer.DOUBLE_CLICK_FACTOR);
    }

    @Override
    public void onScale(float focusX, float focusY, float scaleFactor) {
        Log.v(LOG_TAG, "onScale(" + focusX + ", " + focusY + ")");
        mapContainer.onScale(focusX, focusY, scaleFactor);

        CameraUpdate update = CameraUpdateFactory.zoomBy(
                scaleFactor - 1,
                new Point((int) focusX, (int) focusY)
        );

        if (mapContainer.mapplsMap != null) {
            mapContainer.mapplsMap.animateCamera(update);
        }
    }

    @Override
    public synchronized void onScroll(float distanceX, float distanceY) {
        Log.v(LOG_TAG, "onScroll distanceX(" + distanceX + ") distanceY(" + distanceY + ")");
        mapContainer.scrollBy(distanceX, distanceY);
    }

    @Override
    public void onClick(float x, float y) {

    }

    @Override
    public void onFling(float velocityX, float velocityY) {

    }

    @Override
    public void setSelectedLocation(LocationList location) {
        Log.v("locationCallBack:: ", "setSelectedLocation " + location.getLatLng());
        mapContainer.addMarker(location.getLatLng());
    }

    public void getCurrentLocation() {
        if(NavigationContext.getNavigationContext().getCurrentLocation() != null){
            Location loc = NavigationContext.getNavigationContext().getCurrentLocation();
            CarMapContainer.mapplsMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc), 16), 500);
        }
    }

    public void startNavigationCars() {
        NavLocation location = mapContainer.getUserLocation();
        if (location == null)
            return;

        mapContainer.startNavigation();
    }

    public void stopNavigationCars() {
       mapContainer.stopNavigtion();
    }
}
