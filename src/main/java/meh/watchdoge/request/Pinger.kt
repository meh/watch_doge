package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Pinger(var id: Int?): Builder {
	lateinit private var sub: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.PINGER;
		sub.build(msg);
	}

	fun create(target: String) {
		create(target) { }
	}

	fun create(target: String, body: Create.() -> Unit) {
		val next = Create(target);
		next.body();

		sub = next;
	}

	fun start() {
		sub = Start(id!!);
	}

	fun stop() {
		sub = Stop(id!!);
	}

	fun subscribe() {
		sub = Subscribe(id!!);
	}

	fun unsubscribe() {
		sub = Unsubscribe(id!!);
	}

	fun destroy() {
		sub = Destroy(id!!);
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
