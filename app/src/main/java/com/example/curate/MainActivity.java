package com.example.curate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    QueueFragment queueFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queueFragment = QueueFragment.newInstance();
        queueFragment.setRetainInstance(true);
        getSupportFragmentManager().beginTransaction().replace(R.id.flPlaceholder, queueFragment).commit();
    }
}
