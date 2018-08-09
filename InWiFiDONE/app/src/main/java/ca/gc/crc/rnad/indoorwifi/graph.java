package ca.gc.crc.rnad.indoorwifi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Date;

public class graph extends Activity {
   private RelativeLayout mainLayout;
   private LineChart mChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph);
       // Button Ar = findViewById(R.id.Ar);
        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
        mChart = new LineChart(this);
        mainLayout.addView(mChart);

         mChart.setDescription("");
         mChart.setNoDataTextDescription("No data at the moment");

         mChart.setHighlightEnabled(true);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.BLACK);
        LineData data = new LineData();
        LineData data2 = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        mChart.setData(data2);

       Legend legend  = mChart.getLegend();
       legend.setForm(Legend.LegendForm.LINE);
       legend.setTextColor(Color.WHITE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMaxValue(60f);
        y1.setAxisMinValue(50f);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);


    }
    @Override
    protected void onResume(){
        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {
               while(true){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addEntry();

                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //
                }
            }
            }
        }).start();
    }

    private void addEntry(){
        LineData data = mChart.getData();
         if (data != null){
             LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);

             if(set == null){
                 set = createSet();
                 data.addDataSet(set);
             }
            data.addXValue("");
             data.addEntry(new Entry(
                     (float) (displayDetails()+100),set.getEntryCount()
             ),0);
         }
         mChart.notifyDataSetChanged();
         mChart.setVisibleXRange(10000);
         mChart.moveViewToX(data.getXValCount()+1);

    }
    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null,"Power Receiver (dBm)");
      //  set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244,117,177));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);



        return set;
    }
    public float displayDetails(){

        @SuppressLint("WifiManagerLeak")WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();


       float  strength =  wifiInfo.getRssi();


        return strength;

    }
    public void onArClick(View v){
        if(v.getId() == R.id.graph){
            Intent i = new Intent(graph.this,MainActivity.class);
            startActivity(i);

        }
    }
}