package com.techtown.simplediary;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.stanfy.gsonxml.GsonXml;
import com.stanfy.gsonxml.GsonXmlBuilder;
import com.stanfy.gsonxml.XmlParserCreator;
import com.techtown.simplediary.data.WeatherItem;
import com.techtown.simplediary.data.WeatherResult;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class MainActivity extends AppCompatActivity implements OnTabItemSelectedListener, OnRequestListener, MyApplication.OnResponseListener {
    private static final String TAG = "MainActivity";

    Fragment1 fragment1;
    Fragment2 fragment2;
    Fragment3 fragment3;

    BottomNavigationView bottomNavigationView;

    Location currentLocation;
    GPSListener gpsListener;

    int locationCount = 0;
    String currentWeather;
    String currentAddress;
    String currentDateString;
    Date currentDate;
    SimpleDateFormat todayDateFormat;

    /**
     * 데이터베이스 인스턴스
     */
    public static NoteDatabase mDatabase = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 초기 화면 설정
        fragment1 = new Fragment1();
        fragment2 = new Fragment2();
        fragment3 = new Fragment3();

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment1).commit();

        // 하단 메뉴바 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if(itemId == R.id.tab1){
                    Toast.makeText(getApplicationContext(), "첫 번째 탭 선택됨", Toast.LENGTH_SHORT).show();
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment1).commit();
                    return true;
                }else if(itemId == R.id.tab2){
                    Toast.makeText(getApplicationContext(), "두 번째 탭 선택됨", Toast.LENGTH_SHORT).show();
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment2).commit();
                    return true;
                }else if(itemId == R.id.tab3){
                    Toast.makeText(getApplicationContext(), "세 번째 탭 선택됨", Toast.LENGTH_SHORT).show();
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment3).commit();
                    return true;
                }
                return false;
            }
        });

        setPicturePath(); //사진 저장할 경로 설정

        // 권한 설정
        AndPermission.with(this)
                .runtime()
                .permission(
                        Permission.ACCESS_COARSE_LOCATION,
                        Permission.ACCESS_FINE_LOCATION,
                        Permission.READ_EXTERNAL_STORAGE,
                        Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        showToast("허용된 권한의 수 : "+data.size());
                    }
                }).onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        showToast("거부된 권한의 수 : "+data.size());
                    }
                })
                .start();

        //데이터베이스 열기
        openDatabase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mDatabase != null){
            mDatabase.close();
            mDatabase = null;
        }
    }

    /**
     *  데이터베이스 열기 (데이터베이스가 없을 때는 생성)
     */
    public void openDatabase(){
        // 데이터베이스가 있으면 먼저 닫기
        if(mDatabase != null){
            mDatabase.close();
            mDatabase = null;
        }

        mDatabase = NoteDatabase.getInstance(this);
        boolean isOpen = mDatabase.open();//열고 열린 것 확인
        
        if(isOpen){
            Log.d(TAG, "Note database is open.");
        }else{
            Log.d(TAG, "Note database is not open.");
        }
    }

    public void setPicturePath(){
        String folderPath = getFilesDir().getAbsolutePath();
        AppConstants.FOLDER_PHOTO = folderPath + File.separator + "photo"; //photo라는 경로 생성

        File photoFoler = new File(AppConstants.FOLDER_PHOTO);
        if(!photoFoler.exists()){
            photoFoler.mkdirs();
        }
    }

    @Override
    public void onTabSelected(int position) {
        if(position == 0){
            bottomNavigationView.setSelectedItemId(R.id.tab1);
        }else if(position == 1){
            fragment2 = new Fragment2(); //새로 만들어서 바꾼다

            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment2).commit();
        }else if(position == 2){
            bottomNavigationView.setSelectedItemId(R.id.tab3);
        }
    }

    @Override
    public void showFragment2(Note item) {
        fragment2 = new Fragment2();
        fragment2.setItem(item);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment2).commit();
    }

    public void showToast(String data){
        Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequest(String command) {
        if(command != null){
            if(command.equals("getCurrentLocation")){
                getCurrentLocation();
            }
        }
    }

    /**
     * 날짜, 위치 전역 변수에 저장
     */
    public void getCurrentLocation(){

        // set current time
        currentDate = new Date();

//         currentDateString = AppConstants.dateFormat3.format(currentDate);
        if(todayDateFormat == null){
            todayDateFormat = AppConstants.dateFormat3;
        }

        currentDateString = todayDateFormat.format(currentDate);
        AppConstants.println("currentDateString : "+currentDateString);

        if(fragment2 != null){
            fragment2.setDateString(currentDateString);
        }

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try{
            currentLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(currentLocation != null){
                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();

                String message = "Last Location -> Latitude : "+latitude+", Longitude : "+longitude;
                println(message);

                getCurrentWeather();
                getCurrentAddress(); //각 location에 대한 날씨와 주소를 가져온다
            }

            gpsListener = new GPSListener();
            long minTime = 10000;
            float minDistance = 0;

            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);

            println("Current location requested");

        }catch (SecurityException e){
            e.printStackTrace();
        }


    }

    public void stopLocationService(){
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try{
            manager.removeUpdates(gpsListener); //gpsListener등록 해제
            println("Current location requested");
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    public void println(String data){
        Log.d(TAG, data);
    }

    class GPSListener implements LocationListener{
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLocation = location; //바뀐 위치 저장
            locationCount++;

            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            String message = "Current Location -> Latitude : "+latitude+", Longitude : "+longitude;
            println(message);

            getCurrentWeather();
            getCurrentAddress();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LocationListener.super.onStatusChanged(provider, status, extras);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            LocationListener.super.onProviderEnabled(provider);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            LocationListener.super.onProviderDisabled(provider);
        }
    }

    public void getCurrentAddress(){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;

        try{
            addresses = geocoder.getFromLocation(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    1);
        }catch (Exception e){
            e.printStackTrace();
        }

        if(addresses != null && addresses.size() > 0){
            currentAddress = null;

            Address address = addresses.get(0);

            if(address.getLocality() != null){
                currentAddress = address.getLocality();//먼저 가장 넓은 범주의 주소를 저자하고
            }

            if(address.getSubLocality() != null){
                if(currentAddress != null){
                    currentAddress += " "+ address.getSubLocality();
                }else{
                    currentAddress = address.getSubLocality();
                }
            }

            String adminArea = address.getAdminArea(); //넓은 범위? 좁은 범위? 모르겠네
            String country = address.getCountryName();
            println("Address : "+country+" "+adminArea+" "+currentAddress);

            if(fragment2 != null){
                fragment2.setAddress(currentAddress);
            }
        }
    }

    public void getCurrentWeather(){
        Map<String, Double> gridMap = GridUtil.getGrid(currentLocation.getLatitude(), currentLocation.getLongitude());
        double gridX = gridMap.get("x");
        double gridY = gridMap.get("y");
        println("x -> "+gridX + ", y -> "+gridY);

        sendLocalWeatherReq(gridX, gridY);
    }

    public void sendLocalWeatherReq(double gridX, double gridY){
        String url = "http://www.kma.go.kr/wid/queryDFS.jsp";
        url += "?gridx="+Math.round(gridX);
        url += "&gridy="+Math.round(gridY);

        Map<String, String> params = new HashMap<>();

        MyApplication.send(AppConstants.REQ_WEATHER_BY_GRID, Request.Method.GET, url, params, this);
    }

    @Override
    public void processResponse(int requestCode, int responseCode, String response) {
        if(responseCode == 200){
            if(requestCode == AppConstants.REQ_WEATHER_BY_GRID){
                //Grid 좌표를 이용한 날씨 정보 처리 응답
                // println ("response -> "+response);
                XmlParserCreator parserCreator = new XmlParserCreator() {
                    @Override
                    public XmlPullParser createParser() {
                        try{
                            //새로운 XmlPullParser 인스턴스를 보내준다
                            return XmlPullParserFactory.newInstance().newPullParser();
                        }catch (Exception e){
                            throw new RuntimeException(e);
                        }
                    }
                };

                GsonXml gsonXml = new GsonXmlBuilder()
                        .setXmlParserCreator(parserCreator)
                        .setSameNameLists(true)
                        .create();

                WeatherResult weatherResult = gsonXml.fromXml(response, WeatherResult.class);

                // 현재 기준 시간
                try{
                    // DateFormat.format -> Date 객체를 String으로
                    // DateFormat.parse -> String을 Date 객체로
                    Date tmDate = AppConstants.dateFormat1.parse(weatherResult.header.tm);
                    String tmDateText = AppConstants.dateFormat2.format(tmDate);
                    println("기준 시간 : "+tmDateText);

                    for(int i = 0; i<weatherResult.body.datas.size(); i++){
                        WeatherItem item = weatherResult.body.datas.get(i);
                        println("#"+i+" 시간 : "+item.hour + "시, "+item.day+"일째");
                        println("  날씨 : "+ item.wfKor);
                        println("  기온 : "+item.temp);
                        println("  강수확률 : "+item.pop);

                        println("debug 1 : "+(int)Math.round(item.ws * 10));
                        float ws = Float.valueOf(String.valueOf((int)Math.round(item.ws * 10))) / 10.0f;
                        println("  풍속 : "+ws + " m/s");
                    }

                    //set current weather
                    WeatherItem item = weatherResult.body.datas.get(0);
                    currentWeather = item.wfKor;
                    if(fragment2 != null){
                        fragment2.setWeather(item.wfKor);
                    }

                    // stop request location service after 2 times
                    if(locationCount > 1){
                        stopLocationService();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else {
                println("Unknown request code : "+requestCode);
            }
        }else {
            println("Failure response code : "+responseCode);
        }
    }
}