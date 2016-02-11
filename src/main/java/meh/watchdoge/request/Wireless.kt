package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Wireless(): Builder {
	lateinit private var sub: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.WIRELESS;
		sub.build(msg);
	}

	fun status() {
		sub = Status();
	}

	fun subscribe() {
		sub = Subscribe();
	}

	class Status(): As(Command.Wireless.STATUS);
	class Subscribe(): As(Command.Wireless.SUBSCRIBE);
	class Unsubscribe(): As(Command.Wireless.UNSUBSCRIBE);
}
