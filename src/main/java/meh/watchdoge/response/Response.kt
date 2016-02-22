package meh.watchdoge.response;

import android.os.Message;

class Response: Builder {
	lateinit var type: Builder;

	override fun build(msg: Message) {
		type.build(msg);
	}

	fun control(body: Control.() -> Unit) {
		val next = Control();
		next.body();

		type = next;
	}

	fun event(body: Event.() -> Unit) {
		val next = Event();
		next.body();

		type = next;
	}
}
