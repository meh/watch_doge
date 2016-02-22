package meh.watchdoge.response;

import android.os.Message;

interface Builder {
	fun build(msg: Message);
}

fun build(body: Response.() -> Unit): Message {
	val message  = Message.obtain();
	val response = Response();

	response.body();
	response.build(message);

	return message;
}
