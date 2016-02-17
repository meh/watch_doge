package meh.watchdoge.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

open class Component: ContextWrapper {
	protected val _view: View;

	constructor(context: Context, view: View) : super(context) {
		_view = view;
		prepare();
	}

	constructor(context: Context, layout: Int) : super(context) {
		_view = LayoutInflater.from(context).inflate(layout, null)
		prepare();
	}

	open fun prepare() { }

	fun view(): View {
		return _view;
	}

	fun addTo(group: ViewGroup, index: Int) {
		group.addView(_view, index);
	}

	fun appendTo(group: ViewGroup) {
		group.addView(_view);
	}

	fun prependTo(group: ViewGroup) {
		group.addView(_view, 0);
	}
}
