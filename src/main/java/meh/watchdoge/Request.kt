package meh.watchdoge;

import android.os.Bundle;
import android.os.Messenger;

class Request(id: Int, family: Int, command: Int, arg: Int, bundle: Bundle?, origin: Messenger) {
	private var _id      = id;
	private val _family  = family;
	private val _command = command;
	private val _arg     = arg;
	private val _bundle  = bundle;
	private val _origin  = origin;

	fun id()      = _id;
	fun family()  = _family;
	fun command() = _command;
	fun arg()     = _arg;
	fun bundle()  = _bundle;
	fun origin()  = _origin;

	fun id(value: Int): Request {
		_id = value;
		return this;
	}
}
