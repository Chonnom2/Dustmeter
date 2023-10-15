package com.cookandroid.myfirstproject;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class GpsTracker extends Service implements LocationListener { // GPS Tracker Class
    private final Context mContext;
    Location location;
    double latitude;
    double longitude;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 업데이트에 필요한 최소거리 (m) 현재 설정값 10m
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1 ; // 업데이트 할 시간 (ms) : 현재 설정값 1초
    protected LocationManager locationManager;


    public GpsTracker(Context context) { // 생성자
        this.mContext = context;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE); // 서비스 초기화

            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); // GPS가 가능한지 확인
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER); //Network가 가능한지 확인

            if (!isGPSEnabled && !isNetworkEnabled) { // GPS 사용 불가에 네트워크도 사용 불가시
                Toast.makeText(getApplicationContext(),"GPS나 네트워크 사용 불가!", Toast.LENGTH_SHORT);
            }
            else { // GPS와 네트워크 둘 다 사용가능한 경우
                int hasFineLocationPermission = ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION); //권한
                int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION); // 권한


                if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                        hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

                    ;
                } else
                    return null; // 권한 승인 안된 경우는 종료


                if (isNetworkEnabled) { // 네트워크가 가능한 경우
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this); // 현재 위치 업데이트
                    if (locationManager != null) // location Manager 에 오류가 없는경우
                    {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        // 네트워크로 최근위치 들고오기
                        if (location != null) // 정상적으로 위치를 얻으면
                        {
                            latitude = location.getLatitude(); // 위도정보
                            longitude = location.getLongitude(); //경도정보
                        }
                    }
                }
                if (isGPSEnabled) // GPS사용 가능한 경우
                {
                    if (location == null)// network로 위치정보를 얻지 못한경우
                    {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this); // 위치정보 주기적 업데이트
                        if (locationManager != null) // Location Manager가 오류 없는경우
                        {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // 최근 위치정보 받아오기
                            if (location != null) // 위치정보를 얻은경우이면
                            {
                                latitude = location.getLatitude(); //업데이트
                                longitude = location.getLongitude(); // 업데이트
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) //오류 발생시
        {
            Log.d("Debug_Cho", ""+e.toString());
        }

        return location;
    }

    public double getLatitude()
    {
        if(location != null)
        {
            latitude = location.getLatitude();
        }

        return latitude;
    }

    public double getLongitude()
    {
        if(location != null)
        {
            longitude = location.getLongitude();
        }

        return longitude;
    }

    @Override
    public void onLocationChanged(Location location)
    {
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }


    public void stopUsingGPS()
    {
        if(locationManager != null)
        {
            locationManager.removeUpdates(GpsTracker.this);
        }
    }


}
