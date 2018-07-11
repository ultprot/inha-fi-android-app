package com.example.exercise1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button  startBtn = (Button)findViewById(R.id.startButton);
        startBtn.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent(getApplicationContext(),Start.class);
                startIntent.putExtra("org.examples.SOMETHING","Successful");
                //"org.examples.SOMETHING":"Successful" will transmitted to Start.class
                //we can use it later, for transmit user's information like userid or caneid ... if we use login action
                startActivity(startIntent);
            }
        });

        Button enrollBtn = (Button)findViewById(R.id.Enroll);
        enrollBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent(getApplicationContext(),Eroll.class);
                startIntent.putExtra("org.examples.ENROLL","enrolling");
                //"org.examples.ENROLL":"enrolling" will transmitted to Eroll.class
                startActivity(startIntent);
            }
        });

    }
}
