package meh.watchdoge.backend;

import java.util.ArrayDeque;

import meh.watchdoge.util.*;
import meh.watchdoge.Response;
import meh.watchdoge.response.isResponse;
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
	private val _requests: ArrayDeque<Deferred<Response, Response.Exception>>;

	private val _sniffer:  Sniffer.Connection;
	private val _wireless: Wireless.Connection;
	private val _pinger:   Pinger.Connection;

	init {
		_receiver   = Messenger(Handler());
		_connection = ServiceConnection();
		_first      = true;

		_ready    = ready;
		_requests = ArrayDeque();
		_sniffer  = Sniffer.Connection();
		_pinger   = Pinger.Connection();
		_wireless = Wireless.Connection();

		context.bindService(context.intentFor<Backend>(),
			_connection, Context.BIND_AUTO_CREATE);
	}

	fun request(body: Request.() -> Unit): Promise<Response, Response.Exception> {
		val req = deferred<Response, Response.Exception>();

		synchronized(_requests) {
			_requests.add(req);
			(_sender to _receiver).request(body)
		}

		return req.promise;
	}

	fun subscribe(body: Subscribe.() -> Promise<Subscriber, Exception>): Promise<Subscriber, Exception> {
		return Subscribe().body();
	}

	interface Subscriber {
		fun unsubscribe();
	}

	inner class Subscribe {
		fun sniffer(id: Int, body: (Sniffer.Event) -> Unit): Promise<Subscriber, Exception> {
			return request { sniffer(id) { subscribe() } } then {
				_sniffer.subscribe(id, body)
			};
		}

		fun wireless(body: (Wireless.Event) -> Unit): Promise<Subscriber, Exception> {
			return request { wireless { subscribe() } } then {
				_wireless.subscribe(body)
			};
		}

		fun pinger(id: Int, body: (Pinger.Event) -> Unit): Promise<Subscriber, Exception> {
			return request { pinger(id) { subscribe() } } then {
				_pinger.subscribe(id, body)
			}
		}
	}

	inner class Handler(): android.os.Handler() {
		override fun handleMessage(msg: Message) {
			when {
				msg.isResponse() -> {
					val response = msg.into();
					val promise  = synchronized(_requests) { _requests.remove() };

					if (response.isSuccess()) {
						promise.resolve(response);
					}
					else {
						promise.reject(response.exception()!!);
					}
				}

				_sniffer.handle(msg) ->
					Unit

				_wireless.handle(msg) ->
					Unit

				_pinger.handle(msg) ->
					Unit

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
