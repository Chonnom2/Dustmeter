package com.cookandroid.myfirstproject;

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {
    Button mBtnRegister;
    GpsTracker gpsTracker;
    MainActivity.ConnectedBluetoothThread mBluetoothThread;

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.register_layout);

        mBluetoothThread = ((MainActivity)MainActivity.context_main).mThreadConnectedBluetooth;
        mBtnRegister = (Button) findViewById(R.id.BtnRegister);
        gpsTracker = new GpsTracker(this);
        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

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
                mBluetoothThread.write(registerData);
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();
    }

}
