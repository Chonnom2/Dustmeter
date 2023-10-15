package com.cookandroid.myfirstproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
//import com.cookandroid.myfirstproject.databinding.ActivityLocationBinding;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationActivity extends FragmentActivity implements OnMapReadyCallback , GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    LatLng myLocation;
    Button mBtnRegister;
    GpsTracker gpsTracker;
    MainActivity.ConnectedBluetoothThread mBluetoothThread;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    public static final int REQUESTED_CODE_PERMISSIONS = 1000;
    //private ActivityMapsBinding binding;

    @Override
    public void onConnected(@Nullable Bundle bundle){ }
    @Override
    public void onConnectionSuspended(int i){ }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){ }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        mBluetoothThread = ((MainActivity)MainActivity.context_main).mThreadConnectedBluetooth;
        mBtnRegister = (Button) findViewById(R.id.BtnStore);
        gpsTracker = new GpsTracker(this);
        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);




        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        myLocation = new LatLng(latitude,longitude);



        mBtnRegister.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                locationRegister(latitude, longitude);

            }
        });

    }


    void locationRegister(double lat, double lon){
        final EditText editText = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("장소 등록");
        builder.setMessage("장소 이름을 입력하세요");
        builder.setView(editText);
        builder.setPositiveButton("등록", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String registerData = "3," + editText.getText().toString() + "," + lat + "," + lon;
                if(mBluetoothThread == null){
                    Toast.makeText(getApplicationContext(), "연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
                else{
                    mBluetoothThread.write(registerData);
                    Toast.makeText(getApplicationContext(), "정상적으로 등록 되었습니다.", Toast.LENGTH_SHORT).show();
                }

            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode){
            case REQUESTED_CODE_PERMISSIONS:
                if(ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_DENIED){
                    Toast.makeText(this, "권한 체크 거부됨", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    protected void onStart(){
        mGoogleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera

        mMap.addMarker(new MarkerOptions().position(myLocation).title("현재위치"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,19));
        //

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                /*
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel: 01056267350"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }

                 */
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}