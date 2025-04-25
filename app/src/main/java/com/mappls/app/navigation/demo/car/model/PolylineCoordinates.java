package com.mappls.app.navigation.demo.car.model;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class PolylineCoordinates implements Parcelable {

    double lat;
    double lng;

    public PolylineCoordinates(double lat, double lng){
        this.lat = lat;
        this.lng = lng;
    }

    protected PolylineCoordinates(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
    }

    public static final Creator<PolylineCoordinates> CREATOR = new Creator<PolylineCoordinates>() {
        @Override
        public PolylineCoordinates createFromParcel(Parcel in) {
            return new PolylineCoordinates(in);
        }

        @Override
        public PolylineCoordinates[] newArray(int size) {
            return new PolylineCoordinates[size];
        }
    };

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lng);
    }
}
