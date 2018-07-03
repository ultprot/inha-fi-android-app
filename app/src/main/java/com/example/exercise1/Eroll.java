package com.example.exercise1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class Eroll extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eroll);

        if (getIntent().hasExtra("org.examples.ENROLL")){
            TextView enrollText = (TextView) findViewById(R.id.enrollText);
            String text  = getIntent().getExtras().getString("org.examples.ENROLL");
            enrollText.setText(text);
        }
    }
}
