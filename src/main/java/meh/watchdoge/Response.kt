package meh.watchdoge;

import android.os.Bundle;
import meh.watchdoge.backend.Command;

class Response(family: Int, command: Int, status: Int, arg: Int, bundle: Bundle?) {
	private val _family  = family;
	private val _command = command;
	private val _status  = status;
	private val _arg     = arg;
	private val _bundle  = bundle;

	fun family()  = _family;
	fun command() = _command;
	fun status()  = _status;
	fun arg()     = _arg;
	fun bundle()  = _bundle;

	fun isSuccess(): Boolean = status() == Command.SUCCESS;
	fun isFailure(): Boolean = status() != Command.SUCCESS;

	fun exception(): Exception? {
		return if (isFailure()) {
			Exception(this)
		}
		else {
			null
		}
	}

	class Exception(res: Response) : java.lang.Exception() {
		private val _r = res;

		fun family()  = _r.family()
		fun command() = _r.command()
		fun status()  = _r.status()
		fun arg()     = _r.arg()
		fun bundle()  = _r.bundle()
	}
}
