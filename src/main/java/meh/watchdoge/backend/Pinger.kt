package meh.watchdoge.backend;

import java.util.HashMap;
import java.util.HashSet;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import meh.watchdoge.Request;
import meh.watchdoge.request.command;
import meh.watchdoge.util.*;

class Pinger {
	open class Event(id: Int, bundle: Bundle) {
		protected val _id     = id;
		protected val _bundle = bundle;

		companion object {
			fun from(msg: Message): Event {
				return when (msg.arg1) {
					Command.Event.Pinger.STATS ->
						Stats(msg.arg2, msg.getData())

					else ->
						throw IllegalArgumentException("unknown event type")
				}
			}
		}

		fun owner(): Int {
			return _id;
		}

		fun bundle(): Bundle {
			return _bundle;
		}
	}

	class Connection {
		private val _subscribers: HashMap<Int, HashSet<(Event) -> Unit>> = HashMap();

		inner class Subscriber(id: Int, body: (Event) -> Unit): meh.watchdoge.backend.Connection.Subscriber {
			private val _id   = id;
			private val _body = body;

			override fun unsubscribe() {
				synchronized(_subscribers) {
					_subscribers.get(_id)?.remove(_body)
				}
			}
		}

		fun subscribe(id: Int, body: (Event) -> Unit): Subscriber {
			synchronized(_subscribers) {
				if (!_subscribers.containsKey(id)) {
					_subscribers.put(id, HashSet());
				}

				_subscribers.get(id)!!.add(body);
			}

			return Subscriber(id, body);
		}

		fun handle(msg: Message): Boolean {
			if (msg.what != Command.Event.PINGER) {
				return false;
			}

			val event = Event.from(msg);

			synchronized(_subscribers) {
				val subs = _subscribers.get(event.owner());

				if (subs != null) {
					for (sub in subs) {
						sub(event)
					}
				}
			}

			return true;
		}
	}

	class Module(backend: Backend): meh.watchdoge.backend.Module(backend) {
		private val _subscribers: HashMap<Int, HashSet<Messenger>> = HashMap();

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

					synchronized(_subscribers) {
						_subscribers.put(id, HashSet());
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

			synchronized(_subscribers) {
				if (!_subscribers.containsKey(id)) {
					response(msg, Command.Pinger.Error.NOT_FOUND);
				}
				else {
					_subscribers.get(id)!!.add(msg.replyTo);
					response(msg, Command.SUCCESS);
				}
			}
		}

		private fun unsubscribe(msg: Message) {
			// TODO: uguu~
		}
	}

	class Sent(id: Int, bundle: Bundle): Event(id, bundle) {

	}

	class Stats(id: Int, bundle: Bundle): Event(id, bundle) {

	}
}
