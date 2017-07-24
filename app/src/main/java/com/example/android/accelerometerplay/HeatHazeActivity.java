package com.example.android.accelerometerplay;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import ca.hss.heatmaplib.HeatMap;

public class HeatHazeActivity extends Activity {

	ArrayList<Point> values = new ArrayList<>();

	private Timer _timer = new Timer();

	private int numberOfItems = 20;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_heat_haze);

		final HeatMap mHeatMap = (HeatMap) findViewById(R.id.heatmap);

		mHeatMap.setMinimum(0);
		mHeatMap.setMaximum(200);
		mHeatMap.setRadius(1000);
		randomData(mHeatMap);


		//make the colour gradient from pink to yellow
		Map<Float, Integer> colorStops = new ArrayMap<>();
		colorStops.put(0.0f, 0xffee42f4);
		colorStops.put(1.0f, 0xffeef442);
		mHeatMap.setColorStops(colorStops);


		mHeatMap.forceRefresh();

		_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// use runOnUiThread(Runnable action)
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						randomData(mHeatMap);
						mHeatMap.forceRefresh();
					}
				});
			}

		}, 7000, 100);
	}


	private void randomData(HeatMap mHeatMap) {
		//add random data to the map
		Random rand = new Random();
		mHeatMap.clearData();
		for (int i = 0; i < numberOfItems; i++) {
			mHeatMap.addData(new HeatMap.DataPoint(rand.nextFloat(), rand.nextFloat(), rand.nextDouble() * 100.0));
		}
	}

	class Point {
		float x;
		float y;
		double value;

		public Point(float x, float y, double value) {
			this.x = x;
			this.y = y;
			this.value = value;
		}

	}
}
