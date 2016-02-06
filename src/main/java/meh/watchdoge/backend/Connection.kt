package meh.watchdoge.backend;

import java.util.ArrayDeque;

import meh.watchdoge.util.*;
import meh.watchdoge.Response;
import meh.watchdoge.response.isResponse;
import meh.watchdoge.response.isSniffer;
import meh.watchdoge.response.isWireless;
import meh.watchdoge.response.into;
import meh.watchdoge.request.Request;

import java.util.HashMap;
import java.util.HashSet;

import org.jetbrains.anko.*;
import nl.komponents.kovenant.*;

import android.os.IBinder;
import android.os.Bundle;
import android.os.Messenger;
import android.os.Message;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;

class Connection(context: Context, ready: (Connection) -> Unit) {
	private          val _receiver:   Messenger;
	private          val _connection: ServiceConnection;
	private lateinit var _sender:     Messenger;
	private          var _first:      Boolean;

	private val _ready:    (Connection) -> Unit;
	private var _requests: ArrayDeque<Deferred<Response, Response.Exception>>;
	private var _sniffers: HashMap<Int, HashSet<(Sniffer.Event) -> Unit>>;
	private var _wireless: HashSet<(Wireless.Event) -> Unit>;

	init {
		_receiver   = Messenger(Handler());
		_connection = ServiceConnection();
		_first      = true;

		_ready    = ready;
		_requests = ArrayDeque();
		_sniffers = HashMap();
		_wireless = HashSet();

		context.bindService(context.intentFor<Backend>(),
			_connection, Context.BIND_AUTO_CREATE);
	}

	fun request(body: Request.() -> Unit): Promise<Response, Response.Exception> {
		val req = deferred<Response, Response.Exception>();

		_requests.add(req);
		(_sender to _receiver).request(body)

		return req.promise;
	}

	fun subscribe(body: Subscriber.() -> Promise<Response, Response.Exception>): Promise<Response, Response.Exception> {
		return Subscriber().body();
	}

	inner class Subscriber {
		fun sniffer(id: Int, body: (Sniffer.Event) -> Unit): Promise<Response, Response.Exception> {
			return request { sniffer(id) { subscribe() } } success {
				synchronized(_sniffers) {
					if (!_sniffers.containsKey(id)) {
						_sniffers.put(id, HashSet());
					}

					_sniffers.get(id)!!.add(body);
				}
			};
		}

		fun wireless(body: (Wireless.Event) -> Unit): Promise<Response, Response.Exception> {
			return request { wireless { subscribe() } } success {
				synchronized(_wireless) {
					_wireless.add(body);
				}
			};
		}
	}

	inner class Handler(): android.os.Handler() {
		override fun handleMessage(msg: Message) {
			when {
				msg.isResponse() -> {
					val response = msg.into();
					val promise  = _requests.remove();

					if (response.isSuccess()) {
						promise.resolve(response);
					}
					else {
						promise.reject(response.exception()!!);
					}
				}

				msg.isSniffer() -> {
					val event = Sniffer.Event.from(msg);

					synchronized(_sniffers) {
						val subs = _sniffers.get(event.owner());

						if (subs != null) {
							for (sub in subs) {
								sub(event)
							}
						}
					}
				}

				msg.isWireless() -> {
					val event = Wireless.Event.from(msg);

					synchronized(_wireless) {
						for (sub in _wireless) {
							sub(event)
						}
					}
				}

				else ->
					super.handleMessage(msg)
			}
		}
	}

	inner class ServiceConnection(): android.content.ServiceConnection {
		override fun onServiceDisconnected(name: ComponentName) {
			// TODO: figure out what to do here
		}

		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			_sender = Messenger(service);

			if (_first) {
				_first = false;
				_ready(this@Connection);
			}
		}
	}
}
