package com.example.exercise1;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.widget.Toast;

import com.kakao.auth.ApiResponseCallback;
import com.kakao.auth.AuthService;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.auth.network.response.AccessTokenInfoResponse;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

import java.util.HashMap;
import java.util.Map;

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
    private SessionCallback callback;

    //백버튼 처리를 위한 변수
    long first_time;
    long second_time;
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
        if (Session.getCurrentSession().checkAndImplicitOpen()) {
            // 액세스토큰 유효하거나 리프레시 토큰으로 액세스 토큰 갱신을 시도할 수 있는 경우
            Log.e("login","login remained");

            try {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                String getUserID = pref.getString("userId", "X");
                String getGard = pref.getString("gard","X");

                if (getUserID == "X" || getGard == "X") {
                    if(getUserID=="X")
                        Log.e("저장필요","이용자아이디 없음.");
                    else
                        Log.e("저장필요","보호자번호 없음.");
                    redirectEnrollActivity();
                }else{
                    redirectExecuteActivity();
                }
            }catch (Exception err){
                redirectEnrollActivity();
            }
        } else {
            // 무조건 재로그인을 시켜야 하는 경우
            Log.e("login","have to login");

            callback = new SessionCallback();
            Session.getCurrentSession().addCallback(callback);
            Session.getCurrentSession().checkAndImplicitOpen();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }

    //for redirect page after login success
    protected void redirectExecuteActivity() {
        Log.e("redirect","in redirect Excute Activity");

        Intent executeIntent = new Intent(getApplicationContext(),ExecuteActivity.class);
        executeIntent.putExtra("latitude",initialLatitude);
        executeIntent.putExtra("longitude",initialLongitude);
        executeIntent.putExtra("code",areaCode);
        startActivity(executeIntent);
    }
    protected void redirectEnrollActivity() {
        Log.e("redirect","in redirect Singup Activity");

        Intent enrollIntent = new Intent(getApplicationContext(),EnrollActivity.class);
        startActivity(enrollIntent);
    }
    //kakao Login Callback
    private class SessionCallback implements ISessionCallback {

        // 로그인에 성공한 상태
        @Override
        public void onSessionOpened() {
            Log.e("session opned","open success");
            redirectEnrollActivity();
        }

        // 로그인에 실패한 상태
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.getMessage());
            }
    }


    // kakao 사용자 정보 요청
    public void requestMe() {
        // 사용자정보 요청 결과에 대한 Callback
        UserManagement.getInstance().requestMe(new MeResponseCallback() {
            // 세션 오픈 실패. 세션이 삭제된 경우,
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                Log.e("SessionCallback :: ", "onSessionClosed : " + errorResult.getErrorMessage());
            }

            // 회원이 아닌 경우,
            @Override
            public void onNotSignedUp() {
                Log.e("SessionCallback :: ", "onNotSignedUp");
            }

            // 사용자정보 요청에 성공한 경우,
            @Override
            public void onSuccess(UserProfile userProfile) {
                Log.e("SessionCallback :: ", "onSuccess");

                String nickname = userProfile.getNickname();
                String email = userProfile.getEmail();
                String profileImagePath = userProfile.getProfileImagePath();
                String thumnailPath = userProfile.getThumbnailImagePath();
                String UUID = userProfile.getUUID();
                long id = userProfile.getId();
                Log.e("Profile : ", nickname + "");
                Log.e("Profile : ", email + "");
                Log.e("Profile : ", profileImagePath + "");
                Log.e("Profile : ", thumnailPath + "");
                Log.e("Profile : ", UUID + "");
                Log.e("Profile : ", id + "");

            }

            // 사용자 정보 요청 실패
            @Override
            public void onFailure(ErrorResult errorResult) {
                Log.e("SessionCallback :: ", "onFailure : " + errorResult.getErrorMessage());
            }
        });
    }

    //for kakao logout //로그아웃 시 MainActivity로 돌아감. 즉 로그인 페이지로 돌아감
    private void onClickLogout() {
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
    }


    }







    //for get kakao user's token for auto login
    private void requestAccessTokenInfo() {
        AuthService.getInstance().requestAccessTokenInfo(new ApiResponseCallback<AccessTokenInfoResponse>() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);      //MainActivity로 돌아감.
            }

            @Override
            public void onNotSignedUp() {
                // not happened
            }

            @Override
            public void onFailure(ErrorResult errorResult) {
                Logger.e("failed to get access token info. msg=" + errorResult);
            }

            @Override
            public void onSuccess(AccessTokenInfoResponse accessTokenInfoResponse) {
                long userId = accessTokenInfoResponse.getUserId();
                Logger.d("this access token is for userId=" + userId);
                long expiresInMilis = accessTokenInfoResponse.getExpiresInMillis();
                Logger.d("this access token expires after " + expiresInMilis + " milliseconds.");
            }


        }
        public void makeNewGpsService(){
            if(gps==null){
                gps=new GPSTracker(this,myHandler);
            }else{
                gps.Update();
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