package meh.watchdoge.backend;

import meh.watchdoge.util.*;
import meh.watchdoge.Command;
import meh.watchdoge.Request;
import meh.watchdoge.request.family;
import meh.watchdoge.request.command;
import meh.watchdoge.request.incoming;
import meh.watchdoge.request.into;

import android.util.Log;
import android.text.TextUtils;
import java.util.HashMap;
import org.jetbrains.anko.*;

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
	lateinit var _process:   Process;
	lateinit var _packer:    MessagePacker;
	lateinit var _unpacker:  MessageUnpacker;
	lateinit var _messenger: Messenger;

	var _request:  Int                                    = 0;
	var _requests: HashMap<Int, Pair<Messenger, Request>> = HashMap();
	var _sniffers: HashMap<Int, Array<Messenger>>         = HashMap();

	override fun onCreate() {
		_messenger = Messenger(Handler());

		try {
			_process  = Runtime.getRuntime().exec(arrayOf("su", "-c", getFileStreamPath("backend").getPath()));
			_packer   = MessagePack.newDefaultPacker(DataOutputStream(_process.getOutputStream()));
			_unpacker = MessagePack.newDefaultUnpacker(DataInputStream(_process.getInputStream()));
		}
		catch (e: Exception) {
			// TODO: report to user he dun goofed
			Log.e("B", e.toString());
		}

		startForeground(1, Notification.Builder(getApplicationContext())
			.setOngoing(true)
			.setContentTitle(getText(R.string.app_name))
			.setContentText(";^)")
			.setSmallIcon(R.drawable.notification)
			.build());

		Looper().start();
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		return START_STICKY;
  }

  override fun onBind(intent: Intent?): IBinder {
		return _messenger.getBinder();
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
		private fun forward(msg: Message, body: (MessagePacker) -> Unit): Int {
			val id = synchronized(_requests) {
				_request += 1;
				_requests.put(_request, Pair(msg.replyTo, msg.into(_request)));
				_request
			}

			synchronized(_packer) {
				_packer.packInt(id);
				_packer.packInt(msg.family());
				_packer.packInt(msg.command());

				body(_packer);

				_packer.flush();
			}

			return id;
		}

		override fun handleMessage(msg: Message) {
			if (!msg.incoming()) {
				super.handleMessage(msg);
				return;
			}

			when (msg.family()) {
				Command.SNIFFER -> {
					when (msg.command()) {
						Command.Sniffer.CREATE -> {
							var ip = msg.getData().getString("ip");

							if (ip == null) {
								ip = wifiManager.getConnectionInfo().getIpAddress().let {
									"%d.%d.%d.%d".format(
										((it       ) and 0xff),
										((it shr  8) and 0xff),
										((it shr 16) and 0xff),
										((it shr 24) and 0xff))
								};
							}

							forward(msg) {
								it.packString(ip);
							}

							Log.d("B", "SNIFFER/CREATE:\t ip=${ip}");
						}

						Command.Sniffer.START -> {
							val id = msg.getData().getInt("id");

							forward(msg) {
								it.packInt(id);
							}

							Log.d("B", "SNIFFER/START:\t id=${id}");
						}

						Command.Sniffer.FILTER -> {
							val id     = msg.getData().getInt("id");
							val filter = msg.getData().getString("filter");

							forward(msg) {
								it.packInt(id);

								if (filter == null) {
									it.packNil();
								}
								else {
									it.packString(filter);
								}
							}

							Log.d("B", "SNIFFER/FILTER:\t id=${id} filter=${filter}");
						}
					}
				}
			}

			super.handleMessage(msg);
		}
	}

	inner class Looper(): Thread() {
		override fun run() {
			try {
				while (true) {
					val family = _unpacker.unpackInt();

					when (family) {
						Command.CONTROL -> {
							val id     = _unpacker.unpackInt();
							val status = _unpacker.unpackInt();

							val (messenger, request) = synchronized(_requests) {
								_requests.remove(id)!!
							}

							if (request.matches(Command.SNIFFER, Command.Sniffer.CREATE)) {
								val id = _unpacker.unpackInt();

								messenger.response(request, status) {
									it.putInt("id", id);
								}
							}
							else {
								messenger.response(request, status)
							}
						}

						Command.SNIFFER -> {
							val id   = _unpacker.unpackInt();
							val size = _unpacker.unpackInt();
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
