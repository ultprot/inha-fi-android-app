package com.example.exercise1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private double initialLatitude; //시작 시의 위도
    private double initialLongitude;    //시작 시의 경도
    private int areaCode;
    GPSTracker gps=null;
    public MyHandler myHandler=new MyHandler();

    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            if(msg.what==GPSTracker.RENEW_GPS){
                makeNewGpsService();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView executeBtn = findViewById(R.id.imageView4);
        executeBtn.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent executeIntent = new Intent(getApplicationContext(),ExecuteActivity.class);
                executeIntent.putExtra("latitude",initialLatitude);
                executeIntent.putExtra("longitude",initialLongitude);
                executeIntent.putExtra("code",areaCode);
                startActivity(executeIntent);
            }
        });
        if(gps==null){
            gps=new GPSTracker(this,myHandler);
        }else{
            gps.Update();
        }
        if(gps.canGetLocation()){
            initialLatitude=gps.getLatitude();
            initialLongitude=gps.getLongitude();
        }else{
            gps.showSettingsAlert();
        }
        Log.d("내 로그","여기까지는 올 걸?");
        makePostData(Double.toString(initialLatitude),Double.toString(initialLongitude));
    }

    public void makeNewGpsService(){
        if(gps==null){
            gps=new GPSTracker(this,myHandler);
        }else{
            gps.Update();
        }
    }

    public void makePostData(String lat,String lon){
        Log.d("내 로그","post 들어옴");
        JSONObject currentLocation=new JSONObject();
        try{
            currentLocation.accumulate("lat",initialLatitude);
            currentLocation.accumulate("lon",initialLongitude);
        }catch(Exception e)
        {

        }


        OkHttpClient client=new OkHttpClient();
        RequestBody jsonBody=RequestBody.create(MediaType.parse("application/json")
                ,currentLocation.toString());

        Request request=new Request.Builder()
                .url("http://14.63.161.4:26531/areaclass")
                .post(jsonBody)
                .build();
        client.newCall(request).enqueue(makePostDataCallBack);
    }
    private Callback makePostDataCallBack=new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.d("내 로그","오류 발생");
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            final String responseData=response.body().string();
            try{
                JSONObject result=new JSONObject(responseData);
                areaCode=result.optInt("largeCd");
                Log.d("내 로그","areaCode"+areaCode);
                Log.d("내 로그","결과"+result.optInt("largeCd"));
            }catch(Exception e){

            }
        }
    };

}
