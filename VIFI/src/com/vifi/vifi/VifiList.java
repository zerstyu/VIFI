package com.vifi.vifi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VifiList extends Activity implements OnItemClickListener {

	static String serverIp;

	Toast toast;

	private VifiList wifilist;
	
	private WifiManager mwifi;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // TODO Auto-generated method stub
	    setContentView(R.layout.activity_vifi_list);

		wifilist = this;

		mwifi = (WifiManager) wifilist.getSystemService(WIFI_SERVICE);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		ScanResult scanResult = null;
		String string = null;
		List<ScanResult> Aplist = mwifi.getScanResults(); // 리스트

		int size = Aplist.size();

		ArrayList<String> arraylist = new ArrayList<String>();

		for (int i = 0; i < size; i++) {

			scanResult = (ScanResult) Aplist.get(i);
			string = scanResult.SSID;
			
			//if (string.contains("TV")) // 와이파이가 해당 문자를 포함하는가
			{
				if (!arraylist.contains(string)) {
					arraylist.add(scanResult.SSID); // list에 추가
				}
			}
			
			ArrayAdapter<String> Adapter;
			
			Adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, arraylist);

			ListView list1 = (ListView) findViewById(R.id.list);
			list1.setAdapter(Adapter);
//
//			if(arraylist.size()==0)
//			{
//				arraylist.add("연결 가능한 TV가 없습니다.");
//			}
			
			// 클릭 이벤트
			list1.setOnItemClickListener(this);
		}
	}
	// item이 선택되면
	public void onItemClick(AdapterView<?> adapterView, View clickedView,
			int pos, long id) {

		String toastMessage = ((TextView) clickedView).getText().toString();

		WifiInfo wifiInfo = mwifi.getConnectionInfo();
		//wifiInfo.getRssi(); // 신호 세기를 얻을 수 있음

		ScanResult scanResult = null;
		String string = null;

		List<ScanResult> Aplist = mwifi.getScanResults();
		// 리스트
		int size = Aplist.size();

		// 리스트를 돈다 여기 잘 모르겠다.
		for (int i = 0; i < size; i++) {
			scanResult = (ScanResult) Aplist.get(i);
			string = scanResult.SSID;
			// ssid 와 선택한 wifi의 ssid가 같을때 들어간다.
			if (wifiInfo.getSSID() != null && string.equals(toastMessage)
					&& wifiInfo.getNetworkId() != -1) {

				// 네트워크 연결
				mwifi.enableNetwork(wifiInfo.getNetworkId(), true);

				break;
			}
		}
		Intent intent = getIntent(); // 이 액티비티를 시작하게 한 인텐트를 호출
		intent.putExtra("data_name", toastMessage.toString());

		setResult(RESULT_OK, intent); // 추가 정보 입력

		finish(); // 액티비티 종료
	}

}
