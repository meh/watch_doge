package meh.watchdoge.ui;

import meh.watchdoge.R;
import org.jetbrains.anko.*;
import meh.watchdoge.ui.util.*;
import nl.komponents.kovenant.*;
import nl.komponents.kovenant.ui.*;

import android.os.Bundle;
import meh.watchdoge.backend.Connection;

import android.view.LayoutInflater;
import android.view.View;

class Sniff: ProgressFragment(R.layout.sniff) {
	override fun load(view: View, bundle: Bundle?) {
		backend() success { conn ->
			conn.request { control { root() } } successUi {
				if (it.bundle()!!.getBoolean("status")) {
					view.find<View>(R.id.supported).setVisibility(View.VISIBLE);
				}
				else {
					view.find<View>(R.id.unsupported).setVisibility(View.VISIBLE);
				}
			} always {
				show()
			}
		}
	}
}
