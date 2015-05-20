package com.vifi.vifi;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class BackPressCloseHandler extends Activity {

	// Back�� ���°��� �����ϱ� ���� ����
	boolean m_close_flag = false;
	Handler m_close_handler;

	public BackPressCloseHandler() {
		// ���� �ð� �� ���°��� �ʱ�ȭ�ϱ� ���� �ڵ鷯
		m_close_handler = new Handler() {
			public void handleMessage(Message msg) {
				m_close_flag = false;
			}
		};

	}

	// ... ���� ���� �ڵ� ���� ...

	// Back Ű�� ��ġ�Ǹ� ȣ��Ǵ� �޼ҵ�
	public void onBackPressed() {
		// m_close_flag �� false �̸� ù��°�� Ű�� ���� ���̴�.
		if (m_close_flag == false) { // Back Ű�� ù��°�� ���� ���

			// �ȳ� �޼����� �佺Ʈ�� ����Ѵ�.
			//Toast.makeText( this , "���Ű�� �ѹ� �� �����ø� ����˴ϴ�.", 
			//		Toast.LENGTH_LONG).show();
			
			// ���°� ����
			m_close_flag = true;

			// �ڵ鷯�� �̿��Ͽ� 3�� �Ŀ� 0�� �޼����� �����ϵ��� �����Ѵ�.
			m_close_handler.sendEmptyMessageDelayed(0, 2000);

		} else { // Back Ű�� 3�� ���� ���޾Ƽ� �ι� ���� ���

			m_close_flag = false;
			// ��Ƽ��Ƽ�� �����ϴ� ���� Ŭ������ onBackPressed �޼ҵ带 ȣ���Ѵ�.
			//super.onBackPressed();
		}
	}

	protected void onStop() {
		super.onStop();

		// �ڵ巯�� ��ϵ� 0�� �޼����� ��� �����.
		m_close_handler.removeMessages(0);
	}
	public boolean getFlag() {
			return m_close_flag;
	}
}
