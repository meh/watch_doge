package meh.watchdoge.ui.util;

import meh.watchdoge.backend.Connection;
import nl.komponents.kovenant.*;
import meh.watchdoge.util.*;

import android.app.Activity;
import android.support.v4.app.Fragment;

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
