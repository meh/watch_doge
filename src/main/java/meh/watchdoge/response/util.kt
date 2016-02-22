package meh.watchdoge.response;

import android.os.Message;
import android.os.Bundle;

open class EventFor(family: Int): Builder {
	protected          val _family = family;
	protected          var _type   = 0;
	protected lateinit var _bundle: (Bundle) -> Unit;

	override fun build(msg: Message) {
		msg.what = _family;
		msg.arg1 = _type;

		_bundle(msg.getData());
	}
}

open class EventForId(family: Int, id: Int): EventFor(family) {
	protected val _id     = id;

	override fun build(msg: Message) {
		super.build(msg);

		msg.arg2 = _id;
	}
}
