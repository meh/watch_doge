package meh.watchdoge.ui;

import meh.watchdoge.R;
import org.jetbrains.anko.*;
import meh.watchdoge.ui.util.*;
import nl.komponents.kovenant.*;

import android.os.Bundle;
import meh.watchdoge.backend.Connection;
import android.view.View;

class Dig(): ProgressFragment(R.layout.dig) {
	override fun load(view: View, bundle: Bundle?) {
		show();
	}
}
