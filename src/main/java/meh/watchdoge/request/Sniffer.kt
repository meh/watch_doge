package meh.watchdoge.request;

import meh.watchdoge.Command;
import android.os.Message;

class Sniffer(var id: Int?): Builder {
	lateinit private var sub: Builder;

	override fun build(msg: Message) {
		msg.arg1 = Command.SNIFFER;
		sub.build(msg);
	}

	fun create() {
		sub = Create();
	}

	fun create(ip: String) {
		sub = Create(ip);
	}

	fun start() {
		sub = Start(id!!);
	}

	fun filter(filter: String?) {
		sub = Filter(id!!, filter);
	}

	class Create(ip: String? = null): Builder {
		private var ip = ip;

		override fun build(msg: Message) {
			msg.arg2 = Command.Sniffer.CREATE;

			if (ip != null) {
				msg.getData().putString("ip", ip);
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
}


