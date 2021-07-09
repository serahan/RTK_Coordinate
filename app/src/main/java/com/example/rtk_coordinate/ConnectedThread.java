package com.example.rtk_coordinate;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private double latitude = 0.0;
    private double longitude = 0.0;

    private double minLatitude = 0.0;
    private double minLongitude = 0.0;

    private String Satellite = "0";
    private String Coordinate = "위도 : \n경도 : ";
    private String RTKstate = "Invalid";
    private String Accuracy = "0m";
    private int minAccuracy = 0;

    static Context mMain;
    final static MainActivity mainActivity = new MainActivity();

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
                    // B562 파싱 잘못되는 문제 -> 임시로 catch문으로 이동
//                    result = "B56201131C00000000008871D80AAEAE97EDA411B61880333116DA0ED800D4DF0000DB5624474E47";
                    Log.d("TAG:readStream", "result : " + result);

                    // 전체 들어오는 대로 이어붙이기
                    strBytes = strBytes + result;
                    Log.d("TAG:readStream", "strBytes : " + strBytes);


                    // UBX 시작부분 찾으면
                    if (strBytes.contains("B562")) {
                        B562.addAll(Arrays.asList(strBytes.split("B562")));

//                        Log.d("TAG:readStream", "B562 length : " + B562.length());
                        Log.d("TAG:readStream", "B562 size() : " + B562.size());

                        for (int i = 1; i < B562.size(); i++) {
                            B562.set(i, "B562" + B562.get(i));
                            Log.d("TAG:readStream", "B562[" + i + "].length : " + B562.get(i).length());
                            Log.d("TAG:readStream", "B562[" + i + "] : " + B562.get(i));
                        }
                        // strBytes의 마지막 4글자가 B562라면, split으로 인해 제거된 B562 새로운 arrayList에 추가
                        if (strBytes.substring(strBytes.length() - 4).equals("B562")) {
                            B562.add("B562");
                        }


                        int index = 0;
                        // 마지막 0A 탐색
//                        int index = B562.get(B562.size() - 1).indexOf("0A");
                        for (int i = 0; i < B562.get(B562.size() - 1).length(); i = i + 2) {
                            String twoWords = B562.get(B562.size() - 1).substring(i, i + 2);
                            if (twoWords.equals("0A")) {
                                index = i;
                                Log.d("TAG:Index", "index : " + index);
                            }
                        }

                        // strBytes가 B5에서 끝나 split이 제대로 안되었다면 분리
                        if (index > 72) {
                            if (index != -1) {
                                if (index + 1 < B562.get(B562.size() - 1).length() - 1) {
                                    B562.add(B562.get(B562.size() - 1).substring(index + 2));
                                    B562.set(B562.size() - 2, B562.get(B562.size() - 2).substring(0, index + 2));
                                    Log.d("TAG:readStream", "B5로 끝나 split이 제대로 되지 않아 분리함");
                                    Log.d("TAG:readStream", "B562[B562.size() - 1] : " + B562.get(B562.size() - 1));
                                }
                            }
                        }

                        // 21-07-02 220 Bytes 제거
                        // arrayList의 마지막 array가 제대로 한 문장이 들어오지 않은 경우 이어서 받도록 구현
                        // 길이가 72 초과, 마지막 두 글자가 "OA"인 경우
                        if (!(index > 72) || !(B562.get(B562.size() - 1).substring(B562.get(B562.size() - 1).length() - 2)).equals("0A")) {

                            strBytes = B562.get(B562.size() - 1);
                            Log.d("TAG:readStream", "남은 부분 이어받도록");
                            Log.d("TAG:readStream", "strBytes : " + strBytes);
                        } else {
                            strBytes = "";
                        }

                        // 21-07-02 220 Bytes 제거
                        // UBX + GNGNS 제대로 한문장 들어온 것 찾기
                        for (int i = B562.size() - 1; i > 0; i--) {
                            // if (B562.get(i).contains("0A"))
                            if ((index > 72) && (B562.get(B562.size() - 1).substring(B562.get(B562.size() - 1).length() - 2)).equals("0A")) {
                                strHex = B562.get(i);
                                Log.d("TAG:readStream", "strHex : " + strHex);
                                break;
                            }
                        }

                        Log.d("TAG:readStream", "strHex : " + strHex);

                        B562.clear();

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
                            Log.d("TAG:readStream", "splitData[0] : " + splitResult[0]);
                            Log.d("TAG:readStream", "splitData[1].length : " + splitResult[1].length());
                            Log.d("TAG:readStream", "splitData[1] : " + splitResult[1]);
                            // splitResult[0] 에는 UBX hex, splitResult[1] 에는 GNGNA hex 상태.

                            // splitResult[1] 를 ASCII로 변경
                            splitResult[1] = hexToAscii(splitResult[1]);

                            // Accuracy 추출
                            String accuracy = splitResult[0].substring(60, 68);
                            int accuracyInt = 0;
                            if(accuracy.equals("FFFFFFFF")) {
                                throw new NumberFormatException();
                            } else {
                                accuracyInt = toLittleEndian(accuracy);
                            }

//                            StringBuffer accuracyString = new StringBuffer();
//                            accuracyString.append(Integer.toString(accuracyInt));
//                            accuracyString.insert(accuracyString.length() - 4, ".");

                            int test = toLittleEndian(accuracy);
//                            double test_double = test * 0.0001;

                            String test_String = Integer.toString(test);
                            if (test_String.length() <= 5) {
                                int i = test_String.length();
                                for(; i < 5; i++) {
                                    test_String = 0 + test_String;
                                }
                            }

                            StringBuffer accuracyString = new StringBuffer();
                            accuracyString.append(test_String);
                            accuracyString.insert(accuracyString.length()-4, ".");

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

                                Coordinate =  latitude + "\n" + longitude;

                                // 일시정지 시, 정확도 min값으로 변경되었을 때
                                // minLatitude, minLongitude

                                if (mainActivity.stateStop == true) {
                                    Log.d("TAG:Accuracy", "Accuracy start");
                                    if (minAccuracy == 0) {
                                        minAccuracy = accuracyInt;
                                    }
                                    Log.d("TAG:Accuracy", "minAccuracy : " + minAccuracy);
                                    Log.d("TAG:Accuracy", "AccuracyInt : " + accuracyInt);
                                    if (minAccuracy > accuracyInt) {
                                        minAccuracy = accuracyInt;
                                        minLatitude = latitude;
                                        minLongitude = longitude;
                                        Log.d("TAG:Accuracy", "minAccuracy Latitude Longitude set");

                                        Message msg = handler.obtainMessage();
                                        handler.sendMessage(msg);
                                    }
                                }

                                Log.d("TAG:readStream", "latitude : " + latitude);
                                Log.d("TAG:readStream", "longitude : " + longitude);

                                // RTK 상태
                                if (splitData[6].equals("0")) {
                                    RTKstate = "Invalid";
                                } else if (splitData[6].equals("1")) {
                                    RTKstate = "3D";
                                } else if (splitData[6].equals("2")) {
                                    RTKstate = "DGNSS";
                                } else if (splitData[6].equals("4")) {
                                    RTKstate = "Fixed RTK";
                                } else if (splitData[6].equals("5")) {
                                    RTKstate = "Float RTK";
                                } else if (splitData[6].equals("6")) {
                                    RTKstate = "Dead Reckoning";
                                }

                                // 위성 개수
//                                ((TextView) ((Activity) mMain).findViewById(R.id.textView_Satellite_num)).setText(splitData[7]);
                                Satellite = splitData[7];


                                // UI 기록
                                if (mainActivity.stateStop == false) {
                                    Message msg = handler.obtainMessage();
                                    handler.sendMessage(msg);

                                    if (minAccuracy != 0) {
                                        minAccuracy = 0;
                                    }
                                }
                            }
                        }

                        strHex = "";


                        Log.d("TAG:readStream", "");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
                strBytes = "";
                strHex = "";
                Log.d("TAG:readStream", "Error : StringindexOutOfBoundsException");
                Log.d("TAG:Error", "Error : StringindexOutOfBoundsException");
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                strBytes = "";
                strHex = "";
                Log.d("TAG:readStream", "Error : ArrayIndexOutOfBoundsException");
                Log.d("TAG:Error", "Error : ArrayIndexOutOfBoundsException");
            } catch (NumberFormatException e) {
                e.printStackTrace();
                strBytes = "";
                strHex = "";
                Log.d("TAG:readStream", "Error : NumberFormatException");
                Log.d("TAG:Error", "Error : NumberFormatException");
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

    // byte 배열을 16진수 String으로 변환
    public static String byteToHexString(byte[] byteArray, int bytes) {
        StringBuffer sb = new StringBuffer();

        for (byte b : byteArray) {
            sb.append(String.format("%02x", b).toUpperCase());
        }
        return sb.toString();
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


