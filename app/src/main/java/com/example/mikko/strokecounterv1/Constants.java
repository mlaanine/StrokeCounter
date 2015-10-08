package com.example.mikko.strokecounterv1;

/**
 * Created by Mikko on 05-Oct-15.
 * * Based on Google's BluetoothChat example app.
 */
public interface Constants {
    // Message types sent from the BluetoothService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;

    // Key names received from the BluetoothService Handler
    String DEVICE_NAME = "device_name";

}
