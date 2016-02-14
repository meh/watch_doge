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
import meh.watchdoge.backend.Pinger;
import meh.watchdoge.backend.Command;
import meh.watchdoge.Response;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

class Track(): ProgressFragment(R.layout.track) {
	override fun load(view: View, bundle: Bundle?) {
		val ping = Ping(getContext()).tap {
			it.addTo(view.find<ViewGroup>(R.id.ping));
		};

		val traceroute = Traceroute(getContext()).tap {
			it.addTo(view.find<ViewGroup>(R.id.traceroute));
		};

		backend() success { conn ->
			conn.request { root() } successUi {
				if (it.bundle().getBoolean("status")) {
					view.find<View>(R.id.supported).setVisibility(View.VISIBLE);
				}
				else {
					view.find<View>(R.id.unsupported).setVisibility(View.VISIBLE);
				}
			} bind {
				if (it.bundle().getBoolean("status")) {
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
		private var id                                 = 0;
		private var subscriber: Connection.Subscriber? = null;

		fun connect(conn: Connection) {
			onStart {
				start();

				if (id == 0) {
					conn.request { pinger { create(target()) } } bind {
						id = it.bundle().getInt("id");

						conn.subscribe {
							pinger(id) {
								promiseOnUi {
									update(it)
								}
							}
						}
					} then {
						subscriber = it;
					}
				}
				else {
					Promise.of(0)
				} bind {
					conn.request { pinger(id) { start() } }
				} successUi {
					ok()
				} failUi {
					error(it)
				}
			}

			onStop {
				conn.request { pinger(id) { stop() } } successUi {
					stop()
				} failUi {
					error(it)
				}
			}

			onClear {
				subscriber?.unsubscribe();

				if (id != 0) {
					conn.request { pinger(id) { destroy() } }
				}
				else {
					Promise.of(0)
				} then {
					id = 0
				} successUi {
					clear()
				} failUi {
					error(it)
				}
			}
		}

		fun onStart(block: Ping.() -> Unit) {
			view.find<Button>(R.id.start).onClick {
				this.block();
			}

			view.find<EditText>(R.id.target).onEditorAction { view, id, event ->
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
			view.find<Button>(R.id.stop).onClick {
				this.block();
			}
		}

		fun onClear(block: Ping.() -> Unit) {
			view.find<Button>(R.id.clear).onClick {
				this.block();
			}
		}

		fun target(): String {
			return view.find<EditText>(R.id.target).getText().toString().trim();
		}

		fun update(event: Pinger.Event) {
			android.util.Log.d("UI", "PINGER/UPDATE: ${event}");
		}

		fun start() {
			// edit
			view.find<EditText>(R.id.target).disable();

			// button
			view.find<View>(R.id.start).setVisibility(View.GONE);
			view.find<View>(R.id.stop).setVisibility(View.VISIBLE);

			// error
			view.find<View>(R.id.error).setVisibility(View.GONE);

			// progress
			view.find<View>(R.id.progress).setVisibility(View.VISIBLE);

			// container
			view.find<View>(R.id.active).setVisibility(View.VISIBLE);
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
			id = 0;

			// button
			view.find<View>(R.id.start).setVisibility(View.GONE);
			view.find<View>(R.id.stop).setVisibility(View.GONE);
			view.find<View>(R.id.clear).setVisibility(View.VISIBLE);

			// progress
			view.find<View>(R.id.progress).setVisibility(View.GONE);

			// error
			view.find<TextView>(R.id.error).tap {
				it.setText(text);
				it.setVisibility(View.VISIBLE);
			}

			// container
			view.find<View>(R.id.active).setVisibility(View.VISIBLE);
		}

		fun ok() {
			// error
			view.find<View>(R.id.error).setVisibility(View.GONE);

			// content
			view.find<View>(R.id.ok).setVisibility(View.VISIBLE);
		}

		fun stop() {
			// progress
			view.find<View>(R.id.progress).setVisibility(View.GONE);

			// button
			view.find<View>(R.id.stop).setVisibility(View.GONE);
			view.find<View>(R.id.start).setVisibility(View.GONE);
			view.find<View>(R.id.clear).setVisibility(View.VISIBLE);
		}

		fun clear() {
			// edit
			view.find<EditText>(R.id.target).setText("");
			view.find<EditText>(R.id.target).enable();

			// packets
			view.find<TextView>(R.id.packet_sent).setText("0");
			view.find<TextView>(R.id.packet_received).setText("0");
			view.find<TextView>(R.id.packet_loss).setText("0%");

			// time
			view.find<TextView>(R.id.time_minimum).setText("0ms");
			view.find<TextView>(R.id.time_maximum).setText("0ms");
			view.find<TextView>(R.id.time_average).setText("0ms");

			// button
			view.find<View>(R.id.start).setVisibility(View.VISIBLE);
			view.find<View>(R.id.stop).setVisibility(View.GONE);
			view.find<View>(R.id.clear).setVisibility(View.GONE);

			// progress
			view.find<View>(R.id.progress).setVisibility(View.GONE);

			// error
			view.find<View>(R.id.error).setVisibility(View.GONE);

			// container
			view.find<View>(R.id.active).setVisibility(View.GONE);
		}
	}

	class Traceroute(context: Context): Component(context, R.layout.track_traceroute) {

	}
}
