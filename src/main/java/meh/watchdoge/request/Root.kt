package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Root(): Builder {
	override fun build(msg: Message) {
		msg.arg1 = Command.CONTROL;
		msg.arg2 = Command.Control.ROOT;
	}
}
