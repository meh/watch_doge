package meh.watchdoge;

import android.os.Bundle;

data class Request(val id: Int, val family: Int, val command: Int, val details: Bundle) {
	fun matches(family: Int, command: Int): Boolean {
		return this.family == family && this.command == command;
	}
}
