package meh.watchdoge.backend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.jetbrains.anko.*;

import meh.watchdoge.Request;
import meh.watchdoge.request.command;
import meh.watchdoge.util.*;
import meh.watchdoge.backend.util.*;

import android.os.RemoteException;

class Sniffer {
	open class Event(id: Int, bundle: Bundle) {
		protected val _id     = id;
		protected val _bundle = bundle;

		companion object {
			fun from(msg: Message): Event {
				return when (msg.arg1) {
					Command.Event.Sniffer.PACKET ->
						Packet(msg.arg2, msg.getData())

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

		fun subscribe(id: Int, body: (Event) -> Unit): Subscriber  {
			synchronized(_subscribers) {
				if (!_subscribers.containsKey(id)) {
					_subscribers.put(id, HashSet());
				}

				_subscribers.get(id)!!.add(body);
			}

			return Subscriber(id, body);
		}

		fun handle(msg: Message): Boolean {
			if (msg.what != Command.Event.SNIFFER) {
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

		override fun receive() {
			val id    = _unpacker.unpackInt();
			val event = _unpacker.unpackInt();

			when (event) {
				Command.Event.Sniffer.PACKET ->
					packet(id)
			}
		}

		override fun response(messenger: Messenger, request: Request, status: Int) {
			when (request.command()) {
				Command.Sniffer.CREATE -> {
					val id = _unpacker.unpackInt();

					synchronized(_subscribers) {
						_subscribers.put(id, HashSet());
					}

					messenger.response(request, status) {
						it.putInt("id", id);
					}
				}

				Command.Sniffer.FILTER -> {
					if (status == Command.Sniffer.Error.INVALID_FILTER) {
						val error = _unpacker.unpackString();

						messenger.response(request, status) {
							it.putString("reason", error);
						}
					}
					else {
						messenger.response(request, status)
					}
				}

				else ->
					messenger.response(request, status)
			}
		}

		override fun request(msg: Message): Boolean {
			when (msg.command()) {
				Command.Sniffer.CREATE ->
					create(msg)

				Command.Sniffer.START ->
					start(msg)

				Command.Sniffer.FILTER ->
					filter(msg)

				Command.Sniffer.STOP ->
					stop(msg)

				Command.Sniffer.DESTROY ->
					destroy(msg)

				Command.Sniffer.SUBSCRIBE ->
					subscribe(msg)

				Command.Sniffer.UNSUBSCRIBE ->
					unsubscribe(msg)

				else ->
					return false
			}

			return true;
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

			synchronized(_subscribers) {
				if (_subscribers.containsKey(id)) {
					_subscribers.get(id)?.retainAll {
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

		private fun create(msg: Message) {
			var truncate = msg.getData().getInt("truncate");
			var ip       = msg.getData().getString("ip")
				?: address(wifiManager.getConnectionInfo().getIpAddress());

			forward(msg) {
				it.packInt(truncate);

				if (ip != null) {
					it.packString(ip);
				}
				else {
					it.packNil();
				}
			}
		}

		private fun start(msg: Message) {
			val id = msg.getData().getInt("id");

			forward(msg) {
				it.packInt(id);
			}
		}

		private fun filter(msg: Message) {
			val id     = msg.getData().getInt("id");
			val filter = msg.getData().getString("filter");

			forward(msg) {
				it.packInt(id);

				if (filter == null) {
					it.packNil();
				}
				else {
					it.packString(filter);
				}
			}
		}

		private fun stop(msg: Message) {
			// TODO: uguu~
		}

		private fun destroy(msg: Message) {
			// TODO: uguu~
		}

		private fun subscribe(msg: Message) {
			val id = msg.getData().getInt("id");

			synchronized(_subscribers) {
				if (!_subscribers.containsKey(id)) {
					response(msg, Command.Sniffer.Error.NOT_FOUND);
				}
				else {
					_subscribers.get(id)!!.add(msg.replyTo);
					response(msg, Command.SUCCESS);
				}
			}
		}

		private fun unsubscribe(msg: Message) {
			val id = msg.getData().getInt("id");

			synchronized(_subscribers) {
				if (!_subscribers.containsKey(id)) {
					// TODO: send error
				}
				else {
					_subscribers.get(id)!!.remove(msg.replyTo);
				}
			}
		}
	}

	class Packet(id: Int, bundle: Bundle): Event(id, bundle) {

	}
}
