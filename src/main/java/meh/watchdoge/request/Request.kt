package meh.watchdoge.request;

import android.os.Message;

class Request(id: Int): Builder {
	private          val _id = id;
	private lateinit var family: Family;

	override fun build(msg: Message) {
		msg.what = _id;
		family.build(msg);
	}

	fun control(body: Control.() -> Unit) {
		val next = Control();
		next.body();

		family = next;
	}

	fun sniffer(id: Int?, body: Sniffer.() -> Unit) {
		val next = Sniffer(id);
		next.body();

		family = next;
	}

	fun sniffer(body: Sniffer.() -> Unit) {
		sniffer(null, body);
	}

	fun wireless(body: Wireless.() -> Unit) {
		val next = Wireless();
		next.body();

		family = next;
	}

	fun pinger(id: Int?, body: Pinger.() -> Unit) {
		val next = Pinger(id);
		next.body();

		family = next;
	}

	fun pinger(body: Pinger.() -> Unit) {
		pinger(null, body);
	}
}
