package meh.watchdoge.request;

import android.os.Message;

class Request(var id: Int): Builder {
	lateinit private var sub: Builder;

	override fun build(msg: Message) {
		msg.what = id;
		sub.build(msg);
	}

	fun root() {
		sub = Root();
	}

	fun sniffer(id: Int?, body: Sniffer.() -> Unit) {
		val next = Sniffer(id);
		next.body();

		sub = next;
	}

	fun sniffer(body: Sniffer.() -> Unit) {
		sniffer(null, body);
	}

	fun wireless(body: Wireless.() -> Unit) {
		val next = Wireless();
		next.body();

		sub = next;
	}

	fun ping(id: Int?, body: Ping.() -> Unit) {
		val next = Ping(id);
		next.body();

		sub = next;
	}

	fun ping(body: Ping.() -> Unit) {
		ping(null, body);
	}
}
