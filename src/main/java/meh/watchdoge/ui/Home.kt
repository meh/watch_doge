package meh.watchdoge.ui;

import android.util.Log;

import meh.watchdoge.R;
import org.jetbrains.anko.*;
import meh.watchdoge.ui.util.*;
import nl.komponents.kovenant.*;
import nl.komponents.kovenant.ui.*;

import android.os.Bundle;
import meh.watchdoge.backend.Connection;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;

class Home(): ProgressFragment(R.layout.home) {
	override fun load(view: View, bundle: Bundle?) {
		backend() then { conn ->
			conn.request { root() } successUi { res ->
				val root   = res.details.getBoolean("status");
				val header = view.find<TextView>(R.id.whoami_header);
				val value  = view.find<TextView>(R.id.whoami_value);

				if (root) {
					value.setText("root");
					header.setBackgroundColor(colorFor(R.color.success));
				}
				else {
					header.setBackgroundColor(colorFor(R.color.failure));
					value.setText("a faggot");
				}

				show();
			}
		}
	}
}
