package meh.watchdoge.backend;

import java.util.HashMap;
import java.util.HashSet;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import nl.komponents.kovenant.*;

import meh.watchdoge.Request;
import meh.watchdoge.request.command;
import meh.watchdoge.util.*;

class Pinger {
	class Conn(conn: Connection): Module.Connection(conn) {
		private val _subscriber = Module.Connection.SubscriberWithId<Event>();

		inner class Subscription(id: Int, body: (Event) -> Unit): Module.Connection.SubscriptionWithId<Event>(id, body) {
			override fun unsubscribe() {
				unsubscribe(_subscriber);

				if (_subscriber.empty(_id)) {
					request { pinger(_id) { unsubscribe() } }
				}
			}
		}

		fun subscribe(id: Int, body: (Event) -> Unit): Promise<Module.Connection.ISubscription, Exception> {
			return if (_subscriber.empty(id)) {
				request { pinger(id) { subscribe() } }
			}
			else {
				Promise.of(1);
			} then {
				_subscriber.subscribe(id, body);
				Subscription(id, body)
			}
		}

		override fun handle(msg: Message): Boolean {
			if (msg.what != Command.Event.PINGER) {
				return false;
			}

			_subscriber.emit(Pinger.event(msg));

			return true;
		}
	}

	class Mod(backend: Backend): Module(backend) {
		private val _map: HashMap<Int, HashSet<Messenger>> = HashMap();

		override fun request(msg: Message): Boolean {
			when (msg.command()) {
				Command.Pinger.CREATE ->
					create(msg)

				Command.Pinger.START ->
					start(msg)

				Command.Pinger.STOP ->
					stop(msg)

				Command.Pinger.DESTROY ->
					destroy(msg)

				Command.Pinger.SUBSCRIBE ->
					subscribe(msg)

				Command.Pinger.UNSUBSCRIBE ->
					unsubscribe(msg)

				else ->
					return false;
			}

			return true;
		}

		override fun receive() {
			val id    = _unpacker.unpackInt();
			val event = _unpacker.unpackInt();

			when (event) {
				Command.Event.Pinger.SENT ->
					Unit

				Command.Event.Pinger.STATS ->
					Unit
			}
		}

		override fun response(messenger: Messenger, request: Request, status: Int) {
			when (request.command()) {
				Command.Pinger.CREATE -> {
					val id = _unpacker.unpackInt();

					synchronized(_map) {
						_map.put(id, HashSet());
					}

					messenger.response(request, status) {
						it.putInt("id", id);
					}
				}

				else ->
					messenger.response(request, status)
			}
		}

		private fun create(msg: Message) {
			var target   = msg.getData().getString("target");
			var interval = msg.getData().getInt("interval");

			forward(msg) {
				it.packString(target);
				it.packInt(interval);
			}
		}

		private fun start(msg: Message) {
			val id = msg.getData().getInt("id");

			forward(msg) {
				it.packInt(id);
			}
		}

		private fun stop(msg: Message) {
			val id = msg.getData().getInt("id");

			forward(msg) {
				it.packInt(id);
			}
		}

		private fun destroy(msg: Message) {
			val id = msg.getData().getInt("id");

			forward(msg) {
				it.packInt(id);
			}
		}

		private fun subscribe(msg: Message) {
			val id = msg.getData().getInt("id");

			synchronized(_map) {
				if (!_map.containsKey(id)) {
					response(msg, Command.Pinger.Error.NOT_FOUND);
				}
				else {
					_map.get(id)!!.add(msg.replyTo);
					response(msg, Command.SUCCESS);
				}
			}
		}

		private fun unsubscribe(msg: Message) {
			val id = msg.getData().getInt("id");

			synchronized(_map) {
				if (_map.containsKey(id)) {
					_map.get(id)!!.add(msg.replyTo);
					response(msg, Command.SUCCESS);
				}
				else {
					response(msg, Command.Pinger.Error.NOT_FOUND);
				}
			}
		}
	}

	companion object {
		fun event(msg: Message): Event {
			return when (msg.arg1) {
				Command.Event.Pinger.STATS ->
					Stats(msg.arg2, msg.getData())

				else ->
					throw IllegalArgumentException("unknown event type")
			}
		}
	}

	open class Event(id: Int, bundle: Bundle): Module.EventWithId(id, bundle);
	class Sent(id: Int, bundle: Bundle): Event(id, bundle);
	class Stats(id: Int, bundle: Bundle): Event(id, bundle);
}
