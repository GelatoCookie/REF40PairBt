package com.zebra.rfid.scanpair;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ScanPair.ScanPairListener {

    public static final String EXTRA_SCAN_INPUT = "scan_input";

    ScanPair scanPair;
    Button buttonClear;
    Button buttonPair;
    EditText scanCode;
    View loadingOverlay;
    TextView loadingText;

    private ArrayAdapter<String> mAdapter;
    private AdapterView.OnItemClickListener mItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String readerDevice = (String) mAdapter.getItem(position);
            scanPair.unpair(readerDevice);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI
        scanCode = findViewById(R.id.editText);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText);
        applyLaunchInput();

        buttonClear = findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanCode.setText("");
            }
        });

        buttonPair = findViewById(R.id.buttonPair);
        buttonPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanPair != null) {
                    scanPair.barcodeDeviceNameConnect(scanCode.getText().toString());
                } else {
                    if (checkAndRequestPermissions()) {
                        initScanPair();
                        if (scanPair != null) {
                            scanPair.barcodeDeviceNameConnect(scanCode.getText().toString());
                        }
                    }
                }
            }
        });

        // permissions
        if (checkAndRequestPermissions()) {
            initScanPair();
        }
    }

    private void applyLaunchInput() {
        String launchInput = getIntent().getStringExtra(EXTRA_SCAN_INPUT);
        if (launchInput != null && !launchInput.trim().isEmpty()) {
            scanCode.setText(launchInput.trim());
            scanCode.setSelection(scanCode.getText().length());
        }
    }

    private void initScanPair() {
        if (scanPair == null) {
            scanPair = new ScanPair();
            scanPair.Init(this);
            scanPair.setListener(this);

            mAdapter = new ArrayAdapter(this, R.layout.readers_list_item, scanPair.readers);

            ListView list = findViewById(R.id.readerList);
            list.setAdapter(mAdapter);
            list.setOnItemClickListener(mItemClick);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanPair != null) {
            scanPair.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanPair != null) {
            scanPair.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanPair != null) {
            scanPair.onDestroy();
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            // Even on Android 12+, some legacy Bluetooth APIs or specific device behaviors might still require Location
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            // Android 11 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 101);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            boolean bluetoothGranted = true;
            boolean locationGranted = true;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.BLUETOOTH_SCAN) || 
                        permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT)) {
                        bluetoothGranted = false;
                    }
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) || 
                        permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        locationGranted = false;
                    }
                }
            }

            if (bluetoothGranted && locationGranted) {
                initScanPair();
            } else {
                Toast.makeText(this, "Bluetooth and Location permissions are required for this app to function.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void refreshList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAdapter != null)
                    mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onOperationStarted(int messageResId, Object... formatArgs) {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingText.setText(getString(messageResId, formatArgs));
            }
        });
    }

    @Override
    public void onOperationEnded() {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onListUpdated() {
        refreshList();
    }

    @Override
    public void onMessage(int messageResId, Object... formatArgs) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, getString(messageResId, formatArgs), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMessage(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }
}
