package meh.watchdoge.backend;

import android.util.Log;
import android.text.TextUtils;

import android.os.IBinder;
import android.content.Intent;

import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;

public class Service extends android.app.Service {
	Process          _process;
	DataOutputStream _output;
	DataInputStream  _input;

  @Override
  public void onCreate() {
		String[] cmd = { "su", "-c", getFileStreamPath("backend").getPath() };

		try {
			_process = Runtime.getRuntime().exec(cmd);
			_output  = new DataOutputStream(_process.getOutputStream());
			_input   = new DataInputStream(_process.getInputStream());
		}
		catch (Exception e) {
			// TODO: report to user he dun goofed
			Log.e("B", e.toString());
		}
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
		return null;
  }

  @Override
  public void onDestroy() {
		// TODO: send the subprocess the cleanup message and `_process.wait()`
		Log.d("B", "RIP");
  }
}
