package com.vifi.vifi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import android.util.Log;

public class Client implements Runnable {

	private char a = 'I';

	byte[] data = new byte[1024]; // 데이터 저장소

	private static final int PORT = 2521;
	// private static final String GroupAddress = "226.1.1.1";
	public TcpIpMultichatClient aa;
	private static String GroupAddress;

	public Client() {
		GroupAddress = aa.temp_2;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		DatagramPacket dp = new DatagramPacket(data, data.length);

		Log.e("data", "333");
		// port를 소스로 해서 객체 생성
		MulticastSocket ms = null;
		// TODO Auto-generated method stub
		try {

			Log.e("data", "444");
			ms = new MulticastSocket(PORT);
			InetAddress ia = InetAddress.getByName(GroupAddress);
			Log.e("data", "GroupAddress=========>" + GroupAddress);
			ms.joinGroup(ia);
		} catch (IOException e1) {
			e1.printStackTrace();

		}
		while (true) {
			try {

				// Log.e("data", "client");
				ms.receive(dp);

				// String a = new String(data, 0, dp.getLength());

				// android.util.Log.i("zzz", a);

			} catch (UnknownHostException e) {
				e.printStackTrace();
				break;

			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
