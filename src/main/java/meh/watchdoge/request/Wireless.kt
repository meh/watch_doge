package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Wireless(): Builder {
	private lateinit var _next: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.WIRELESS;
		_next.build(msg);
	}

	fun status() {
		_next = Status();
	}

	fun subscribe() {
		_next = Subscribe();
	}

	class Status(): As(Command.Wireless.STATUS);
	class Subscribe(): As(Command.Wireless.SUBSCRIBE);
	class Unsubscribe(): As(Command.Wireless.UNSUBSCRIBE);
}
