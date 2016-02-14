package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Pinger(id: Int?): Builder {
	private          val _id = id;
	private lateinit var _next: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.PINGER;
		_next.build(msg);
	}

	fun create(target: String) {
		create(target) { }
	}

	fun create(target: String, body: Create.() -> Unit) {
		val next = Create(target);
		next.body();

		_next = next;
	}

	fun start() {
		_next = Start(_id!!);
	}

	fun stop() {
		_next = Stop(_id!!);
	}

	fun subscribe() {
		_next = Subscribe(_id!!);
	}

	fun unsubscribe() {
		_next = Unsubscribe(_id!!);
	}

	fun destroy() {
		_next = Destroy(_id!!);
	}

	class Create(target: String): As(Command.Pinger.CREATE) {
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

	class Destroy(id: Int): WithId(id, Command.Pinger.DESTROY);

	class Start(id: Int): WithId(id, Command.Pinger.START);
	class Stop(id: Int): WithId(id, Command.Pinger.STOP);

	class Subscribe(id: Int): WithId(id, Command.Pinger.SUBSCRIBE);
	class Unsubscribe(id: Int): WithId(id, Command.Pinger.UNSUBSCRIBE);
}
