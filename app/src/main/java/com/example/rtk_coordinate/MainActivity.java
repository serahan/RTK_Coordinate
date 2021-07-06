package com.example.rtk_coordinate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView textViewState;
    TextView textViewAccuracy;
    TextView textViewCoordinate;
    TextView textViewRTKState;
    TextView textViewSatellite;
    Button btnConnect;
    Button btnStop;
    ListView listView;
    LinearLayout linearLayoutMiddle;
    LinearLayout linearLayoutLower;

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothSocket btSocket = null;
    ConnectedThread connectedThread;
    public static boolean stateStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectedThread.InitExam(this);

        // Get permission
        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // variables
        textViewState = (TextView) findViewById(R.id.textViewState);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnStop = (Button) findViewById(R.id.btnStop);
        listView = (ListView) findViewById(R.id.listView);
        textViewAccuracy = (TextView) findViewById(R.id.textViewAccuracy);
        textViewCoordinate = (TextView) findViewById(R.id.textViewCoordinate);
        textViewRTKState = (TextView) findViewById(R.id.textViewRTKState);
        textViewSatellite = (TextView) findViewById(R.id.textViewSatellite);
        linearLayoutMiddle = (LinearLayout) findViewById(R.id.LinearLayoutMiddle);
        linearLayoutLower = (LinearLayout) findViewById(R.id.LinearLayoutLower);

        // Show paired devices
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

//        btnStop.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // TODO
//            }
//        });

        // 통신
        listView.setOnItemClickListener(new myOnItemClickListener());
    }

    public void onClickButtonPaired(View view) {
        listView.setVisibility(View.VISIBLE);

        btArrayAdapter.clear();
        if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
            deviceAddressArray.clear();
        }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
            }
        }
    }

    public void onClickButtonStop(View view) {
        if(stateStop == false) {
            stateStop = true;
        } else {
            stateStop = false;
        }
    }

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

            textViewState.setText("Connecting...");
            textViewState.setTextColor(Color.DKGRAY);

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device);
                btSocket.connect();
            } catch (IOException e) {
                flag = false;
                textViewState.setText("failed");
                textViewState.setTextColor(Color.RED);
                e.printStackTrace();
            }

            // start bluetooth communication
            if (flag) {
                textViewState.setText(name);
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
                listView.setVisibility(View.GONE);
                linearLayoutMiddle.setVisibility(View.VISIBLE);
                linearLayoutLower.setVisibility(View.VISIBLE);
            }

        }
    }

    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }
}