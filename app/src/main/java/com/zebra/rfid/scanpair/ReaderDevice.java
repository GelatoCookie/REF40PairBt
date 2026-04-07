package com.zebra.rfid.scanpair;

import android.bluetooth.BluetoothDevice;

/**
 * Created by qvfr34 on 2/6/2015.
 */
public class ReaderDevice {
    private String password;
    private String name;
    private String Address;
    private String serial;
    private String model;
    private boolean isConnected;
    private BluetoothDevice bluetoothDevice;

    /**
     * Empty Constructor. Handles the initialization.
     */
    public ReaderDevice() {

    }

    /**
     * Constructor. Handles the initialization.
     *
     * @param bluetoothDevice BT reference
     * @param name            name of the device
     * @param address         address of the device
     * @param serial          device serial
     * @param model           device model
     * @param isConnected     whether connected or not
     */
    public ReaderDevice(BluetoothDevice bluetoothDevice, String name, String address, String serial, String model, boolean isConnected) {
        this.bluetoothDevice = bluetoothDevice;
        this.name = name;
        Address = address;
        this.serial = serial;
        this.model = model;
        this.isConnected = isConnected;
    }

    /**
     * get reader's connect password
     *
     * @return connect poassword
     */
    public String getPassword() {
        return password;
    }

    /**
     * set connect password
     *
     * @param password connect password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * get device serial
     *
     * @return device serial
     */
    public String getSerial() {
        return serial;
    }

    /**
     * set device serial
     *
     * @param serial device serial
     */
    public void setSerial(String serial) {
        this.serial = serial;
    }

    /**
     * get name of the device
     *
     * @return device name
     */
    public String getName() {
        return name;
    }

    /**
     * set device name
     *
     * @param name device name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get device address
     *
     * @return device address
     */
    public String getAddress() {
        return Address;
    }

    /**
     * set device adress
     *
     * @param address address of the device
     */
    public void setAddress(String address) {
        Address = address;
    }

    /**
     * get device model
     *
     * @return device model
     */
    public String getModel() {
        return model;
    }

    /**
     * set device model
     *
     * @param model device model
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * method to tell whether device is connected
     *
     * @return true if connected or false if not connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * set device connection status
     *
     * @param isConnected true if connected or false if not connected
     */
    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    /**
     * get BT reference of the reader device
     *
     * @return
     */
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    /**
     * set BT reference of the reader device
     *
     * @return
     */
    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ReaderDevice)) {
            return false;
        }
        final ReaderDevice readerDevice = (ReaderDevice) o;
        return ((this.getAddress() != null && readerDevice.getAddress() != null) && (readerDevice.getAddress().equalsIgnoreCase(this.getAddress())));
    }

    @Override
    public int hashCode() {
        int hash = 4;
        hash = 53 * hash + (this.getAddress() != null ? this.getAddress().hashCode() : 0);
        return hash;
    }
}
