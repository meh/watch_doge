package meh.watchdoge.response;

import android.os.Bundle;
import android.os.Message;

import meh.watchdoge.backend.Command as C;

class Control: Builder {
	var family:  Int = C.UNKNOWN;
	var command: Int = C.UNKNOWN;
	var status:  Int = C.ERROR;
	var arg:     Int = 0;

	private var _bundle: Bundle? = null;

	override fun build(msg: Message) {
		msg.what = 0xBADB01;
		msg.arg1 = (status shl 16) or (command shl 8) or family;
		msg.arg2 = arg;

		if (_bundle != null) {
			msg.setData(_bundle);
		}
	}

	fun bundle(body: (Bundle) -> Unit) {
		_bundle = Bundle();
		body(_bundle!!);
	}
}
