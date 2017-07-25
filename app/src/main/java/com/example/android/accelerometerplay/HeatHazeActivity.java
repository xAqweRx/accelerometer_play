package com.example.android.accelerometerplay;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
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

	volatile ArrayList<HeatMap.DataPoint> values;

	private Timer _timer = new Timer();

	private int numberOfItems = 20;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_heat_haze);

		values = new ArrayList<>();
		final HeatMap mHeatMap = (HeatMap) findViewById(R.id.heatmap);

		mHeatMap.setMinimum(0);
		mHeatMap.setMaximum(200);
		mHeatMap.setRadius(800);
		randomData(mHeatMap);


		//make the colour gradient from pink to yellow
		Map<Float, Integer> colorStops = new ArrayMap<>();
		colorStops.put(0.0f, getResources().getColor(R.color.red_dark));
		colorStops.put(1.0f, getResources().getColor(R.color.yellow_light));
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

		}, 1000, 50);
	}


	private void randomData(HeatMap mHeatMap) {
		//add random data to the map
		Random rand = new Random();
		mHeatMap.clearData();
		if (values.size() > 0) {
			for (int i = 0; i < numberOfItems; i++) {
				HeatMap.DataPoint tempPoint = values.get(i);
				tempPoint.setValue(tempPoint.getValue() + rand.nextDouble() / 100 * (rand.nextDouble() > 0.5 ? 1 : -1));


				tempPoint.setX( tempPoint.getX() + rand.nextFloat() / 100 * (rand.nextDouble() > 0.5 ? 1 : -1));
				if (tempPoint.getX() < 0)
					tempPoint.setX(-1 * tempPoint.getX());
				else if (tempPoint.getX() > 1)
					tempPoint.setX(1 - (tempPoint.getX() - 1));


				tempPoint.setY(tempPoint.getY() + rand.nextFloat() / 100 * (rand.nextDouble() > 0.5 ? 1 : -1));
				if (tempPoint.getY() < 0)
					tempPoint.setY(-1 * tempPoint.getY());
				else if (tempPoint.getY() > 1)
					tempPoint.setY(1 - (tempPoint.getY() - 1));

				values.set(i, tempPoint);
			}
		}
		else {
			for (int i = 0; i < numberOfItems; i++) {
				values.add(new HeatMap.DataPoint(rand.nextFloat(), rand.nextFloat(), rand.nextDouble() * 1000.0));
			}
		}

		mHeatMap.addDataSet(new ArrayList<>(values));
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
