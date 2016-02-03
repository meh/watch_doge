package meh.watchdoge;

import android.os.Bundle;

data class Response(val status: Int, val family: Int, val command: Int, val request: Bundle, val details: Bundle) {
	fun isSuccess(): Boolean = status == Command.SUCCESS;
	fun isFailure(): Boolean = status != Command.SUCCESS;

	fun matches(family: Int, command: Int): Boolean {
		return this.family == family && this.command == command;
	}

	fun exception(): Exception? {
		return if (isFailure()) {
			Exception(this)
		}
		else {
			null
		}
	}

	class Exception(res: Response) : java.lang.Exception() {
		private val response = res;

		val code: Int
			get() = response.status;

		val family: Int
			get() = response.family

		val command: Int
			get() = response.command

		val details: Bundle
			get() = response.details
	}
}
