package meh.watchdoge.request;

import android.os.Message;

class Request(id: Int): Builder {
	private          val _id = id;
	private lateinit var _next: Builder;

	override fun build(msg: Message) {
		msg.what = _id;
		_next.build(msg);
	}

	fun root() {
		_next = Root();
	}

	fun sniffer(id: Int?, body: Sniffer.() -> Unit) {
		val next = Sniffer(id);
		next.body();

		_next = next;
	}

	fun sniffer(body: Sniffer.() -> Unit) {
		sniffer(null, body);
	}

	fun wireless(body: Wireless.() -> Unit) {
		val next = Wireless();
		next.body();

		_next = next;
	}

	fun pinger(id: Int?, body: Pinger.() -> Unit) {
		val next = Pinger(id);
		next.body();

		_next = next;
	}

	fun pinger(body: Pinger.() -> Unit) {
		pinger(null, body);
	}
}
