package com.example.exercise1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InformationService extends Service{
    NotificationManager notificationManager;    //알림 매니저
    Notification.Builder builder;   //알림 생성
    ActionReceiver actionReceiver;

    @Override
    public IBinder onBind(Intent intent){   //액티비티와의 통신용
        return null;
    }
    @Override
    public void onCreate(){ //최초 한 번 호출
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags,int startId){
        //서비스 호출시 마다 실행

        return super.onStartCommand(intent,flags,startId);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();  //서비스 종료시 실행

    }
}
