package com.techtown.simplediary.data;

import androidx.annotation.NonNull;

public class GeometryLocation {
    public double lat;
    public double lng;

    @NonNull
    @Override
    public String toString() {
        return "GeometryLocation{" +
                "lat="+lat+", " +
                "lng="+lng+
                "}";
    }
}
