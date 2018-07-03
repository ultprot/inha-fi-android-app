package com.example.exercise1;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Start extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        if (getIntent().hasExtra("org.examples.SOMETHING")){
            TextView resultStartText = (TextView) findViewById(R.id.resultStartText);
            String text  = getIntent().getExtras().getString("org.examples.SOMETHING");
            resultStartText.setText(text);
        }
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
}
