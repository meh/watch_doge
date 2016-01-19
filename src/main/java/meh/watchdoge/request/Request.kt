package meh.watchdoge.request;

import android.os.Message;

class Request(var id: Int): Builder {
	lateinit private var sub: Builder;

	override fun build(msg: Message) {
		msg.what = id;
		sub.build(msg);
	}

	fun sniffer(id: Int?, body: Sniffer.() -> Unit) {
		val next = Sniffer(id);
		next.body();

		sub = next;
	}

	fun sniffer(body: Sniffer.() -> Unit) {
		sniffer(null, body);
	}
}
