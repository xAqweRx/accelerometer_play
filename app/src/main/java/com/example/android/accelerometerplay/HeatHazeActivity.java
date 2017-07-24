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

	private int numberOfItems = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_heat_haze);

		final HeatMap mHeatMap = (HeatMap) findViewById(R.id.heatmap);

		mHeatMap.setMinimum(0);
		mHeatMap.setMaximum(10000F);
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
					}
				});
			}

		}, 2000, 50);
	}


	private void randomData(HeatMap mHeatMap) {
		//add random data to the map
		Random rand = new Random();
		boolean forceRefreh = true;
		if (values.isEmpty()) {
			forceRefreh = false;
			for (int i = 0; i < numberOfItems; i++) {
				Point point = new Point(rand.nextFloat(), rand.nextFloat(), ( rand.nextDouble() * 100.0 ) );
				values.add(point);
			}
		}
		else {
			ArrayList<Point> temp = new ArrayList<>();

			for (Point point : values) {
				Point newPoint = new Point((point.x + rand.nextFloat() / 100 * (rand.nextFloat() > 0.5 ? -1 : 1)) % 1, (point.y + rand.nextFloat() / 100 * (rand.nextFloat() > 0.5 ? -1 : 1)) % 1, point.value + rand.nextDouble() * 100.0);
				temp.add(newPoint);
			}

			values = temp;
			mHeatMap.clearData();
		}


		for (Point point : values) {
			mHeatMap.addData(new HeatMap.DataPoint(point.x, point.y, point.value));
		}

		if (forceRefreh)
			mHeatMap.forceRefresh();
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
