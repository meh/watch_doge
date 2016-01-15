package meh.watchdoge.backend;

import android.util.Log;
import android.text.TextUtils;

import meh.watchdoge.R;

import android.os.IBinder;
import android.content.Intent;

import android.app.Notification;

import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;

public class Service extends android.app.Service {
	Process       _process;
	MessagePacker _packer;
	MessageUnpacker _unpacker;

  @Override
  public void onCreate() {
		Log.d("B", "service created");

		String[] cmd = { "su", "-c", getFileStreamPath("backend").getPath() };

		try {
			_process  = Runtime.getRuntime().exec(cmd);
			_packer   = MessagePack.newDefaultPacker(new DataOutputStream(_process.getOutputStream()));
			_unpacker = MessagePack.newDefaultUnpacker(new DataInputStream(_process.getInputStream()));

			_packer.packInt(23);
			_packer.packInt(42);
			_packer.packInt(0);
			_packer.flush();
		}
		catch (Exception e) {
			// TODO: report to user he dun goofed
			Log.e("B", e.toString());
		}

		Notification notification = new Notification.Builder(getApplicationContext())
			.setOngoing(true)
			.setContentTitle(getText(R.string.app_name))
			.setContentText(";^)")
			.setSmallIcon(R.drawable.notification)
			.build();

		startForeground(1, notification);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("B", "service started");

		return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
		return null;
  }

  @Override
  public void onDestroy() {
		Log.d("B", "RIP");

		// TODO: send cleanup message
		_process.destroy();

		try {
			_process.waitFor();
		} catch (InterruptedException e) { }

		Log.d("B", "in pieces");
  }
}
