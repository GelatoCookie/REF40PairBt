package com.zebra.rfid.scanpair;

import android.Manifest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ScanPair.ScanPairListener {

    public static final String EXTRA_SCAN_INPUT = "scan_input";

    ScanPair scanPair;
    Button buttonClear;
    Button buttonPair;
    EditText scanCode;
    View loadingOverlay;
    TextView loadingText;
    private ToneGenerator toneGenerator;
    private final Handler toneHandler = new Handler(Looper.getMainLooper());

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
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

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
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    private void playPattern(int toneType, int repeatCount) {
        if (toneGenerator == null || repeatCount <= 0) {
            return;
        }

        final int toneDurationMs = 180;
        final int gapMs = 110;
        for (int i = 0; i < repeatCount; i++) {
            final int delay = i * (toneDurationMs + gapMs);
            toneHandler.postDelayed(() -> {
                if (toneGenerator != null) {
                    toneGenerator.startTone(toneType, toneDurationMs);
                }
            }, delay);
        }
    }

    private void playWarningBeep() {
        playPattern(ToneGenerator.TONE_PROP_BEEP, 1);
    }

    private void playConfirmationBeepTwice() {
        playPattern(ToneGenerator.TONE_PROP_ACK, 2);
    }

    private void playErrorBeepThreeTimes() {
        playPattern(ToneGenerator.TONE_SUP_ERROR, 3);
    }

    private void handleMessageTone(int messageResId) {
        if (messageResId == R.string.error_device_not_found) {
            playWarningBeep();
            return;
        }

        if (messageResId == R.string.info_already_paired_connecting) {
            playConfirmationBeepTwice();
            return;
        }

        try {
            String entryName = getResources().getResourceEntryName(messageResId);
            if (entryName != null && entryName.startsWith("error_")) {
                playErrorBeepThreeTimes();
            }
        } catch (Exception ignored) {
            // Ignore invalid/non-app resources for tone mapping.
        }
    }

    private void handleMessageTone(String message) {
        if (message == null) {
            return;
        }

        String normalized = message.toLowerCase(Locale.US);
        if (normalized.contains("device not found")) {
            playWarningBeep();
            return;
        }

        if (normalized.contains("already paired") || normalized.contains("device ready to connect")) {
            playConfirmationBeepTwice();
            return;
        }

        if (normalized.contains("error")) {
            playErrorBeepThreeTimes();
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
            handleMessageTone(messageResId);
        });
    }

    @Override
    public void onMessage(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            handleMessageTone(message);
        });
    }
}
