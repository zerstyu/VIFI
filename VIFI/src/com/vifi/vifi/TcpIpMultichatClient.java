package com.vifi.vifi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import android.util.Log;

public class TcpIpMultichatClient {

	static public MainActivity tt;
	
	private Thread sender;
	private Thread receiver ;
	
	private static boolean isConnect = false;
	static String serverIp = "192.168.42.13";

	static char[] a = new char[3];
	static char temp;
	static String temp_2;
	public TcpIpMultichatClient(char[] buff){
		a[0] = buff[0];
		a[1] = buff[1];
	}


	public void start() {
		try {
			// 소켓을 생성하여 연결을 요청한다.
			Socket socket = new Socket(serverIp, 2150);
			System.out.println("서버에 연결되었습니다.");

			sender = new Thread(new ClientSender(socket));
			receiver = new Thread(new ClientReceiver(socket));

			sender.start();
			receiver.start();
		} catch (ConnectException ce) {
			Log.e("connection", ce.toString());
		} catch (Exception e) {

			Log.e("connection", e.toString());
		}
	} // main

	static class ClientSender extends Thread {
		Socket socket;
		DataOutputStream out;

		public ClientSender(Socket socket2) {
			// TODO Auto-generated constructor stub
			this.socket = socket2;
			try {
				out = new DataOutputStream(socket.getOutputStream());

			} catch (Exception e) {
			}

		}

		public void run() {
			try {
				if (out != null) {
					out.write(a[0]);
					out.write(a[1]);

				}
			} catch (IOException e) {
				System.out.println("오류발생");
			}
		} // run()
	}

	static class ClientReceiver extends Thread {
		Socket socket;
		DataInputStream in;

		ClientReceiver(Socket socket) {
			this.socket = socket;
			try {
				in = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {
			}
		}

		public void run() {
			//while (in != null) {
			String str; // 서버로부터 데이터 읽음
			try {
				while ((str=in.readLine()) != null) {
					//String str = in.readLine(); // 서버로부터 데이터 읽음
					temp = str.charAt(0);//str.split(delimiter); // 가상서버로부터 데이터받음
					Log.e("data","temp==========>"+temp);
					//tt.temp_Y = temp;
					//Log.e("data","tt.temp_Y==========>"+tt.temp_Y);
					temp_2 = str.substring(2, 11);
					Log.e("data","str==========>"+str);
					Log.e("data","temp_2==========>"+temp_2);
					isConnect = true;
					Log.e("data","isConnect==========>"+isConnect);
					break;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} // run

	}
}
