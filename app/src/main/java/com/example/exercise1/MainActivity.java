package com.example.exercise1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView executeBtn = findViewById(R.id.imageView4);
        executeBtn.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent executeIntent = new Intent(getApplicationContext(),ExecuteActivity.class);
                startActivity(executeIntent);
            }
        });

    }
}
