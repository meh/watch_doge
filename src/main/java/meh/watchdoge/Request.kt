package meh.watchdoge;

import android.os.Bundle;
import android.os.BaseBundle;

class Request(id: Int, family: Int, command: Int, bundle: Bundle) {
	private val _id      = id;
	private val _family  = family;
	private val _command = command;
	private val _bundle  = bundle;

	fun id()      = _id;
	fun family()  = _family;
	fun command() = _command;
	fun bundle()  = _bundle;
}
