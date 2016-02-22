package meh.watchdoge.response;

import android.os.Bundle;
import android.os.Message;

import meh.watchdoge.backend.Command as C;

class Event(): Builder {
	private lateinit var _type: Builder;

	override fun build(msg: Message) {
		_type.build(msg);
	}

	fun sniffer(id: Int, body: Sniffer.() -> Unit) {
		val next = Sniffer(id);
		next.body();

		_type = next;
	}

	fun pinger(id: Int, body: Pinger.() -> Unit) {
		val next = Pinger(id);
		next.body();

		_type = next;
	}

	fun wireless(body: Wireless.() -> Unit) {
		val next = Wireless();
		next.body();

		_type = next;
	}

	class Sniffer(id: Int): EventForId(C.Event.SNIFFER, id) {
		fun packet(body: (Bundle) -> Unit) {
			_type   = C.Event.Sniffer.PACKET;
			_bundle = body;
		}
	}

	class Pinger(id: Int): EventForId(C.Event.PINGER, id) {
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

	class Wireless: EventFor(C.Event.WIRELESS) {
		fun status(body: (Bundle) -> Unit) {
			_type   = C.Event.Pinger.STATS;
			_bundle = body;
		}
	}
}
