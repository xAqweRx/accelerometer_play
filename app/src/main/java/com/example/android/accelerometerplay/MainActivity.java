package com.example.android.accelerometerplay;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.accelerometer).setOnClickListener(this);
		findViewById(R.id.waves).setOnClickListener(this);
		findViewById(R.id.heat).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
			case R.id.accelerometer:
				intent = new Intent(this, AccelerometerPlayActivity.class);
				break;
			case R.id.waves:
				intent = new Intent(this, AccelerometerPlayActivity.class);
		}

		if (intent != null)
			startActivity(intent);
	}
}
