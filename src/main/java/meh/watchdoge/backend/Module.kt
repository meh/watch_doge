package meh.watchdoge.backend;

import meh.watchdoge.Request;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.content.ContextWrapper;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

abstract class Module(backend: Backend): ContextWrapper(backend.getApplicationContext()) {
	protected val _backend:  Backend;
	protected val _unpacker: MessageUnpacker;

	init {
		_backend  = backend;
		_unpacker = backend.unpacker();
	}

	fun response(msg: Message, status: Int, body: ((Bundle) -> Unit)? = null): Int {
		return _backend.response(msg, status, body);
	}

	fun forward(msg: Message, body: (MessagePacker) -> Unit): Int {
		return _backend.forward(msg, body);
	}

	abstract fun receive();
	abstract fun response(messenger: Messenger, request: Request, status: Int);
	abstract fun request(msg: Message): Boolean;
}
