package com.example.rtk_coordinate;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.UnsupportedEncodingException;


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

    // Byte로 변경
    ByteArrayInputStream byteArrayInputStream;
    ByteArrayOutputStream byteArrayOutputStream;

    public static void InitExam(Context main) {
        mMain = main;
    }

    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;       // 읽는 데 사용
        OutputStream tmpOut = null;     // 쓰는 데 사용

//        ByteArrayInputStream tmpIn2 = null;
//        ByteArrayInputStream tmpOut2 = null;

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
        String strHex = "";
        String[] strUBX;
        String[] splitData;
        String StrToHex = "";
        String result = "";
        String strBytes = "";
//        String[] B562;

        List<String> B562 = new ArrayList<String>();

        double up;
        double down;

        byte[] buffer;

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if (bytes != 0) {
                    buffer = new byte[bytes];

//                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
//                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    Log.d("TAG:readStream", "bytes : " + bytes);
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                            // 21-06-30 11:42 am
                    // byte로 읽는 걸 mmInStream.read(buffer, 0, bytes)로 변경하여 성공했다고 생각하고 Hex 변환 진행

//                    result = byteToHexString(buffer, bytes);
                    result = "B56201131C0000000000F43D6C1689AF97ED7B08B61843333116F0F2200062E70000F8A624474E4747412C3038323933342E35302C333535362E36373031302C4E2C31323634302E39363338322C452C312C31322C302E37302C32312E312C4D2C31382E382C4D2C2C2A37360D0A";
                    Log.d("TAG:readStream", "result.length : " + result.length());

                    // 전체 들어오는 대로 이어붙이기
                    strBytes = strBytes + result;

                    // UBX 시작부분 찾으면
                    if(strBytes.contains("B562")) {
                        B562.addAll(Arrays.asList(strBytes.split("B562")));

//                        Log.d("TAG:readStream", "B562 length : " + B562.length());
                        Log.d("TAG:readStream", "B562 length : " + B562.size());


                        //arraylist 변경중

                        if(!strBytes.substring(0,4).equals("B562")) {
                            B562.set(0,"B562" + B562.get(0));
                        }
                        for(int i=1;i<B562.size();i++) {
                            B562.set(i,"B562" + B562.get(i));
                            Log.d("TAG:readStream", "B562[" + i + "].length : " + B562.get(i).length());
                        }
                        if(B562.get(B562.size()-1).length() == 222) {
                            B562.add(B562.get(B562.size()-1).substring(220,222));
                            B562.set(B562.size()-2, B562.get(B562.size()-2).substring(0,220));
                        }
                        for(int i = B562.size() - 1; i > 0; i++) {
                            if(B562.get(i).length() == 220) {
                                strHex = B562.get(i);
                                break;
                            }
                        }
                        Log.d("TAG:readStream", "strHex : " + strHex);

                        // GNGGA 파싱 및 분리
                        // UBX는 파싱 그대로 진행

                        splitData = strHex.split("24474E474741");
                        splitData[1] = "24474E474741" + splitData[1];

                        Log.d("TAG:readStream", "splitData[0].length : " + splitData[0].length());
                        Log.d("TAG:readStream", "splitData[1].length : " + splitData[1].length());
                        // splitData[0] 에는 UBX hex, splitData[1] 에는 GNGGA hex 상태.

                        splitData[1] = hexToAscii(splitData[1]);
                        Log.d("TAG:readStream", "strHex : " + strHex);
                        Log.d("TAG:readStream", "str : " + splitData[1]);

                        String accuracy = splitData[0].substring(60, 68);
                        Log.d("TAG:readStream", "accuarcy : " + accuracy);

                        int test = toLittleEndian(accuracy);
                        double test_double = test * 0.0001;
                        Log.d("TAG:readStream", "test : " + test_double);


















//                        if(!strBytes.substring(0,4).equals("B562")) {
//                            B562[0] = "B562" + B562[0];
//                        }
//                        for(int i=1;i<B562.length;i++) {
//                            B562[i] = "B562" + B562[i];
//                            Log.d("TAG:readStream", "B562[" + i + "].length : " + B562[i].length());
//                        }
//                        if(B562[B562.length-1].length() == 222) {
//                            // 220에서 자르고
//                            // 그 다음 배열에 나머지 넣기
//                            // 구현할 방법 못찾음
//
//                            // B5 만 제거하는 걸로 진행
//                            B562[B562.length] = B562[B562.length-1].substring(220,222);
//                            B562[B562.length-1] = B562[B562.length-1].substring(0,220);
//
//                        }

                        Log.d("TAG:readStream", "");
                    }















                    isReader = new InputStreamReader(mmInStream);
                    reader = new BufferedReader(isReader);

//                    while ((str = reader.readLine()) != null) {
//                        Log.d("TAG:readStream", "readStream : " + str + "\n");
//
//                        // 첫 hex 값이 b5 이고, $ 발견을 못했다면 전부 들어온 게 아니므로 str 저장
//                        // 첫 hex 값이 b5가 아니라면, 이전 값의 뒷 부분이 들어온 것이므로 str에 이어붙임
//
//                        // => GNGGA가 포함되어 있으면 전체 문구가 들어온 것이고,
//                        // GNGGA가 없으면 전체가 들어오지 않았으므로
//
//                        if (str.contains("GPGGA") || str.contains("GNGGA")) { // GPGGA, GNGGA를 포함하고 있는 경우
//                            Log.d("TAG:readStream", "readStream : " + str + "\n");
//
//                            if(!StrToHex.equals("")) { // TODO : "" 이 null로 들어가는지 확인
//                                str = StrToHex + str;
//                            }
//
//                            strUBX = str.split("GNGGA,");
//
//                            // strUBX[0] 에는 UBX가, strUBX[1]에는 GNGGA 이후 내용이 들어가 있음
//                            strUBX[0] = strUBX[0].substring(0, strUBX[0].length() - 1);         // $ 제거
//                            strUBX[1] = "$GNGGA," + strUBX[1];                                  // $GNGGA 추가
//
//                            // UBX 파싱 부분 ##########################################################################################################
//                            // strUBX[0]
//
//                            // String to Byte
//                            byte[] bytesUBX = strUBX[0].getBytes();
////                            String hexText = new java.math.BigInteger(bytesUBX).toString(16);
//                            //result = byteToHexString(bytesUBX);
//                            result = byteToHexString(bytesUBX);
////                            result = stringToHex(strUBX[0]);
//                            Log.d("TAG:readStream", "result : " + result);
//
//
//
//
//
//
//                            // GNGGA 파싱 부분 ########################################################################################################
//                            // strUBX[1]
//
//                            splitData = strUBX[1].split(",");
//
////                            for(int i=0; i<splitData.length;i++) {
////                                Log.d("TAG:split", "split[" + i + "] " + splitData[i]);
////                            }
//
//                            if ((!splitData[2].equals("")) && (!splitData[4].equals(""))) {
//                                // 위도 계산
//                                up = Double.parseDouble(splitData[2].substring(0, 2));              // 35.00 double
//                                down = Double.parseDouble(splitData[2].substring(2));               // 56.67005 double
//                                down /= 60;
//                                latitude = up + down;
//
//                                // 경도 계산
//                                up = Double.parseDouble(splitData[4].substring(0, 3));
//                                down = Double.parseDouble(splitData[4].substring(3));
//                                down /= 60;
//                                longitude = up + down;
//
//                                Log.d("TAG:test", "latitude : " + latitude);
//                                Log.d("TAG:test", "longitude : " + longitude);
//
//                                listLatitude.add(new Double(latitude));
//                                listLongitude.add(new Double(longitude));
//
//                                if (((TextView) ((Activity) mMain).findViewById(R.id.textViewRTKState)).getText().equals("NO FIX")) {
////                                    ((TextView) ((Activity) mMain).findViewById(R.id.textviewGNSS)).setText("3D FIX");
//                                    FIX_state = "3D FIX";
//                                }
//
//                                // 위성 개수
////                                ((TextView) ((Activity) mMain).findViewById(R.id.textView_Satellite_num)).setText(splitData[7]);
//                                Satellite = splitData[7];
//
//                                if (((TextView) ((Activity) mMain).findViewById(R.id.btnStop)).getText().equals("고정 모드")) {
//                                    // do nothing
//                                } else if (((TextView) ((Activity) mMain).findViewById(R.id.btnStop)).getText().equals("일반 모드")) {
////                                    ((TextView) ((Activity) mMain).findViewById(R.id.textview_Coordinate_LatLng)).setText("위도 : " + latitude + "\n" + "경도 : " + longitude);
//                                    Coordinate = latitude + "\n" + longitude;
////                                    distance = distanceByHaversine(latitude, longitude, PUBLIC_LATITUDE, PUBLIC_LONGITUDE);
//
//                                    String strDistance = Double.toString(distance);
//
//                                    // 소수점 탐색
//                                    int dot = strDistance.indexOf(".");
//
//                                    if (distance >= 1) {
////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 4) + "m");
//                                        Accuracy = strDistance.substring(0,dot) + "km " + strDistance.substring(dot + 1, dot + 4) + "m";
//                                        Log.d("TAG:textviewAccuracy", "textviewAccuracy : " + strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 4) + "m");
//                                    } else if ((distance >= 0.001) && (distance < 1)) {
////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
//                                        Accuracy = strDistance.substring(dot + 1, dot + 4) + "m " + strDistance.substring(dot + 4, dot + 6) + "cm";
//                                        Log.d("TAG:textviewAccuracy", "textviewAccuracy : " + strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
//                                    } else if (distance < 0.001) {
////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 4, dot + 6) + "cm");
//                                        Accuracy = strDistance.substring(dot + 4, dot + 6) + "cm";
//                                        Log.d("TAG:textviewAccuracy", "textviewAccuracy : " + strDistance.substring(dot + 4, dot + 6) + "cm");
//                                    }
//
////                            ((TextView) ((Activity) mMain).findViewById(R.id.textviewAccuracy)).setText("" + distance);     //??????
//                                }
//
//                                Message msg = handler.obtainMessage();
//                                handler.sendMessage(msg);
//
//                                // TODO : StrToHex 초기화
//                                StrToHex = "";
//                            }
//
////                            ((TextView) ((Activity) mMain).findViewById(R.id.buttonRecalculate)).setOnClickListener(new Button.OnClickListener() {
////                                @Override
////                                public void onClick(View v) {
////                                    Log.d("TAG:Recalculate", "================================클릭-끝================================");
////                                    int size = listLatitude.size();
////
////                                    // 초기화
////                                    calculateLatitude = 0.0;
////                                    calculateLongitude = 0.0;
////
////                                    for (int i = 0; i < size; i++) {
////                                        calculateLatitude += listLatitude.get(i);
////                                        calculateLongitude += listLongitude.get(i);
////                                    }
////                                    calculateLatitude /= size;
////                                    calculateLongitude /= size;
////
////                                    Log.d("TAG:Recalculate", "latitude : " + calculateLatitude);
////                                    Log.d("TAG:Recalculate", "longitude : " + calculateLongitude);
////
////                                    // 위도 경도 표시
//////                                    ((TextView) ((Activity) mMain).findViewById(R.id.textview_Coordinate_LatLng)).setText("위도 : " + calculateLatitude + "\n" + "경도 : " + calculateLongitude);
////                                    Coordinate = calculateLatitude + "\n" + calculateLongitude;
////
////                                    distance = distanceByHaversine(calculateLatitude, calculateLongitude, PUBLIC_LATITUDE, PUBLIC_LONGITUDE);
////                                    Log.d("TAG:Recalculate", "distance : " + distance);
////                                    String strDistance = Double.toString(distance);
////
////                                    // 소수점 탐색
////                                    int dot = strDistance.indexOf(".");
////
////                                    if (distance >= 1) {
//////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 3) + "m");
////                                        Accuracy = strDistance.substring(0,dot) + "km " + strDistance.substring(dot + 1, dot + 4) + "m";
////                                        Log.d("TAG:textviewAccuracy2", "textviewAccuracy : " + strDistance.substring(0, dot) + "km  " + strDistance.substring(dot + 1, dot + 3) + "m");
////                                    } else if ((distance >= 0.001) && (distance < 1)) {
//////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
////                                        Accuracy = strDistance.substring(dot + 1, dot + 4) + "m " + strDistance.substring(dot + 4, dot + 6) + "cm";
////                                        Log.d("TAG:textviewAccuracy2", "textviewAccuracy : " + strDistance.substring(dot + 1, dot + 4) + "m  " + strDistance.substring(dot + 4, dot + 6) + "cm");
////                                    } else if (distance < 0.001) {
//////                                        ((TextView) ((Activity) mMain).findViewById(R.id.textView_Accuracy_m)).setText(strDistance.substring(dot + 4, dot + 6) + "cm");
////                                        Accuracy = strDistance.substring(dot + 4, dot + 6) + "cm";
////                                        Log.d("TAG:textviewAccuracy2", "textviewAccuracy : " + strDistance.substring(dot + 4, dot + 6) + "cm");
////                                    }
////
////                                    Message msg = handler.obtainMessage();
////                                    handler.sendMessage(msg);
////                                }
////                            });
//
//                        } else {
//                            StrToHex += str;
//                        }
//                    }
                        } else {
                            // 수신된 좌표 데이터가 없을때
                            if (((TextView) ((Activity) mMain).findViewById(R.id.textViewRTKState)).getText().equals("3D FIX")) {
//                        ((TextView) ((Activity) mMain).findViewById(R.id.textviewGNSS)).setText("NO FIX");
                                FIX_state = "NO FIX";

                                Message msg = handler.obtainMessage();
                                handler.sendMessage(msg);
                            }
                        }
                    }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
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

    // byte 배열을 16진수 String으로 변환
    public static String byteToHexString(byte[] byteArray, int bytes) {
        StringBuffer sb = new StringBuffer();
        // byteArray.length인 bytes를 byte[] 로 변환
//        byte[] intArr;
//        intArr = intToByteArray(bytes);

        for(byte b : byteArray) {
            sb.append(String.format("%02x", b).toUpperCase());
        }
        return sb.toString();
    }

    // Int를 Byte로 변경하여 진행하려고 하였으나 불가. byteArr(intArr)의 배열 개수 = 4 로 인식함.
    public static byte[] intToByteArray(int value) {
        byte[] byteArr = new byte[4];
        byteArr[3] = (byte)(value >> 24);
        byteArr[2] = (byte)(value >> 16);
        byteArr[1] = (byte)(value >> 8);
        byteArr[0] = (byte)(value);
        Log.d("TAG:readStream", "intArr : " + byteArr[3] + " " + byteArr[2] + " " + byteArr[1] + " " + byteArr[0]);
        return byteArr;
    }

    // 다른 사람 코드 // 썼더니 깨짐
    static String stringToHex(String string) {
        StringBuilder buf = new StringBuilder(200);
        for (char ch : string.toCharArray()) {
            if (buf.length() > 0)
                buf.append(' ');
            buf.append(String.format("%02x", (int) ch));
        }
        return buf.toString();
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    public static int toLittleEndian(final String hex) {
        int ret = 0;
        String hexLittleEndian = "";
        if (hex.length() % 2 != 0) return ret;
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            hexLittleEndian += hex.substring(i, i + 2);
        }
        ret = Integer.parseInt(hexLittleEndian, 16);
        return ret;
    }
}


