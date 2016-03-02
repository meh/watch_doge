package meh.watchdoge.request;

import meh.watchdoge.backend.Command as C;
import android.os.Message;

class Pinger(id: Int?): Family(C.PINGER) {
	private val _id = id;

	fun create(target: String) {
		create(target) { }
	}

	fun create(target: String, body: Create.() -> Unit) {
		val next = Create(target);
		next.body();

		_command = next;
	}

	fun start() {
		_command = Start(_id!!);
	}

	fun stop() {
		_command = Stop(_id!!);
	}

	fun subscribe() {
		_command = Subscribe(_id!!);
	}

	fun unsubscribe() {
		_command = Unsubscribe(_id!!);
	}

	fun destroy() {
		_command = Destroy(_id!!);
	}

	class Create(target: String): Command(C.Pinger.CREATE) {
		var target   = target;
		var interval = 0.0;

		override fun build(msg: Message) {
			super.build(msg);

			msg.getData().putString("target", target);

			if (interval != 0.0) {
				msg.getData().putDouble("interval", interval);
			}
		}
	}

	class Destroy(id: Int): CommandWithId(id, C.Pinger.DESTROY);

	class Start(id: Int): CommandWithId(id, C.Pinger.START);
	class Stop(id: Int): CommandWithId(id, C.Pinger.STOP);

	class Subscribe(id: Int): CommandWithId(id, C.Pinger.SUBSCRIBE);
	class Unsubscribe(id: Int): CommandWithId(id, C.Pinger.UNSUBSCRIBE);
}
