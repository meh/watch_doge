package meh.watchdoge.util;

import android.util.Log;

import android.os.Messenger;
import android.os.Message;
import android.os.Bundle;

import org.msgpack.value.Value;
import org.msgpack.value.ValueType;
import org.msgpack.core.MessageTypeException;

import meh.watchdoge.request.Request;
import meh.watchdoge.request.build;

inline fun<T: Any, R> T.tap(tap: (T) -> R): T {
  tap(this)
  return this
}

infix fun Messenger.to(other: Messenger): Pair<Messenger, Messenger> {
	return Pair(this, other);
}

fun Pair<Messenger, Messenger>.request(body: Request.() -> Unit) {
	this.first.send(build(body).tap { it.replyTo = this.second });
}

fun Messenger.response(request: meh.watchdoge.Request, status: Int, body: ((Bundle) -> Unit)? = null) {
	if (request.id == 0) {
		return;
	}

	var msg = Message.obtain();

	msg.what = status;
	msg.arg1 = request.family;
	msg.arg2 = request.command;
	msg.obj  = request.details;

	if (body != null) {
		body(msg.getData());
	}

	this.send(msg);
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
