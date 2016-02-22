package meh.watchdoge.request;

import meh.watchdoge.backend.Command as C;

class Wireless(): Family(C.WIRELESS) {
	fun status() {
		command = Status();
	}

	fun subscribe() {
		command = Subscribe();
	}

	class Status(): Command(C.Wireless.STATUS);
	class Subscribe(): Command(C.Wireless.SUBSCRIBE);
	class Unsubscribe(): Command(C.Wireless.UNSUBSCRIBE);
}
