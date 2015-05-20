package com.vifi.vifi;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class BackPressCloseHandler extends Activity {

	// Back의 상태값을 저장하기 위한 변수
	boolean m_close_flag = false;
	Handler m_close_handler;

	public BackPressCloseHandler() {
		// 일정 시간 후 상태값을 초기화하기 위한 핸들러
		m_close_handler = new Handler() {
			public void handleMessage(Message msg) {
				m_close_flag = false;
			}
		};

	}

	// ... 관련 없는 코드 생략 ...

	// Back 키가 터치되면 호출되는 메소드
	public void onBackPressed() {
		// m_close_flag 가 false 이면 첫번째로 키가 눌린 것이다.
		if (m_close_flag == false) { // Back 키가 첫번째로 눌린 경우

			// 안내 메세지를 토스트로 출력한다.
			//Toast.makeText( this , "취소키를 한번 더 누르시면 종료됩니다.", 
			//		Toast.LENGTH_LONG).show();
			
			// 상태값 변경
			m_close_flag = true;

			// 핸들러를 이용하여 3초 후에 0번 메세지를 전송하도록 설정한다.
			m_close_handler.sendEmptyMessageDelayed(0, 2000);

		} else { // Back 키가 3초 내에 연달아서 두번 눌린 경우

			m_close_flag = false;
			// 액티비티를 종료하는 상위 클래스의 onBackPressed 메소드를 호출한다.
			//super.onBackPressed();
		}
	}

	protected void onStop() {
		super.onStop();

		// 핸드러에 등록된 0번 메세지를 모두 지운다.
		m_close_handler.removeMessages(0);
	}
	public boolean getFlag() {
			return m_close_flag;
	}
}
