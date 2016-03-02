package meh.watchdoge.backend;

import java.util.HashMap;
import java.util.HashSet;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.jetbrains.anko.*;
import nl.komponents.kovenant.*;

import meh.watchdoge.Request;
import meh.watchdoge.response.build as buildResponse;
import meh.watchdoge.response.Event.Pinger as EventBuilder;
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

		override fun request(req: Request): Boolean {
			when (req.command()) {
				Command.Pinger.CREATE ->
					create(req)

				Command.Pinger.START ->
					start(req)

				Command.Pinger.STOP ->
					stop(req)

				Command.Pinger.DESTROY ->
					destroy(req)

				Command.Pinger.SUBSCRIBE ->
					subscribe(req)

				Command.Pinger.UNSUBSCRIBE ->
					unsubscribe(req)

				else ->
					return false;
			}

			return true;
		}

		private fun create(req: Request) {
			var target   = req.bundle()!!.getString("target");
			var interval = req.bundle()!!.getDouble("interval");

			forward(req) {
				it.packString(target);

				if (interval == 0.0) {
					it.packLong(Math.round(defaultSharedPreferences
						.getString("ping_interval", "0").toDuration() * 1000.0));
				}
				else {
					it.packLong(Math.round(interval * 1000.0));
				}
			}
		}

		private fun start(req: Request) {
			val id = req.arg();

			forward(req) {
				it.packInt(id);
			}
		}

		private fun stop(req: Request) {
			val id = req.arg();

			forward(req) {
				it.packInt(id);
			}
		}

		private fun destroy(req: Request) {
			val id = req.arg();

			forward(req) {
				it.packInt(id);
			}
		}

		private fun subscribe(req: Request) {
			val id = req.arg();

			synchronized(_map) {
				if (!_map.containsKey(id)) {
					response(req, Command.Pinger.Error.NOT_FOUND);
				}
				else {
					_map.get(id)!!.add(req.origin());
					response(req, Command.SUCCESS);
				}
			}
		}

		private fun unsubscribe(req: Request) {
			val id = req.arg();

			synchronized(_map) {
				if (_map.containsKey(id)) {
					_map.get(id)!!.add(req.origin());
					response(req, Command.SUCCESS);
				}
				else {
					response(req, Command.Pinger.Error.NOT_FOUND);
				}
			}
		}

		override fun response(messenger: Messenger, req: Request, status: Int) {
			when (req.command()) {
				Command.Pinger.CREATE -> {
					val id = _unpacker.unpackInt();

					if (status == Command.SUCCESS) {
						synchronized(_map) {
							_map.put(id, HashSet());
						}
					}

					messenger.response(req, status) {
						arg = id;
					}
				}

				else ->
					messenger.response(req, status)
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
			send(id) {
				stats {
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
			}
		}

		private fun packet(id: Int) {
			send(id) {
				packet {
					it.putString("source", _unpacker.unpackString());
					it.putInt("sequence", _unpacker.unpackInt());
					it.putInt("ttl", _unpacker.unpackInt());
					it.putLong("trip", _unpacker.unpackLong());
				}
			}
		}

		private fun error(id: Int) {
			send(id) {
				error {
					it.putString("source", _unpacker.unpackString());
					it.putInt("sequence", _unpacker.unpackInt());
					it.putInt("ttl", _unpacker.unpackInt());
					it.putString("reason", _unpacker.unpackString());
				}
			}
		}

		private fun send(id: Int, body: EventBuilder.() -> Unit) {
			var msg = buildResponse {
				event {
					pinger(id) {
						this.body();
					}
				}
			}

			synchronized(_map) {
				if (_map.containsKey(id)) {
					_map.get(id)?.retainAll {
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
