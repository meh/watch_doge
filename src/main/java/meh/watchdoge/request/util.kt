package meh.watchdoge.request;

import android.os.Message;

open class Command(command: Int): Builder {
	val command = command;

	override fun build(msg: Message) {
		msg.arg1 = msg.arg1 or (command shl 8);
	}
}

open class WithId(id: Int, command: Int): Command(command) {
	val id = id;

	override fun build(msg: Message) {
		super.build(msg);
		msg.arg2 = id;
	}
}

open class Family(family: Int): Builder {
	         val family = family;
	lateinit var command: Command;

	override fun build(msg: Message) {
		msg.arg1 = msg.arg1 or family;

		command.build(msg);
	}
}
