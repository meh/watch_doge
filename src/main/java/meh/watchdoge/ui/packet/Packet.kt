package meh.watchdoge.ui.packet;

import meh.watchdoge.util.*;

import android.os.Bundle;
import org.jetbrains.anko.*;

import java.util.Date;

class Packet<T>(packet: Bundle) : AnkoComponent<T> {
	private val packet = packet;

	override fun createView(ui: AnkoContext<T>) = with(ui) {
		verticalLayout {
			padding = dip(5);

			textView("Packet Size: ${this@Packet.size}") {
				textSize = 10f
			}
		}
	}

	private val size: Int
		get() = packet.get("size") as Int

	private val timestamp: Date
		get() {
			val secs  = packet.get("secs") as Long;
			val msecs = packet.get("msecs") as Long;

			return Date(secs + (msecs / 1000));
		}
}
