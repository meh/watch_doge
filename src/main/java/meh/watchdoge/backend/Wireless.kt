package meh.watchdoge.backend;

import android.os.Bundle;
import android.os.Message;

class Wireless {
	open class Event(bundle: Bundle) {
		protected val _bundle = bundle;

		companion object {
			fun from(msg: Message): Event {
				return when (msg.arg1) {
					Command.Event.Wireless.STATUS ->
						Status(msg.getData())

					else ->
						throw IllegalArgumentException("unknown event type")
				}
			}
		}

		fun bundle(): Bundle {
			return _bundle;
		}
	}

	class Status(bundle: Bundle): Event(bundle) {

	}
}
