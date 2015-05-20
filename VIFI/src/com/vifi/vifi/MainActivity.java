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

	private boolean play = false; // �Ҹ� ������ �ȳ�����
	private boolean CallList = false; // vifi����Ʈ ȣ���ϰ� ���ϰ�
	private boolean isConnect = false; // ������ ������ �Ǿ��ִ��� �ȵǾ��մ���

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

	String temp; // vifiList���� �޾ƿ��� ����
	AlertDialog.Builder alertDlg;

	// AudioRecord ��ü���� ���ļ��� 8kHz, ����� ä���� �ϳ�, ������ 16��Ʈ�� ���
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	// FFT ��ü�� ���� AudioRecord ��ü���� �� ���� 256���� ������ �ٷ��
	private RealDoubleFFT transformer;
	int blockSize = 256;
	Button startStopButton;
	boolean started = true;
	/*
	 * // RecordAudio�� ���⿡�� ���ǵǴ� ���� Ŭ�����μ� AsyncTask�� Ȯ���Ѵ�. RecordAudio
	 * recordTask;
	 * 
	 * // Bitmap �̹����� ǥ���ϱ� ���� ImageView�� ����Ѵ�. �� �̹����� ���� ����� ��Ʈ������ ���ļ����� ������
	 * ��Ÿ����. ImageView imageView; Bitmap bitmap; Canvas canvas; Paint paint;
	 */

	private BackPressCloseHandler backPressCloseHandler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		alertDlg = new AlertDialog.Builder(MainActivity.this);
		alertDlg.setTitle("�˸�");
		alertDlg.setMessage("TV��ȣ�� ���� ������� ���߽��ϴ�.");
		alertDlg.setCancelable(true); // ��ҹ�ư Ŭ���� ���

		alertDlg.setNegativeButton("Ȯ��", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}

		});

		seekVolume = (SeekBar) findViewById(R.id.seekBar1); // ���� ����
		toggle = (Switch) findViewById(R.id.switch1); // ����ġ ����
		button_play = (Button) findViewById(R.id.button_play); // �÷��� ��ư
		button_stop = (Button) findViewById(R.id.button_stop); // �÷��� ��ư
		button_connect = (Button) findViewById(R.id.button_connect); // �����ư

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int nMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int nCurrentVolumn = audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);

		seekVolume.setMax(nMax);
		seekVolume.setProgress(nCurrentVolumn);

		wifiview = (TextView) findViewById(R.id.textView2);

		// ���� �������� ���� �޾ƿ���
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

		// ����ġ ����
		if (wifiManager.isWifiEnabled()) // �������̰� ����Ǿ� ������
			toggle.setChecked(true);
		else
			toggle.setChecked(false);

		// RealDoubleFFT Ŭ���� ����Ʈ���ʹ� �ѹ��� ó���� ���õ��� ���� �޴´�. �׸��� ��µ� ���ļ� �������� ����
		// ��Ÿ����.
		transformer = new RealDoubleFFT(blockSize);
		/*
		 * // ImageView �� ���� ��ü ���� �κ� imageView = (ImageView)
		 * findViewById(R.id.imageView01); // bitmap = Bitmap.createBitmap((int)
		 * 256, (int) 100, // Bitmap.Config.ARGB_8888); //300 200 bitmap =
		 * Bitmap.createBitmap((int) 256, (int) 200, Bitmap.Config.ARGB_8888);
		 * canvas = new Canvas(bitmap);
		 * 
		 * paint = new Paint(); paint.setColor(Color.argb(255, 247, 150, 70));
		 * paint.setStrokeWidth(2); // �� ����
		 * 
		 * imageView.setImageBitmap(bitmap);
		 * 
		 * recordTask = new RecordAudio(); recordTask.execute();// ����Ʈ�� Ȱ��ȭ
		 */
		// ���α׷����� ����
		waitDialog = new ProgressDialog(MainActivity.this);
		waitDialog.setMessage("��� ��ٸ�����...");
		waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		waitDialog.setCancelable(false);

		progressThread = new ProgressThread();

		button_stop.setEnabled(false);
		button_play.setEnabled(true);

		backPressCloseHandler = new BackPressCloseHandler();

		// ��Ƽ��Ƽ ��ȯ
		if (wifiManager.isWifiEnabled()) // wifi������̸�
		{
			IntentVifiList();
		} else {
			alertDlg.setMessage("Wifi�� ����Ǿ����� �ʽ��ϴ�.");
			alertDlg.show();
		}

		super.onResume(); // �����

		// TODO Auto-generated method stub
	}

	@Override
	protected void onResume() {
		/*
		 * if (recordTask.isCancelled()) { recordTask = new RecordAudio();
		 * recordTask.execute();// ����Ʈ�� Ȱ��ȭ }
		 */

		toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) { // �������̰� ����.
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
			// ����ġ ����
			if (wifiManager.isWifiEnabled()) // �������̰� ����Ǿ� ������
				toggle.setChecked(true);
			else
				toggle.setChecked(false);
			
			switch (nFlag) {
			case CLICK_WIFI_ON:
				toggleWiFi(true);
				Toast.makeText(getApplicationContext(), "�������� ����",
						Toast.LENGTH_SHORT).show();

				wifiview.setText("����");

				waitDialog.show(); // ���α׷����� ����

				CallList = true;

				if (progressThread.isAlive() == false)
					progressThread = new ProgressThread();

				progressThread.start();

				SetTimer();
				nFlag = 0;
				break;
			case CLICK_WIFI_OFF:
				toggleWiFi(false);
				Toast.makeText(getApplicationContext(), "�������� ����",
						Toast.LENGTH_SHORT).show();

				wifiview.setText("����");
				nFlag = 0;
				break;
			case CLICK_CONNECT:

				if (wifiManager.isWifiEnabled()) // wifi������̸�
				{
					IntentVifiList();// Sub_Activity ȣ��

				} else {
					alertDlg.setMessage("Wifi�� ����Ǿ����� �ʽ��ϴ�.");
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
				play = false; // ����� ������
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

		super.onResume(); // �����
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
		 * // ���� ���� ���� ���� if(isConnect == true) { buff[0] = 'O'; buff[1] =
		 * temp_Y; TcpIpMultichatClient1 = new TcpIpMultichatClient(buff);
		 * TcpIpMultichatClient1.start(); }
		 */
	}// ///////////////////////////////////////�⺻ ����

	private void SetTimer() {
		if (timer == null) {
			myTask = new TimerTask() {
				public void run() {
					Log.e("data", "myTask");

					waitDialog.dismiss();
					while (progressThread != null && progressThread.isAlive()) {
						progressThread.interrupt(); // ������ ����

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

		startActivityForResult(intent, 1); // Sub_Activity ȣ��
	}

	// ��Ƽ��Ƽ ���� �޾ƿ���
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.e("data", "4");

		if (resultCode == RESULT_OK) { // ��Ƽ��Ƽ�� ���������� ����Ǿ��� ���
			if (requestCode == 1) // InformationInput���� ȣ���� ��쿡�� ó���մϴ�.
			{
				temp = data.getStringExtra("data_name");
				Log.e("data", "temp=======>" + temp);
				/*
				 * // �������� �ڵ� ���� wfc.allowedAuthAlgorithms.clear();
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
				 * // ���� �������� ���� �޾ƿ��� wifiManager = (WifiManager)
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
				waitDialog.show(); // ���α׷����� ����

				if (progressThread.isAlive() == false)
					progressThread = new ProgressThread();

				progressThread.start();

				SetTimer();

				/*
				 * // ���� ���� ���� if(isConnect == false) { buff[0] = 'I';
				 * 
				 * TcpIpMultichatClient1 = new TcpIpMultichatClient(buff);
				 * TcpIpMultichatClient1.start(); Log.e("data", "iiiii");
				 * 
				 * new Thread(new Client()).start(); } else { //���� �����ٰ� �ٽ� ���� }
				 * Log.e("data", "kkkk"); }
				 */
			} else {
				Log.e("Error", "else");
			}

			super.onStart(); // ȭ�� �� ���
		}
	}

	// wifi�� ���������̸� ����, ���� �����̸� Ų��.
	public void toggleWiFi(boolean status) {
		if (status == true && !wifiManager.isWifiEnabled()) {
			// ���� on���� wifi������ �ȵ�����
			wifiManager.setWifiEnabled(true); // wifi����
		} else if (status == false && wifiManager.isWifiEnabled()) {
			// ���� off����, wifi������ �Ǿ�����
			wifiManager.setWifiEnabled(false); // wifi����
		}
	}

	// //////////////////////////////////////////////////////
	class ProgressThread extends Thread {
		public void run() {
			try {
				Thread.sleep(1000); // �ϴ� �ѹ� ����
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
			
			Log.e("data", "�ڵ鷯 ���� " + info.getSSID());

			switch (msg.what) {
			case MSG_BEFORE_CONNECT:

				IntentVifiList();// Sub_Activity ȣ��
				break;

			case CONNECT_ERROR:
				alertDlg.setMessage("�ֺ��� vifi��ȣ�� ���մϴ�.");
				alertDlg.show();

				wifiview.setText("����");

				break;
			case MSG_AFTER_CONNECT:

				Log.e("data", "temp=======>" + temp);
				Log.e("data", "info.getSSID()=======>" + info.getSSID());

				if (info.getSSID().contains(temp)) // ������ wifi�� ���� wifi�� ������
					wifiview.setText(info.getSSID());
				else {
					alertDlg.setMessage("TV��ȣ�� ���� ������� ���߽��ϴ�.");
					alertDlg.show();

					wifiview.setText("����");
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
	 * // AsyncTask : ����� �������̽��� ���ϴ� �ְ� �ϴ� �޼ҵ���� ������ ������� �����Ѵ�. private class
	 * RecordAudio extends AsyncTask<Void, double[], Void> {
	 * 
	 * // doInBackground �޼ҵ忡 �� �� �ִ� ���̸� ������ �̷� ������ ������ �� �ִ�.
	 * 
	 * @Override protected Void doInBackground(Void... params) { try { //
	 * AudioRecord�� �����ϰ� ��� int bufferSize =
	 * AudioRecord.getMinBufferSize(frequency, channelConfiguration,
	 * audioEncoding);
	 * 
	 * AudioRecord audioRecord = new AudioRecord(
	 * MediaRecorder.AudioSource.CAMCORDER, frequency, channelConfiguration,
	 * audioEncoding, bufferSize);
	 * 
	 * short[] buffer = new short[blockSize]; // ���� PCM ������ AudioRecord ��ü���� ����.
	 * fft���� doubleŸ�� �ʿ� double[] toTransform = new double[blockSize];
	 * 
	 * audioRecord.startRecording(); // ���� ����
	 * 
	 * while (started) { // ������ ���� int bufferReadResult =
	 * audioRecord.read(buffer, 0, blockSize);
	 * 
	 * for (int i = 0; i < blockSize && i < bufferReadResult; i++) { // short��
	 * double�� �ٲ� toTransform[i] = (double) buffer[i] / Short.MAX_VALUE; // ��ȣ
	 * // �ִ� // 16��Ʈ }
	 * 
	 * // �迭�� FFT ��ü�� �Ѱ��ش� transformer.ft(toTransform); // publishProgress�� ȣ���ϸ�
	 * onProgressUpdate�� ȣ��ȴ�. publishProgress(toTransform);
	 * 
	 * }
	 * 
	 * audioRecord.stop(); } catch (Throwable t) { Log.e("AudioRecord",
	 * "Recording Failed"); }
	 * 
	 * return null; }
	 * 
	 * // ���� ������� ����Ǽ� ����� �������̽��� ��ȣ�ۿ� ����
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

			onResume(); // ȭ�� �����
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);

			nCurrentVolumn = audioManager
					.getStreamVolume(AudioManager.STREAM_MUSIC);
			seekVolume.setProgress(nCurrentVolumn);

			onResume(); // ȭ�� �����
			return true;
		case KeyEvent.KEYCODE_BACK:

			Log.e("data", "KEYCODE_BACK!!");
			backPressCloseHandler.onBackPressed();
			if (backPressCloseHandler.getFlag() == true) {
				Toast.makeText(this, "���Ű�� �ѹ� �� �����ø� ����˴ϴ�.", Toast.LENGTH_LONG)
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
