package meh.watchdoge.request;

import meh.watchdoge.backend.Command as C;
import android.os.Message;

class Sniffer(id: Int?): Family(C.SNIFFER) {
	private val _id = id;

	fun create() {
		create() { }
	}

	fun create(body: Create.() -> Unit) {
		val next = Create();
		next.body();

		command = next;
	}

	fun start() {
		command = Start(_id!!);
	}

	fun filter(filter: String?) {
		command = Filter(_id!!, filter);
	}

	fun subscribe() {
		command = Subscribe(_id!!);
	}

	fun unsubscribe() {
		command = Unsubscribe(_id!!);
	}

	fun list() {
		command = List();
	}

	class Create(): Command(C.Sniffer.CREATE) {
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

	class Start(id: Int): WithId(id, C.Sniffer.START);
	class Stop(id: Int): WithId(id, C.Sniffer.STOP);

	class Filter(id: Int, filter: String?): WithId(id, C.Sniffer.FILTER) {
		val filter = filter;

		override fun build(msg: Message) {
			super.build(msg);

			if (filter != null) {
				msg.getData().putString("filter", filter);
			}
		}
	}

	class List(): Command(C.Sniffer.LIST);
	class Subscribe(id: Int): WithId(id, C.Sniffer.SUBSCRIBE);
	class Unsubscribe(id: Int): WithId(id, C.Sniffer.UNSUBSCRIBE);
}
