package meh.watchdoge.backend;

import meh.watchdoge.Request;
import meh.watchdoge.request.Request as RequestBuilder;
import meh.watchdoge.Response;
import meh.watchdoge.response.Control;

import java.util.HashMap;
import java.util.HashSet;

import nl.komponents.kovenant.*;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.content.ContextWrapper;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

abstract class Module(backend: Backend): ContextWrapper(backend.getApplicationContext()) {
	protected val _backend:  Backend;
	protected val _unpacker: MessageUnpacker;

	init {
		_backend  = backend;
		_unpacker = backend.unpacker();
	}

	fun response(req: Request, status: Int, body: (Control.() -> Unit)? = null): Int {
		return _backend.response(req, status, body);
	}

	fun forward(req: Request, body: (MessagePacker) -> Unit): Int {
		return _backend.forward(req, body);
	}

	abstract fun receive();
	abstract fun response(messenger: Messenger, req: Request, status: Int);
	abstract fun request(req: Request): Boolean;

	abstract class Connection(conn: meh.watchdoge.backend.Connection) {
		protected val _connection = conn;

		fun request(body: RequestBuilder.() -> Unit): Promise<Response, Response.Exception> {
			return _connection.request(body);
		}

		abstract fun handle(msg: Message): Boolean;

		interface ISubscription {
			fun unsubscribe();
		}

		abstract class Subscription<T: IEvent>(body: (T) -> Unit): ISubscription {
			protected val _body = body;

			fun unsubscribe(sub: ISubscriber<T>) {
				sub.with {
					it.remove(_body);
				}
			}
		}

		abstract class SubscriptionWithId<T: IEventWithId>(id: Int, body: (T) -> Unit): ISubscription {
			protected val _id   = id;
			protected val _body = body;

			fun unsubscribe(sub: ISubscriberWithId<T>) {
				sub.with {
					it.get(_id)?.remove(_body);
				}
			}
		}

		interface IEmitter<T: IEvent> {
			fun emit(event: T);
		}

		interface ISubscriberWithId<T: IEventWithId>: IEmitter<T> {
			fun with(body: (HashMap<Int, HashSet<(T) -> Unit>>) -> Unit);
			fun empty(id: Int): Boolean;
			fun subscribe(id: Int, body: (T) -> Unit);
		}

		interface ISubscriber<T: IEvent>: IEmitter<T> {
			fun with(body: (HashSet<(T) -> Unit>) -> Unit);
			fun empty(): Boolean;
			fun subscribe(body: (T) -> Unit);
		}

		open class Subscriber<T: IEvent>: ISubscriber<T> {
			protected val _set: HashSet<(T) -> Unit> = HashSet();

			override fun with(body: (HashSet<(T) -> Unit>) -> Unit) {
				synchronized(_set) {
					body(_set);
				}
			}

			override fun empty(): Boolean {
				synchronized(_set) {
					return _set.isEmpty();
				}
			}

			override fun subscribe(body: (T) -> Unit) {
				synchronized(_set) {
					_set.add(body);
				}
			}

			override fun emit(event: T) {
				synchronized(_set) {
					for (sub in _set) {
						sub(event);
					}
				}
			}
		}

		open class SubscriberWithId<T: IEventWithId>: ISubscriberWithId<T> {
			protected val _map: HashMap<Int, HashSet<(T) -> Unit>> = HashMap();

			override fun with(body: (HashMap<Int, HashSet<(T) -> Unit>>) -> Unit) {
				synchronized(_map) {
					body(_map);
				}
			}

			override fun empty(id: Int): Boolean {
				synchronized(_map) {
					if (!_map.containsKey(id)) {
						return true;
					}

					return _map.get(id)!!.isEmpty();
				}
			}

			override fun subscribe(id: Int, body: (T) -> Unit) {
				synchronized(_map) {
					if (!_map.containsKey(id)) {
						_map.put(id, HashSet());
					}

					_map.get(id)!!.add(body);
				}
			}

			override fun emit(event: T) {
				synchronized(_map) {
					val subs = _map.get(event.id());

					if (subs != null) {
						for (sub in subs) {
							sub(event)
						}
					}
				}
			}
		}
	}

	interface IEvent {
		fun bundle(): Bundle;
	}

	interface IEventWithId: IEvent {
		fun id(): Int;
	}

	open class Event(bundle: Bundle): IEvent {
		protected val _bundle = bundle;

		override fun bundle(): Bundle {
			return _bundle;
		}
	}

	open class EventWithId(id: Int, bundle: Bundle): Event(bundle), IEventWithId {
		protected val _id = id;

		override fun id(): Int {
			return _id;
		}
	}
}
