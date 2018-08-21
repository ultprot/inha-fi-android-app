package com.example.exercise1;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;

import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.List;
import java.util.Locale;


import android.support.v7.app.AlertDialog;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import android.Manifest;
import android.telephony.SmsManager;


public class ExecuteActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{


    Button button;
    Button logoutBtn;
    String speechRecognitionResult;
    //백버튼 처리를 위한 변수
    long first_time;
    long second_time;

    static String lastIntent = null;
    static String lastSessionID = null;
    static JSONObject lastjson = null;
    static JSONArray lastpois=null;
    static JSONObject exacpoi=null;
    static JSONObject buspath=null;
    static JSONObject subwaypath=null;
    static JSONObject intepath=null;
    static JSONObject pedespath=null;
    private TextToSpeech myTTS;
    private SpeechRecognizer mySpeechRecognizer;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;

    private Button mMessage;
    private Button mCall;
    private final int PERMISSIONS_REQUEST_RESULT = 1;

    TextView Path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);

        //문자전송
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_RESULT);
        }

        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String userId = pref.getString("userId","");
        String gard = pref.getString("gard","");

        Log.d("기록","이용자 아이디 : "+userId);
        Log.d("기록","이용자 보호자 연락처 : "+gard);

        Path=findViewById(R.id.textView);
        if (getIntent().hasExtra("org.examples.SOMETHING")) {
            String text = getIntent().getExtras().getString("org.examples.SOMETHING");
        }//end

        initializeTextToSpeech();
        initializeSpeechRecognizer();

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

        Button logoutBtn = (Button) findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 서버에 유저정보 삭제 & 앱 file에 저장된 정보 삭제
                new SendPostDelete().execute("http://14.63.161.4:26533/deleteuser");

                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);

                String loginWay = pref.getString("login","");
                if(loginWay == "gogle"){
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
                }else{
                    //kakaologout 수행
                    Log.d("알림","카카오 logout");
                    onClickLogout();
                }

            }
        });

        Button startBtn = (Button) findViewById(R.id.startButton);
        startBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i;
                i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);            //intent 생성
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                mySpeechRecognizer.startListening(i);
            }
        });

        Button enrollBtn = (Button) findViewById(R.id.Enroll);
        enrollBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent(getApplicationContext(), EnrollActivity.class);
                startIntent.putExtra("org.examples.ENROLL", "enrolling");
                startActivity(startIntent);
            }
        });
        //end speech recognition


        mMessage = (Button) findViewById(R.id.smsBtn);
        mCall = (Button) findViewById(R.id.callBtn);

        mMessage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                String getGard = pref.getString("gard","");
                Messenger messenger = new Messenger(getApplicationContext());
                messenger.sendMessage(getGard, "위급상황입니다");
            }
        });
        mCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:01030159163")));
            }
        });


    }

    //벡버튼 두번눌리면 종료
    @Override
    public void onBackPressed() {
        second_time = System.currentTimeMillis();
        Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        if(second_time - first_time < 2000){
            super.onBackPressed();
            finishAffinity();
        }
        first_time = System.currentTimeMillis();
    }

        public class PostTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                JSONObject jsonObject = new JSONObject();
                Log.d("내 로그","json 오브젝트 만듬");
                String lat = "37.5395634";
                String lon = "127.0017441";
                jsonObject.accumulate("query", speechRecognitionResult);
                jsonObject.accumulate("lat", lat);
                jsonObject.accumulate("lon", lon);
                Log.d("내 로그","쿼리, 경위도 추가");
                Log.d("내 로그","lastIntent"+lastIntent);
                Log.d("내 로그","lastSession"+lastSessionID);
                if(lastIntent!=null) {
                    if (lastIntent.equals("destination")) {
                        Log.d("내 로그","poi_result로 들어옴");
                        jsonObject.accumulate("sessionID",lastSessionID);
                    } else if (lastIntent.equals("destination_poi_select")) {
                        jsonObject.accumulate("sessionID", lastSessionID);
                        jsonObject.put("query",exacpoi.optString("name"));
                        jsonObject.accumulate("endLat",exacpoi.optString("frontLat"));
                        jsonObject.accumulate("endLon",exacpoi.optString("frontLon"));
                    } else if (lastIntent.equals("destination_path_results")) {
                        jsonObject.accumulate("sessionID",lastSessionID);

                    } else if (lastIntent == "destination_path_select") {
                        jsonObject.accumulate("sessionID",lastSessionID);
                    } else {
                        Log.d("내 로그", "해당사항 없지롱");
                    }
                }
                HttpURLConnection con = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(urls[0]);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");//POST method
                    con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
                    con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
                    con.setRequestProperty("Accept", "application/json");//서버 response json
                    con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
                    con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
                    con.connect();//서버로 보내기위해서 스트림 만듬
                    OutputStream outStream = con.getOutputStream();//버퍼를 생성하고 넣음
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();//버퍼를 받아줌
                    //서버로 부터 데이터를 받음
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line;
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

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            JSONObject rstJson;
            String ffText = "";
            try {
                rstJson = new JSONObject(result);
                lastIntent=rstJson.optString("intent");
                if(rstJson.has("data"))
                {
                    lastjson=rstJson.optJSONObject("data");
                }
                if(rstJson.has("sessionID"))
                {
                    lastSessionID=rstJson.optString("sessionID");
                }
                ffText = rstJson.optString("fulfillmentText");
            } catch (JSONException e) {
                ffText = "error";
            }
            if(lastIntent!=null)
            {
                if(lastIntent.equals("destination"))
                {
                    Log.d("내 로그","인텐트 파악함");

                    String namestr="";
                    JSONObject poijson=lastjson.optJSONObject("searchPoiInfo");
                    poijson=poijson.optJSONObject("pois");
                    lastpois=poijson.optJSONArray("poi");

                    int len=lastpois.length();
                    Log.d("내 로그", "갯수 찾아냄"+len);

                    if(len>5)
                    {
                        len=5;
                    }
                    if(len==0)
                    {
                        speak("결과가 없습니다.");
                    }
                    else
                    {
                        for(int i=0;i<len;i++)
                        {
                            JSONObject tempjson;
                            String tempstr;
                            try
                            {
                                tempjson=lastpois.getJSONObject(i);
                                tempstr=tempjson.optString("name");
                            }
                            catch(JSONException e)
                            {
                                tempstr="error";
                            }
                            namestr+=Integer.toString(i+1);
                            namestr+="번, ";
                            namestr+=tempstr;
                            namestr+=", ";
                        }
                        Log.d("내 로그",namestr);
                        ffText+=namestr;
                    }
                }
                else if(lastIntent.equals("destination_poi_select"))
                {
                    Log.d("내 로그","poi_select로 파악");
                    if(lastjson.has("number"))
                    {
                        int poiNumber=lastjson.optInt("number");
                        exacpoi=lastpois.optJSONObject(poiNumber);
                    }
                    else if(lastjson.has("dest"))
                    {
                        String poidest=lastjson.optString("dest");
                        for(int i=0;i<lastpois.length();i++)
                        {
                            String th;
                            String tha;
                            th=lastpois.optJSONObject(i).optString("name").replaceAll("[^\\uAC00-\\uD7A3xfe0-9a-zA-Z\\\\s]","");
                            tha=poidest.replaceAll("[^\\uAC00-\\uD7A3xfe0-9a-zA-Z\\\\s]","");
                            if(th.equals(tha))
                            {
                                exacpoi=lastpois.optJSONObject(i);
                            }
                        }
                    }
                    float radius=Float.parseFloat(exacpoi.optString("radius"));
                    if(radius<=0.5)
                    {
                        new ExecuteActivity.PostTask().execute("http://14.63.161.4:26531/pedes");//AsyncTask 시작시킴
                    }
                    else
                    {
                        new ExecuteActivity.PostTask().execute("http://14.63.161.4:26531/query");//AsyncTask 시작시킴
                    }
                }
                else if(lastIntent.equals("destination_path_results"))
                {
                    JSONArray pubpath=lastjson.optJSONObject("result").optJSONArray("path");
                    int cur=1;
                    for(int i=0;i<pubpath.length();i++)
                    {
                        try{
                            if(pubpath.getJSONObject(i).optInt("pathType")==cur)
                            {
                                if(cur==1)
                                {
                                    subwaypath=pubpath.getJSONObject(i);
                                    Log.d("내 로그","지하철 경로"+subwaypath);
                                    cur++;
                                }
                                else if(cur==2)
                                {
                                    buspath=pubpath.getJSONObject(i);
                                    Log.d("내 로그","버스 경로"+buspath);
                                    cur++;
                                }
                                else if(cur==3)
                                {
                                    intepath=pubpath.getJSONObject(i);
                                    Log.d("내 로그","복합 경로"+intepath);
                                    cur++;
                                }
                                else
                                {
                                }
                            }
                        }
                        catch(JSONException e)
                        {

                        }
                    }
                }
                else if(lastIntent.equals("destination_path_select"))
                {
                    switch(lastjson.optString("transportation")) {
                        case "s":
                            Log.d("내 로그",subwaypath.toString());
                            buspath = null;
                            intepath = null;
                            Path.setText(subwaypath.toString());
                            break;
                        case "b":
                            Log.d("내 로그",buspath.toString());
                            subwaypath = null;
                            intepath = null;
                            Path.setText(buspath.toString());
                            break;
                        case "m":
                            Log.d("내 로그",intepath.toString());
                            buspath = null;
                            subwaypath = null;
                            Path.setText(intepath.toString());
                            break;
                    }
                    lastIntent = null;
                    lastSessionID = null;
                    lastpois=null;
                    exacpoi=null;
                    lastjson=null;
                }
                else if(lastIntent.equals("pedes_search"))
                {
                    pedespath=lastjson;
                    Path.setText(pedespath.toString());
                    lastIntent = null;
                    lastSessionID = null;
                    lastpois=null;
                    exacpoi=null;
                    lastjson=null;
                }

            }
            speak(ffText);
            Log.d("내 로그","말 할 것"+ffText);
            //Log.d("내 로그","lastIntent"+lastIntent+'\n'+"lastSessionID"+lastSessionID);
        }
    }

    //initialize
    private void initializeTextToSpeech() {
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (myTTS.getEngines().size() == 0) {
                    Toast.makeText(ExecuteActivity.this, "There is no TTs engine", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    myTTS.setLanguage(Locale.KOREA);
                }
            }
        });
    }//!initializeTextToSpeech

    //speech recognizer
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            mySpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mySpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int error) {
                }

                @Override
                public void onResults(Bundle results) {
                    List<String> res = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                    );
                    processResult(res.get(0));
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
    }//!initializeSpeechRdcognizer

    //use result of speech recognizer
    private void processResult(String s) {
        speechRecognitionResult = s;
        Log.d("내 로그","음성 인식은 됨."+speechRecognitionResult);
        new ExecuteActivity.PostTask().execute("http://14.63.161.4:26531/query");//AsyncTask 시작시킴

    }//!processResult

    //speak string
    private void speak(String message) {
        if (Build.VERSION.SDK_INT >= 21) {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }//!speak

    @Override
    protected void onPause() {
        super.onPause();
        myTTS.shutdown();
    }//!onPause


    private void onClickLogout() {
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {//로그아웃 성공 시
                Log.e("redirect","in redirect Main Activity");
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
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while((line = reader.readLine()) != null){
                        buffer.append(line);
                    }
                    return buffer.toString();//서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
                } catch (MalformedURLException e){
                    e.printStackTrace();
                } catch (IOException e) {
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

            if(result == "fail"){
                Log.d("저장","삭제중 서버오류");
            }else {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor ed = pref.edit();
                ed.clear();
                ed.commit();

                Log.d("저장", "앱 파일 모두 데이터삭제");

                Log.d("저장", "삭제결과 : " + result);
            }
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v("알림", "onConnectionFailed");
    }

    // 로그아웃
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
                                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.clear();
                                editor.commit();
                                Log.d("저장","모든 file 기록 삭제");
                                Log.v("알림", "로그아웃 성공");
                                setResult(1);
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
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
