package com.zebra.rfid.scanpair;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zebra.rfid.scanpair.ScanPair.TAG;

public class Bt {
    private static final String REQUEST_ENABLE_BT = "";
    final int SCANNING_TIMEOUT = 10000; //ms

    private String pairMacAddress = null;
    private String pairName = null;

    private ScanPair parentObj = null;
    private Context grandParentObj = null;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mDeviceList = null;
    private Set<BluetoothDevice> mPairedDevices = null;

    private int operationType = 0;
    private IntentFilter filter = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isReceiverRegistered = false;

    public int init(Context grandParent, ScanPair parent) {
        int ret = Defines.NO_ERROR;

        grandParentObj = grandParent;
        parentObj = parent;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDeviceList = new ArrayList<BluetoothDevice>();

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiverIfNeeded();

        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            try {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                grandParentObj.startActivity(enableBtIntent);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for ACTION_REQUEST_ENABLE", e);
            }
        }

        return (ret);
    }

    public void onResume() {
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            try {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                grandParentObj.startActivity(enableBtIntent);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for ACTION_REQUEST_ENABLE in onResume", e);
            }
        }
        if (filter != null) {
            registerReceiverIfNeeded();
        }
    }

    public void onPause() {
        unregisterReceiverIfNeeded();
    }

    public void onDestroy() {
        unregisterReceiverIfNeeded();
    }

    public int abortOperation() {
        if (operationType == 0 && mBluetoothAdapter != null) {
            try {
                mBluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for cancelDiscovery", e);
            }
        }
        return (0);
    }

    public int scanningDevices() {
        operationType = 0;
        pairName = null;
        pairMacAddress = null;
        try {
            mBluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for startDiscovery", e);
        }
        scheduler.schedule(this::abortOperation, SCANNING_TIMEOUT, TimeUnit.MILLISECONDS);
        return (0);
    }

    public int scanningDevices(String data, boolean isMacAddress) {
        operationType = 0;
        pairMacAddress = null;
        pairName = null;

        if (isMacAddress)
            pairMacAddress = data;
        else
            pairName = data;
        try {
            mBluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for startDiscovery", e);
        }
        return (0);
    }

    public ArrayList<BluetoothDevice> GetScannedDeviceList() {
        return (mDeviceList);
    }

    public BluetoothAdapter GetBluetoothAdapter() {
        return (mBluetoothAdapter);
    }

    public int pair(BluetoothDevice foundDevice, boolean pairFlag) {
        int ret = Defines.NO_ERROR;

        operationType = 1;
        try {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();
            if (mPairedDevices != null) {
                for (BluetoothDevice device : mPairedDevices) {
                    if (device.getAddress().equals(foundDevice.getAddress())) {
                        ret = Defines.INFO_ALREADY_PAIRED;
                        break;
                    }
                }
            }

            if ((ret == Defines.NO_ERROR) && (pairFlag)) {
                ret = pairFunc(foundDevice.getAddress());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for pair", e);
            ret = Defines.ERROR_PAIRING_FAILED;
        }

        return (ret);
    }

    public int pair(String macAddress) {
        int ret = Defines.NO_ERROR;
        try {
            BluetoothAdapter adapter = GetBluetoothAdapter();
            if (adapter == null) {
                return Defines.ERROR_PAIRING_FAILED;
            }
            BluetoothDevice remoteDevice = adapter.getRemoteDevice(macAddress);
            ret = pairDevice(remoteDevice);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "Permission denied for pair(mac)", e);
            ret = Defines.ERROR_PAIRING_FAILED;
        }
        return (ret);
    }

    private void registerReceiverIfNeeded() {
        if (!isReceiverRegistered && grandParentObj != null && filter != null) {
            grandParentObj.registerReceiver(mReceiver, filter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiverIfNeeded() {
        if (isReceiverRegistered && grandParentObj != null) {
            try {
                grandParentObj.unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver state was already reset by framework.
            } finally {
                isReceiverRegistered = false;
            }
        }
    }

    private int pairFunc(String macAddress) {
        int ret = Defines.ERROR_PAIRING_FAILED;
        for (BluetoothDevice device : mDeviceList) {
            if (device.getAddress().equals(macAddress)) {
                pairDevice(device);
                ret = Defines.NO_ERROR;
                break;
            }
        }
        return (ret);
    }

    public int unpair(String macAddres) {
        int ret = Defines.ERROR_UNPAIRING_FAILED;
        operationType = 2;
        try {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();
            if (mPairedDevices != null) {
                for (BluetoothDevice device : mPairedDevices) {
                    if (device.getAddress().equals(macAddres)) {
                        unpairDevice(device);
                        ret = Defines.NO_ERROR;
                        break;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for unpair", e);
        }

        return (ret);
    }

    public int unpairReader(String name) {
        int ret = Defines.ERROR_UNPAIRING_FAILED;
        operationType = 2;
        try {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();
            if (mPairedDevices != null) {
                for (BluetoothDevice device : mPairedDevices) {
                    String devName = device.getName();
                    if (devName != null && devName.equals(name)) {
                        unpairDevice(device);
                        ret = Defines.NO_ERROR;
                        break;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for unpairReader", e);
        }

        return (ret);
    }

    public int unpair() {
        int ret = Defines.ERROR_UNPAIRING_FAILED;
        operationType = 2;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }

        try {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();
            if ((mPairedDevices != null) && (mPairedDevices.size() > 0)) {
                for (BluetoothDevice device : mPairedDevices) {
                    String devName = device.getName();
                    if (devName != null && devName.contains(Defines.NameStartString)) {
                        unpairDevice(device);
                    }
                }
                ret = Defines.NO_ERROR;
            } else {
                ret = Defines.INFO_UNPAIRING_NO_PAIRED;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for unpair()", e);
        }

        return (ret);
    }

    private int pairDevice(BluetoothDevice device) {
        int ret = Defines.NO_ERROR;
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            ret = Defines.ERROR_PAIRING_FAILED;
        }

        return (ret);
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d(TAG, "BT BroadcastReceiver " + action);
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    mDeviceList.clear();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    if (operationType == 0) {
                        parentObj.btScanningDone(mDeviceList, pairMacAddress != null);
                    }
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        mDeviceList.add(device);
                        try {
                            if (pairMacAddress != null && device.getAddress().equals(pairMacAddress)) {
                                mBluetoothAdapter.cancelDiscovery();
                            }
                            String devName = device.getName();
                            if (pairName != null && devName != null && devName.equals(pairName)) {
                                mBluetoothAdapter.cancelDiscovery();
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "Permission denied in ACTION_FOUND", e);
                        }
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    if (prevState == BluetoothDevice.BOND_BONDING) {
                        parentObj.btPairingDone(state == BluetoothDevice.BOND_BONDED, (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                    } else if (prevState == BluetoothDevice.BOND_BONDED) {
                        parentObj.btUnpairingDone(state == BluetoothDevice.BOND_NONE);
                    }
                }
            } catch (Exception ex) {
                Log.d(TAG, "receiver exception: " + ex.getMessage());
                ex.printStackTrace();
                try {
                    mBluetoothAdapter.cancelDiscovery();
                } catch (SecurityException e) {}
                parentObj.btScanningDone(null, true);
            }
        }
    };

    public boolean isValidMacAddress(String recvdMacAddress) {
        return mBluetoothAdapter != null && mBluetoothAdapter.checkBluetoothAddress(recvdMacAddress);
    }

    public void getAvailableDevices(Set<BluetoothDevice> devices) {
        if (mBluetoothAdapter == null) return;
        try {
            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice device : bondedDevices) {
                    if (isRFIDReader(device))
                        devices.add(device);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for getBondedDevices", e);
        }
    }

    public boolean isRFIDReader(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name != null && name.startsWith("RFD40");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for getName", e);
            return false;
        }
    }
}
