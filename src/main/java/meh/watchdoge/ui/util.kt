package meh.watchdoge.ui.util;

import meh.watchdoge.backend.Connection;
import meh.watchdoge.ui.ProgressFragment;
import meh.watchdoge.util.*;
import nl.komponents.kovenant.*;

import android.content.Context;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import me.zhanghai.android.materialedittext.internal.ViewCompat;
import me.zhanghai.android.materialedittext.MaterialEditTextBackgroundDrawable;
import android.widget.EditText;
import android.graphics.Color;

fun Activity.backend(): Promise<Connection, Exception> {
  val defer = deferred<Connection, Exception>();

  Connection(getApplicationContext()) { conn ->
    defer.resolve(conn);
  }

  return defer.promise;
}

fun Fragment.backend(): Promise<Connection, Exception> {
  return getActivity().backend();
}

fun Context.colorFor(id: Int): Int {
	return ContextCompat.getColor(this, id);
}

fun ProgressFragment.colorFor(id: Int): Int {
	return getContext().colorFor(id);
}

fun EditText.enable() {
	setEnabled(true);
	setCursorVisible(true);
	ViewCompat.setBackground(this, MaterialEditTextBackgroundDrawable(getContext()));
}

fun EditText.disable() {
	setEnabled(false);
	setCursorVisible(false);
	setBackgroundColor(Color.TRANSPARENT);
}
