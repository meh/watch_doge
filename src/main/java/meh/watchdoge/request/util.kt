package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

open class As(command: Int): Builder {
	val command = command;

	override fun build(msg: Message) {
		msg.arg2 = command;
	}
}

open class WithId(id: Int, command: Int): As(command) {
	val id = id;

	override fun build(msg: Message) {
		super.build(msg);
		msg.getData().putInt("id", id);
	}
}
