package meh.watchdoge.request;

import meh.watchdoge.backend.Command as C;

class Control(): Family(C.CONTROL) {
	fun root() {
		_command = Root();
	}

	class Root(): Command(C.Control.ROOT);
}
