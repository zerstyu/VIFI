package com.vifi.vifi;

import java.util.Timer;
import java.util.TimerTask;

import ac.uol.aig.fftpack.RealDoubleFFT;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnCompletionListener {

	static public char temp_Y;
	static char[] buff = new char[3];

	static public TcpIpMultichatClient TcpIpMultichatClient1;

	private int nFlag = 0;

	private static final int MSG_AFTER_CONNECT = 1;
	private static final int MSG_BEFORE_CONNECT = 2;
	private static final int CONNECT_ERROR = 3;

	private static final int CLICK_WIFI_ON = 1;
	private static final int CLICK_WIFI_OFF = 2;
	private static final int CLICK_CONNECT = 3;
	private static final int CLICK_PLAY = 4;
	private static final int CLICK_STOP = 5;
	private static final int CLICK_VOLUME = 6;

	private ProgressDialog waitDialog;
	private ProgressThread progressThread;
	private MyHandler myHandler = new MyHandler();

	private boolean play = false; // 소리 나오고 안나오고
	private boolean CallList = false; // vifi리스트 호출하고 안하고
	private boolean isConnect = false; // 서버와 연결이 되어있는지 안되어잇는지

	// Handler mHandler = new Handler();
	Timer timer;
	private TimerTask myTask;

	WifiConfiguration wfc = new WifiConfiguration();

	Button button_play, button_connect, button_stop;
	TextView wifiview;

	Switch toggle;
	SeekBar seekVolume;

	AudioManager audioManager;
	WifiManager wifiManager;

	String temp; // vifiList에서 받아오는 변수
	AlertDialog.Builder alertDlg;

	// AudioRecord 객체에서 주파수는 8kHz, 오디오 채널은 하나, 샘플은 16비트를 사용
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	// FFT 객체를 통해 AudioRecord 객체에서 한 번에 256가지 샘플을 다룬다
	private RealDoubleFFT transformer;
	int blockSize = 256;
	Button startStopButton;
	boolean started = true;
	/*
	 * // RecordAudio는 여기에서 정의되는 내부 클래스로서 AsyncTask를 확장한다. RecordAudio
	 * recordTask;
	 * 
	 * // Bitmap 이미지를 표시하기 위해 ImageView를 사용한다. 이 이미지는 현재 오디오 스트림에서 주파수들의 레벨을
	 * 나타낸다. ImageView imageView; Bitmap bitmap; Canvas canvas; Paint paint;
	 */

	private BackPressCloseHandler backPressCloseHandler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		alertDlg = new AlertDialog.Builder(MainActivity.this);
		alertDlg.setTitle("알림");
		alertDlg.setMessage("TV신호가 약해 연결되지 못했습니다.");
		alertDlg.setCancelable(true); // 취소버튼 클릭시 취소

		alertDlg.setNegativeButton("확인", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}

		});

		seekVolume = (SeekBar) findViewById(R.id.seekBar1); // 볼륨 설정
		toggle = (Switch) findViewById(R.id.switch1); // 스위치 설정
		button_play = (Button) findViewById(R.id.button_play); // 플레이 버튼
		button_stop = (Button) findViewById(R.id.button_stop); // 플레이 버튼
		button_connect = (Button) findViewById(R.id.button_connect); // 연결버튼

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int nMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int nCurrentVolumn = audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);

		seekVolume.setMax(nMax);
		seekVolume.setProgress(nCurrentVolumn);

		wifiview = (TextView) findViewById(R.id.textView2);

		// 현재 와이파이 상태 받아오기
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

		// 스위치 설정
		if (wifiManager.isWifiEnabled()) // 와이파이가 연결되어 있으면
			toggle.setChecked(true);
		else
			toggle.setChecked(false);

		// RealDoubleFFT 클래스 컨스트럭터는 한번에 처리할 샘플들의 수를 받는다. 그리고 출력될 주파수 범위들의 수를
		// 나타낸다.
		transformer = new RealDoubleFFT(blockSize);
		/*
		 * // ImageView 및 관련 객체 설정 부분 imageView = (ImageView)
		 * findViewById(R.id.imageView01); // bitmap = Bitmap.createBitmap((int)
		 * 256, (int) 100, // Bitmap.Config.ARGB_8888); //300 200 bitmap =
		 * Bitmap.createBitmap((int) 256, (int) 200, Bitmap.Config.ARGB_8888);
		 * canvas = new Canvas(bitmap);
		 * 
		 * paint = new Paint(); paint.setColor(Color.argb(255, 247, 150, 70));
		 * paint.setStrokeWidth(2); // 선 굻기
		 * 
		 * imageView.setImageBitmap(bitmap);
		 * 
		 * recordTask = new RecordAudio(); recordTask.execute();// 스펙트럼 활성화
		 */
		// 프로그래스바 설정
		waitDialog = new ProgressDialog(MainActivity.this);
		waitDialog.setMessage("잠시 기다리세요...");
		waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		waitDialog.setCancelable(false);

		progressThread = new ProgressThread();

		button_stop.setEnabled(false);
		button_play.setEnabled(true);

		backPressCloseHandler = new BackPressCloseHandler();

		// 액티비티 전환
		if (wifiManager.isWifiEnabled()) // wifi사용중이면
		{
			IntentVifiList();
		} else {
			alertDlg.setMessage("Wifi가 연결되어있지 않습니다.");
			alertDlg.show();
		}

		super.onResume(); // 재시작

		// TODO Auto-generated method stub
	}

	@Override
	protected void onResume() {
		/*
		 * if (recordTask.isCancelled()) { recordTask = new RecordAudio();
		 * recordTask.execute();// 스펙트럼 활성화 }
		 */

		toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) { // 와이파이가 켜짐.
					nFlag = CLICK_WIFI_ON;
				} else {
					nFlag = CLICK_WIFI_OFF;
				}
			}
		});

		button_connect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				nFlag = CLICK_CONNECT;
			}
		});

		button_play.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				nFlag = CLICK_PLAY;
			}
		});

		button_stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				nFlag = CLICK_STOP;
			}
		});

		seekVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
						progress, 0);
			}
		});

		while (true) {
			// 스위치 설정
			if (wifiManager.isWifiEnabled()) // 와이파이가 연결되어 있으면
				toggle.setChecked(true);
			else
				toggle.setChecked(false);
			
			switch (nFlag) {
			case CLICK_WIFI_ON:
				toggleWiFi(true);
				Toast.makeText(getApplicationContext(), "와이파이 켜짐",
						Toast.LENGTH_SHORT).show();

				wifiview.setText("없음");

				waitDialog.show(); // 프로그래스바 보임

				CallList = true;

				if (progressThread.isAlive() == false)
					progressThread = new ProgressThread();

				progressThread.start();

				SetTimer();
				nFlag = 0;
				break;
			case CLICK_WIFI_OFF:
				toggleWiFi(false);
				Toast.makeText(getApplicationContext(), "와이파이 꺼짐",
						Toast.LENGTH_SHORT).show();

				wifiview.setText("없음");
				nFlag = 0;
				break;
			case CLICK_CONNECT:

				if (wifiManager.isWifiEnabled()) // wifi사용중이면
				{
					IntentVifiList();// Sub_Activity 호출

				} else {
					alertDlg.setMessage("Wifi가 연결되어있지 않습니다.");
					alertDlg.show();
				}
				nFlag = 0;
				break;
			case CLICK_PLAY:
				play = true;
				Log.e("data", "play==>ture");
				button_stop.setEnabled(true);
				button_play.setEnabled(false);
				nFlag = 0;
				break;
			case CLICK_STOP:
				Log.e("data", "play==>false");
				play = false; // 재생을 중지함
				button_stop.setEnabled(false);
				button_play.setEnabled(true);
				nFlag = 0;
				break;
			case CLICK_VOLUME:
				break;
			}
			if(nFlag == 0)
				break;
		}

		super.onResume(); // 재시작
	}// ///////////////////////////////////////onResume()

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		started = false;

		// while (recordTask.isCancelled() == false)
		// recordTask.cancel(true);

		/*
		 * // 가상 서버 접속 종료 if(isConnect == true) { buff[0] = 'O'; buff[1] =
		 * temp_Y; TcpIpMultichatClient1 = new TcpIpMultichatClient(buff);
		 * TcpIpMultichatClient1.start(); }
		 */
	}// ///////////////////////////////////////기본 구현

	private void SetTimer() {
		if (timer == null) {
			myTask = new TimerTask() {
				public void run() {
					Log.e("data", "myTask");

					waitDialog.dismiss();
					while (progressThread != null && progressThread.isAlive()) {
						progressThread.interrupt(); // 스레드 종료

						Log.e("data", "progressThread.isAlive()==>"
								+ progressThread.isAlive());
						Log.e("data", "progressThread.isInterrupted()==>"
								+ progressThread.isInterrupted());

					}
				}
			};
			timer = new Timer();
			timer.schedule(myTask, 20000);
		}
	}

	private void IntentVifiList() {

		Intent intent = new Intent(MainActivity.this, VifiList.class);

		startActivityForResult(intent, 1); // Sub_Activity 호출
	}

	// 액티비티 정보 받아오기
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.e("data", "4");

		if (resultCode == RESULT_OK) { // 액티비티가 정상적으로 종료되었을 경우
			if (requestCode == 1) // InformationInput에서 호출한 경우에만 처리합니다.
			{
				temp = data.getStringExtra("data_name");
				Log.e("data", "temp=======>" + temp);
				/*
				 * // 와이파이 자동 연결 wfc.allowedAuthAlgorithms.clear();
				 * wfc.allowedGroupCiphers.clear();
				 * wfc.allowedPairwiseCiphers.clear();
				 * wfc.allowedProtocols.clear();
				 * wfc.allowedKeyManagement.clear(); wfc.SSID =
				 * "\"".concat(temp).concat("\""); wfc.status =
				 * WifiConfiguration.Status.DISABLED; wfc.priority = 40;
				 * wfc.allowedAuthAlgorithms
				 * .set(WifiConfiguration.AuthAlgorithm.OPEN);
				 * wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA); //
				 * For WPA
				 * wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN); //
				 * For WPA2
				 * wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt
				 * .WPA_PSK);
				 * wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt
				 * .WPA_EAP);
				 * wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher
				 * .CCMP);
				 * wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher
				 * .TKIP);
				 * wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher
				 * .CCMP);
				 * wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher
				 * .TKIP);
				 * 
				 * wfc.preSharedKey = "\"".concat("25212521").concat("\"");
				 * 
				 * // 현재 와이파이 상태 받아오기 wifiManager = (WifiManager)
				 * this.getSystemService(Context.WIFI_SERVICE);
				 * 
				 * int networkId = wifiManager.addNetwork(wfc); if (networkId !=
				 * -1) { Log.e("zzzz", "zzzzzzzz"); }
				 * 
				 * wifiManager.saveConfiguration();
				 * wifiManager.setWifiEnabled(true);
				 * wifiManager.enableNetwork(networkId, true); Log.e("opener",
				 * "addnetwork returned" + networkId );
				 */
				waitDialog.show(); // 프로그래스바 보임

				if (progressThread.isAlive() == false)
					progressThread = new ProgressThread();

				progressThread.start();

				SetTimer();

				/*
				 * // 가상 서버 접속 if(isConnect == false) { buff[0] = 'I';
				 * 
				 * TcpIpMultichatClient1 = new TcpIpMultichatClient(buff);
				 * TcpIpMultichatClient1.start(); Log.e("data", "iiiii");
				 * 
				 * new Thread(new Client()).start(); } else { //연결 끊었다가 다시 접속 }
				 * Log.e("data", "kkkk"); }
				 */
			} else {
				Log.e("Error", "else");
			}

			super.onStart(); // 화면 재 출력
		}
	}

	// wifi가 켜진상태이면 끄고, 꺼진 상태이면 킨다.
	public void toggleWiFi(boolean status) {
		if (status == true && !wifiManager.isWifiEnabled()) {
			// 현재 on상태 wifi연결이 안되있음
			wifiManager.setWifiEnabled(true); // wifi연결
		} else if (status == false && wifiManager.isWifiEnabled()) {
			// 현재 off상태, wifi연결이 되어있음
			wifiManager.setWifiEnabled(false); // wifi해제
		}
	}

	// //////////////////////////////////////////////////////
	class ProgressThread extends Thread {
		public void run() {
			try {
				Thread.sleep(1000); // 일단 한번 쉬고
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			WifiInfo info = wifiManager.getConnectionInfo();
			ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			while (this.isInterrupted() == false
					&& (wifiManager.isWifiEnabled() == true
							|| manager.getNetworkInfo(
									ConnectivityManager.TYPE_WIFI)
									.isConnectedOrConnecting() == false || info
							.getSSID() == null)) {
				try {

					info = wifiManager.getConnectionInfo();
					Thread.sleep(500);
					Log.e("data", "info.getSSID=======>" + info.getSSID());

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e("data", "Thread dead!!");
				}
			}

			// manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()
			Log.e("data", "!!!info.isConnected=======>"
					+ manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.isConnected());
			Log.e("data", "!!!info.isConnectedOrConnecting=======>"
					+ manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.isConnectedOrConnecting());
			Log.e("data", "!!!info.getSSID=======>" + info.getSSID());
			Log.e("data",
					"!!!info.isWifiEnabled=======>"
							+ wifiManager.isWifiEnabled());

			if (CallList == true) {
				myHandler.sendEmptyMessage(MSG_BEFORE_CONNECT);
				CallList = false;
			} else if (Thread.currentThread().isInterrupted() == true) {
				myHandler.sendEmptyMessage(CONNECT_ERROR);
			} else
				myHandler.sendEmptyMessage(MSG_AFTER_CONNECT);
		}
	}

	class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			WifiInfo info = wifiManager.getConnectionInfo();

			waitDialog.dismiss();

			if (timer != null) {
				timer.cancel();
				timer = null;
			}
			
			Log.e("data", "핸들러 들어옴 " + info.getSSID());

			switch (msg.what) {
			case MSG_BEFORE_CONNECT:

				IntentVifiList();// Sub_Activity 호출
				break;

			case CONNECT_ERROR:
				alertDlg.setMessage("주변에 vifi신호가 약합니다.");
				alertDlg.show();

				wifiview.setText("없음");

				break;
			case MSG_AFTER_CONNECT:

				Log.e("data", "temp=======>" + temp);
				Log.e("data", "info.getSSID()=======>" + info.getSSID());

				if (info.getSSID().contains(temp)) // 선택한 wifi와 현재 wifi가 같으면
					wifiview.setText(info.getSSID());
				else {
					alertDlg.setMessage("TV신호가 약해 연결되지 못했습니다.");
					alertDlg.show();

					wifiview.setText("없음");
				}
				break;
			default:
				wifiview.setText(info.getSSID());
				break;
			}
			
			super.handleMessage(msg);
		}
	}

	// /////////////////////////////////////////////////////////////////////
	/*
	 * // AsyncTask : 사용자 인터페이스를 멍하니 있게 하는 메소드들을 별도의 스레드로 실행한다. private class
	 * RecordAudio extends AsyncTask<Void, double[], Void> {
	 * 
	 * // doInBackground 메소드에 둘 수 있는 것이면 뭐든지 이런 식으로 실행할 수 있다.
	 * 
	 * @Override protected Void doInBackground(Void... params) { try { //
	 * AudioRecord를 설정하고 사용 int bufferSize =
	 * AudioRecord.getMinBufferSize(frequency, channelConfiguration,
	 * audioEncoding);
	 * 
	 * AudioRecord audioRecord = new AudioRecord(
	 * MediaRecorder.AudioSource.CAMCORDER, frequency, channelConfiguration,
	 * audioEncoding, bufferSize);
	 * 
	 * short[] buffer = new short[blockSize]; // 원시 PCM 샘플을 AudioRecord 객체에서 받음.
	 * fft에서 double타입 필요 double[] toTransform = new double[blockSize];
	 * 
	 * audioRecord.startRecording(); // 녹음 시작
	 * 
	 * while (started) { // 데이터 읽음 int bufferReadResult =
	 * audioRecord.read(buffer, 0, blockSize);
	 * 
	 * for (int i = 0; i < blockSize && i < bufferReadResult; i++) { // short를
	 * double로 바꿈 toTransform[i] = (double) buffer[i] / Short.MAX_VALUE; // 부호
	 * // 있는 // 16비트 }
	 * 
	 * // 배열을 FFT 객체로 넘겨준다 transformer.ft(toTransform); // publishProgress를 호출하면
	 * onProgressUpdate가 호출된다. publishProgress(toTransform);
	 * 
	 * }
	 * 
	 * audioRecord.stop(); } catch (Throwable t) { Log.e("AudioRecord",
	 * "Recording Failed"); }
	 * 
	 * return null; }
	 * 
	 * // 메인 스레드로 실행되서 사용자 인터페이스와 상호작용 가능
	 * 
	 * @Override protected void onProgressUpdate(double[]... toTransform) {
	 * canvas.drawColor(Color.argb(255, 01, 01, 01));
	 * 
	 * for (int i = 0; i < toTransform[0].length; i++) { int x = i; // int downy
	 * = (int) (100 - (toTransform[0][i] * 10)); int downy = (int) (100 -
	 * (toTransform[0][i] * 200)); int upy = 100;
	 * 
	 * canvas.drawLine(x, downy, x, upy, paint); } imageView.invalidate(); } }
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		AudioManager audio = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		int nCurrentVolumn;

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);

			nCurrentVolumn = audioManager
					.getStreamVolume(AudioManager.STREAM_MUSIC);
			seekVolume.setProgress(nCurrentVolumn);

			onResume(); // 화면 재출력
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);

			nCurrentVolumn = audioManager
					.getStreamVolume(AudioManager.STREAM_MUSIC);
			seekVolume.setProgress(nCurrentVolumn);

			onResume(); // 화면 재출력
			return true;
		case KeyEvent.KEYCODE_BACK:

			Log.e("data", "KEYCODE_BACK!!");
			backPressCloseHandler.onBackPressed();
			if (backPressCloseHandler.getFlag() == true) {
				Toast.makeText(this, "취소키를 한번 더 누르시면 종료됩니다.", Toast.LENGTH_LONG)
						.show();
			} else {

				wfc.preSharedKey = "\"".concat("25212111").concat("\"");

				int networkId = wifiManager.addNetwork(wfc);
				Log.e("check", "check");
				if (networkId != -1) {
					Log.e("zzzz", "zzzzzzzz");
				}
				wifiManager.saveConfiguration();
				wifiManager.setWifiEnabled(false);
				// wifiManager.enableNetwork(networkId, false);
				wfc.allowedAuthAlgorithms.clear();
				wfc.allowedGroupCiphers.clear();
				wfc.allowedPairwiseCiphers.clear();
				wfc.allowedProtocols.clear();
				wfc.allowedKeyManagement.clear();
				wifiManager.setWifiEnabled(true);
				System.exit(0);
			}

			Log.e("data", "KEYCODE_BACK!! ==END==");
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
	}

}
