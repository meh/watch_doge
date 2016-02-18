package meh.watchdoge.ui;

import meh.watchdoge.R;
import meh.watchdoge.util.*;
import org.jetbrains.anko.*;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

class Settings() : PreferenceFragmentCompat() {
	override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.preferences);
	}
}
