package com.techtown.simplediary.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class WeatherBody {

    @SerializedName("data")
    public ArrayList<WeatherItem> datas;
}
