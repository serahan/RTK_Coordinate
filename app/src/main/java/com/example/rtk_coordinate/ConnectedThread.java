package com.example.rtk_coordinate;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private InputStreamReader isReader;
    private BufferedReader reader;

    private double latitude = 0.0;
    private double longitude = 0.0;
    private double distance = 0.0;

    private String FIX_state = "NO FIX";
    private String Satellite = "0";
    private String Coordinate = "위도 : \n경도 : ";
    private String Accuracy = "0m";

    static Context mMain;
    List<Double> listLatitude = new ArrayList<>();
    List<Double> listLongitude = new ArrayList<>();

    public static void InitExam(Context main) {
        mMain = main;
    }

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

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
//            ((TextView) ((Activity) mMain).findViewById(R.id.textViewGNSS)).setText(FIX_state);
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewSatellite)).setText(Satellite);
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewCoordinate)).setText(Coordinate);
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewAccuracy)).setText(Accuracy);
        }
    };

    @Override
    public void run() {
//        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        String str;
        String[] strUBX;
        String[] splitData;
        String StrToHex = "";
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

                    while ((str = reader.readLine()) != null) {
                        Log.d("TAG:readStream", "readStream : " + str + "\n");

                        // 첫 hex 값이 b5 이고, $ 발견을 못했다면 전부 들어온 게 아니므로 str 저장
                        // 첫 hex 값이 b5가 아니라면, 이전 값의 뒷 부분이 들어온 것이므로 str에 이어붙임

                        // => GNGGA가 포함되어 있으면 전체 문구가 들어온 것이고,
                        // GNGGA가 없으면 전체가 들어오지 않았으므로

                        if (str.contains("GPGGA") || str.contains("GNGGA")) { // GPGGA, GNGGA를 포함하고 있는 경우
                            Log.d("TAG:readStream", "readStream : " + str + "\n");

                            if(!StrToHex.equals(null)) { // TODO : "" 이 null로 들어가는지 확인
                                str = StrToHex + str;
                            }

                            // TODO : GNGGA로 나누고 진행할 것 / UBX에 , 가 들어왔을 경우 망함
                            strUBX = str.split("GNGGA,");
                            // strUBX[0] 에는 UBX가, strUBX[1]에는 GNGGA 이후 내용이 들어가 있음
                            // strUBX[1]에 $GNGGA 추가해줌
                            strUBX[1] = "$GNGGA," + strUBX[1];
                            strUBX[0] = strUBX[0].substring(0, strUBX[0].length() - 1);

                            splitData = str.split(",");

//                            for(int i=0; i<splitData.length;i++) {
//                                Log.d("TAG:split", "split[" + i + "] " + splitData[i]);
//                            }

                            // UBX 파싱 ====================================================================================================================
                            String UBX = "";
                            UBX = splitData[0].replace("$GNGGA","");
                            Log.d("TAG:readStream", "UBX : " + UBX + "\n");
                            // todo


                            if ((!splitData[2].equals("")) && (!splitData[4].equals(""))) {
                                // 위도 계산
                                up = Double.parseDouble(splitData[2].substring(0, 2));              // 35.00 double
                                down = Double.parseDouble(splitData[2].substring(2));               // 56.67005 double
                                down /= 60;
                                latitude = up + down;

                                // 경도 계산
                                up = Double.parseDouble(splitData[4].substring(0, 3));
                                down = Double.parseDouble(splitData[4].substring(3));
                                down /= 60;
                                longitude = up + down;

                                Log.d("TAG:test", "latitude : " + latitude);
                                Log.d("TAG:test", "longitude : " + longitude);

                                listLatitude.add(new Double(latitude));
                                listLongitude.add(new Double(longitude));

                                if (((TextView) ((Activity) mMain).findViewById(R.id.textViewRTKState)).getText().equals("NO FIX")) {
//                                    ((TextView) ((Activity) mMain).findViewById(R.id.textviewGNSS)).setText("3D FIX");
                                    FIX_state = "3D FIX";
                                }

                                // 위성 개수
//                                ((TextView) ((Activity) mMain).findViewById(R.id.textView_Satellite_num)).setText(splitData[7]);
                                Satellite = splitData[7];

                                if (((TextView) ((Activity) mMain).findViewById(R.id.btnStop)).getText().equals("고정 모드")) {
                                    // do nothing
                                } else if (((TextView) ((Activity) mMain).findViewById(R.id.btnStop)).getText().equals("일반 모드")) {
//                                    ((TextView) ((Activity) mMain).findViewById(R.id.textview_Coordinate_LatLng)).setText("위도 : " + latitude + "\n" + "경도 : " + longitude);
                                    Coordinate = latitude + "\n" + longitude;
//                                    distance = distanceByHaversine(latitude, longitude, PUBLIC_LATITUDE, PUBLIC_LONGITUDE);

                                    String strDistance = Double.toString(distance);

                                    // 소수점 탐색
                                    int dot = strDistance.indexOf(".");

                                    if (distance >= 1) {
//                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 4) + "m");
                                        Accuracy = strDistance.substring(0,dot) + "km " + strDistance.substring(dot + 1, dot + 4) + "m";
                                        Log.d("TAG:textviewAccuracy", "textviewAccuracy : " + strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 4) + "m");
                                    } else if ((distance >= 0.001) && (distance < 1)) {
//                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
                                        Accuracy = strDistance.substring(dot + 1, dot + 4) + "m " + strDistance.substring(dot + 4, dot + 6) + "cm";
                                        Log.d("TAG:textviewAccuracy", "textviewAccuracy : " + strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
                                    } else if (distance < 0.001) {
//                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 4, dot + 6) + "cm");
                                        Accuracy = strDistance.substring(dot + 4, dot + 6) + "cm";
                                        Log.d("TAG:textviewAccuracy", "textviewAccuracy : " + strDistance.substring(dot + 4, dot + 6) + "cm");
                                    }

//                            ((TextView) ((Activity) mMain).findViewById(R.id.textviewAccuracy)).setText("" + distance);     //??????
                                }

                                Message msg = handler.obtainMessage();
                                handler.sendMessage(msg);

                                // TODO : StrToHex 초기화
                                StrToHex = "";
                            }

//                            ((TextView) ((Activity) mMain).findViewById(R.id.buttonRecalculate)).setOnClickListener(new Button.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    Log.d("TAG:Recalculate", "================================클릭-끝================================");
//                                    int size = listLatitude.size();
//
//                                    // 초기화
//                                    calculateLatitude = 0.0;
//                                    calculateLongitude = 0.0;
//
//                                    for (int i = 0; i < size; i++) {
//                                        calculateLatitude += listLatitude.get(i);
//                                        calculateLongitude += listLongitude.get(i);
//                                    }
//                                    calculateLatitude /= size;
//                                    calculateLongitude /= size;
//
//                                    Log.d("TAG:Recalculate", "latitude : " + calculateLatitude);
//                                    Log.d("TAG:Recalculate", "longitude : " + calculateLongitude);
//
//                                    // 위도 경도 표시
////                                    ((TextView) ((Activity) mMain).findViewById(R.id.textview_Coordinate_LatLng)).setText("위도 : " + calculateLatitude + "\n" + "경도 : " + calculateLongitude);
//                                    Coordinate = calculateLatitude + "\n" + calculateLongitude;
//
//                                    distance = distanceByHaversine(calculateLatitude, calculateLongitude, PUBLIC_LATITUDE, PUBLIC_LONGITUDE);
//                                    Log.d("TAG:Recalculate", "distance : " + distance);
//                                    String strDistance = Double.toString(distance);
//
//                                    // 소수점 탐색
//                                    int dot = strDistance.indexOf(".");
//
//                                    if (distance >= 1) {
////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 3) + "m");
//                                        Accuracy = strDistance.substring(0,dot) + "km " + strDistance.substring(dot + 1, dot + 4) + "m";
//                                        Log.d("TAG:textviewAccuracy2", "textviewAccuracy : " + strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 3) + "m");
//                                    } else if ((distance >= 0.001) && (distance < 1)) {
////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
//                                        Accuracy = strDistance.substring(dot + 1, dot + 4) + "m " + strDistance.substring(dot + 4, dot + 6) + "cm";
//                                        Log.d("TAG:textviewAccuracy2", "textviewAccuracy : " + strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
//                                    } else if (distance < 0.001) {
////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 4, dot + 6) + "cm");
//                                        Accuracy = strDistance.substring(dot + 4, dot + 6) + "cm";
//                                        Log.d("TAG:textviewAccuracy2", "textviewAccuracy : " + strDistance.substring(dot + 4, dot + 6) + "cm");
//                                    }
//
//                                    Message msg = handler.obtainMessage();
//                                    handler.sendMessage(msg);
//                                }
//                            });

                        } else {
                            StrToHex += str;
                        }
                    }
                } else {
                    // 수신된 좌표 데이터가 없을때
                    if (((TextView) ((Activity) mMain).findViewById(R.id.textViewRTKState)).getText().equals("3D FIX")) {
//                        ((TextView) ((Activity) mMain).findViewById(R.id.textviewGNSS)).setText("NO FIX");
                        FIX_state = "NO FIX";

                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
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

    public String toHex(String arg) {
        return String.format("%x", new BigInteger(1, arg.getBytes()));
    }

    // 1번
//    public static double distanceByHaversine(double lat1, double longi1, double lat2, double longi2) {
//        // 공식에서는 지구가 완전한 구형이라고 가정
//        // 실제 지구는 적도 쪽이 좀 더 길쭉한 타원형이라 완벽히 정확하다 할 수 없음
//        double distance;
//        double radius = 6371;   // 지구 반지름(km)
//        double toRadian = Math.PI / 180;
//
//        double deltaLatitude = Math.abs(lat1 - lat2) * toRadian;
//        double deltaLongitude = Math.abs(longi1 - longi2) * toRadian;
//
//        double sinDeltaLat = Math.sin(deltaLatitude / 2);
//        double sinDeltaLng = Math.sin(deltaLongitude / 2);
//        double squareRoot = Math.sqrt(sinDeltaLat * sinDeltaLat + Math.cos(lat1 * toRadian) * Math.cos(lat2 * toRadian) * sinDeltaLng * sinDeltaLng);
//
//        distance = 2 * radius * Math.asin(squareRoot);
//
//        Log.d("TAG:distance", "distance : " + distance);
//
//        return distance;
//    }

    // 2번
    public static double distanceByHaversine(double lat1, double longi1, double lat2, double longi2) {
        double deg2radMultiplier = Math.PI / 180;
        lat1 = lat1 * deg2radMultiplier;
        longi1 = longi1 * deg2radMultiplier;
        lat2 = lat2 * deg2radMultiplier;
        longi2 = longi2 * deg2radMultiplier;

        double radius = 6378.137;
        double dlng = longi2 - longi1;
        double distance = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(dlng)) * radius;

        Log.d("TAG:distance", "distance : " + distance);

        return distance;
    }
}


