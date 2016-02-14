package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Sniffer(id: Int?): Builder {
	private          val _id = id;
	private lateinit var _next: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.SNIFFER;
		_next.build(msg);
	}

	fun create() {
		create() { }
	}

	fun create(body: Create.() -> Unit) {
		val next = Create();
		next.body();

		_next = next;
	}

	fun start() {
		_next = Start(_id!!);
	}

	fun filter(filter: String?) {
		_next = Filter(_id!!, filter);
	}

	fun subscribe() {
		_next = Subscribe(_id!!);
	}

	fun unsubscribe() {
		_next = Unsubscribe(_id!!);
	}

	fun list() {
		_next = List();
	}

	class Create(): As(Command.Sniffer.CREATE) {
		var ip:       String? = null;
		var truncate: Int     = 0;

		override fun build(msg: Message) {
			super.build(msg);

			if (ip != null) {
				msg.getData().putString("ip", ip);
			}

			if (truncate != 0) {
				msg.getData().putInt("truncate", truncate);
			}
		}
	}

	class Start(id: Int): WithId(id, Command.Sniffer.START);
	class Stop(id: Int): WithId(id, Command.Sniffer.STOP);

	class Filter(id: Int, filter: String?): WithId(id, Command.Sniffer.FILTER) {
		val filter = filter;

		override fun build(msg: Message) {
			super.build(msg);

			if (filter != null) {
				msg.getData().putString("filter", filter);
			}
		}
	}

	class Subscribe(id: Int): WithId(id, Command.Sniffer.SUBSCRIBE);
	class Unsubscribe(id: Int): WithId(id, Command.Sniffer.UNSUBSCRIBE);

	class List(): Builder {
		override fun build(msg: Message) {
			msg.arg2 = Command.Sniffer.LIST;
		}
	}
}
