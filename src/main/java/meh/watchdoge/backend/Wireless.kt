package meh.watchdoge.backend;
import android.util.Log;

import java.util.HashSet;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.NetworkInfo;

import meh.watchdoge.Request;
import meh.watchdoge.request.command;
import meh.watchdoge.util.*;
import meh.watchdoge.backend.util.*;
import org.jetbrains.anko.*;

import android.os.RemoteException;

class Wireless {
	open class Event(bundle: Bundle) {
		protected val _bundle = bundle;

		companion object {
			fun from(msg: Message): Event {
				return when (msg.arg1) {
					Command.Event.Wireless.STATUS ->
						Status(msg.getData())

					else ->
						throw IllegalArgumentException("unknown event type")
				}
			}
		}

		fun bundle(): Bundle {
			return _bundle;
		}
	}

	class Connection {
		private val _subscribers: HashSet<(Event) -> Unit> = HashSet();

		fun subscribe(body: (Event) -> Unit) {
			synchronized(_subscribers) {
				_subscribers.add(body);
			}
		}

		fun handle(msg: Message): Boolean {
			if (msg.what != Command.Event.WIRELESS) {
				return false;
			}

			val event = Event.from(msg);

			synchronized(_subscribers) {
				for (sub in _subscribers) {
					sub(event)
				}
			}

			return true;
		}
	}

	class Module(backend: Backend): meh.watchdoge.backend.Module(backend) {
		private val _subscribers: HashSet<Messenger> = HashSet();

		init {
			backend.registerReceiver(Receiver(),
				IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		}

		override fun receive() {
			// uguu~
		}

		override fun request(msg: Message): Boolean {
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

		override fun response(messenger: Messenger, request: Request, status: Int) {
			// uguu~
		}

		private fun status(msg: Message) {
			response(msg, Command.SUCCESS) {
				status(it)
			}
		}

		private fun subscribe(msg: Message) {
			synchronized(_subscribers) {
				_subscribers.add(msg.replyTo);
			}

			response(msg, Command.SUCCESS);
		}

		private fun unsubscribe(msg: Message) {
			synchronized(_subscribers) {
				_subscribers.remove(msg.replyTo);
			}

			response(msg, Command.SUCCESS);
		}

		private fun status(res: Bundle) {
			Log.d("UI", "wireless: status");

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

		inner class Receiver(): android.content.BroadcastReceiver() {
			override fun onReceive(@Suppress("UNUSED_PARAMETER") context: Context, intent: Intent) {
				when (intent.getAction()) {
					WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
						val message = Message.obtain().tap {
							it.what = Command.Event.WIRELESS;
							it.arg1 = Command.Event.Wireless.STATUS;
						};

						status(message.getData());

						synchronized(_subscribers) {
							_subscribers.retainAll {
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

	class Status(bundle: Bundle): Event(bundle) {
	}
}
