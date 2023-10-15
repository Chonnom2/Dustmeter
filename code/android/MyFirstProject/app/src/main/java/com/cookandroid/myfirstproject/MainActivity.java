package com.cookandroid.myfirstproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.w3c.dom.Document;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ImageButton mBtnBLT;
    ImageButton mBtnMenu;
    TextView mTvDustValue;
    TextView mTvHumValue;
    TextView mTvTempValue;
    ImageView mIvEmotion;
    DrawerLayout mDrawerLayout;
    TextView mTvMap;
    TextView mTvRegister;
    TextView mTvRecText;
    TextView mTvTest;
    //위젯 변수들

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;  //페어링 된 디바이스
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;//스레드 인수 선언
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;
    TxThread mTxThread;

    GpsTracker gpsTracker;
    public static Context context_main;

    public int ReceivedLocationFlag = 0;
    public int ReceivedDataFlag = 0;


    private static final int GPS_ENABLE_REQUEST_CODE = 2001; // 권한 요청 코드
    private static final int PERMISSIONS_REQUEST_CODE = 100; // 권한 요청 코드
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    /*GPS 관련 인수들*/

    private boolean mAllowInsecureConnections;
    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static int GPS_REQUEST_ENABLE = 4;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    String Dust = null;
    String Temperature = null;
    String Humidity = null;
    String[] ReceivedLocationData = null;
    String[] ReceivedData = null;
    private NotficationHelper mNotificationhelper ;
    int notifyflag = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context_main = this;

        mNotificationhelper = new NotficationHelper(this);
        mAllowInsecureConnections = true;
        mBtnBLT = (ImageButton) findViewById(R.id.BtnBluetooth);
        mBtnMenu =(ImageButton) findViewById(R.id.BtnMenu);
        mTvDustValue = (TextView) findViewById(R.id.TvDustValue);
        mTvHumValue = (TextView)findViewById(R.id.TvHumValue);
        mTvTempValue = (TextView)findViewById(R.id.TvTempValue);
        mIvEmotion = (ImageView) findViewById(R.id.IvEmotion);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mTvMap = (TextView) findViewById(R.id.TvMap);
        mTvRegister = (TextView) findViewById(R.id.TvRegister);
        mTvRecText = (TextView) findViewById(R.id.TvRecText);
        mTvTest = (TextView) findViewById(R.id.TvTest);


        /*GPS Part */
        if (checkLocationServicesStatus()){
            checkRunTimePermission();
        }
        else{
            showDialogForLocationServiceSetting();
        }
        gpsTracker = new GpsTracker(MainActivity.this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPairedDevices = mBluetoothAdapter.getBondedDevices();


        mBtnBLT.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                listPairedDevices();
            }
        });

        mBtnMenu.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!mDrawerLayout.isDrawerOpen(Gravity.RIGHT)){
                    mDrawerLayout.openDrawer(Gravity.RIGHT);
                }
                else{
                    mDrawerLayout.closeDrawer(Gravity.RIGHT);
                }
            }
        });

        mTvRegister.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), LocationActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
            }
        });

        mTvMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),CheckActicity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
            }
        });
        mTvTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "`0,180.0,26.0,51,";
                byte[] temp_buffer = data.getBytes();

                mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, data.length(), -1, temp_buffer).sendToTarget();
            }
        });


        mBluetoothHandler = new Handler(){ // 핸들러 정의 부분 ...공부필요
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){ // 메시지가 InputStream으로 읽은 메시지의 경우
                    String readMessage = null;
                    try{
                        readMessage = new String((byte[]) msg.obj, "UTF-8"); // Byte형 메시지를 String으로 변환
                    }
                    catch (UnsupportedEncodingException e){ // 변환 실패시 예외
                        e.printStackTrace();
                    }
                    String[] split_p_Message = readMessage.split("`");
                    Log.d("Debug_Cho", "길이: " + Integer.toString(split_p_Message.length));
                    Log.d("Debug_Cho", readMessage);
                    if(split_p_Message.length > 2){
                        for(int i = 1; i < split_p_Message.length; ++i){
                            String[] splitMessage = split_p_Message[i].split(",");
                            Log.d("Debug_Cho", i+" 번째 for문 부분: "+ Arrays.toString(splitMessage));
                            Log.d("Debug_Cho", i+" 번째 헤더부분: "+splitMessage[0]);
                            if(splitMessage[0].equals("0")){ // 센서값을 담고있는 데이터
                                Dust = splitMessage[1];
                                Temperature = splitMessage[2];
                                Humidity = splitMessage[3];

                                mTvDustValue.setText(Dust);
                                mTvTempValue.setText(Temperature);
                                mTvHumValue.setText(Humidity);
                                String rectext;
                                String title;
                                String message;
                                double val_humi = Double.parseDouble(Humidity);
                                double val_dust = Double.parseDouble(Dust);
                                if(val_dust <= 30){
                                    notifyflag = 0;
                                    rectext = "미세먼지가 좋아요.";
                                    mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_very_satisfied_24);
                                }
                                else if(val_dust <= 80){

                                    rectext = "미세먼지가 보통이예요. 몸 상태에 따라 유의하여 활동해 주세요.";
                                    title = "미세먼지 경고";
                                    message = "미세먼지가 보통입니다. 몸 상태에 따라 유의하여 활동해 주세요.";
                                    if(notifyflag < 1){
                                        sendOnChannel1(title,message);
                                    }
                                    notifyflag = 1;
                                    mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_satisfied_alt_24);
                                }
                                else if(val_dust <= 150){
                                    rectext = "미세먼지가 나빠요. 마스크를 착용해 주세요.";
                                    title = "미세먼지 경고";
                                    message = "미세먼지가 나빠요. 마스크를 착용해 주세요.";
                                    if(notifyflag < 2){
                                        sendOnChannel1(title,message);
                                    }
                                    notifyflag = 2;
                                    mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_dissatisfied_24);
                                }
                                else{

                                    rectext = "미세먼지가 매우 나빠요. 시급한 환기가 필요합니다.";
                                    title = "미세먼지 경고";
                                    message = "미세먼지가 매우 나빠요. 시급한 환기가 필요합니다.";
                                    if(notifyflag < 3){
                                        sendOnChannel1(title,message);
                                    }
                                    notifyflag = 3;
                                    mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_very_dissatisfied_24);
                                }
                                if(val_humi < 40){
                                    rectext += "\n습도가 낮아요. 가습기를 틀어주세요.";
                                }
                                else if(val_humi < 61){
                                    rectext += "\n습도가 적당해요.";
                                }
                                else{
                                    rectext += "\n습도가 높아요.";
                                }
                                mTvRecText.setText(rectext);

                            }
                            else if(splitMessage[0].equals("2")){ // 요청한 장소의 데이터에 대한 응답을 담은 데이터
                                ReceivedData = splitMessage;
                                ReceivedDataFlag = 2;
                            }

                            else if(splitMessage[0].equals("4")){ // 장소 목록 요청에 대한 응답을 담은 데이터
                                ReceivedLocationData = splitMessage;
                                ReceivedLocationFlag = 4;
                            }
                        }
                    }
                    else{
                        String[] splitMessage = split_p_Message[1].split(",");
                        Log.d("Debug_Cho", "핸들러 부분: "+splitMessage[0]);
                        Log.d("Debug_Cho", split_p_Message[1]);

                        if(splitMessage[0].equals("0")){
                            Dust = splitMessage[1];
                            Temperature = splitMessage[2];
                            Humidity = splitMessage[3];

                            mTvDustValue.setText(Dust);
                            mTvTempValue.setText(Temperature);
                            mTvHumValue.setText(Humidity);

                            String rectext;
                            String title;
                            String message;
                            double val_humi = Double.parseDouble(Humidity);
                            double val_dust = Double.parseDouble(Dust);
                            if(val_dust <= 30){
                                notifyflag = 0;
                                rectext = "미세먼지가 좋아요.";
                                mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_very_satisfied_24);
                            }
                            else if(val_dust <= 80){

                                rectext = "미세먼지가 보통이예요. 몸 상태에 따라 유의하여 활동해 주세요.";
                                title = "미세먼지 경고";
                                message = "미세먼지가 보통입니다. 몸 상태에 따라 유의하여 활동해 주세요.";
                                if(notifyflag < 1){
                                    sendOnChannel1(title,message);
                                }
                                notifyflag = 1;
                                mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_satisfied_alt_24);
                            }
                            else if(val_dust <= 150){
                                rectext = "미세먼지가 나빠요. 마스크를 착용해 주세요.";
                                title = "미세먼지 경고";
                                message = "미세먼지가 나빠요. 마스크를 착용해 주세요.";
                                if(notifyflag < 2){
                                    sendOnChannel1(title,message);
                                }
                                notifyflag = 2;
                                mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_dissatisfied_24);
                            }
                            else{
                                rectext = "미세먼지가 매우 나빠요. 시급한 환기가 필요합니다.";
                                title = "미세먼지 경고";
                                message = "미세먼지가 매우 나빠요. 시급한 환기가 필요합니다.";
                                if(notifyflag < 3){
                                    sendOnChannel1(title,message);
                                }
                                notifyflag = 3;
                                mIvEmotion.setImageResource(R.drawable.ic_baseline_sentiment_very_dissatisfied_24);
                            }
                            if(val_humi < 40){
                                rectext += "\n습도가 낮아요. 가습기를 틀어주세요.";
                            }
                            else if(val_humi < 61){
                                rectext += "\n습도가 적당해요.";
                            }
                            else{
                                rectext += "\n습도가 높아요.";
                            }
                            mTvRecText.setText(rectext);

                        }
                        else if(splitMessage[0].equals("2")){
                            ReceivedData = splitMessage;
                            ReceivedDataFlag = 2;
                        }

                        else if(splitMessage[0].equals("4")){
                            ReceivedLocationData = splitMessage;
                            ReceivedLocationFlag = 4;
                        }
                    }
                }
            }
        };

        connectSelectedDevice("raspberrypi"); // 어플 켜지실 시 자동연결
    }

    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.*/
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;
            // 모든 퍼미션을 허용했는지 체크합니다.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                //위치 값을 가져올 수 있음
                ;
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(getApplicationContext(), "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    void checkRunTimePermission() {
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        /*권한을 가지고 있는지 확인*/

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            /*권한을 가지는 부분*/


        } else {  //권한이 현재 없는 경우

            //권한을 거절한 적이 있는 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 권한 요청 Toast 메시지
                Toast.makeText(getApplicationContext(), "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 권한 요청, 요청 결과는 onRequestPermissionResult 에 수신
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 권한을 거절한 적이 없는 경우 바로 요청
                // 결과는 onRequestPermissionResult에 수신
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_REQUEST_ENABLE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }




    @Override
    protected void onDestroy() {
        mThreadConnectedBluetooth.cancle();
        mTxThread.cancle();
        super.onDestroy();
    }

    void bluetoothOn(){
        if(mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBluetoothAdapter.isEnabled()){
                Toast.makeText(getApplicationContext(),"이미 블루투스가 켜져있습니다.", Toast.LENGTH_SHORT).show();
                //mTvBLTStatus.setText("활성화");
            }
            else{
                Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BT_REQUEST_ENABLE);
            }
        }

    }

    void bluetoothOff() {
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(),"블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            //mTvBLTStatus.setText("비활성화");
        }
        else{
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case BT_REQUEST_ENABLE:
                if(resultCode == RESULT_OK){
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    //mTvBLTStatus.setText("활성화");
                }
                else if(resultCode == RESULT_CANCELED){
                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_SHORT).show();
                    //mTvBLTStatus.setText("비활성화");
                }
                break;

            case GPS_REQUEST_ENABLE:
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("Debug_GPS", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void listPairedDevices(){
        if(mBluetoothAdapter.isEnabled()) {

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                //페어링된 기기들 리스트로 만드는 부분

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        connectSelectedDevice(items[i].toString()); // 해당 리스트 클릭시 연결
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_SHORT).show();
            }

        }
        else{
            Toast.makeText(getApplicationContext(),"블루투스가 활성화 되어있지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    void connectSelectedDevice(String selectedDeviceName){
        Log.d("Debug_Cho", "실행완료");
        for(BluetoothDevice tempDevice : mPairedDevices){
            if(selectedDeviceName.equals(tempDevice.getName())){
                Log.d("Debug_Cho", selectedDeviceName + " " + tempDevice.getName());
                mBluetoothDevice = tempDevice;
                break;
            }
        }

        BluetoothSocket tmp = null;

        try {
            if ( mAllowInsecureConnections ) {
                Method method;
                method = mBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class } );
                tmp = (BluetoothSocket) method.invoke(mBluetoothDevice, 1);
            }
            else {
                tmp = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord( MY_UUID );
            }
        } catch (Exception e) {
            Log.e("Debug_Cho", "create() failed", e);
        }
        mBluetoothSocket = tmp;
        try{
            mBluetoothSocket.connect();
            Log.d("Debug_Cho", "Connect 성공");
            Toast.makeText(getApplicationContext(),"연결 성공!!", Toast.LENGTH_SHORT).show();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mTxThread = new TxThread(mThreadConnectedBluetooth, gpsTracker);
            mThreadConnectedBluetooth.start();
            mTxThread.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();

        } catch (IOException e){
            Log.e("Debug_con", "Socket's create() method failed", e);
            Toast.makeText(getApplicationContext(),"블루투스 연결 중 오류가 발생하였습니다." ,Toast.LENGTH_SHORT).show();
        }
    }

    public class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e){
                Toast.makeText(getApplicationContext(),"소켓 연결 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            /*InputStream, OutputStream 초기 설정 부분*/
        }
        public void run(){
            byte[] buffer = new byte[4096];
            int bytes;

            while(true){ // 여기서 계속 돌아가는 상황, 즉 별도의 스레드 함수 부분
                try{
                    bytes = mmInStream.available();
                    if(bytes != 0){
                        SystemClock.sleep(100);
                        bytes = mmInStream.available(); // InputStream에서 내가 읽을수 있는 바이트수
                        Arrays.fill(buffer,(byte)0);
                        bytes = mmInStream.read(buffer, 0, bytes); // bytes수 만큼 버퍼에 0부터 저장
                        String readData = new String(buffer, 0, bytes);
                        //Log.d("Debug_Cho", "Bytes 수: "+Integer.toString(bytes));
                        //Log.d("Debug_Cho", "Data: " + readData);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget(); // 핸들러로 수신한 정보 전달
                        //Log.d("Debug_Cho", "수신스레드 동작중");
                    }

                }
                catch(IOException e){
                    Log.d("Debug_Cho", "hi"+e.toString());
                    cancle();
                    break;
                }
            }
        }
        public void write(String str){
            byte[] bytes = str.getBytes();
            try{
                mmOutStream.write(bytes);
            }
            catch(IOException e){
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
        public void cancle() {
            try{
                mmSocket.close();
            }
            catch (IOException e){
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class TxThread extends Thread{
        private GpsTracker gpsTracker; // GPS 위치 정보 객체
        private ConnectedBluetoothThread mConnectedBLThread;

        public TxThread(ConnectedBluetoothThread connectedBluetoothThread, GpsTracker gps){
            mConnectedBLThread = connectedBluetoothThread;
            gpsTracker = gps;
            Toast.makeText(getApplicationContext(), "스레드 생성 성공", Toast.LENGTH_SHORT).show();
        }
        public void run(){
            try{
                while(true){
                    //Log.d("Debug_Cho", "현재 작동중");
                    if(Dust != null){
                        double latitude = gpsTracker.getLatitude();
                        double longitude = gpsTracker.getLongitude();
                        String gpsData = String.format("%.6f,%.6f", latitude, longitude);
                        String Data = "0," + Dust + "," + Temperature +"," +Humidity+ "," + gpsData; //0: 센서 값 전송
                        mConnectedBLThread.write(Data);
                    }
                    Thread.sleep(1000);
                }


            }
            catch (Exception e){
                cancle();
                Log.d("Debug_Cho", "Error: "+e.toString());
            }

        }
        public void cancle(){
            gpsTracker.stopUsingGPS();
        }
    }



    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void sendOnChannel1(String title, String message){
        NotificationCompat.Builder nb = mNotificationhelper.getChannelNotification(title, message);
        mNotificationhelper.getManager().notify(1,nb.build());
    }
}
