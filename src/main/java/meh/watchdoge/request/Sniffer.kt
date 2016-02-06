package meh.watchdoge.request;

import meh.watchdoge.backend.Command;
import android.os.Message;

class Sniffer(var id: Int?): Builder {
	lateinit private var sub: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.SNIFFER;
		sub.build(msg);
	}

	fun create() {
		sub = Create(null, 0);
	}

	fun create(body: Create.() -> Unit) {
		val next = Create(null, 0);
		next.body();

		sub = next;
	}

	fun start() {
		sub = Start(id!!);
	}

	fun filter(filter: String?) {
		sub = Filter(id!!, filter);
	}

	fun subscribe() {
		sub = Subscribe(id!!);
	}

	class Create(ip: String?, truncate: Int): Builder {
		private var ip       = ip;
		private var truncate = truncate;

		override fun build(msg: Message) {
			msg.arg2 = Command.Sniffer.CREATE;

			if (ip != null) {
				msg.getData().putString("ip", ip);
			}

			if (truncate != 0) {
				msg.getData().putInt("truncate", truncate);
			}
		}
	}

	class Start(id: Int): Builder {
		private val id = id;

		override fun build(msg: Message) {
			msg.arg2 = Command.Sniffer.START;
			msg.getData().putInt("id", id);
		}
	}

	class Filter(id: Int, filter: String?): Builder {
		private val id     = id;
		private val filter = filter;

		override fun build(msg: Message) {
			msg.arg2 = Command.Sniffer.FILTER;

			msg.getData().putInt("id", id);

			if (filter != null) {
				msg.getData().putString("filter", filter);
			}
		}
	}

	class Subscribe(id: Int): Builder {
		private val id = id;

		override fun build(msg: Message) {
			msg.arg2 = Command.Sniffer.SUBSCRIBE;
			msg.getData().putInt("id", id);
		}
	}
}
