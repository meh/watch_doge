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

		command = next;
	}

	fun start() {
		command = Start(_id!!);
	}

	fun stop() {
		command = Stop(_id!!);
	}

	fun subscribe() {
		command = Subscribe(_id!!);
	}

	fun unsubscribe() {
		command = Unsubscribe(_id!!);
	}

	fun destroy() {
		command = Destroy(_id!!);
	}

	class Create(target: String): Command(C.Pinger.CREATE) {
		var target   = target;
		var interval = 0;

		override fun build(msg: Message) {
			super.build(msg);

			msg.getData().putString("target", target);

			if (interval != 0) {
				msg.getData().putInt("interval", interval);
			}
		}
	}

	class Destroy(id: Int): WithId(id, C.Pinger.DESTROY);

	class Start(id: Int): WithId(id, C.Pinger.START);
	class Stop(id: Int): WithId(id, C.Pinger.STOP);

	class Subscribe(id: Int): WithId(id, C.Pinger.SUBSCRIBE);
	class Unsubscribe(id: Int): WithId(id, C.Pinger.UNSUBSCRIBE);
}
