package com.example.android.accelerometerplay;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.gelitenight.waveview.library.WaveView;

import java.util.ArrayList;
import java.util.List;

public class WavesActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_waves);

		WaveView mWaveView = (WaveView) findViewById(R.id.waves);


		mWaveView.setShapeType(WaveView.ShapeType.SQUARE);
		mWaveView.setWaveShiftRatio(1f );
		mWaveView.setWaveColor(Color.parseColor("#28b8f1ed"), Color.parseColor("#58b8f1ed"));


		List<Animator> animators = new ArrayList<>();
		// horizontal animation.
// wave waves infinitely.
		ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(
				mWaveView, "waveShiftRatio", 0f, 1f);
		waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
		waveShiftAnim.setDuration(1000);
		waveShiftAnim.setInterpolator(new LinearInterpolator());

		// vertical animation.
		// water level increases from 0 to center of WaveView
		ObjectAnimator waterLevelAnim = ObjectAnimator.ofFloat(mWaveView, "waterLevelRatio", 0f, 0.5f);
		waterLevelAnim.setDuration(10000);
		waterLevelAnim.setInterpolator(new DecelerateInterpolator());
		animators.add(waterLevelAnim);

		// amplitude animation.
		// wave grows big then grows small, repeatedly
		ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat( mWaveView, "amplitudeRatio", 0f, 0.05f);
		amplitudeAnim.setRepeatCount(ValueAnimator.INFINITE);
		amplitudeAnim.setRepeatMode(ValueAnimator.REVERSE);
		amplitudeAnim.setDuration(5000);
		amplitudeAnim.setInterpolator(new LinearInterpolator());
		animators.add(amplitudeAnim);

		AnimatorSet mAnimatorSet = new AnimatorSet();
		mAnimatorSet.playTogether(animators);

		mWaveView.setShowWave(true);
		mAnimatorSet.start();
	}
}
