package meh.watchdoge.backend;

import java.util.HashMap;
import java.util.HashSet;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.jetbrains.anko.*;
import nl.komponents.kovenant.*;

import meh.watchdoge.Request;
import meh.watchdoge.request.command;
import meh.watchdoge.util.*;

import android.os.RemoteException;

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

		private fun create(msg: Message) {
			var target   = msg.getData().getString("target");
			var interval = msg.getData().getInt("interval");

			forward(msg) {
				it.packString(target);

				if (interval == 0) {
					it.packLong(Math.round(defaultSharedPreferences
						.getString("ping_interval", "0").toDuration() * 1000.0));
				}
				else {
					it.packInt(interval);
				}
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

		override fun receive() {
			val id    = _unpacker.unpackInt();
			val event = _unpacker.unpackInt();

			when (event) {
				Command.Event.Pinger.STATS ->
					stats(id)

				Command.Event.Pinger.PACKET ->
					packet(id)

				Command.Event.Pinger.ERROR ->
					error(id)
			}
		}

		private fun stats(id: Int) {
			val message = Message.obtain().tap {
				it.what = Command.Event.PINGER;
				it.arg1 = Command.Event.Pinger.STATS;
				it.arg2 = id;
			}

			message.getData().tap {
				it.putParcelable("packet", Bundle().tap {
					it.putLong("sent", _unpacker.unpackLong());
					it.putLong("received", _unpacker.unpackLong());
					it.putFloat("loss", _unpacker.unpackFloat());
				});

				it.putParcelable("trip", Bundle().tap {
					it.putLong("minimum", _unpacker.unpackLong());
					it.putLong("maximum", _unpacker.unpackLong());
					it.putLong("average", _unpacker.unpackLong());
				});
			}

			send(id, message);
		}

		private fun packet(id: Int) {
			val message = Message.obtain().tap {
				it.what = Command.Event.PINGER;
				it.arg1 = Command.Event.Pinger.PACKET;
				it.arg2 = id;
			}

			message.getData().tap {
				it.putString("source", _unpacker.unpackString());
				it.putInt("sequence", _unpacker.unpackInt());
				it.putInt("ttl", _unpacker.unpackInt());
				it.putLong("trip", _unpacker.unpackLong());
			}

			send(id, message);
		}

		private fun error(id: Int) {
			val message = Message.obtain().tap {
				it.what = Command.Event.PINGER;
				it.arg1 = Command.Event.Pinger.ERROR;
				it.arg2 = id;
			}

			message.getData().tap {
				it.putString("source", _unpacker.unpackString());
				it.putInt("sequence", _unpacker.unpackInt());
				it.putInt("ttl", _unpacker.unpackInt());
				it.putString("reason", _unpacker.unpackString());
			}

			send(id, message);
		}

		private fun send(id: Int, message: Message) {
			synchronized(_map) {
				if (_map.containsKey(id)) {
					_map.get(id)?.retainAll {
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

	companion object {
		fun event(msg: Message): Event {
			return when (msg.arg1) {
				Command.Event.Pinger.STATS ->
					Stats(msg.arg2, msg.getData())

				Command.Event.Pinger.PACKET ->
					Packet(msg.arg2, msg.getData())

				Command.Event.Pinger.ERROR ->
					Error(msg.arg2, msg.getData())

				else ->
					throw IllegalArgumentException("unknown event type")
			}
		}
	}

	open class Event(id: Int, bundle: Bundle): Module.EventWithId(id, bundle);

	class Stats(id: Int, bundle: Bundle): Event(id, bundle) {
		data class Packet(val sent: Long, val received: Long, val loss: Float);
		data class Trip(val minimum: Long, val maximum: Long, val average: Long);

		fun packet(): Packet {
			return bundle().getParcelable<Bundle>("packet").let {
				Packet(it.getLong("sent"), it.getLong("received"), it.getFloat("loss"))
			};
		}

		fun trip(): Trip {
			return bundle().getParcelable<Bundle>("trip").let {
				Trip(it.getLong("minimum"), it.getLong("maximum"), it.getLong("average"))
			};
		}
	}

	open class Entry(id: Int, bundle: Bundle): Event(id, bundle) {
		fun source(): String {
			return bundle().getString("source")
		}

		fun sequence(): Int {
			return bundle().getInt("sequence")
		}

		fun ttl(): Int {
			return bundle().getInt("ttl")
		}
	}

	class Packet(id: Int, bundle: Bundle): Entry(id, bundle) {
		fun trip(): Long {
			return bundle().getLong("trip")
		}
	}

	class Error(id: Int, bundle: Bundle): Entry(id, bundle) {
		fun reason(): String {
			return Regex("""-(.)""").replace(bundle().getString("reason").capitalize()) {
				" ${it.groups.get(1)!!.value.toUpperCase()}"
			}
		}
	}
}
