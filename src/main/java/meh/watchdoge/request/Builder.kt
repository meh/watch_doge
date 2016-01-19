package meh.watchdoge.request;

import android.os.Message;

interface Builder {
	fun build(msg: Message);
}

fun build(body: Request.() -> Unit): Message {
	val message = Message.obtain();
	val request = Request(0xBADB01);

	request.body();
	request.build(message);

	return message;
}
