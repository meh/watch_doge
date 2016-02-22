package meh.watchdoge.request;

import android.os.Message;

open class Command(command: Int): Builder {
	protected val _command = command;

	override fun build(msg: Message) {
		msg.arg1 = msg.arg1 or (_command shl 8);
	}
}

open class CommandWithId(id: Int, command: Int): Command(command) {
	protected val _id = id;

	override fun build(msg: Message) {
		super.build(msg);
		msg.arg2 = _id;
	}
}

open class Family(family: Int): Builder {
	protected          val _family = family;
	protected lateinit var _command: Command;

	override fun build(msg: Message) {
		msg.arg1 = msg.arg1 or _family;
		_command.build(msg);
	}
}
