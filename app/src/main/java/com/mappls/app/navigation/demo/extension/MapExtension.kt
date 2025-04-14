package com.mappls.app.navigation.demo.extension

import android.util.Log
import com.mappls.sdk.maps.MapView
import com.mappls.sdk.maps.MapplsMap
import com.mappls.sdk.maps.OnMapReadyCallback

fun MapView.getMapAsyncLambda(onMapReady: (MapplsMap) -> Unit) {
    this.getMapAsync(object : OnMapReadyCallback {
        override fun onMapReady(map: MapplsMap) {
            // Invoke the lambda when the map is ready
            Log.e("onMapReady","$map")
            onMapReady(map)
        }

        override fun onMapError(p0: Int, p1: String?) {
        }
    })
}