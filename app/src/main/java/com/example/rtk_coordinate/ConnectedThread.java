package com.example.rtk_coordinate;

import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private InputStreamReader isReader;
    private BufferedReader reader;
    private StringBuffer sb = new StringBuffer();
    private double PUBLIC_LATITUDE = 35.9427195389;
    private double PUBLIC_LONGITUDE = 126.68026195;
    private double latitude = 0.0;
    private double longitude = 0.0;


    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;       // 읽는 데 사용
        OutputStream tmpOut = null;     // 쓰는 데 사용

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
//        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        String str;
        String[] splitData;
        double up;
        double down;

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if (bytes != 0) {
//                    buffer = new byte[1024];
//                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
//                    bytes = mmInStream.available(); // how many bytes are ready to be read?
//                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
//                    Log.d("TAG:readStream", "readStream : " + bytes);
                    isReader = new InputStreamReader(mmInStream);
                    reader = new BufferedReader(isReader);
                    while((str = reader.readLine())!=null) {
//                        Log.d("TAG:readStream", "readStream : " + str + "\n");
                        if (str.contains("GPGGA") || str.contains("GNGGA")) { // GPGGA, GNGGA를 포함하고 있는 경우
                            Log.d("TAG:readStream", "readStream : " + str + "\n");

                            splitData = str.split(",");

//                            for(int i=0; i<splitData.length;i++) {
//                                Log.d("TAG:split", "split[" + i + "] " + splitData[i]);
//                            }

                            // 위도 계산
                            up = Double.parseDouble(splitData[2].substring(0, 2));               // 35.00 double
                            down = Double.parseDouble(splitData[2].substring(2));          // 56.67005 double
                            down /= 60;
                            latitude = up + down;

                            // 경도 계산
                            up = Double.parseDouble(splitData[4].substring(0,3));
                            down = Double.parseDouble(splitData[4].substring(3));
                            down /= 60;
                            longitude = up + down;

                            Log.d("TAG:test", "latitude : " + latitude);
                            Log.d("TAG:test", "longitude : " + longitude);

                            distanceByHaversine(latitude, longitude, PUBLIC_LATITUDE, PUBLIC_LONGITUDE);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes();           //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }

    public static double distanceByHaversine(double lat1, double longi1, double lat2, double longi2) {
        double distance;
        double radius = 6371;   // 지구 반지름(km)
        double toRadian = Math.PI / 180;

        double deltaLatitude = Math.abs(lat1 - lat2) * toRadian;
        double deltaLongitude = Math.abs(longi1 - longi2) * toRadian;

        double sinDeltaLat = Math.sin(deltaLatitude / 2);
        double sinDeltaLng = Math.sin(deltaLongitude / 2);
        double squareRoot = Math.sqrt(sinDeltaLat * sinDeltaLat + Math.cos(lat1 * toRadian) * Math.cos(lat2 * toRadian) * sinDeltaLng * sinDeltaLng);

        distance = 2 * radius * Math.asin(squareRoot);

        Log.d("TAG:distance", "distance : " + distance);

        return distance;
    }
}
