package com.example.mikko.strokecounterv1;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends Activity implements SelectBtDeviceDialog.SelectBtDeviceDialogListener {

    // UI elements
    protected TextView strokeCount;
    protected Chronometer timeCount;
    protected TextView averageCount;
    protected Button startButton;
    protected Button resetButton;
    protected Button connectButton;
    protected TextView connectState;
    protected TextView selectedBtDevice;
    // State variables and preferences
    protected int strokeCountValue = 0;
    protected boolean counterIsRunning = false;
    protected long timeCountValue = 0;
    protected double averageCountValue = 0;
    protected SharedPreferences pref;
    protected SharedPreferences.Editor prefEditor;
    protected static final int PRIVATE_MODE = 0;
    protected static final String BT_INDEX = "savedBtDeviceIndex";
    // Set up Bluetooth
    protected BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    protected Set<BluetoothDevice> pairedDevices;
    protected ArrayList<String> pairedDeviceNames = new ArrayList<>();
    protected ArrayList<String> pairedDeviceAddresses = new ArrayList<>();
    protected int selectedBtDeviceIndex;
    private BluetoothService mBtService = null;
    // Serial commands for the Arduino
    protected static final int CMD_START = 0x53; // "S"
    protected static final int CMD_STOP = 0x73;  // "s"
    protected static final int CMD_RESET = 0x72; // "r"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        strokeCount = (TextView)findViewById(R.id.stroke_count);
        strokeCount.setText(String.format("%d", strokeCountValue));
        timeCount = (Chronometer)findViewById(R.id.time_count);
        averageCount = (TextView)findViewById(R.id.average_count);
        averageCount.setText(String.format("%.0f", averageCountValue));
        startButton = (Button)findViewById(R.id.start_button);
        startButton.setText(R.string.start_button_label);
        startButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_white_36dp, 0, 0, 0);
        resetButton = (Button)findViewById(R.id.reset_button);
        connectButton = (Button)findViewById(R.id.connect_button);
        connectState = (TextView)findViewById(R.id.connect_state);
        connectState.setText(R.string.bt_not_connected);
        selectedBtDevice = (TextView)findViewById(R.id.selected_bt_device);

        // Retrieve the saved BT device index
        pref = getApplicationContext().getSharedPreferences("MyPref", PRIVATE_MODE);
        selectedBtDeviceIndex = pref.getInt(BT_INDEX, -1);  // if value was never saved, set to -1
        if(selectedBtDeviceIndex > -1){
            // Get the currently paired devices
            GetPairedBtDevices();
            if(pairedDeviceNames.size() > 0) {
                // The list may have shortened, if some BT device was deleted. If the saved index was bigger than
                // the current list size, select the last device in the list to avoid an error:
                selectedBtDeviceIndex = pairedDeviceNames.size() > selectedBtDeviceIndex ? selectedBtDeviceIndex : pairedDeviceNames.size() - 1;
                selectedBtDevice.setText(pairedDeviceNames.get(selectedBtDeviceIndex));
            }
        }

        // Set up the average count display
        timeCount.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                if(counterIsRunning){
                    double minutes = (double)((SystemClock.elapsedRealtime() - timeCount.getBase())/1000)/60;
                    // This avoids the display of Infinity/NaN
                    if(minutes > 0) {
                        averageCountValue = strokeCountValue / minutes;
                        averageCount.setText(String.format("%.0f", averageCountValue));
                    }
                }
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        if (mBtService == null) {
            setupBt();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.select_bt_device) {
            if(mBluetoothAdapter.isEnabled()) {
                if (pairedDeviceNames.size() > 0) {
                    // Show the Dialog
                    DialogFragment dialog = SelectBtDeviceDialog.newInstance(pairedDeviceNames);
                    dialog.show(getFragmentManager(), "Select_bt_device");
                }
            }
            return true;
        }

        if(id == R.id.about){
            new AboutDialog().show(getFragmentManager(), "About");
        }

        return super.onOptionsItemSelected(item);
    }

    // Interface called when user selects BT device from SelectBtDeviceDialog
    public void onSelectBtDevice(int which){
        selectedBtDeviceIndex = which;
        selectedBtDevice.setText(pairedDeviceNames.get(selectedBtDeviceIndex));
        // Save the selected device index to sharedPreferences:
        prefEditor = pref.edit();
        prefEditor.putInt(BT_INDEX, selectedBtDeviceIndex);
        prefEditor.apply();
    }

    public void onStartClickHandler(View view){
        byte[] commandStart = {CMD_START};
        byte[] commandStop = {CMD_STOP};
        if(!counterIsRunning){
            counterIsRunning = true;
            mBtService.write(commandStart);
            startButton.setText(R.string.stop_button_label);
            startButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop_white_36dp, 0, 0, 0);
            // (Re)start the timer
            timeCount.setBase(SystemClock.elapsedRealtime() - timeCountValue);
            timeCount.start();
        }
        else{
            counterIsRunning = false;
            mBtService.write(commandStop);
            // Store the elapsed time and stop the timer
            timeCountValue = SystemClock.elapsedRealtime() - timeCount.getBase();
            timeCount.stop();
            startButton.setText(R.string.start_button_label);
            startButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_white_36dp, 0, 0, 0);
        }
    }

    public void onResetClickHandler(View view){
        byte[] commandReset = {CMD_RESET};
        mBtService.write(commandReset);
        // Reset the count and timer
        strokeCountValue = 0;
        timeCount.setBase(SystemClock.elapsedRealtime());
        timeCountValue = 0;
    }

    public void onConnectClickHandler(View view){

        if(mBluetoothAdapter.isEnabled() && pairedDevices.size() > 0 ){
            // Get the device MAC address
            String mac = pairedDeviceAddresses.get(selectedBtDeviceIndex);
            // Get the BluetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac);
            // Attempt to connect to the device
            mBtService.connect(device);
        }
    }

    private void setupBt(){
        // Initialize the BluetoothService to perform bluetooth connections
        mBtService = new BluetoothService(this, mBtHandler);
    }

    public void GetPairedBtDevices(){
        if(mBluetoothAdapter.isEnabled()) {
            // Get the list of paired devices and save the names & MAC addresses
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                pairedDeviceNames.clear();
                pairedDeviceAddresses.clear();
                for (BluetoothDevice device : pairedDevices) {
                    pairedDeviceNames.add(device.getName());
                    pairedDeviceAddresses.add(device.getAddress());
                }
            }
        }
        else {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mBtHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            connectState.setText(R.string.bt_connected);
                            connectButton.setEnabled(false);
                            // The Arduino may or may not have the counter enabled, stop & reset it here
                            mBtService.write(new byte[] {CMD_RESET});  // "r"
                            mBtService.write(new byte[] {CMD_STOP});  // "s"
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            connectState.setText(R.string.bt_connecting);
                            connectButton.setEnabled(false);
                            break;
                        case BluetoothService.STATE_NONE:
                            connectState.setText(R.string.bt_not_connected);
                            connectButton.setEnabled(true);
                            break;
                    }
                    break;
                // Bluetooth thread sends back the message it has sent
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    // Get the string sent from ConnectedThread
                    String readMessage = (String)msg.obj;
                    // Handle the START, STOP, RESET messages
                    if(readMessage.equals("START")){
                        counterIsRunning = true;
                    }
                    else if (readMessage.equals("STOP")){
                        counterIsRunning = false;
                    }
                    else if (readMessage.equals("RESET")) {
                        strokeCount.setText("0");
                    }
                    else{
                        try{
                            strokeCountValue = Integer.parseInt(readMessage);
                        } catch(NumberFormatException e) { /* NaN */ }
                        strokeCount.setText(readMessage);
                    }
                    break;
            }
        }
    };
}
