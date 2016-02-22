package meh.watchdoge.response;

import android.os.Bundle;
import android.os.Message;

import meh.watchdoge.backend.Command as C;

class Event(): Builder {
	lateinit var type: Builder;

	override fun build(msg: Message) {
		type.build(msg);
	}

	fun sniffer(id: Int, body: Sniffer.() -> Unit) {
		val next = Sniffer(id);
		next.body();

		type = next;
	}

	fun pinger(id: Int, body: Pinger.() -> Unit) {
		val next = Pinger(id);
		next.body();

		type = next;
	}

	fun wireless(body: Wireless.() -> Unit) {
		val next = Wireless();
		next.body();

		type = next;
	}

	class Sniffer(id: Int): Builder {
		         val _id   = id;
		         var _type = 0;
		lateinit var _bundle: (Bundle) -> Unit;

		override fun build(msg: Message) {
			msg.what = C.Event.SNIFFER;
			msg.arg1 = _type;
			msg.arg2 = _id;

			_bundle(msg.getData());
		}

		fun packet(body: (Bundle) -> Unit) {
			_type   = C.Event.Sniffer.PACKET;
			_bundle = body;
		}
	}

	class Pinger(id: Int): Builder {
		         val _id   = id;
		         var _type = 0;
		lateinit var _bundle: (Bundle) -> Unit;

		override fun build(msg: Message) {
			msg.what = C.Event.PINGER;
			msg.arg1 = _type;
			msg.arg2 = _id;

			_bundle(msg.getData());
		}

		fun stats(body: (Bundle) -> Unit) {
			_type   = C.Event.Pinger.STATS;
			_bundle = body;
		}

		fun packet(body: (Bundle) -> Unit) {
			_type   = C.Event.Pinger.PACKET;
			_bundle = body;
		}

		fun error(body: (Bundle) -> Unit) {
			_type   = C.Event.Pinger.ERROR;
			_bundle = body;
		}
	}

	class Wireless: Builder {
		         var _type = 0;
		lateinit var _bundle: (Bundle) -> Unit;

		override fun build(msg: Message) {
			msg.what = C.Event.WIRELESS;
			msg.arg1 = _type;

			_bundle(msg.getData());
		}

		fun status(body: (Bundle) -> Unit) {
			_type   = C.Event.Pinger.STATS;
			_bundle = body;
		}
	}
}
