package meh.watchdoge.backend;

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
import meh.watchdoge.response.build as buildResponse;
import meh.watchdoge.response.Event.Wireless as EventBuilder;
import meh.watchdoge.util.*;
import meh.watchdoge.backend.util.*;
import org.jetbrains.anko.*;
import nl.komponents.kovenant.*;

import android.os.RemoteException;

class Wireless {
	class Conn(conn: Connection): Module.Connection(conn) {
		private val _subscriber = Module.Connection.Subscriber<Event>();

		inner class Subscription(body: (Event) -> Unit): Module.Connection.Subscription<Event>(body) {
			override fun unsubscribe() {
				unsubscribe(_subscriber);

				if (_subscriber.empty()) {
					request { wireless { unsubscribe() } }
				}
			}
		}

		fun subscribe(body: (Event) -> Unit): Promise<Module.Connection.ISubscription, Exception> {
			return if (_subscriber.empty()) {
				request { wireless { subscribe() } }
			}
			else {
				Promise.of(1);
			} then {
				_subscriber.subscribe(body);
				Subscription(body)
			}
		}

		override fun handle(msg: Message): Boolean {
			if (msg.what != Command.Event.WIRELESS) {
				return false;
			}

			_subscriber.emit(Wireless.event(msg));

			return true;
		}
	}

	class Mod(backend: Backend): Module(backend) {
		private val _list: HashSet<Messenger> = HashSet();

		init {
			backend.registerReceiver(Receiver(),
				IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		}

		override fun receive() {
			// uguu~
		}

		override fun request(req: Request): Boolean {
			when (req.command()) {
				Command.Wireless.STATUS ->
					status(req)

				Command.Wireless.SUBSCRIBE ->
					subscribe(req)

				Command.Wireless.UNSUBSCRIBE ->
					unsubscribe(req)
				
				else ->
					return false
			}

			return true;
		}

		override fun response(messenger: Messenger, req: Request, status: Int) {
			// uguu~
		}

		private fun status(req: Request) {
			response(req, Command.SUCCESS) {
				bundle {
					status(it)
				}
			}
		}

		private fun subscribe(req: Request) {
			synchronized(_list) {
				_list.add(req.origin());
			}

			response(req, Command.SUCCESS);
		}

		private fun unsubscribe(req: Request) {
			synchronized(_list) {
				_list.remove(req.origin());
			}

			response(req, Command.SUCCESS);
		}

		private fun status(res: Bundle) {
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
						send {
							status {
								status(it);
							}
						}
					}
				}
			}
		}

		private fun send(body: EventBuilder.() -> Unit) {
			var msg = buildResponse {
				event {
					wireless {
						this.body();
					}
				}
			}

			synchronized(_list) {
				_list.retainAll {
					try {
						it.send(msg);
						true
					}
					catch (e: RemoteException) {
						false
					}
				}
			}
		}
	}

	companion object {
		fun event(msg: Message): Event {
			return when (msg.arg1) {
				Command.Event.Wireless.STATUS ->
					Status(msg.getData())

				else ->
					throw IllegalArgumentException("unknown event type")
			}
		}
	}

	open class Event(bundle: Bundle): Module.Event(bundle);
	class Status(bundle: Bundle): Event(bundle);
}
