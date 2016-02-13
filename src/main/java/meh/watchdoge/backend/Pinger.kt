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

		fun subscribe(id: Int, body: (Event) -> Unit) {
			synchronized(_subscribers) {
				if (!_subscribers.containsKey(id)) {
					_subscribers.put(id, HashSet());
				}

				_subscribers.get(id)!!.add(body);
			}
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
			// uguu~
		}

		override fun response(messenger: Messenger, request: Request, status: Int) {
			// uguu~
		}

		private fun create(msg: Message) {
			// TODO: uguu~
		}

		private fun start(msg: Message) {
			// TODO: uguu~
		}

		private fun stop(msg: Message) {
			// TODO: uguu~
		}

		private fun destroy(msg: Message) {
			// TODO: uguu~
		}

		private fun subscribe(msg: Message) {
			// TODO: uguu~
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
