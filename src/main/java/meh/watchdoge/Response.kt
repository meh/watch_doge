package meh.watchdoge;

import android.os.Bundle;

data class Response(val status: Int, val family: Int, val command: Int, val request: Bundle, val details: Bundle) {
	fun matches(family: Int, command: Int): Boolean {
		return this.family == family && this.command == command;
	}
}
