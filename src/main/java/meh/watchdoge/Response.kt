package meh.watchdoge;

import android.os.Bundle;
import meh.watchdoge.backend.Command;

class Response(status: Int, family: Int, command: Int, request: Bundle, bundle: Bundle) {
	private val _status  = status;
	private val _family  = family;
	private val _command = command;
	private val _request = request;
	private val _bundle  = bundle;

	fun status()  = _status;
	fun family()  = _family;
	fun command() = _command;
	fun request() = _request;
	fun bundle()  = _bundle;

	fun isSuccess(): Boolean = status() == Command.SUCCESS;
	fun isFailure(): Boolean = status() != Command.SUCCESS;

	fun matches(family: Int, command: Int): Boolean {
		return family() == family && command() == command;
	}

	fun exception(): Exception? {
		return if (isFailure()) {
			Exception(this)
		}
		else {
			null
		}
	}

	class Exception(res: Response) : java.lang.Exception() {
		private val _response = res;

		fun status()  = _response.status();
		fun family()  = _response.family();
		fun command() = _response.command();
		fun request() = _response.request();
		fun bundle()  = _response.bundle();
	}
}
