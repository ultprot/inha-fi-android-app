package com.example.exercise1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.net.HttpURLConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

public class EnrollActivity extends AppCompatActivity {

    TextView gardText;
    Button gardBtn;
    long first_time;
    long second_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);

        SharedPreferences pref = getSharedPreferences("pref",MODE_PRIVATE);
        String user = pref.getString("userId", "X");
        if(user=="X"){
            //서버에 user id 요청 및 등록
            new SendPostEnroll().execute("http://14.63.161.4:26533/newuser");
        }
        gardText = (TextView) findViewById(R.id.gardText);
        gardBtn = (Button) findViewById(R.id.gardBtn);

        gardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SharedPreferences pref = getSharedPreferences("pref",MODE_PRIVATE);
                    SharedPreferences.Editor ed = pref.edit();
                    String gard = gardText.getText().toString();
                    Log.d("저장","받은 보호자 전화번호 : "+gard);
                    ed.putString( "gard" , gard);
                    ed.commit();

                    String getGard = pref.getString("gard", "없음");
                    Log.d("저장테스트","보호자 번호 : "+getGard);

                } catch (Exception err){
                    Log.d("저장","보호자 안됨, "+err);
                }

                Intent executeIntent = new Intent(getApplicationContext(),ExecuteActivity.class);
                startActivity(executeIntent);
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


    //for 서버로 post전송
    public class SendPostEnroll extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String[] urls) {
            try {
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("user_id", "androidTest");
                HttpURLConnection con = null;
                BufferedReader reader = null;
                try{
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
            Log.d("저장","POST변수 넘어옴 : "+result);

            try {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor ed = pref.edit();

                if(result== "fail"){
                    Log.e("저장","서버오류");
                    Toast.makeText(EnrollActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                }else {
                    ed.putString("userId", result); // value : 저장될 값,
                    ed.commit();
                }

                String getUser = pref.getString("userId", "없음");
                Log.d("저장테스트","아이디 : "+getUser);

            }catch (Exception err){
                Log.d("저장","아이디 안됨, "+err);
            }
        }
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
                Log.e("저장","기존아이디존재"+userId);

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

            SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
            SharedPreferences.Editor ed = pref.edit();
            ed.clear();
            ed.commit();

            Log.d("저장","삭제 : "+result);
        }
    }


}
