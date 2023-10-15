package com.cookandroid.myfirstproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

public class CheckActicity extends AppCompatActivity {
    MainActivity.ConnectedBluetoothThread mBluetoothThread;
    Spinner mSpinnerList;
    ArrayList<String> LocationList = new ArrayList<String>();
    Button mBtnDelete;
    LineChart mLineChart;
    TextView mTvRecommend;
    Context current = this;
    String locationRequest = "4,";

    SimpleDateFormat mFormat = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss");
    SimpleDateFormat mHHMMformat = new SimpleDateFormat("HH:mm");
    SimpleDateFormat mHHformat = new SimpleDateFormat("HH");

    long mNow;
    Date mDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_acticity);
        mTvRecommend = (TextView) findViewById(R.id.TvRecommend);
        mLineChart = (LineChart) findViewById(R.id.linechart);
        mSpinnerList = (Spinner) findViewById(R.id.SpinList);
        mBtnDelete = (Button) findViewById(R.id.BtnDelete);
        mBluetoothThread = ((MainActivity)MainActivity.context_main).mThreadConnectedBluetooth;

        if(mBluetoothThread != null){
            mBluetoothThread.write(locationRequest);
            LocationReceiveAsync receivedAsync = new LocationReceiveAsync();
            receivedAsync.execute();
        }
        else{
            Toast.makeText(getApplicationContext(), "연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }


    }


    private String getTime(){
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }
    private String getLast7Day(){
        mNow = System.currentTimeMillis();
        long mAgo = mNow - 604800000;//3600*1000*24*7
        mDate = new Date(mAgo);
        return mFormat.format(mDate);
    }
    private long getlongTime(String time){
        long t = -1;

        try {
            Date d = mFormat.parse(time);
            t = d.getTime();
            Log.d("Debug_Cho", "변환한거: "+mFormat.format(t));
        } catch(ParseException e){
            e.printStackTrace();
            Log.d("Debug_Cho", "오류");
        }
        return t;
    }

    public void DrawLineChart(ArrayList<Integer> dust, ArrayList<String> xVals){
        mLineChart.clear();
        List<Entry> entries = new ArrayList<>();
        LineData chartData = new LineData();
        entries.clear();
        chartData.clearValues();

        for(int i = 0; i < dust.size(); i++){
            int dustdata = dust.get(i);
            entries.add(new Entry(i, dustdata));
        }

        LineDataSet lineDataSet1 = new LineDataSet(entries, "DustData"); // 데이터가 담긴 Arraylist 를 LineDataSet 으로 변환한다.
        lineDataSet1.setColor(Color.BLACK); // 해당 LineDataSet의 색 설정 :: 각 Line 과 관련된 세팅은 여기서 설정한다.
        lineDataSet1.setDrawCircles(false);
        lineDataSet1.setDrawCircleHole(false);
        lineDataSet1.setLineWidth(3f);
        //lineDataSet1.setCircleColor(Color.BLACK);
        //lineDataSet1.setCircleHoleColor(Color.BLACK);

        chartData.addDataSet(lineDataSet1);
        chartData.setValueTextSize(9);


        XAxis xaxis = mLineChart.getXAxis();
        xaxis.setPosition(XAxis.XAxisPosition.TOP);
        xaxis.setValueFormatter(new IndexAxisValueFormatter(xVals));
        xaxis.setLabelCount(6);
        xaxis.setTextColor(Color.BLACK);
        //xaxis.setGridColor(Color.RED);

        //xaxis.setDrawLabels(true);

        YAxis yAxisLeft = mLineChart.getAxisLeft(); //Y축의 왼쪽면 설정
        yAxisLeft.setTextColor(Color.BLACK); //Y축 텍스트 컬러 설정
        yAxisLeft.setGridColor(Color.GRAY); // Y축 줄의 컬러 설정
        YAxis yAxisRight = mLineChart.getAxisRight(); //Y축의 오른쪽면 설정
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawAxisLine(false);
        yAxisRight.setDrawGridLines(false);

        //mLineChart.setVisibleXRangeMinimum(60 * 60 * 24 * 1000 * 5);
        mLineChart.setDescription(null);

        mLineChart.setTouchEnabled(false); // 차트 터치 disable
        mLineChart.setData(chartData); // 차트에 위의 DataSet을 넣는다.
        mLineChart.invalidate(); // 차트 업데이트

    }
    public void deleteButton(ArrayList<String> arrays){
        AlertDialog.Builder builder = new AlertDialog.Builder(current);

        builder.setTitle("삭제할 장치");
        final CharSequence[] items = arrays.toArray(new CharSequence[arrays.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AlertDialog.Builder warning = new AlertDialog.Builder(current);
                warning.setTitle("경고");
                warning.setMessage("정말로 "+ items[i].toString() +"을 삭제하시겠습니까?");
                warning.setPositiveButton("예", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int j) {
                        String d = "5,";
                        d = d + items[i].toString() + ",";
                        mBluetoothThread.write(d);
                        try{
                            Thread.sleep(100);
                        }
                        catch (InterruptedException e){
                            Log.d("Debug_Cho", e.toString());
                        }

                        /*여기 주의하기*/
                        mBluetoothThread.write(locationRequest);
                        //LocationReceiveAsync receivedAsync = new LocationReceiveAsync();
                        //receivedAsync.execute();
                    }
                });
                AlertDialog warndialog = warning.create();
                warndialog.show();
            }
        });

        mBtnDelete.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }
    public void dynamic_spinner(ArrayList<String> arrays){

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                current, android.R.layout.simple_spinner_item, arrays);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerList.setAdapter(adapter);
        mSpinnerList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String D = "1," + getLast7Day() + "," + getTime() + ","  + arrays.get(i);
                mBluetoothThread.write(D);
                DataReceiveAsync DatareceiveThread = new DataReceiveAsync();
                DatareceiveThread.execute();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Log.d("Debug_Cho", "함수 실행 완료");
    }


    private class LocationReceiveAsync extends AsyncTask {
        private String[] data;
        private int i;
        ArrayList<String> temp = new ArrayList<String>();


        @Override
        protected Object doInBackground(Object[] objects) {
            i = 0;
            while(((MainActivity)MainActivity.context_main).ReceivedLocationFlag != 4) {
                i += 1;
                try {

                    Thread.sleep(100);
                    if(i > 40){
                        break;
                    }
                    Log.d("Debug_Cho",  "위치정보 스레드 대기중" + Integer.toString(((MainActivity) MainActivity.context_main).ReceivedLocationFlag));
                } catch (InterruptedException e) {
                    Log.d("Debug_Cho", e.toString());
                }
            }
            data = ((MainActivity)MainActivity.context_main).ReceivedLocationData;

            if(data[0].equals("4") && i <= 40){
                Log.d("Debug_Cho", "위치 불러오기");
                if (data[1].equals("None")) {
                    //Toast.makeText(getApplicationContext(), "불러올 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.d("Debug_Cho", "불러올 데이터가 없습니다.");
                    ((MainActivity)MainActivity.context_main).ReceivedLocationFlag = 0;
                }
                else {
                    for(String row : data){
                        if(row.length() > 100){
                            break;
                        }
                        else if(!row.equals("4")){
                            Log.d("Debug_Cho", row);
                            temp.add(row);
                        }
                    }
                    LocationList = temp;
                    Log.d("Debug_Cho", temp.toString());
                    ((MainActivity)MainActivity.context_main).ReceivedLocationFlag = 0;
                }
            }

            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(i <= 40){
                deleteButton(temp);
                dynamic_spinner(temp);
            }

        }
    }

    private class DataReceiveAsync extends AsyncTask {
        private String[] data;
        private int i;
        ArrayList<Integer> PM10Array = new ArrayList<Integer>();
        ArrayList<String> xVals;
        ArrayList<Integer>[] timeDust = new ArrayList[24];

        public DataReceiveAsync(){
            for(int i = 0; i < 24; i++){
                timeDust[i] = new ArrayList<Integer>();
            }
            PM10Array.clear();
            xVals = new ArrayList<String>(){{
                add("00:00");add("01:00");add("02:00");add("03:00");add("04:00");add("05:00");
                add("06:00");add("07:00");add("08:00");add("09:00");add("10:00");add("11:00");
                add("12:00");add("13:00");add("14:00");add("15:00");add("16:00");add("17:00");
                add("18:00");add("19:00");add("20:00");add("21:00");add("22:00");add("23:00");
            }};

        }
        @Override
        protected Object doInBackground(Object[] objects) {
            i = 0;
            while(((MainActivity)MainActivity.context_main).ReceivedDataFlag != 2) {
                i += 1;
                try {
                    Thread.sleep(100);
                    if(i > 40){
                        break;
                    }
                    Log.d("Debug_Cho","데이터 수신 스레드 대기중 :" + Integer.toString(((MainActivity) MainActivity.context_main).ReceivedDataFlag));
                } catch (InterruptedException e) {
                    Log.d("Debug_Cho", e.toString());
                }
            }
            data = ((MainActivity)MainActivity.context_main).ReceivedData;

            if(data[0].equals("2") && i <= 40){
                if (data[1].equals("None")) {
                    //Toast.makeText(getApplicationContext(), "불러올 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.d("Debug_Cho", "None인 경우");
                    ((MainActivity)MainActivity.context_main).ReceivedDataFlag = 0;
                }
                else {
                    for (String row : data) {
                        if(row.length() > 100){
                            break;
                        }
                        else if(!row.equals("2")){
                            String[] temp = row.split("-");

                            long datelong = getlongTime(temp[0]);
                            /*
                            if(!xVals.contains(mHHMMformat.format(datelong))){
                                xVals.add(mHHMMformat.format(datelong));
                            }
                            */


                            int hh = Integer.parseInt(mHHformat.format(datelong));
                            timeDust[hh].add(Integer.parseInt(temp[1]));

                        }


                    }

                    ((MainActivity)MainActivity.context_main).ReceivedDataFlag = 0;
                }
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(i <= 40){
                for(ArrayList<Integer> dust : timeDust){
                    int sum = 0;
                    if(dust.isEmpty()){
                        PM10Array.add(0);
                    }
                    else{
                        for(int val : dust){
                            sum += val;
                        }
                        PM10Array.add(Math.round(sum/dust.size()));
                        //Log.d("Debug_Cho", Integer.toString(Math.round(sum/dust.size())));
                    }
                }
                DrawLineChart(PM10Array, xVals);
            }

        }
    }
}