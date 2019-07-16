package com.example.curate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class QueueActivity extends AppCompatActivity {

	QueueFragment queueFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_queue);
		queueFragment = QueueFragment.newInstance();
		getSupportFragmentManager().beginTransaction().replace(R.id.flPlaceholder, queueFragment).commit();
	}
}
