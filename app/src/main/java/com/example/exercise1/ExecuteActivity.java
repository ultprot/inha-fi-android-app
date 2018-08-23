package com.example.exercise1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

public class ExecuteActivity extends AppCompatActivity {

    Button button;
    Button logoutBtn;

    //백버튼 처리를 위한 변수
    long first_time;
    long second_time;

    private int areaCode;

    TextView Path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);
        startService(new Intent(ExecuteActivity.this,MainService.class)
                .putExtra("code",areaCode));
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String userId = pref.getString("userId","");
        String gard = pref.getString("gard","");

        Log.d("기록","이용자 아이디 : "+userId);
        Log.d("기록","이용자 보호자 연락처 : "+gard);

        Path=findViewById(R.id.textView);
        if(getIntent().hasExtra("code"))
        {
            areaCode=getIntent().getIntExtra("cityCode",-1);
        }

        Button logoutBtn = (Button) findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 서버에 유저정보 삭제 & 앱 file에 저장된 정보 삭제
                new SendPostDelete().execute("http://14.63.161.4:26533/deleteuser");

                //logout 수행
                onClickLogout();
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
    }

    //벡버튼 두번눌리면 종료
    @Override
    public void onBackPressed() {
        second_time = System.currentTimeMillis();
        Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        if (second_time - first_time < 2000) {
            super.onBackPressed();
            finishAffinity();
        }
        first_time = System.currentTimeMillis();
    }

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
}
