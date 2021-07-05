package com.example.rtk_coordinate;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ViewDebug;
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

    private String Satellite = "0";
    private String Coordinate = "위도 : \n경도 : ";
    private String Accuracy = "0m";
    private String RTKstate = "Invalid";

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
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewRTKState)).setText(RTKstate);
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewSatellite)).setText(Satellite);
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewCoordinate)).setText(Coordinate);
            ((TextView) ((Activity) mMain).findViewById(R.id.textViewAccuracy)).setText(Accuracy);
        }
    };

    @Override
    public void run() {
//        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        String strHex = "";
        String[] splitResult;
        String[] splitData;
        String result = "";
        String strBytes = "";
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

                    result = byteToHexString(buffer, bytes);
                    // GNGGA
//                    result = "B56201131C0000000000F43D6C1689AF97ED7B08B61843333116F0F2200062E70000F8A624474E4747412C3038323933342E35302C333535362E36373031302C4E2C31323634302E39363338322C452C312C31322C302E37302C32312E312C4D2C31382E382C4D2C2C2A37360D0A";
                    // GNGNS
//                    result = "B56201131C0000000000B465DA1ABAB197EDF007B618AC333116E4112400D13801003A6F24474E474E532C3035303832322E35302C333535362E36373139382C4E2C31323634302E39363133382C452C41414E412C31342C302E37322C31382E312C31382E382C2C2C562A32430D0AB5";
//                    result = "B56201131C0000000000C4A62A06D7B197ED780AB6183A363116E115140032FE0000173424474E4747412C3034343430302E35302C333535362E36373332352C4E2C31323634302E39353836352C452C312C31322C302E38342C32362E302C4D2C31382E382C4D2C2C2A37460D";
                    Log.d("TAG:readStream", "result : " + result);

                    // 전체 들어오는 대로 이어붙이기
                    strBytes = strBytes + result;
                    Log.d("TAG:readStream", "strBytes : " + strBytes);


                    // UBX 시작부분 찾으면
                    if (strBytes.contains("B562")) {
                        B562.addAll(Arrays.asList(strBytes.split("B562")));

//                        Log.d("TAG:readStream", "B562 length : " + B562.length());
                        Log.d("TAG:readStream", "B562 size() : " + B562.size());

                        // 뭔가 이상, 코드 이해 불가.
//                        if (!strBytes.substring(0, 4).equals("B562")) {
//                            B562.set(0, "B562" + B562.get(0));
//                        }
                        for (int i = 1; i < B562.size(); i++) {
                            B562.set(i, "B562" + B562.get(i));
                            Log.d("TAG:readStream", "B562[" + i + "].length : " + B562.get(i).length());
                        }
                        // strBytes의 마지막 4글자가 B562라면, split으로 인해 제거된 B562 새로운 arrayList에 추가
                        if (strBytes.substring(strBytes.length() - 4).equals("B562")) {
                            B562.add("B562");
                        }

                        // strBytes가 B5에서 끝나 split이 제대로 안되었다면 분리
                        int index = B562.get(B562.size() - 1).indexOf("0A");
                        if (index > 72) {
                            if (index != -1) {
                                if (index + 1 < B562.get(B562.size() - 1).length() - 1) {
                                    B562.add(B562.get(B562.size() - 1).substring(index + 2));
                                    B562.set(B562.size() - 2, B562.get(B562.size() - 2).substring(0, index + 2));
                                }
                            }
                        }

                        // 21-07-02 220 Bytes 제거
                        // arrayList의 마지막 array가 제대로 한 문장이 들어오지 않은 경우 이어서 받도록 구현
                        // 길이가 72 초과, 마지막 두 글자가 "OA"인 경우
                        if (!(B562.get(B562.size()-1).length() > 72) || !(B562.get(B562.size()-1).substring(B562.get(B562.size()-1).length()-2)).equals("0A")) {
                            strBytes = B562.get(B562.size()-1);
                        } else {
                            strBytes = "";
                        }

//                        if (!B562.get(B562.size() - 1).contains("0A")) {
//                            strBytes = B562.get(B562.size() - 1);
//                        }

                        // 21-07-02 220 Bytes 제거
                        // UBX + GNGNS 제대로 한문장 들어온 것 찾기
                        for (int i = B562.size() - 1; i > 0; i--) {
                            // if (B562.get(i).contains("0A"))
                            if ((B562.get(B562.size()-1).length() > 72) && (B562.get(B562.size()-1).substring(B562.get(B562.size()-1).length()-2)).equals("0A")) {
                                strHex = B562.get(i);
                                break;
                            }
                        }

                        Log.d("TAG:readStream", "strHex : " + strHex);

                        B562.clear();
                        // TODO : B562.clear() 뒤로 보내고 다른 초기화 필요한 배열들 초기화 진행

                        // GNGGA 파싱 및 분리
                        // UBX는 파싱 그대로 진행

                        if (!strHex.equals("")) {
                            // GNGGA
                            splitResult = strHex.split("24474E474741");
                            splitResult[1] = "24474E474741" + splitResult[1];
                            // GNGNS
//                            splitResult = strHex.split("24474E474E53");
//                            splitResult[1] = "24474E474E53" + splitResult[1];

                            Log.d("TAG:readStream", "splitData[0].length : " + splitResult[0].length());
                            Log.d("TAG:readStream", "splitData[1].length : " + splitResult[1].length());
                            // splitResult[0] 에는 UBX hex, splitResult[1] 에는 GNGNA hex 상태.

                            // splitResult[1] 를 ASCII로 변경
                            splitResult[1] = hexToAscii(splitResult[1]);
                            Log.d("TAG:readStream", "strHex : " + strHex);
                            Log.d("TAG:readStream", "str : " + splitResult[1]);

                            // Accuracy 추출
                            String accuracy = splitResult[0].substring(60, 68);
                            int accuracyInt = toLittleEndian(accuracy);

                            StringBuffer accuracyString = new StringBuffer();
                            accuracyString.append(Integer.toString(accuracyInt));
                            accuracyString.insert(1, ".");
                            Log.d("TAG:readStream", "StringBuffer : " + accuracyString);


                            Accuracy = accuracyString + "m";
                            Log.d("TAG:readStream", "test : " + Accuracy);

                            // [STATE] splitResult[0] : UBX hex, splitResult[1] : GNGGA ASCII

                            // GNGGA 파싱 부분 ########################################################################################################
                            // strUBX[1]

                            splitData = splitResult[1].split(",");

                            if ((!splitData[2].equals("")) && (!splitData[4].equals(""))) {
                                // 위도 계산
                                up = Double.parseDouble(splitData[2].substring(0, 2));              // [state] 35.00 double
                                down = Double.parseDouble(splitData[2].substring(2));               // [state] 56.67005 double
                                down /= 60;
                                latitude = up + down;

                                // 경도 계산
                                up = Double.parseDouble(splitData[4].substring(0, 3));
                                down = Double.parseDouble(splitData[4].substring(3));
                                down /= 60;
                                longitude = up + down;

                                Coordinate = "위도 : " + latitude + "\n경도 : " + longitude;

                                Log.d("TAG:test", "latitude : " + latitude);
                                Log.d("TAG:test", "longitude : " + longitude);

                                // TODO : Accuracy minimum 값일 때 저장
                                // minLatitude, minLongitude

                                // RTK 상태
                                if(splitData[6].equals("0")) {
                                    RTKstate = "Invalid";
                                } else if(splitData[6].equals("1")) {
                                    RTKstate = "3D";
                                } else if(splitData[6].equals("2")) {
                                    RTKstate = "DGNSS";
                                } else if(splitData[6].equals("4")) {
                                    RTKstate = "Fixed RTK";
                                } else if(splitData[6].equals("5")) {
                                    RTKstate = "Float RTK";
                                } else if(splitData[6].equals("6")) {
                                    RTKstate = "Dead Reckoning";
                                }

                                // 위성 개수
//                                ((TextView) ((Activity) mMain).findViewById(R.id.textView_Satellite_num)).setText(splitData[7]);
                                Satellite = splitData[7];


                                // UI 기록
                                Message msg = handler.obtainMessage();
                                handler.sendMessage(msg);
                            }
                        }

                        strHex = "";


                        Log.d("TAG:readStream", "");
                    }
                }
            } catch (IOException e) {
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


