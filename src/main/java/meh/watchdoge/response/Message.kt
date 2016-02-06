package meh.watchdoge.response;

import meh.watchdoge.Response;
import meh.watchdoge.backend.Command;

import android.os.Bundle;
import android.os.Message;

fun Message.isResponse(): Boolean {
	return this.what < 0xBADB01 && this.replyTo == null;
}

fun Message.isSniffer(): Boolean {
	return this.what == Command.Event.SNIFFER;
}

fun Message.isWireless(): Boolean {
	return this.what == Command.Event.WIRELESS;
}

fun Message.status(): Int {
	return this.what;
}

fun Message.family(): Int {
	return this.arg1;
}

fun Message.command(): Int {
	return this.arg2;
}

fun Message.request(): Bundle {
	return this.obj as Bundle;
}

fun Message.matches(family: Int, command: Int): Boolean {
	return this.arg1 == family && this.arg2 == command;
}

fun Message.into(): Response {
	return Response(this.status(), this.family(), this.command(), this.request(), this.getData());
}
