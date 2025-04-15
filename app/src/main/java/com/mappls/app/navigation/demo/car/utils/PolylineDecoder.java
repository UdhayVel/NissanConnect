package com.mappls.app.navigation.demo.car.utils;
import com.mappls.sdk.maps.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

public class PolylineDecoder {
    protected PolylineDecoder(){

    }

    public static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> path = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            path.add(new LatLng((lat / 1E6), (lng / 1E6)));
        }
        return path;
    }
}
