package meh.watchdoge.backend;

import android.os.Bundle;
import android.os.Message;

class Sniffer {
	open class Event(id: Int, bundle: Bundle) {
		protected val _id     = id;
		protected val _bundle = bundle;

		companion object {
			fun from(msg: Message): Event {
				return when (msg.arg1) {
					Command.Event.Sniffer.PACKET ->
						Packet(msg.arg2, msg.getData())

					else ->
						throw IllegalArgumentException("unknown event type")
				}
			}
		}

		fun owner(): Int {
			return _id;
		}

		fun bundle(): Bundle {
			return _bundle;
		}
	}

	class Packet(id: Int, bundle: Bundle): Event(id, bundle) {

	}
}
