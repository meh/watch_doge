package meh.watchdoge.request;

import meh.watchdoge.Request;
import android.os.Message;

fun Message.isRequest(): Boolean {
	return this.what == 0xBADB01 && this.replyTo != null;
}

fun Message.family(): Int {
	return this.arg1;
}

fun Message.command(): Int {
	return this.arg2;
}

fun Message.matches(family: Int, command: Int): Boolean {
	return this.arg1 == family && this.arg2 == command;
}

fun Message.into(id: Int): Request {
	return Request(id, this.arg1, this.arg2, this.getData());
}
