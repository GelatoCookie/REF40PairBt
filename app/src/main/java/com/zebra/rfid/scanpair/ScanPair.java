package com.zebra.rfid.scanpair;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanPair {

    public interface ScanPairListener {
        void onOperationStarted(int messageResId, Object... formatArgs);
        void onOperationEnded();
        void onListUpdated();
        void onMessage(int messageResId, Object... formatArgs);
        void onMessage(String message);
    }

    private ScanPairListener listener;

    public void setListener(ScanPairListener listener) {
        this.listener = listener;
    }

    public ArrayList<String> readers = new ArrayList<>();
    private Bt btConnection = null;
    private ArrayList<BluetoothDevice> mRFD8500PairedDeviceList = null;
    private BluetoothDevice mLastToPairedDevice = null;

    private String recvdMacAddress = null;
    private String recvdBarcodeName = null;

    private Context activityObject = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean pairingConnectIdleFlag = true;

    private boolean isDeviceConnectionConfirmationRequested = false;
    private boolean deviceConnectionConfirmationReceived = false;

    public static ArrayList<ReaderDevice> availableReaders = new ArrayList<ReaderDevice>();

    public boolean isDevicePairing = false;
    public static String TAG = "PAIR-DEMO";

    public void Init(Context activity) {
        activityObject = activity;

        btConnection = new Bt();
        btConnection.init(activityObject, this);

        mRFD8500PairedDeviceList = new ArrayList<BluetoothDevice>();

        loadAvailableReaders();
    }


    public void setActivityObject(Context activityObject) {
        this.activityObject = activityObject;
    }

    public void onResume() {
        if (btConnection != null) {
            btConnection.onResume();
        }
    }

    public void onPause() {
        if (btConnection != null) {
            btConnection.onPause();
        }
    }

    public void onDestroy() {
        if (btConnection != null) {
            btConnection.onDestroy();
            btConnection = null;
        }
    }

    public void barcodeDeviceNameConnect(String barcodeData) {
        boolean connecting_pairingFlag = false;

        try {
            if (barcodeData == null || barcodeData.trim().isEmpty()) {
                showToast(R.string.error_scanning_failed);
            } else {
                if ((pairingConnectIdleFlag)) {
                    recvdMacAddress = null;
                    recvdBarcodeName = null;
                    barcodeData = barcodeData.toUpperCase();
                    if (barcodeData.length() == Defines.BT_ADDRESS_LENGTH) {
                        recvdMacAddress = barcodeData.replaceAll("(.{2})(?!$)", "$1:");
                        if (btConnection.isValidMacAddress(recvdMacAddress))
                            connecting_pairingFlag = pairConnect(recvdMacAddress, true);
                        else {
                            showToast(R.string.error_invalid_bt_address, recvdMacAddress);
                        }
                    } else if (barcodeData.length() > Defines.BT_ADDRESS_LENGTH) {
                        recvdBarcodeName = Defines.NameStartString + barcodeData;
                        connecting_pairingFlag = pairConnect(recvdBarcodeName, false);
                    }

                    if (connecting_pairingFlag) {
                        pairingConnectIdleFlag = false;
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception in barcodeDeviceNameConnect", ex);
            pairingConnectIdleFlag = true;
        }
    }

    private boolean pairConnect(String data, boolean isMacAddress) {
        boolean connecting_pairingFlag = false;
        isDevicePairing = false;
        Log.d(TAG, "pairConnect");
        try {
            ArrayList<ReaderDevice> readersList = new ArrayList<>();
            readersList.addAll(getAvailableReaders());
            BluetoothDevice tmpDev = null;
            boolean isFound = false;

            for (ReaderDevice rdDevice : readersList) {
                try {
                    if (isMacAddress) {
                        isFound = rdDevice.getBluetoothDevice().getAddress().equals(data);
                        recvdMacAddress = data;
                    } else {
                        String devName = rdDevice.getBluetoothDevice().getName();
                        isFound = devName != null && devName.equals(data);
                        recvdBarcodeName = data;
                    }

                    if (isFound) {
                        tmpDev = rdDevice.getBluetoothDevice();
                        recvdMacAddress = rdDevice.getBluetoothDevice().getAddress();
                        if (rdDevice.isConnected()) {
                            // Already connected
                        } else {
                            connecting_pairingFlag = true;
                            showToast(R.string.info_already_paired_connecting);
                            ConnectDevice(rdDevice, true);
                        }
                        break;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied in pairConnect loop", e);
                }
            }

            if (tmpDev == null) {
                Log.d(TAG, "pairConnect nothing found");
                if (isMacAddress) {
                    showToast(R.string.info_pairing, recvdMacAddress);
                    int pairResult = btConnection.pair(recvdMacAddress);
                    if (pairResult == Defines.NO_ERROR) {
                        isDevicePairing = true;
                        connecting_pairingFlag = true;
                    } else {
                        showToast(R.string.error_pairing_failed);
                        pairingConnectIdleFlag = true;
                    }
                } else {
                    if (listener != null) {
                        listener.onOperationStarted(R.string.status_scanning, recvdBarcodeName);
                    }
                    btConnection.scanningDevices(recvdBarcodeName, false);
                    connecting_pairingFlag = true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showToast("EXCEPTION(ScanPair) - 'pairConnect' " + ex.getMessage());
        }

        return (connecting_pairingFlag);
    }

    private void ConnectDevice(ReaderDevice rdDevice, boolean b) {
        showToast("Device ready to connect: " + rdDevice.getName());
    }


    private void loadAvailableReaders() {
        Log.d(TAG, "loadAvailableReaders");
        availableReaders.clear();
        readers.clear();
        HashSet<BluetoothDevice> btAvailableReaders = new HashSet<>();
        btConnection.getAvailableDevices(btAvailableReaders);
        for (BluetoothDevice device : btAvailableReaders) {
            try {
                String name = device.getName();
                String address = device.getAddress();
                availableReaders.add(new ReaderDevice(device, name, address, null, null, false));
                readers.add(name);
                Log.d(TAG, "Found: " + name);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied in loadAvailableReaders", e);
            }
        }
        if (activityObject instanceof MainActivity) {
            ((MainActivity) activityObject).refreshList();
        }
        if (listener != null) {
            listener.onListUpdated();
        }
    }

    public Collection<? extends ReaderDevice> getAvailableReaders() {
        loadAvailableReaders();
        return availableReaders;
    }

    private void showToast(String message) {
        Log.d(TAG, message);
        if (listener != null) {
            listener.onMessage(message);
        }
    }

    private void showToast(int resId, Object... args) {
        if (listener != null) {
            listener.onMessage(resId, args);
        }
    }

    public void btScanningDone(ArrayList<BluetoothDevice> btDeviceList, boolean isMacAddress) {
        BluetoothDevice tmpBTDevice = null;
        Log.d(TAG, "btScanningDone");
        try {
            if (listener != null) {
                listener.onOperationEnded();
            }
            if (btDeviceList != null) {
                if (!isMacAddress) {
                    for (BluetoothDevice device : btDeviceList) {
                        try {
                            String devName = device.getName();
                            if (devName != null && devName.equals(recvdBarcodeName)) {
                                recvdMacAddress = device.getAddress();
                                tmpBTDevice = device;
                                break;
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "Permission denied in btScanningDone", e);
                        }
                    }
                } else {
                    for (BluetoothDevice device : btDeviceList) {
                        if (device.getAddress().equals(recvdMacAddress)) {
                            tmpBTDevice = device;
                            break;
                        }
                    }
                }

                if (tmpBTDevice != null) {
                    executePairTask(tmpBTDevice);
                } else {
                    showToast(R.string.error_device_not_found);
                    pairingConnectIdleFlag = true;
                }
            } else {
                pairingConnectIdleFlag = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showToast("EXCEPTION - Scanning failed! " + ex.getMessage());
            pairingConnectIdleFlag = true;
        }
    }

    public void btPairingDone(boolean successFlag, BluetoothDevice btDevice) {
        try {
            if (listener != null) {
                listener.onOperationEnded();
            }

            mLastToPairedDevice = btDevice;

            if (successFlag) {
                if (mLastToPairedDevice != null) {
                    loadAvailableReaders();
                    boolean tmpFlag = false;
                    for (ReaderDevice rdDevice : getAvailableReaders()) {
                        if (rdDevice.getBluetoothDevice().getAddress().equals(recvdMacAddress)) {
                            tmpFlag = true;
                            ConnectDevice(rdDevice, false);
                            break;
                        }
                    }
                }
            } else {
                showToast(R.string.error_pairing_failed);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            pairingConnectIdleFlag = true;
        }
    }

    public void btUnpairingDone(boolean successFlag) {
        try {
            if (listener != null) {
                listener.onOperationEnded();
            }
            updatePairedDevList();
            loadAvailableReaders();

            if (successFlag) {
                showToast(R.string.info_unpairing_done);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            pairingConnectIdleFlag = true;
        }
    }

    public void unpair(String readerDevice) {
        if (btConnection != null) {
            btConnection.unpairReader(readerDevice);
        }
    }


    private void executeUnpairTask() {
        if (listener != null) {
            listener.onOperationStarted(R.string.status_unpairing);
        }

        executor.execute(() -> {
            Integer errorResId = null;
            try {
                int retValue = btConnection.unpair();
                switch (retValue) {
                    case Defines.NO_ERROR:
                        break;
                    case Defines.INFO_UNPAIRING_NO_PAIRED:
                        errorResId = R.string.error_unpairing_no_paired;
                        break;
                    case Defines.ERROR_UNPAIRING_TIMEOUT:
                        errorResId = R.string.error_unpairing_timeout;
                        break;
                    case Defines.ERROR_UNPAIRING_FAILED:
                    default:
                        errorResId = R.string.error_unpairing_failed;
                        break;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception in UnpairTask", ex);
            }

            final Integer result = errorResId;
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onOperationEnded();
                }
                updatePairedDevList();
                if (result != null) {
                    showToast(result);
                }
            });
        });
    }

    private void executePairTask(BluetoothDevice btDevice) {
        String devName;
        try {
            devName = btDevice.getName();
        } catch (SecurityException e) {
            devName = "Device";
        }
        if (listener != null) {
            listener.onOperationStarted(R.string.status_pairing, devName);
        }

        executor.execute(() -> {
            Integer errorResId = null;
            int retValue = 0;

            try {
                mLastToPairedDevice = btDevice;
                retValue = btConnection.pair(btDevice, true);
                switch (retValue) {
                    case Defines.NO_ERROR:
                        break;
                    case Defines.INFO_ALREADY_PAIRED:
                        errorResId = R.string.info_already_paired_connecting;
                        mainHandler.post(() -> {
                            ArrayList<ReaderDevice> readersList = new ArrayList<>();
                            readersList.addAll(getAvailableReaders());
                            for (ReaderDevice rdDevice : readersList) {
                                if (rdDevice.getBluetoothDevice().getAddress().equals(recvdMacAddress)) {
                                    ConnectDevice(rdDevice, true);
                                    break;
                                }
                            }
                        });
                        break;
                    case Defines.ERROR_PAIRING_TIMEOUT:
                        errorResId = R.string.error_pairing_timeout;
                        break;
                    case Defines.ERROR_PAIRING_FAILED:
                    default:
                        errorResId = R.string.error_pairing_failed;
                        break;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception in PairTask", ex);
            }
            
            final Integer result = errorResId;
            mainHandler.post(() -> {
                updatePairedDevList();
                if (result != null) {
                    showToast(result);
                }
            });
        });
    }

    private void updatePairedDevList() {
        try {
            if (mRFD8500PairedDeviceList == null) {
                mRFD8500PairedDeviceList = new ArrayList<>();
            }
            mRFD8500PairedDeviceList.clear();
            if (btConnection == null || btConnection.GetBluetoothAdapter() == null) return;

            Set<BluetoothDevice> mPairedDevices = btConnection.GetBluetoothAdapter().getBondedDevices();
            if (mPairedDevices != null) {
                for (BluetoothDevice device : mPairedDevices) {
                    try {
                        String name = device.getName();
                        if (name != null && name.contains(Defines.NameStartString)) {
                            mRFD8500PairedDeviceList.add(device);
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied in updatePairedDevList loop", e);
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied in updatePairedDevList", e);
        } catch (Exception ex) {
            Log.e(TAG, "Exception in updatePairedDevList", ex);
        }
    }

    private void DeviceConnectionConfirmationRequest() {
        isDeviceConnectionConfirmationRequested = true;
        deviceConnectionConfirmationReceived = false;
    }

    private void DeviceConnectionConfirmationReset() {
        isDeviceConnectionConfirmationRequested = false;
        deviceConnectionConfirmationReceived = false;
    }

    public boolean DeviceConnectionConfirmationRequested() {
        return isDeviceConnectionConfirmationRequested;
    }

    public void DeviceConnectionConfirmed() {
        isDeviceConnectionConfirmationRequested = false;
        deviceConnectionConfirmationReceived = true;
    }
}
