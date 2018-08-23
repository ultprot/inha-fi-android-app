package com.example.exercise1;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.speech.SpeechRecognizer.isRecognitionAvailable;

public class InfoService extends Service {

    String speechRecognitionResult; //음성인식 결과 문자열
    Intent i;

    static int areaCode;
    static String lastIntent=null;  //서버에서 받은 D.F.의 마지막 인텐트
    static String lastSessionID=null;   //서버에서 받은 D.F.의 마지막 세션 아이디
    static String station;  //
    static String busNum;
    static String count;
    static JSONObject lastjson=null; //서버에서 결과로 받은 마지막 json
    static JSONArray lastpois=null;    //서버에서 받은 마지막 poi들 정보 배열
    static JSONObject exacpoi=null; //서버에서 받은 poi중 원하는 poi정보
    static JSONObject buspath=null; //길찾기 버스경로 정보
    static JSONObject subwaypath=null;  //길찾기 지하철경로 정보
    static JSONObject intepath=null;    //길찾기 복합 경로 정보
    static JSONObject pedespath=null;   //길찾기 보행 경로 정보
    private TextToSpeech myTTS; //음성합성 개체
    private SpeechRecognizer mySpeechRecognizer;    //음성 인식 개체
    private Double latitude;    //현재 위도
    private Double longitude;   //현재 경도
    protected AudioManager mAudioManager;   //블루투스 제어용 오디오 매니저 객체
    GPSTracker gps=null;    //gps조작용 GPSTracker 객체

    public MyHandler myHandler=new InfoService.MyHandler();
    class MyBinder extends Binder {
        InfoService getService(){
            return InfoService.this;
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            if(msg.what==GPSTracker.RENEW_GPS){
                makeNewGpsService();
            }
        }
    }

    public void infoPostData(String url){
        Log.d("내 로그","post data 들어옴");
        JSONObject jsonObject=new JSONObject();
        try{
            String lat=Double.toString(gps.getLatitude());
            String lon=Double.toString(gps.getLongitude());
            String code=Integer.toString(areaCode);
            jsonObject.accumulate("query",speechRecognitionResult);
            jsonObject.accumulate("lat",lat);
            jsonObject.accumulate("lon",lon);
            jsonObject.accumulate("areaCode",code);
            if(lastIntent!=null){
                jsonObject.accumulate("sessionID",lastSessionID);
                if(lastIntent.equals("destination_poi_select")){
                    jsonObject.put("query",exacpoi.optString("name"));
                    jsonObject.accumulate("endLat",exacpoi.optString("frontLat"));
                    jsonObject.accumulate("endLon",exacpoi.optString("frontLon"));
                }else if(lastIntent.equals("search_select")){
                    jsonObject.accumulate("endLat",exacpoi.optString("frontLat"));
                    jsonObject.accumulate("endLon",exacpoi.optString("frontLon"));
                }else{

                }
            }
        }catch(Exception e)
        {

        }
        OkHttpClient client=new OkHttpClient();
        RequestBody jsonBody=RequestBody.create(MediaType.parse("application/json;charset=UTF-8"),
                jsonObject.toString());
        Request request=new Request.Builder()
                .url(url)
                .post(jsonBody)
                .build();
        client.newCall(request).enqueue(infoPostDataCallBack);
    }

    private Callback infoPostDataCallBack=new Callback(){

        @Override
        public void onFailure(Call call, IOException e) {

        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            final String responseData=response.body().string();
            JSONObject rstJson;
            String ffText="";
            try{
                rstJson=new JSONObject(responseData);
                lastIntent=rstJson.optString("intent");
                if(rstJson.has("data"))
                {
                    lastjson= rstJson.optJSONObject("data");
                }
                if(rstJson.has("sessionID"))
                {
                    lastSessionID=rstJson.optString("sessionID");
                }
                if(rstJson.has("fulfillmentText"))
                {
                    ffText=rstJson.optString("fulfillmentText");
                }
                if(lastIntent.equals("bus_search")){
                    station=rstJson.optString("station");
                    busNum=rstJson.optString("num");
                    count=rstJson.optString("count");
                }
            }catch(Exception e){
                ffText="error";
            }
            if(lastIntent!=null){
                if(lastIntent.equals("destination")){
                    String namestr="";
                    JSONObject poijson=lastjson.optJSONObject("searchPoiInfo");
                    poijson=poijson.optJSONObject("pois");
                    lastpois=poijson.optJSONArray("poi");

                    int len=lastpois.length();

                    if(len>5){
                        len=5;
                    }
                    if(len==0)
                    {
                        speak("결과가 없습니다.");
                    }else{
                        for(int i=0;i<len;i++){
                            JSONObject tempjson;
                            String tempstr;
                            try{
                                tempjson=lastpois.getJSONObject(i);
                                tempstr=tempjson.optString("name");
                            }
                            catch(JSONException e){
                                tempstr="error";
                            }
                            namestr+=Integer.toString(i+1);
                            namestr+="번, ";
                            namestr+=tempstr;
                            namestr+=", ";
                        }
                        ffText+=namestr;
                    }
                }else if(lastIntent.equals("destination_poi_select")){
                    if(lastjson.has("number")){
                        int poiNumber=lastjson.optInt("number");
                        exacpoi=lastpois.optJSONObject(poiNumber-1);
                    }else if(lastjson.has("dest")){
                        String poidest=lastjson.optString("dest");
                        for(int i=0;i<lastpois.length();i++){
                            String th;
                            String tha;
                            th=lastpois.optJSONObject(i).optString("name")
                                    .replaceAll("[^\\uAC00-\\uD7A3xfe0-9a-zA-Z\\\\s]"
                                            ,"");
                            tha=poidest.replaceAll("[^\\uAC00-\\uD7A3xfe0-9a-zA-Z\\\\s]"
                                    ,"");
                            if(th.equals(tha)){
                                exacpoi=lastpois.optJSONObject(i);
                            }
                        }
                    }
                    float radius=Float.parseFloat(exacpoi.optString("radius"));
                    if(radius<=0.5){
                        infoPostData("http://14.63.161.4:26531/pedes");
                    }else
                    {
                        infoPostData("http://14.63.161.4:26531/query");
                    }
                }
                else if(lastIntent.equals("destination_path_results")
                        ||lastIntent.equals("search_destination")){
                    JSONArray pubpath=lastjson.optJSONObject("result").optJSONArray("path");
                    int cur=1;
                    for(int i=0;i<pubpath.length();i++){
                        try{
                            if(pubpath.getJSONObject(i).optInt("pathType")==cur)
                            {
                                if(cur==1)
                                {
                                    subwaypath=pubpath.getJSONObject(i);
                                    cur++;
                                }else if(cur==2){
                                    buspath=pubpath.getJSONObject(i);
                                    cur++;
                                }else if(cur==3){
                                    intepath=pubpath.getJSONObject(i);
                                    cur++;
                                }else{

                                }
                            }
                        }catch(JSONException e){

                        }
                    }
                }else if(lastIntent.equals("destination_path_select")){
                    switch(lastjson.optString("transportation")){
                        case "s":
                            buspath=null;
                            intepath=null;
                            Toast.makeText(getApplicationContext()
                                    ,subwaypath.toString(),Toast.LENGTH_SHORT).show();
                            break;
                        case "b":
                            subwaypath=null;
                            intepath=null;
                            Toast.makeText(getApplicationContext()
                                    ,buspath.toString(),Toast.LENGTH_SHORT).show();
                            break;
                        case "m":
                            buspath=null;
                            subwaypath=null;
                            Toast.makeText(getApplicationContext()
                                    ,intepath.toString(),Toast.LENGTH_SHORT).show();
                            break;
                    }
                    lastIntent=null;
                    lastSessionID=null;
                    lastpois=null;
                    exacpoi=null;
                    lastjson=null;
                }
                else if(lastIntent.equals("pedes_search")){
                    pedespath=lastjson;
                    Toast.makeText(getApplicationContext()
                            ,pedespath.toString(),Toast.LENGTH_SHORT);
                    lastIntent=null;
                    lastSessionID=null;
                    lastpois=null;
                    exacpoi=null;
                    lastjson=null;
                }
                else if(lastIntent.equals("bus_search")){
                    String temp=station+"에 도착할 예정인 "+busNum+"번 버스에"
                            +count+"명이 타고 있습니다.";
                    ffText=temp;
                }else if(lastIntent.equals("search")){
                    String namestr="";
                    JSONObject poijson=lastjson.optJSONObject("searchPoiInfo");
                    poijson=poijson.optJSONObject("pois");
                    lastpois=poijson.optJSONArray("poi");

                    int len=lastpois.length();

                    if(len>5){
                        len=5;
                    }
                    if(len==0){
                        speak("결과가 없습니다.");
                    }else{
                        for(int i=0;i<len;i++){
                            JSONObject tempjson;
                            String tempstr;
                            try{
                                tempjson=lastpois.getJSONObject(i);
                                tempstr=tempjson.optString("name");
                            }catch(JSONException e){
                                tempstr="error";
                            }
                            namestr+=Integer.toString(i+1);
                            namestr+="번, ";
                            namestr+=tempstr;
                            namestr+=", ";
                        }
                        ffText+=namestr;
                    }
                }
                else if(lastIntent.equals("search_select")){
                    if(lastjson.has("number")){
                        int poiNumber=lastjson.optInt("number");
                        exacpoi=lastpois.optJSONObject(poiNumber-1);
                    }else if(lastjson.has("dest")){
                        String poidest=lastjson.optString("dest");
                        for(int i=0;i<lastpois.length();i++){
                            String th;
                            String tha;
                            th=lastpois.optJSONObject(i).optString("name")
                                    .replaceAll("[^\\uAC00-\\uD7A3xfe0-9a-zA-Z\\\\s]"
                                            ,"");
                            tha=poidest.replaceAll("[^\\uAC00-\\uD7A3xfe0-9a-zA-Z\\\\s]","");
                            if(th.equals(tha))
                            {
                                exacpoi=lastpois.optJSONObject(i);
                            }
                        }
                    }
                    ffText=exacpoi.optString("name")+"에 관한 정보입니다."
                            +exacpoi.optString("desc")+"거리는 "
                            +exacpoi.optString("radius")+"킬로미터 입니다.";
                }else if(lastIntent.equals("search_destination")){
                    float radius=Float.parseFloat(exacpoi.optString("radius"));
                    if(radius<=0.5)
                    {
                        infoPostData("http://14.63.161.4:26532/pedes");
                    }
                    else
                    {
                        JSONArray pubpath=lastjson.optJSONObject("result")
                                .optJSONArray("path");
                        int cur=1;
                        for(int i=0;i<pubpath.length();i++)
                        {
                            try{
                                if(pubpath.getJSONObject(i).optInt("pathType")==cur)
                                {
                                    if(cur==1)
                                    {
                                        subwaypath=pubpath.getJSONObject(i);
                                        cur++;
                                    }
                                    else if(cur==2)
                                    {
                                        buspath=pubpath.getJSONObject(i);
                                        cur++;
                                    }
                                    else if(cur==3)
                                    {
                                        intepath=pubpath.getJSONObject(i);
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
                        infoPostData("http://14.63.161.4:26532/query");
                    }
                }
            }
            speak(ffText);
        }
    };

    private void initializeTextToSpeech(){
        myTTS=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                Log.d("내 로그", "tts 초기화");
                if(myTTS.getEngines().size()==0){
                    //finish();
                }else{
                    myTTS.setLanguage(Locale.KOREA);
                }
            }
        });
    }


    private void processResult(String s){
        speechRecognitionResult=s;
        Log.d("내 로그","인식 결과"+speechRecognitionResult);
        infoPostData("http://14.63.161.4:26531/query");
    }

    private void speak(String message){
        if (Build.VERSION.SDK_INT >= 21) {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private IBinder mIBinder=new MyBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public void onCreate(){
        initializeTextToSpeech();
        mAudioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        mySpeechRecognizer=SpeechRecognizer.createSpeechRecognizer(this);
        mySpeechRecognizer.setRecognitionListener(myListener);
        if(gps==null){
            gps=new GPSTracker(this,myHandler);
        }else{
            gps.Update();
        }
        if(gps.canGetLocation()){
            latitude=gps.getLatitude();
            longitude=gps.getLongitude();
        }else{

        }
    }

    private RecognitionListener myListener=new RecognitionListener() {

        @Override
        public void onReadyForSpeech(Bundle bundle) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int i) {

        }

        @Override
        public void onResults(Bundle bundle) {
            List<String> res = bundle.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
            );
            processResult(res.get(0));
        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        areaCode=intent.getIntExtra("code",-1);
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy(){
        myTTS.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }

    public void getInfo(){
        Log.d("내 로그", "getInfo 들어옴");
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.KOREAN);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        //mAudioManage.setMode(AudioManager.MODE_IN_COMMUNICATION);
        //mAudioManager.startBluetoothSco();
        //mAudioManager.setBluetoothScoOn(true);
        Log.d("내 로그","듣기 직전");

        mySpeechRecognizer.startListening(intent);
    }

    public void makeNewGpsService(){    //gps사용을 위한 서비스 생성
        if(gps==null){
            gps=new GPSTracker(this,myHandler);
        }else{
            gps.Update();
        }
    }
}
