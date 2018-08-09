
package ca.gc.crc.rnad.indoorwifi;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;

import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;

import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;

import android.net.wifi.WifiManager;

import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.view.View;

import android.widget.ArrayAdapter;

import android.widget.Button;

import android.widget.ListView;

import android.widget.Toast;

import android.Manifest;

import java.util.ArrayList;

import java.util.List;



public class WiFiScannerActivity extends Activity{



    private WifiManager wifiManager;

    private ListView listView;

    private Button buttonScan;

    private int size = 0;

    private List<ScanResult> results;

    private ArrayList<String> arrayList = new ArrayList<>();

    private ArrayAdapter adapter;



    @Override

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.networks);

        buttonScan = findViewById(R.id.scanBtn);

        buttonScan.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(WiFiScannerActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(WiFiScannerActivity.this, "You have already granted this permission!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    requestStoragePermission();
                }
                if (ContextCompat.checkSelfPermission(WiFiScannerActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(WiFiScannerActivity.this, "You have already granted this permission!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    requestStoragePermission2();
                }

                scanWifi();

            }

        });



        listView = findViewById(R.id.wifiList);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);



        if (!wifiManager.isWifiEnabled()) {

            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();

            wifiManager.setWifiEnabled(true);

        }



        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);

        listView.setAdapter(adapter);

        scanWifi();

    }



    private void scanWifi() {

        arrayList.clear();

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();

        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();

    }



    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {

            results = wifiManager.getScanResults();

            unregisterReceiver(this);



            for (ScanResult scanResult : results) {

                arrayList.add(" SSID: "+ scanResult.SSID + " CAPABILITY: " + scanResult.capabilities + "  FREQUENCY: "+ scanResult.frequency +"Mhz "+" Strength: " + scanResult.level + "dBm " );

                adapter.notifyDataSetChanged();

            }

        }

    };
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(WiFiScannerActivity.this,
                                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
    private void requestStoragePermission2() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(WiFiScannerActivity.this,
                                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
