package meh.watchdoge.ui;

import android.os.Bundle;

import meh.watchdoge.R;
import org.jetbrains.anko.*;
import android.util.Log;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

abstract class ProgressFragment(layout: Int): Fragment() {
	private          val _id:   Int = layout;
	private lateinit var _view: View;

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.loading, container, false);
	}

	override fun onActivityCreated(bundle: Bundle?) {
		super.onActivityCreated(bundle);

		_view = LayoutInflater.from(getActivity()).inflate(_id, null);
		load(_view, bundle);
	}

	fun show() {
		async() {
			uiThread {
				val root     = getView()!!;
				val progress = root.find<LinearLayout>(R.id.progress);
				val content  = root.find<FrameLayout>(R.id.content);

				content.addView(_view);
				progress.setVisibility(View.GONE);
				content.setVisibility(View.VISIBLE);
			}
		}
	}

	abstract fun load(view: View, bundle: Bundle?);
}
