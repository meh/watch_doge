package meh.watchdoge.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

open class Component(context: Context, layout: Int) {
	protected val view: View;

	init {
		view = LayoutInflater.from(context).inflate(layout, null);
		prepare();
	}

	open fun prepare() { }

	fun addTo(group: ViewGroup) {
		group.addView(view);
	}
}
