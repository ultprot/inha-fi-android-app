package com.example.exercise1;

import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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

import static android.content.ContentValues.TAG;

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

    //---사용자 도시 파악 위한 변수-----------------------
    private double initialLatitude;   //시작 시의 위도
    private double initialLongitude;  //시작 시의 경도
    private int areaCode;             //사용자의 도시 코드
    //----------------------------------------------------
    GPSTracker gps=null;    //gps 사용을 위한 객체 선언

    private SessionCallback callback;

    final int RC_SIGN_IN = 9001; // 로그인 확인여부 코드
    private FirebaseAuth mAuth;
    private SignInButton signInButton; //구글 로그인 버튼
    //private GoogleApiClient mGoogleApiClient; //API 클라이언트
    private GoogleSignInClient googleSignInClient;

    //백버튼 처리를 위한 변수
    long first_time;    //
    long second_time;   //
    //------------------//

    public MyHandler myHandler=new MyHandler(); //gps 처리를 위한 핸들러 객체

    class MyHandler extends Handler{    //gps 사용을 위한 핸들러 클래스
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

        //-------------------gps 초기화-------------------
        if(gps==null){                                  //
            gps=new GPSTracker(this,myHandler); //
        }else{                                          //
            gps.Update();                               //
        }                                               //
        if(gps.canGetLocation()){                       //
            initialLatitude=gps.getLatitude();          //위치 확인 가능하면 위도 저장
            initialLongitude=gps.getLongitude();        //위치 확인 가능하면 경도 저장
        }else{                                          //
            Log.d("내 로그","좌표 불가");     //
        }                                               //
        //------------------------------------------------

        makePostData(Double.toString(initialLatitude)
                ,Double.toString(initialLongitude));   //도시 코드 조회

        if (Session.getCurrentSession().checkAndImplicitOpen()) {
            // 액세스토큰 유효하거나 리프레시 토큰으로 액세스 토큰 갱신을 시도할 수 있는 경우
            Log.e("login","login remained");
            try {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                String getUserID = pref.getString("userId", "X");
                String getGard = pref.getString("gard","X");

                if (getUserID.equals("X") || getGard.equals("X")) {
                    if(getUserID.equals("X"))
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

        //firebase 인증 객체 선언
        mAuth = FirebaseAuth.getInstance(); // 인스턴스 생성

        //Google 로그인을 앱에 통합
        //GoogleSignInOptions 개체를 구성할 때 requestIdToken을 호출
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton = (SignInButton)findViewById(R.id.btn_login_google);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

    }

    public void onStart() { // 사용자가 현재 로그인되어 있는지 확인
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser!=null){ // 만약 로그인이 되어있으면 다음 액티비티 실행

            Log.e("login","login remained 구글");
            try {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                String getUserID = pref.getString("userId", "X");
                String getGard = pref.getString("gard","X");

                if (getUserID.equals("X") || getGard.equals("X")) {
                    if(getUserID.equals("X"))
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
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("login", "google");
                            editor.commit();

                            redirectEnrollActivity();

                            Log.d(TAG, "signInWithCredential:success");
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
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
        else if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // 구글 로그인 성공
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {

            }
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
        executeIntent.putExtra("code",areaCode);    //이후 실행될 서비스에 도시 코드 보냄
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

            SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("login", "kakao");
            editor.commit();

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
        });
    }

    //-----------------http통신을 위한 함수---------------------------------
    public void makePostData (String lat, String lon){                  //
        JSONObject currentLocation = new JSONObject();                  //현재 위치에 관한
        try {                                                           //json 데이터 생성
            currentLocation.accumulate("lat", initialLatitude);  //
            currentLocation.accumulate("lon", initialLongitude); //
        } catch (Exception e) {                                         //
                                                                        //
        }                                                               //
                                                                        //
                                                                        //
        OkHttpClient client = new OkHttpClient();                       //okhttp사용을 위한
        RequestBody jsonBody = RequestBody                              //객체 생성
                .create(MediaType.parse("application/json")             //
                , currentLocation.toString());                          //
                                                                        //
        Request request = new Request.Builder()                         //post요청을 위한
                .url("http://14.63.161.4:26531/areaclass")              //객체 생성. 도시 코드
                .post(jsonBody)                                         //조회
                .build();                                               //
        client.newCall(request).enqueue(makePostDataCallBack);          //응답 받을 콜백 함수
    }                                                                   //설정
    //----------------------------------------------------------------------

    //--------------------okhttp 콜백 함수---------------------------------------------------------
    private Callback makePostDataCallBack = new Callback() {                        //
        @Override                                                                   //
        public void onFailure(Call call, IOException e) {                           //http 요청
            Log.d("내 로그", "makePostData 응답 오류 발생");              //실패시 실행
        }                                                                           //
                                                                                    //
        @Override                                                                   //
        public void onResponse(Call call, Response response) throws IOException {   //http요청
            final String responseData = response.body().string();                   //성공 시 실행
            try {                                                                   //응답받은
                JSONObject result = new JSONObject(responseData);                   //json에서
                areaCode = result.optInt("largeCd");                         //도시코드 파싱
            } catch (Exception e) {                                                 //
                                                                                    //
            }                                                                       //
        }                                                                           //
    };                                                                              //
    //---------------------------------------------------------------------------------------------
    public void makeNewGpsService() {
        if (gps == null) {
            gps = new GPSTracker(this, myHandler);
        } else {
            gps.Update();
        }
    }
}
