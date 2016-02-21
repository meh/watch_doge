package meh.watchdoge.ui;

import meh.watchdoge.R;
import meh.watchdoge.util.*;
import org.jetbrains.anko.*;
import meh.watchdoge.ui.util.*;
import nl.komponents.kovenant.*;
import nl.komponents.kovenant.ui.*;

import android.os.Bundle;
import meh.watchdoge.backend.Connection;
import meh.watchdoge.Response;
import meh.watchdoge.backend.Wireless;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

class Home(): ProgressFragment(R.layout.home) {
	override fun load(view: View, bundle: Bundle?) {
		backend() success { conn ->
			conn.request { root() } successUi {
				root(view, it.bundle())
			} then { conn.request { wireless { status() } } successUi {
				wireless(view, Wireless.Status(it.bundle()))
			} } always {
				show()
			}

			conn.subscribe {
				wireless {
					when (it) {
						is Wireless.Status -> promiseOnUi {
							wireless(view, it)
						}
					}
				}
			}
		}
	}

	private fun root(view: View, res: Bundle) {
		if (res.getBoolean("status")) {
			view.find<TextView>(R.id.whoami).tap {
				it.setBackgroundColor(colorFor(R.color.success));
				it.setText("root");
			}
		}
		else {
			view.find<TextView>(R.id.whoami).tap {
				it.setBackgroundColor(colorFor(R.color.failure));
				it.setText("pleb");
			}
		}
	}

	private fun wireless(view: View, status: Wireless.Status) {
		val state = status.bundle().getString("state")!!;

		if (state == "connected") {
			val client   = status.bundle().getParcelable<Bundle>("client")!!;
			val router   = status.bundle().getParcelable<Bundle>("router")!!;
			val netmask  = status.bundle().getString("netmask");
			val strength = status.bundle().getInt("strength");

			// mark success
			view.find<TextView>(R.id.wireless_header).tap {
				it.setBackgroundColor(colorFor(R.color.success));
				it.setText(router.getString("ssid").removeSuffix("\"").removePrefix("\""));
			}

			// signal strength
			view.find<TextView>(R.id.wireless_rssi).tap {
				it.setText("${strength}%");
			}

			// client
			view.find<TextView>(R.id.wireless_client_mac).tap {
				it.setText(client.getString("mac"));
			}

			view.find<TextView>(R.id.wireless_client_ip).tap {
				it.setText(client.getString("ip"));
			}

			// router
			view.find<TextView>(R.id.wireless_router_mac).tap {
				it.setText(router.getString("mac"));
			}

			view.find<TextView>(R.id.wireless_router_ip).tap {
				it.setText(router.getString("ip"));
			}

			// netmask
			if (netmask != null) {
				view.find<TextView>(R.id.wireless_netmask).tap {
					it.setText(netmask);
					it.setVisibility(View.VISIBLE);
				}
			}
			else {
				view.find<TextView>(R.id.wireless_netmask).tap {
					it.setVisibility(View.GONE);
				}
			}

			// show
			view.find<View>(R.id.wireless_active).tap {
				it.setVisibility(View.VISIBLE);
			}

			view.find<TextView>(R.id.wireless_inactive).tap {
				it.setVisibility(View.GONE);
			}
		}
		else {
			// mark failure
			view.find<TextView>(R.id.wireless_header).tap {
				it.setBackgroundColor(colorFor(R.color.failure));
			}

			// show
			view.find<TextView>(R.id.wireless_inactive).tap {
				it.setText(state.capitalize());
				it.setVisibility(View.VISIBLE);
			}

			view.find<View>(R.id.wireless_active).tap {
				it.setVisibility(View.GONE);
			}
		}
	}
}
