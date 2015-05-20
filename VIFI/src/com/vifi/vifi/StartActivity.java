package com.vifi.vifi;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.Menu;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class StartActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.permitNetwork().build());
		
		Handler mHandler = new Handler();
		mHandler.postDelayed(new Runnable() {
			// Do Something
			public void run() {
				Intent myintent = new Intent(StartActivity.this,	MainActivity.class);
				startActivity(myintent);
				finish();
			}
		}, 2000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	public void destroy()
	{
        System.exit(0);
        
		super.onDestroy();

	}
}
