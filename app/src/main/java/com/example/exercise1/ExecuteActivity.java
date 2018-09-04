package com.example.exercise1;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.support.v7.app.AlertDialog;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import android.Manifest;
import android.telephony.SmsManager;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class ExecuteActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{
    public static Context mExecute;

    private BluetoothSPP bt;
    EditText receiveText;
    String gX;  //자이로센서 값 x,y,z축 변수
    String gY;
    String gZ;
    String acX; //가속도센서 값 x,y,z축 변수
    String acY;
    String acZ;
    Button sendDataBtn;
    boolean dataB;


    Button button;
    Button logoutBtn;


    //백버튼 처리를 위한 변수
    long first_time;
    long second_time;

    private int areaCode;

    public FirebaseAuth mAuth;
    public GoogleApiClient mGoogleApiClient;

    private Button mMessage;
    private Button mCall;
    private final int PERMISSIONS_REQUEST_RESULT = 1;
    public Timer timer;
    int i;

    TextView dataView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);
        startService(new Intent(ExecuteActivity.this,MainService.class)
                .putExtra("code",areaCode));

        mExecute = this;
        i=0;

        //data 전송
        sendDataBtn=findViewById(R.id.sendDataBtn);
        dataView=findViewById(R.id.dataTextView);

        //블루투스
        bt = new BluetoothSPP(this); //Initializing
        receiveText = findViewById(R.id.bluResultText);

        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Log.e("블투","사용불가");
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        //데이터 수신
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.d("블투수신","데이터수신중");
                String aa = "";
                aa= message + aa;
                Log.d("블투수신","수신 데이터 : "+aa);
                try{
                    JSONObject json = new JSONObject(aa);
                    gX=json.getString("angleX");
                    gY=json.getString("angleY");
                    gZ = json.getString("angleZ");
                    acX=json.getString("accX");
                    acY=json.getString("accY");
                    acZ=json.getString("accZ");
                    Log.d("블투수신","자이로 : "+gX + ","+ gY+","+gZ);
                    Log.d("블투수신","가속도 : "+acX+","+acY+","+acZ);

                    Log.d("데이터","전송 시작");

                    dataB=true;
                    sendDataBtn = (Button)findViewById(R.id.sendDataBtn);
                    sendDataBtn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dataB=false;
                            new SendPostData().execute("http://14.63.161.4:26533/dataEmer");
                        }
                    });
                    if(dataB==true)
                        new SendPostData().execute("http://14.63.161.4:26533/data");

                }catch (Exception err){
                    Log.e("블투","json도중 에러");
                }

                receiveText.setText("자이로:"+gX + ","+ gY+","+gZ+"가속도:"+acX+","+acY+","+acZ);
            }
        });

        //연결됐을 때
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Log.d("블투","연결완료"+name);
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
            }

            //연결해제
            public void onDeviceDisconnected() {
                Log.d("블투","연결해제");
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            //연결실패
            public void onDeviceConnectionFailed() {
                Log.e("블투","연결실패");
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        //불루투스 연결시도
        if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
            Log.e("블투","연결시도 if");
            bt.disconnect();
        } else {
            Log.e("블투","연결시도 else");
            Intent bluIntent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(bluIntent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    //!블루투스


        //문자전송
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_RESULT);
        }

        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String userId = pref.getString("userId","");
        String gard = pref.getString("gard","");

        Log.d("기록","이용자 아이디 : "+userId);
        Log.d("기록","이용자 보호자 연락처 : "+gard);

        if(getIntent().hasExtra("code"))
        {
            areaCode=getIntent().getIntExtra("cityCode",-1);
        }

        // GoogleSignInOptions 생성
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder
                (GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this )
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // 로그인 작업의 onCreate 메소드에서 FirebaseAuth 개체의 공유 인스턴스를 가져옵니다.
        mAuth = FirebaseAuth.getInstance();

        logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 // 서버에 유저정보 삭제 & 앱 file에 저장된 정보 삭제
                 //new SendPostDelete().execute("http://14.63.161.4:26533/deleteuser");

                 SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);

                 String loginWay = pref.getString("login", "");
                 if (loginWay.equals("google")) {
                     Log.v("알림", "구글 LOGOUT");
                     AlertDialog.Builder alt_bld = new AlertDialog.Builder(v.getContext());
                     alt_bld.setMessage("로그아웃 하시겠습니까?").setCancelable(false)
                             .setPositiveButton("네",
                                     new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // 네 클릭
                                            // 로그아웃 함수 call
                                            signOut();
                                        }
                             }).setNegativeButton("아니오",
                             new DialogInterface.OnClickListener() {
                                 public void onClick(DialogInterface dialog, int id) {
                                     // 아니오 클릭. dialog 닫기.
                                     dialog.cancel();
                                 }
                             });
                     AlertDialog alert = alt_bld.create();
                     // 대화창 클릭시 뒷 배경 어두워지는 것 막기
                     alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                     // 대화창 제목 설정
                     alert.setTitle("로그아웃");
                     // 대화창 아이콘 설정
                     //alert.setIcon(R.drawable.check_dialog_64);
                     // 대화창 배경 색 설정
                     alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(255, 62, 79, 92)));
                     alert.show();
                     } else {
                         //kakaologout 수행
                         Log.d("알림", "카카오 logout");
                     AlertDialog.Builder alt_bld = new AlertDialog.Builder(v.getContext());
                     alt_bld.setMessage("로그아웃 하시겠습니까?").setCancelable(false)
                             .setPositiveButton("네",
                                     new DialogInterface.OnClickListener() {
                                         public void onClick(DialogInterface dialog, int id) {
                                             // 네 클릭
                                             // 로그아웃 함수 call
                                             onClickLogout();
                                         }
                                     }).setNegativeButton("아니오",
                             new DialogInterface.OnClickListener() {
                                 public void onClick(DialogInterface dialog, int id) {
                                     // 아니오 클릭. dialog 닫기.
                                     dialog.cancel();
                                 }
                             });
                     AlertDialog alert = alt_bld.create();
                     // 대화창 클릭시 뒷 배경 어두워지는 것 막기
                     alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                     // 대화창 제목 설정
                     alert.setTitle("로그아웃");
                     // 대화창 아이콘 설정
                     //alert.setIcon(R.drawable.check_dialog_64);
                     // 대화창 배경 색 설정
                     alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(255, 62, 79, 92)));
                     alert.show();
                    }
             }
        });

    }
    //벡버튼 두번눌리면 종료
    @Override
    public void onBackPressed() {
        second_time = System.currentTimeMillis();
        Toast.makeText(getApplicationContext(), "'뒤로' 버튼을 한 번 더 누르면 종료됩니다."
                , Toast.LENGTH_SHORT).show();
        if (second_time - first_time < 2000) {
            super.onBackPressed();
            finishAffinity();
        }
        first_time = System.currentTimeMillis();
    }

//블루투스
    public void onDestroy() {
        Log.e("블투", "onDestroy()");
        super.onDestroy();
        //bt.stopService(); //블루투스 중지
    }

    public void onStart() {
        Log.e("블투","onStart()");
        super.onStart();
        if (!bt.isBluetoothEnabled()) { //
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
                setup();
            }
        }
    }

    public void setup() {
        Log.e("블투","on setup");
        bt.send("S", true);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("블투","on onActivityResult");
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    //!블루투스


    public class SendPostData extends  AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... urls) {
            try {
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("data", "androidTest");
                jsonObject.accumulate("X_g", gX);   //자이로값
                jsonObject.accumulate("Y_g", gY);
                jsonObject.accumulate("Z_g", gZ);
                jsonObject.accumulate("X_a",acX);   //가속도값
                jsonObject.accumulate("Y_a",acY);
                jsonObject.accumulate("Z_a",acZ);
                Log.d("데이터", "json만들기 완료 : " + jsonObject);

                HttpURLConnection con = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(urls[0]);
                    //연결을 함
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");//POST방식으로 보냄
                    con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
                    con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
                    con.setRequestProperty("Accept", "text/html");//서버에 response 데이터를 html로 받음
                    con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
                    con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
                    con.connect();
                    //서버로 보내기위해서 스트림 만듬
                    OutputStream outStream = con.getOutputStream();
                    //버퍼를 생성하고 넣음
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();//버퍼를 받아줌
                    //서버로 부터 데이터를 받음
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    return buffer.toString();//서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    try {
                        if (reader != null) {
                            reader.close();//버퍼를 닫아줌
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void onClickLogout() {
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {//로그아웃 성공 시
                Log.d("알림","kakao로그아웃 성공");
                Log.e("redirect","in redirect Main Activity");
                new SendPostDelete().execute("http://14.63.161.4:26533/deleteuser");
                Intent mainIntent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(mainIntent);
            }
        });
    }

    public class SendPostDelete extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String[] urls) {
            try {
                Log.e("저장","in delete");
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                JSONObject jsonObject = new JSONObject();

                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                String userId = pref.getString("userId","");
                Log.e("저장","기존아이디삭제 : "+userId);

                jsonObject.accumulate("userId", userId);

                HttpURLConnection con = null;
                BufferedReader reader = null;
                try{
                    //"http://14.63.161.4:26533/deleteuser"
                    URL url = new URL(urls[0]);
                    //연결을 함
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");//POST방식으로 보냄
                    con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
                    con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
                    con.setRequestProperty("Accept", "text/html");//서버에 response 데이터를 html로 받음
                    con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
                    con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
                    con.connect();
                    //서버로 보내기위해서 스트림 만듬
                    OutputStream outStream = con.getOutputStream();
                    //버퍼를 생성하고 넣음
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();//버퍼를 받아줌
                    //서버로 부터 데이터를 받음
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder buffer = new StringBuilder();
                    String line ;
                    while((line = reader.readLine()) != null){
                        buffer.append(line);
                    }
                    return buffer.toString();//서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
                } catch (MalformedURLException e){
                    e.printStackTrace();
                } finally {
                    if(con != null){
                        con.disconnect();
                    }
                    try {
                        if(reader != null){
                            reader.close();//버퍼를 닫아줌
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if(result.equals("fail")){
                Log.d("저장","삭제중 서버오류");
            }else {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor ed = pref.edit();
                ed.clear();
                ed.apply();

                Log.d("저장", "앱 파일 모두 데이터삭제");

                Log.d("저장", "삭제결과 : " + result);
            }
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v("알림", "onConnectionFailed");
    }

    //구글 로그아웃
    public void signOut() {
        mGoogleApiClient.connect();

        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                mAuth.signOut();

                if (mGoogleApiClient.isConnected()) {
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                new SendPostDelete().execute("http://14.63.161.4:26533/deleteuser");
                                Log.d("저장","모든 file 기록 삭제");
                                Log.v("알림", "로그아웃 성공");
                                setResult(1);
                                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(mainIntent);
                            } else {
                                setResult(0);
                            }
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.v("알림", "Google API Client Connection Suspended");
                setResult(-1);
                finish();
            }
        });
    }

    //sms 전송 , 응급 전화
    class Messenger {
        private Context mContext;
        public Messenger(Context mContext) {
            this.mContext = mContext;
        }

        @RequiresApi(api = Build.VERSION_CODES.DONUT)
        public void sendMessage(String phoneNum, String message) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNum, null, message, null, null);
            Toast.makeText(mContext, "Message transmission is completed.",
                    Toast.LENGTH_SHORT).show();
        }
    }

}


