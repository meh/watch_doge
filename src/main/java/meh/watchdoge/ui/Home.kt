package meh.watchdoge.ui;

import android.util.Log;

import meh.watchdoge.R;
import org.jetbrains.anko.*;
import meh.watchdoge.ui.util.*;
import nl.komponents.kovenant.*;

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
			Log.d("UI", "connected");

			conn.request { root() } then { res ->
				val root = res.details.getBoolean("status");
				val text = view.find<TextView>(R.id.root)

				Log.d("UI", "got root: ${root}");

				if (root) {
					text.setText("root");
				}
				else {
					text.setText("not root");
				}

				show();
			}
		}
	}
}
