package meh.watchdoge.backend;

import android.util.Log;
import android.text.TextUtils;
import java.util.HashMap;

import meh.watchdoge.R;

import android.app.Service;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.content.Intent;

import android.app.Notification;

import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;

public class Backend(): Service() {
	lateinit var _process:  Process;
	lateinit var _packer:   MessagePacker;
	lateinit var _unpacker: MessageUnpacker;

	var _sniffers: HashMap<Int, Sniffer> = HashMap();

	override fun onCreate() {
		Log.d("B", "service created");

		val cmd = arrayOf("su", "-c", getFileStreamPath("backend").getPath());

		try {
			_process  = Runtime.getRuntime().exec(cmd);
			_packer   = MessagePack.newDefaultPacker(DataOutputStream(_process.getOutputStream()));
			_unpacker = MessagePack.newDefaultUnpacker(DataInputStream(_process.getInputStream()));

			_packer.packInt(1);
			_packer.packInt(Command.SNIFFER);
			_packer.packInt(Command.Sniffer.CREATE);
			_packer.packString("192.168.1.116");
			_packer.flush();

			_packer.packInt(2);
			_packer.packInt(Command.SNIFFER);
			_packer.packInt(Command.Sniffer.START);
			_packer.packInt(1);
			_packer.flush();

			_packer.packInt(3);
			_packer.packInt(Command.SNIFFER);
			_packer.packInt(Command.Sniffer.FILTER);
			_packer.packInt(1);
			_packer.packString("icmp");
			_packer.flush();
		}
		catch (e: Exception) {
			// TODO: report to user he dun goofed
			Log.e("B", e.toString());
		}

		val notification = Notification.Builder(getApplicationContext())
			.setOngoing(true)
			.setContentTitle(getText(R.string.app_name))
			.setContentText(";^)")
			.setSmallIcon(R.drawable.notification)
			.build();

		startForeground(1, notification);

		Looper().start();
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		Log.d("B", "service started");

		return START_STICKY;
  }

  override fun onBind(intent: Intent): IBinder {
		return Messenger(Handler()).getBinder();
  }

  override fun onDestroy() {
		Log.d("B", "RIP");

		// TODO: send cleanup message
		_process.destroy();

		try {
			_process.waitFor();
		} catch (e: InterruptedException) { }

		Log.d("B", "in pieces");
  }

	inner class Handler(): android.os.Handler() {
		override fun handleMessage(msg: Message) {
			super.handleMessage(msg);
		}
	}

	inner class Looper(): Thread() {
		override fun run() {
			try {
				while (true) {
					val type = _unpacker.unpackInt();

					when (type) {
						Command.CONTROL -> {
							val request = _unpacker.unpackInt();
							val status  = _unpacker.unpackInt();

							Log.d("B", "response: request=" + request + " status=" + status);
						}

						Command.SNIFFER -> {
							val id   = _unpacker.unpackInt();
							val size = _unpacker.unpackInt();

							Log.d("B", "sniffer[" + id + "]: size=" + size);
						}
					}
				}
			}
			catch (e: IOException) {
				// TODO: warn the activity and try a restart
				Log.e("B", "looper died");
			}
		}
	}
}
