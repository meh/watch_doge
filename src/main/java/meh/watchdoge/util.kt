package meh.watchdoge.util;

import android.util.Log;

import android.os.Messenger;
import android.os.Message;
import android.os.Bundle;

import meh.watchdoge.request.Request;
import meh.watchdoge.request.build;

inline fun<T: Any, R> T.tap(tap: (T) -> R): T {
  tap(this)
  return this
}

infix fun Messenger.to(other: Messenger): Pair<Messenger, Messenger> {
	return Pair(this, other);
}

fun Pair<Messenger, Messenger>.request(body: Request.() -> Unit) {
	this.first.send(build(body).tap { it.replyTo = this.second });
}

fun Messenger.response(request: meh.watchdoge.Request, status: Int, body: ((Bundle) -> Unit)? = null) {
	var msg = Message.obtain();

	msg.what = status;
	msg.arg1 = request.family;
	msg.arg2 = request.command;
	msg.obj  = request.details;

	if (body != null) {
		body(msg.getData());
	}

	this.send(msg);
}
