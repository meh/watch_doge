package meh.watchdoge.util;

import android.os.Messenger;
import android.os.Message;
import android.os.Bundle;

import org.msgpack.value.Value;
import org.msgpack.value.ValueType;
import org.msgpack.core.MessageTypeException;

import meh.watchdoge.Request;
import meh.watchdoge.request.Request as RequestBuilder;
import meh.watchdoge.request.build as buildRequest;

import meh.watchdoge.Response;
import meh.watchdoge.response.Response as ResponseBuilder;
import meh.watchdoge.response.Control;
import meh.watchdoge.response.build as buildResponse;

inline fun<T: Any, R> T.tap(tap: (T) -> R): T {
  tap(this)
  return this
}

fun Message.isRequest(): Boolean {
	return this.what == 0xBADB01 && this.replyTo != null;
}

fun Message.isResponse(): Boolean {
	return origin() == 0xBADB01 && replyTo == null
}

fun Message.origin(): Int {
	return what
}

fun Message.family(): Int {
	return (arg1) and 0xff
}

fun Message.command(): Int {
	return (arg1 shr 8) and 0xff
}

fun Message.status(): Int {
	return (arg1 shr 16) and 0xff
}

fun Message.intoResponse(): Response {
	return Response(family(), command(), status(), arg2, peekData());
}

fun Message.intoRequest(id: Int = 0): Request {
	return Request(id, family(), command(), arg2, peekData(), replyTo!!);
}

infix fun Messenger.to(other: Messenger): Pair<Messenger, Messenger> {
	return Pair(this, other);
}

fun Pair<Messenger, Messenger>.request(body: RequestBuilder.() -> Unit) {
	this.first.send(buildRequest(body).tap { it.replyTo = this.second });
}

fun Messenger.response(request: Request, result: Int, body: (Control.() -> Unit)? = null) {
	if (request.id() == 0) {
		return;
	}

	this.send(buildResponse {
		control {
			family  = request.family()
			command = request.command()
			status  = result

			if (body != null) {
				this.body();
			}
		}
	})
}

fun Bundle.putValue(key: String, value: Value) {
	when {
		value.isNilValue() ->
			this.putParcelable(key, null)

		value.isBooleanValue() ->
			this.putBoolean(key, value.asBooleanValue().getBoolean())

		value.isIntegerValue() ->
			when {
				value.asIntegerValue().isInByteRange() ->
					this.putByte(key, value.asIntegerValue().asByte())

				value.asIntegerValue().isInShortRange() ->
					this.putShort(key, value.asIntegerValue().asShort())

				value.asIntegerValue().isInIntRange() ->
					this.putInt(key, value.asIntegerValue().asInt())

				value.asIntegerValue().isInLongRange() ->
					this.putLong(key, value.asIntegerValue().asLong())
			}

		value.isFloatValue() ->
			this.putDouble(key, value.asNumberValue().toDouble())

		value.isStringValue() ->
			this.putString(key, value.asStringValue().asString())

		value.isBinaryValue() ->
			this.putByteArray(key, value.asBinaryValue().asByteArray())

		value.isArrayValue() -> {
			val values = value.asArrayValue();

			when {
				values.size() == 0 ->
					this.putParcelable(key, null);

				values[0].isIntegerValue() -> {

				}

				values[0].isIntegerValue() && values[0].asIntegerValue().isInByteRange() ->
					this.putByteArray(key, ByteArray(values.size()) {
						values[it].asIntegerValue().asByte()
					})

				values[0].isIntegerValue() && values[0].asIntegerValue().isInShortRange() ->
					this.putShortArray(key, ShortArray(values.size()) {
						values[it].asIntegerValue().asShort()
					})

				values[0].isIntegerValue() && values[0].asIntegerValue().isInIntRange() ->
					this.putIntArray(key, IntArray(values.size()) {
						values[it].asIntegerValue().asInt()
					})

				values[0].isIntegerValue() && values[0].asIntegerValue().isInLongRange() ->
					this.putLongArray(key, LongArray(values.size()) {
						values[it].asIntegerValue().asLong()
					})

				values[0].isFloatValue() ->
					this.putDoubleArray(key, DoubleArray(values.size()) {
						values[it].asFloatValue().toDouble()
					})

				values[0].isStringValue() ->
					this.putStringArray(key, Array(values.size()) {
						values[it].asStringValue().asString()
					})

				else ->
					throw MessageTypeException("unknown conversion")
			}
		}

		value.isMapValue() -> {
			this.putParcelable(key, Bundle().tap {
				for ((k, v) in value.asMapValue().map()) {
					it.putValue(k.asStringValue().asString(), v);
				}
			})
		}

		else ->
			throw MessageTypeException("unknown conversion")
	}
}

fun String.toDuration(): Double {
	val match = Regex("""(\d(?:\.\d+)?)(s|ms|us)?""").find(this.trim().toLowerCase());

	if (match == null) {
		return 0.0;
	}

	val value = match.groups.get(1)!!.value.toDouble();
	val unit  = match.groups.get(2);

	return when (unit?.value) {
		"s"  -> value
		"ms" -> value * 1000.0
		"us" -> value * 1000000.0
		else -> value
	}
}
