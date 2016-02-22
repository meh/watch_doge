package meh.watchdoge.backend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.jetbrains.anko.*;
import nl.komponents.kovenant.*;

import meh.watchdoge.Request;
import meh.watchdoge.util.*;
import meh.watchdoge.backend.util.*;

import android.os.RemoteException;

class Sniffer {
	class Conn(conn: Connection): Module.Connection(conn) {
		private val _subscriber = Module.Connection.SubscriberWithId<Event>();

		inner class Subscription(id: Int, body: (Event) -> Unit): Module.Connection.SubscriptionWithId<Event>(id, body) {
			override fun unsubscribe() {
				unsubscribe(_subscriber);

				if (_subscriber.empty(_id)) {
					request { sniffer(_id) { unsubscribe() } }
				}
			}
		}

		fun subscribe(id: Int, body: (Event) -> Unit): Promise<Module.Connection.ISubscription, Exception> {
			return if (_subscriber.empty(id)) {
				request { sniffer(id) { subscribe() } }
			}
			else {
				Promise.of(1);
			} then {
				_subscriber.subscribe(id, body);
				Subscription(id, body)
			}
		}

		override fun handle(msg: Message): Boolean {
			if (msg.what != Command.Event.SNIFFER) {
				return false;
			}

			_subscriber.emit(Sniffer.event(msg));

			return true;
		}
	}

	class Mod(backend: Backend): Module(backend) {
		private val _map: HashMap<Int, HashSet<Messenger>> = HashMap();

		override fun request(req: Request): Boolean {
			when (req.command()) {
				Command.Sniffer.CREATE ->
					create(req)

				Command.Sniffer.START ->
					start(req)

				Command.Sniffer.FILTER ->
					filter(req)

				Command.Sniffer.STOP ->
					stop(req)

				Command.Sniffer.DESTROY ->
					destroy(req)

				Command.Sniffer.SUBSCRIBE ->
					subscribe(req)

				Command.Sniffer.UNSUBSCRIBE ->
					unsubscribe(req)

				else ->
					return false
			}

			return true;
		}

		private fun create(req: Request) {
			var truncate = req.bundle()!!.getInt("truncate");
			var ip       = req.bundle()!!.getString("ip")
				?: address(wifiManager.getConnectionInfo().getIpAddress());

			forward(req) {
				it.packInt(truncate);

				if (ip != null) {
					it.packString(ip);
				}
				else {
					it.packNil();
				}
			}
		}

		private fun start(req: Request) {
			val id = req.arg();

			forward(req) {
				it.packInt(id);
			}
		}

		private fun filter(req: Request) {
			val id     = req.arg();
			val filter = req.bundle()!!.getString("filter");

			forward(req) {
				it.packInt(id);

				if (filter == null) {
					it.packNil();
				}
				else {
					it.packString(filter);
				}
			}
		}

		private fun stop(req: Request) {
			// TODO: uguu~
		}

		private fun destroy(req: Request) {
			// TODO: uguu~
		}

		private fun subscribe(req: Request) {
			val id = req.arg();

			synchronized(_map) {
				if (!_map.containsKey(id)) {
					response(req, Command.Sniffer.Error.NOT_FOUND);
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
				if (!_map.containsKey(id)) {
					// TODO: send error
				}
				else {
					_map.get(id)!!.remove(req.origin());
				}
			}
		}

		override fun response(messenger: Messenger, req: Request, status: Int) {
			when (req.command()) {
				Command.Sniffer.CREATE -> {
					val id = _unpacker.unpackInt();

					synchronized(_map) {
						_map.put(id, HashSet());
					}

					messenger.response(req, status) {
						arg = id
					}
				}

				Command.Sniffer.FILTER -> {
					if (status == Command.Sniffer.Error.INVALID_FILTER) {
						val error = _unpacker.unpackString();

						messenger.response(req, status) {
							bundle {
								it.putString("reason", error);
							}
						}
					}
					else {
						messenger.response(req, status)
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
				Command.Event.Sniffer.PACKET ->
					packet(id)
			}
		}

		private fun packet(id: Int) {
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
				Command.Event.Sniffer.PACKET ->
					Packet(msg.arg2, msg.getData())

				else ->
					throw IllegalArgumentException("unknown event type")
			}
		}
	}

	open class Event(id: Int, bundle: Bundle): Module.EventWithId(id, bundle);
	class Packet(id: Int, bundle: Bundle): Event(id, bundle);
}
