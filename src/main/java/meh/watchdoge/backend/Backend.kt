package meh.watchdoge.backend;

import meh.watchdoge.R;
import meh.watchdoge.util.*;
import meh.watchdoge.backend.util.address;
import meh.watchdoge.Request;
import meh.watchdoge.request.family;
import meh.watchdoge.request.command;
import meh.watchdoge.request.isRequest;
import meh.watchdoge.request.into;

import android.util.Log;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import org.jetbrains.anko.*;

import android.app.Service;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.NetworkInfo;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.app.Notification;

import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessageTypeException;

import java.io.IOException;
import android.os.RemoteException;

public class Backend(): Service() {
	private          var _root:     Boolean = false;
	private lateinit var _process:  Process;
	private lateinit var _packer:   MessagePacker;
	private lateinit var _unpacker: MessageUnpacker;

	private lateinit var _messenger: Messenger;

	private var _request:  Int;
	private val _requests: HashMap<Int, Pair<Messenger, Request>>;
	private val _sniffers: HashMap<Int, HashSet<Messenger>>;
	private val _wireless: HashSet<Messenger>;

	init {
		_request  = 0;
		_requests = HashMap();

		_sniffers = HashMap();
		_wireless = HashSet();
	}

	private fun setting(name: String, body: (MessagePacker) -> Unit) {
		_packer.packString(name);
		body(_packer);
		_packer.flush();
	}

	private fun response(msg: Message, status: Int, body: ((Bundle) -> Unit)? = null): Int {
		val id = synchronized(_requests) {
			_request += 1;
			_request
		};

		try {
			msg.replyTo?.response(msg.into(id), status, body);
		}
		catch (e: RemoteException) { }

		return id;
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

		registerReceiver(Receiver(), IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

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

			val handled = when (msg.family()) {
				Command.CONTROL ->
					Control().handle(msg)

				Command.SNIFFER ->
					Sniffer().handle(msg)

				Command.WIRELESS ->
					Wireless().handle(msg)
				
				else ->
					false
			};

			if (!handled) {
				response(msg, Command.UNKNOWN)
				super.handleMessage(msg);
			}
		}

		inner class Control {
			fun handle(msg: Message): Boolean {
				when (msg.command()) {
					Command.Control.ROOT ->
						root(msg)

					else ->
						return false
				}

				return true;
			}

			private fun root(msg: Message) {
				response(msg, Command.SUCCESS) {
					it.putBoolean("status", _root);
				}
			}
		}

		inner class Sniffer {
			fun handle(msg: Message): Boolean {
				when (msg.command()) {
					Command.Sniffer.CREATE ->
						create(msg)

					Command.Sniffer.START ->
						start(msg)

					Command.Sniffer.FILTER ->
						filter(msg)

					Command.Sniffer.SUBSCRIBE ->
						subscribe(msg)

					Command.Sniffer.UNSUBSCRIBE ->
						unsubscribe(msg)

					else ->
						return false
				}

				return true;
			}

			private fun create(msg: Message) {
				var truncate = msg.getData().getInt("truncate");
				var ip       = msg.getData().getString("ip")
					?: address(wifiManager.getConnectionInfo().getIpAddress());

				forward(msg) {
					it.packInt(truncate);

					if (ip != null) {
						it.packString(ip);
					}
					else {
						it.packNil();
					}
				}
			}

			private fun start(msg: Message) {
				val id = msg.getData().getInt("id");

				forward(msg) {
					it.packInt(id);
				}
			}

			private fun filter(msg: Message) {
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
			}

			private fun subscribe(msg: Message) {
				val id = msg.getData().getInt("id");

				synchronized(_sniffers) {
					if (!_sniffers.containsKey(id)) {
						// TODO: send error
					}
					else {
						_sniffers.get(id)!!.add(msg.replyTo);
					}
				}
			}

			private fun unsubscribe(msg: Message) {
				val id = msg.getData().getInt("id");

				synchronized(_sniffers) {
					if (!_sniffers.containsKey(id)) {
						// TODO: send error
					}
					else {
						_sniffers.get(id)!!.remove(msg.replyTo);
					}
				}
			}
		}

		inner class Wireless {
			fun handle(msg: Message): Boolean {
				when (msg.command()) {
					Command.Wireless.STATUS ->
						status(msg)

					Command.Wireless.SUBSCRIBE ->
						subscribe(msg)

					Command.Wireless.UNSUBSCRIBE ->
						unsubscribe(msg)
					
					else ->
						return false
				}

				return true;
			}

			private fun status(msg: Message) {
				response(msg, Command.SUCCESS) {
					status(it);
				}
			}

			private fun subscribe(msg: Message) {
				synchronized(_wireless) {
					_wireless.add(msg.replyTo);
				}

				response(msg, Command.SUCCESS);
			}

			private fun unsubscribe(msg: Message) {
				synchronized(_wireless) {
					_wireless.remove(msg.replyTo);
				}

				response(msg, Command.SUCCESS);
			}

			fun status(res: Bundle) {
				if (wifiManager.isWifiEnabled()) {
					val info = wifiManager.getConnectionInfo();
					val dhcp = wifiManager.getDhcpInfo();

					if (info.getIpAddress() != 0) {
						res.putString("state", "connected");
						res.putInt("rssi", info.getRssi());
						res.putInt("strength", WifiManager.calculateSignalLevel(info.getRssi(), 100));

						res.putParcelable("client", Bundle().tap {
							it.putString("mac", info.getMacAddress());
							it.putString("ip",  address(info.getIpAddress()));
						});

						res.putParcelable("router", Bundle().tap {
							it.putString("mac", info.getBSSID());
							it.putString("ssid", info.getSSID());

							if (dhcp != null) {
								it.putString("ip", address(dhcp.gateway));
							}
						});

						if (dhcp != null) {
							res.putString("netmask", address(dhcp.netmask));
							res.putString("dns", address(dhcp.dns1));
						}
					}
					else {
						res.putString("state", "disconnected");
					}
				}
				else {
					res.putString("state", "disabled");
				}
			}
		}
	}

	inner class Receiver(): android.content.BroadcastReceiver() {
		override fun onReceive(@Suppress("UNUSED_PARAMETER") context: Context, intent: Intent) {
			when (intent.getAction()) {
				WifiManager.NETWORK_STATE_CHANGED_ACTION ->
					wireless(intent)
			}
		}

		private fun wireless(@Suppress("UNUSED_PARAMETER") intent: Intent) {
			val message = Message.obtain().tap {
				it.what = Command.Event.WIRELESS;
				it.arg1 = Command.Event.Wireless.STATUS;
			};

			Handler().Wireless().status(message.getData());

			synchronized(_wireless) {
				_wireless.retainAll {
					try {
						it.send(message);
						true
					}
					catch (e: RemoteException) {
						false
					}
				}
			}
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
										_sniffers.put(id, HashSet());
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
								it.arg1 = Command.Event.Sniffer.PACKET;
								it.arg2 = id;
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
									_sniffers.get(id)?.retainAll {
										try {
											it.send(message);
											true
										}
										catch (e: RemoteException) {
											false
										}
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
