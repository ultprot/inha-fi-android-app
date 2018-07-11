package com.example.exercise1;

import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class Start extends AppCompatActivity {

    Button button;
    TextView result;
    TextView doing;
    Intent i;

    private TextToSpeech myTTS;
    private SpeechRecognizer mySpeechRecognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        button = (Button)findViewById(R.id.getVoiceBtn);
        result = (TextView)findViewById((R.id.result));
        doing = (TextView)findViewById(R.id.voiceText);

        //get intent form main page and print
        if (getIntent().hasExtra("org.examples.SOMETHING")){
            String text  = getIntent().getExtras().getString("org.examples.SOMETHING");
            doing.setText("page moving "+text);
        }//end

        //start recognize speech
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);            //intent 생성
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
                mySpeechRecognizer.startListening(i);
            }
        });
        initializeTextToSpeech();
        initializeSpeechRecognizer();
        //end recognize speech


        Button SendPostBtn = (Button)findViewById(R.id.SendPost);
        SendPostBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent ProcessIntent = new Intent(getApplicationContext(),InProcess.class);
                ProcessIntent.putExtra("","enrolling");
                startActivity(ProcessIntent);
            }
        });
    }

    //initialize & speek "I'm ready"
    private void initializeTextToSpeech(){
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(myTTS.getEngines().size()==0){
                    Toast.makeText(Start.this,"There is no TTs engine",Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    myTTS.setLanguage(Locale.KOREA);
                    speak("Hello! I am ready.");
                }
            }
        });
    }//!initializeTextToSpeech

    //speech recognizer
    private void initializeSpeechRecognizer(){
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            mySpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mySpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    doing.setText("onReadyForSpeech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    doing.setText("onBeginningOfSpeech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    doing.setText("onRmsChanged"+(int)rmsdB);
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    doing.setText("onBufferReceived");
                }

                @Override
                public void onEndOfSpeech() {
                    doing.setText("onEndOfSpeech");
                }

                @Override
                public void onError(int error) {
                    doing.setText("onError"+"error");
                }

                @Override
                public void onResults(Bundle results) {
                    doing.setText("onResults");
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
        doing.setText("in ProcessResult");
        result.setText(s);
    }//!processResult

    //speak string
    private void speak(String message){
        if(Build.VERSION.SDK_INT>=21){
            myTTS.speak(message,TextToSpeech.QUEUE_FLUSH,null,null);
        }else{
            myTTS.speak(message,TextToSpeech.QUEUE_FLUSH,null);
        }
    }//!speak

    @Override
    protected void onPause() {
        super.onPause();
        myTTS.shutdown();
    }//!onPause

}