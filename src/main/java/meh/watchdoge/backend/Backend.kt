package meh.watchdoge.backend;

import meh.watchdoge.util.*;
import meh.watchdoge.Command;
import meh.watchdoge.Request;
import meh.watchdoge.request.family;
import meh.watchdoge.request.command;
import meh.watchdoge.request.isRequest;
import meh.watchdoge.request.into;

import android.util.Log;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.ArrayList;
import org.jetbrains.anko.*;

import meh.watchdoge.R;

import android.app.Service;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;
import android.content.Intent;

import android.app.Notification;

import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessageTypeException;

import java.io.IOException;

public class Backend(): Service() {
	private          var _root:     Boolean = false;
	private lateinit var _process:  Process;
	private lateinit var _packer:   MessagePacker;
	private lateinit var _unpacker: MessageUnpacker;

	private lateinit var _messenger: Messenger;

	private var _request:  Int                                    = 0;
	private var _requests: HashMap<Int, Pair<Messenger, Request>> = HashMap();
	private var _sniffers: HashMap<Int, ArrayList<Messenger>>     = HashMap();

	private fun setting(name: String, body: (MessagePacker) -> Unit) {
		_packer.packString(name);
		body(_packer);
		_packer.flush();
	}

	override fun onCreate() {
		_messenger = Messenger(Handler());

		try {
			_process  = Runtime.getRuntime().exec(arrayOf("su", "-c", getFileStreamPath("backend").getPath()));
			_packer   = MessagePack.newDefaultPacker(DataOutputStream(_process.getOutputStream()));
			_unpacker = MessagePack.newDefaultUnpacker(DataInputStream(_process.getInputStream()));
			_root     = _unpacker.unpackBoolean();

			setting("cache") {
				_packer.packString(getExternalCacheDir().getPath());
			}

			_packer.packNil();
		}
		catch (e: Exception) {
			_root = false;
		}

		startForeground(1, Notification.Builder(getApplicationContext())
			.setOngoing(true)
			.setContentTitle(getText(R.string.app_name))
			.setContentText(getString(if (_root) { R.string.yes_root } else { R.string.no_root }))
			.setSmallIcon(R.drawable.notification)
			.build());

		if (_root) {
			Looper().start();
		}
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		return START_STICKY;
  }

  override fun onBind(intent: Intent?): IBinder {
		return _messenger.getBinder();
  }

  override fun onDestroy() {
		_packer.packInt(0);
		_packer.packInt(Command.CONTROL);
		_packer.packInt(Command.Control.CLOSE);
		_packer.flush();

		try {
			_process.waitFor();
		} catch (e: InterruptedException) { }
  }

	inner class Handler(): android.os.Handler() {
		private fun response(msg: Message, status: Int, body: ((Bundle) -> Unit)? = null): Int {
			val id = synchronized(_requests) {
				_request += 1;
				_request
			};

			msg.replyTo?.response(msg.into(id), status, body);

			return id;
		}

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
			if (!msg.isRequest()) {
				super.handleMessage(msg);
				return;
			}

			when (msg.family()) {
				Command.CONTROL -> {
					when (msg.command()) {
						Command.Control.ROOT -> {
							response(msg, Command.SUCCESS) {
								it.putBoolean("status", _root);
							}

							return;
						}
					}
				}

				Command.SNIFFER -> {
					when (msg.command()) {
						Command.Sniffer.CREATE -> {
							var ip       = msg.getData().getString("ip");
							var truncate = msg.getData().getInt("truncate");

							if (ip == null) {
								ip = wifiManager.getConnectionInfo().getIpAddress().let {
									if (it != 0) {
										"%d.%d.%d.%d".format(
											((it       ) and 0xff),
											((it shr  8) and 0xff),
											((it shr 16) and 0xff),
											((it shr 24) and 0xff))
									}
									else {
										null
									}
								};
							}

							forward(msg) {
								it.packInt(truncate);

								if (ip != null) {
									it.packString(ip);
								}
								else {
									it.packNil();
								}
							}

							return;
						}

						Command.Sniffer.START -> {
							val id = msg.getData().getInt("id");

							forward(msg) {
								it.packInt(id);
							}

							return;
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

							return;
						}

						Command.Sniffer.SUBSCRIBE -> {
							val id = msg.getData().getInt("id");

							synchronized(_sniffers) {
								if (!_sniffers.containsKey(id)) {
									// TODO: send error
								}
								else {
									val subscribers = _sniffers.get(id)!!;

									if (!subscribers.contains(msg.replyTo)) {
										subscribers.add(msg.replyTo);
									}
								}
							}

							return;
						}

						Command.Sniffer.UNSUBSCRIBE -> {
							val id = msg.getData().getInt("id");

							synchronized(_sniffers) {
								if (!_sniffers.containsKey(id)) {
									// TODO: send error
								}
								else {
									val subscribers = _sniffers.get(id)!!;

									if (subscribers.contains(msg.replyTo)) {
										subscribers.remove(msg.replyTo);
									}
								}
							}

							return;
						}
					}
				}
			}

			response(msg, Command.UNKNOWN)
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

							when {
								request.matches(Command.SNIFFER, Command.Sniffer.CREATE) -> {
									val id = _unpacker.unpackInt();

									synchronized(_sniffers) {
										_sniffers.put(id, ArrayList());
									}

									messenger.response(request, status) {
										it.putInt("id", id);
									}
								}

								request.matches(Command.SNIFFER, Command.Sniffer.FILTER) &&
								status == Command.Sniffer.Error.INVALID_FILTER -> {
									val error = _unpacker.unpackString();

									messenger.response(request, status) {
										it.putString("reason", error);
									}
								}

								else ->
									messenger.response(request, status)
							}
						}

						Command.SNIFFER -> {
							val id      = _unpacker.unpackInt();
							val message = Message.obtain().tap {
								it.what = Command.Event.SNIFFER;
							}

							val packet = message.getData();

							packet.putInt("id", _unpacker.unpackInt());

							packet.putParcelable("size", Bundle().tap {
								packet.putInt("original", _unpacker.unpackInt());
								packet.putInt("recorded", _unpacker.unpackInt());
							});

							packet.putParcelable("timestamp", Bundle().tap {
								it.putLong("sec",  _unpacker.unpackLong());
								it.putLong("usec", _unpacker.unpackLong());
							});

							val layers: ArrayList<Bundle> = arrayListOf();

							do {
								val layer = _unpacker.unpackValue();

								when {
									layer.isStringValue() -> {
										val size = _unpacker.unpackMapHeader();
										val info = Bundle().tap {
											it.putString("_", layer.asStringValue().asString());
										}

										for (i in 1 .. size) {
											val name  = _unpacker.unpackString();
											val value = _unpacker.unpackValue();

											info.putValue(name, value);
										}

										layers.add(info);
									}

									layer.isBinaryValue() -> {
										layers.add(Bundle().tap {
											it.putByteArray("data", layer.asBinaryValue().asByteArray());
										});
									}
								}
							} while (!layer.isNilValue())

							packet.putParcelableArrayList("layers", layers);

							synchronized(_sniffers) {
								if (_sniffers.containsKey(id)) {
									val subscribers = _sniffers.get(id)!!;

									for (subscriber in subscribers) {
										subscriber.send(message);
									}
								}
							}
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
