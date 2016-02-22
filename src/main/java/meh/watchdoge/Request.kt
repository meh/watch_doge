package meh.watchdoge;

import android.os.Bundle;

class Request(id: Int, family: Int, command: Int, arg: Int, bundle: Bundle?) {
	private val _id      = id;
	private val _family  = family;
	private val _command = command;
	private val _arg     = arg;
	private val _bundle  = bundle;

	fun id()      = _id;
	fun family()  = _family;
	fun command() = _command;
	fun arg()     = _arg;
	fun bundle()  = _bundle;
}
