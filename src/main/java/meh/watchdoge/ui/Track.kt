package meh.watchdoge.ui;

import meh.watchdoge.R;
import meh.watchdoge.ui.util.*;
import meh.watchdoge.util.*;
import org.jetbrains.anko.*;
import nl.komponents.kovenant.*;
import nl.komponents.kovenant.ui.*;
import nl.komponents.kovenant.functional.*;

import android.os.Bundle;
import meh.watchdoge.backend.Connection;
import meh.watchdoge.backend.Module.Connection.ISubscription;
import meh.watchdoge.backend.Pinger;
import meh.watchdoge.backend.Command;
import meh.watchdoge.Response;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;

class Track(): ProgressFragment(R.layout.track) {
	override fun load(view: View, bundle: Bundle?) {
		val ping = Ping(ctx).tap {
			it.appendTo(view.find<ViewGroup>(R.id.ping));
		};

		val traceroute = Traceroute(ctx).tap {
			it.appendTo(view.find<ViewGroup>(R.id.traceroute));
		};

		backend() success { conn ->
			conn.request { control { root() } } successUi {
				if (it.bundle()!!.getBoolean("status")) {
					view.find<View>(R.id.supported).setVisibility(View.VISIBLE);
				}
				else {
					view.find<View>(R.id.unsupported).setVisibility(View.VISIBLE);
				}
			} bind {
				if (it.bundle()!!.getBoolean("status")) {
					Promise.of(true)
				}
				else {
					Promise.ofFail(RuntimeException("not rooted"))
				}
			} then {
				ping.connect(conn);
			} always {
				show()
			}
		}
	}

	class Ping(context: Context): Component(context, R.layout.track_ping) {
		companion object {
			fun trip(time: Long): String {
				return when {
					time > 1000000 ->
						"%ds".format(time / 100000)

					time > 1000 ->
						"%dms".format(time / 1000)

					else ->
						"%dμs".format(time)
				};
			}
		}

		fun colorForLoss(loss: Float): Int {
			return when {
				loss > 60 ->
					colorFor(R.color.failure);

				loss > 30 ->
					colorFor(R.color.warning);

				else ->
					colorFor(R.color.success);
			}
		}

		fun colorForTrip(trip: Long): Int {
			return when {
				trip > 300000 ->
					colorFor(R.color.failure);

				trip > 150000 ->
					colorFor(R.color.warning);

				else ->
					colorFor(R.color.success);
			}
		}

		private var _id                         = 0;
		private var _subscriber: ISubscription? = null;
		private var _error                      = 0;

		fun connect(conn: Connection) {
			onStart {
				start();

				if (_id == 0) {
					conn.request { pinger { create(target()) } } bind {
						_id = it.bundle()!!.getInt("id");

						conn.subscribe {
							pinger(_id) {
								promiseOnUi {
									update(it as Pinger.Event)
								}
							}
						}
					} then {
						_subscriber = it;
					}
				}
				else {
					Promise.of(0)
				} bind {
					conn.request { pinger(_id) { start() } }
				} successUi {
					ok()
				} failUi {
					error(it)
				}
			}

			onStop {
				conn.request { pinger(_id) { stop() } } successUi {
					stop()
				} failUi {
					error(it)
				}
			}

			onClear {
				_subscriber?.unsubscribe();

				if (_id != 0) {
					conn.request { pinger(_id) { destroy() } }
				}
				else {
					Promise.of(0)
				} then {
					_id = 0
				} successUi {
					clear()
				} failUi {
					error(it)
				}
			}
		}

		fun onStart(block: Ping.() -> Unit) {
			view().find<Button>(R.id.start).onClick {
				this.block();
			}

			view().find<EditText>(R.id.target).onEditorAction { view, id, event ->
				if (id == EditorInfo.IME_ACTION_SEARCH) {
					this.block();
					true
				}
				else {
					false
				}
			}
		}

		fun onStop(block: Ping.() -> Unit) {
			view().find<Button>(R.id.stop).onClick {
				this.block();
			}
		}

		fun onClear(block: Ping.() -> Unit) {
			view().find<Button>(R.id.clear).onClick {
				this.block();
			}
		}

		fun target(): String {
			return view().find<EditText>(R.id.target).getText().toString().trim();
		}

		fun start() {
			// edit
			view().find<EditText>(R.id.target).disable();

			// button
			view().find<View>(R.id.start).setVisibility(View.GONE);
			view().find<View>(R.id.stop).setVisibility(View.VISIBLE);

			// error
			view().find<View>(R.id.error).setVisibility(View.GONE);

			// progress
			view().find<View>(R.id.progress).setVisibility(View.VISIBLE);

			// container
			view().find<View>(R.id.active).setVisibility(View.VISIBLE);
		}

		fun error(err: Exception) {
			var text = "Something happened :(";

			when (err) {
				is Response.Exception -> when {
					err.family() == Command.PINGER &&
					err.status() == Command.Pinger.Error.UNKNOWN_HOST ->
						text = "Unknown host."

					err.family() == Command.PINGER &&
					err.status() == Command.Pinger.Error.SOCKET ->
						text = "Socket error."
				}
			}

			error(text);
		}

		fun error(text: String) {
			_id = 0;

			// button
			view().find<View>(R.id.start).setVisibility(View.GONE);
			view().find<View>(R.id.stop).setVisibility(View.GONE);
			view().find<View>(R.id.clear).setVisibility(View.VISIBLE);

			// content
			view().find<View>(R.id.ok).setVisibility(View.GONE);

			// progress
			view().find<View>(R.id.progress).setVisibility(View.GONE);

			// error
			view().find<TextView>(R.id.error).tap {
				it.setText(text);
				it.setVisibility(View.VISIBLE);
			}

			// container
			view().find<View>(R.id.active).setVisibility(View.VISIBLE);
		}

		fun ok() {
			// error
			view().find<View>(R.id.error).setVisibility(View.GONE);

			// content
			view().find<View>(R.id.ok).setVisibility(View.VISIBLE);
		}

		fun stop() {
			// progress
			view().find<View>(R.id.progress).setVisibility(View.GONE);

			// button
			view().find<View>(R.id.stop).setVisibility(View.GONE);
			view().find<View>(R.id.start).setVisibility(View.GONE);
			view().find<View>(R.id.clear).setVisibility(View.VISIBLE);
		}

		fun clear() {
			// edit
			view().find<EditText>(R.id.target).setText("");
			view().find<EditText>(R.id.target).enable();

			// packets
			view().find<TextView>(R.id.packet_sent).setText("0");
			view().find<TextView>(R.id.packet_received).setText("0");
			view().find<TextView>(R.id.packet_loss).setText("0%");

			// trip
			view().find<View>(R.id.trip_stats).setVisibility(View.GONE);
			view().find<TextView>(R.id.trip_minimum).setText("0ms");
			view().find<TextView>(R.id.trip_maximum).setText("0ms");
			view().find<TextView>(R.id.trip_average).setText("0ms");

			// button
			view().find<View>(R.id.start).setVisibility(View.VISIBLE);
			view().find<View>(R.id.stop).setVisibility(View.GONE);
			view().find<View>(R.id.clear).setVisibility(View.GONE);

			// progress
			view().find<View>(R.id.progress).setVisibility(View.GONE);

			// error
			view().find<View>(R.id.error).setVisibility(View.GONE);

			// container
			view().find<View>(R.id.active).setVisibility(View.GONE);

			// entries
			view().find<TableLayout>(R.id.entries_table).tap {
				it.removeViews(2, 4);

				for (i in 0 .. 3) {
					it.setColumnCollapsed(i, false);
				}
			}

			view().find<View>(R.id.entries).setVisibility(View.GONE);
		}

		fun update(event: Pinger.Event) {
			when (event) {
				is Pinger.Stats -> {
					event.packet().tap { packet ->
						view().find<TextView>(R.id.packet_sent).setText("${packet.sent}");
						view().find<TextView>(R.id.packet_received).setText("${packet.received}");

						view().find<TextView>(R.id.packet_loss).tap {
							it.setText("${Math.round(packet.loss)}%");
							it.setBackgroundColor(colorForLoss(packet.loss));
						}
					}

					event.trip().tap { trip ->
						if (trip.maximum != 0L) {
							view().find<View>(R.id.trip_stats).setVisibility(View.VISIBLE);

							view().find<TextView>(R.id.trip_minimum).setText(Ping.trip(trip.minimum));
							view().find<TextView>(R.id.trip_maximum).setText(Ping.trip(trip.maximum));

							view().find<TextView>(R.id.trip_average).tap {
								it.setText(Ping.trip(trip.average));
								it.setBackgroundColor(colorForTrip(trip.average));
							}
						}
					}
				}

				is Pinger.Packet ->
					entry(event)

				is Pinger.Error ->
					entry(event)
			}
		}

		inner class Entry: Component {
			constructor(context: Context)
				: super(context, R.layout.track_ping_entry);

			constructor(context: Context, view: View)
				: super(context, view);

			fun update(event: Pinger.Entry) {
				view().find<TextView>(R.id.source).setText(event.source());
				view().find<TextView>(R.id.ttl).setText("${event.ttl()}");
				view().find<TextView>(R.id.sequence).setText("${event.sequence()}");

				when (event) {
					is Pinger.Packet ->
						view().find<TextView>(R.id.status).tap {
							it.setText("${Ping.trip(event.trip())}");
							it.setBackgroundColor(colorForTrip(event.trip()));
						}

					is Pinger.Error ->
						view().find<TextView>(R.id.status).tap {
							it.setText("${event.reason()}");
							it.setBackgroundColor(colorFor(R.color.failure));
						}
				}
			}
		}

		fun entry(event: Pinger.Entry) {
			val container = view().find<TableLayout>(R.id.entries_table);
			var entry: Entry;

			view().find<View>(R.id.entries).setVisibility(View.VISIBLE);

			if (container.getChildCount() < 6) {
				entry = Entry(this);
			}
			else {
				entry = Entry(this, container.getChildAt(5));
				container.removeViewAt(5);
			}

			entry.update(event);
			entry.addTo(container, 2);

			if (event is Pinger.Error) {
				_error = 4;

				for (i in 0 .. 3) {
					container.setColumnCollapsed(i, true);
				}
			}

			if (_error == 0) {
				for (i in 0 .. 3) {
					container.setColumnCollapsed(i, false);
				}
			}

			_error -= 1;
		}
	}

	class Traceroute(context: Context): Component(context, R.layout.track_traceroute) {

	}
}
