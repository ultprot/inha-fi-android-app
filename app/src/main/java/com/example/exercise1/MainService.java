package com.example.exercise1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainService extends Service {

    NotificationManager notificationManager;
    Notification.Builder builder;
    ActionReceiver actionReceiver;

    private InfoService infoService;
    private boolean isInfoBind;
    private int areaCode;

    ServiceConnection infoServiceConnection=new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            InfoService.MyBinder myBinder=(InfoService.MyBinder) iBinder;
            infoService=myBinder.getService();
            isInfoBind=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            infoService=null;
            isInfoBind=false;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        actionReceiver=new ActionReceiver();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags,int startId){
        areaCode=intent.getIntExtra("code",-1);
        notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel functionChannel=new NotificationChannel("0","functions"
                    ,NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(functionChannel);
            builder=new Notification.Builder(this,"0");
        }else{
            builder=new Notification.Builder(this);
        }


        Intent infoIntent=new Intent("INFO");
        infoIntent.putExtra("action","info");
        Intent logoutIntent=new Intent("LOGOUT");
        logoutIntent.putExtra("action","logout");
        Intent exitIntent=new Intent("EXIT");
        exitIntent.putExtra("action","exit");
        IntentFilter filter=new IntentFilter();
        filter.addAction("INFO");
        filter.addAction("LOGOUT");
        filter.addAction("EXIT");

        PendingIntent infoPendingIntent=PendingIntent.getBroadcast(MainService.this
                ,0,infoIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent logoutPendgingIntent=PendingIntent.getBroadcast(MainService.this
                ,0,logoutIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent exitPendingIntent=PendingIntent.getBroadcast(MainService.this
                ,0,exitIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action infoAction=new Notification.Action.Builder(
                Icon.createWithResource(this,R.drawable.ic_launcher_background),
                "질문",infoPendingIntent
        ).build();
        Notification.Action logoutAction=new Notification.Action.Builder(
                Icon.createWithResource(this,R.drawable.ic_launcher_background),
                "로그아웃",logoutPendgingIntent
        ).build();
        Notification.Action exitAction=new Notification.Action.Builder(
                Icon.createWithResource(this,R.drawable.ic_launcher_background),
                "종료",exitPendingIntent
        ).build();

        builder.setSmallIcon(R.drawable.ic_logo);
        builder.setContentTitle("SmartCane");
        builder.setContentText("길위의 도우미 입니다.");
        builder.setAutoCancel(true);
        builder.setContentIntent(infoPendingIntent);
        builder.setContentIntent(logoutPendgingIntent);
        builder.setContentIntent(exitPendingIntent);
        builder.addAction(infoAction);
        builder.addAction(logoutAction);
        builder.addAction(exitAction);

        registerReceiver(actionReceiver,filter);

        startService(new Intent(MainService.this,InfoService.class)
                .putExtra("code",areaCode));
        bindService(new Intent(MainService.this,InfoService.class)
                ,infoServiceConnection,BIND_AUTO_CREATE);

        notificationManager.notify(0,builder.build());
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }

    public class ActionReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getStringExtra("action");
            if(action.equals("info")){
                performActionInfo();
            }
            else if(action.equals("logout")){
                performActionLogout();
            }
            else if(action.equals("exit")){
                performActionExit();
            }
        }
        public void performActionInfo()
        {
            Log.d("내 로그","질문 수행");
            infoService.getInfo();
        }
        public void performActionLogout() {
            Intent mainIntent = new Intent(getApplicationContext(), ExecuteActivity.class);
            startActivity(mainIntent);
        }
        public void performActionExit()
        {
            Log.d("내 로그","종료");
            if(isInfoBind)
            {
                unbindService(infoServiceConnection);
            }
            ((ExecuteActivity)ExecuteActivity.mExecute).Stop_Period();

            stopService(new Intent(MainService.this,InfoService.class));
            unregisterReceiver(actionReceiver);
            notificationManager.cancel(0);
            stopService(new Intent(MainService.this,MainService.class));
        }
    }

}

