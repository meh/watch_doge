package meh.watchdoge;

import meh.watchdoge.util.*;
import meh.watchdoge.backend.Connection;
import meh.watchdoge.Response;

import nl.komponents.kovenant.android.startKovenant;
import nl.komponents.kovenant.android.stopKovenant;

import android.util.Log;

import android.os.IBinder;
import android.os.Bundle;
import android.os.Messenger;
import android.os.Message;
import android.content.Context;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.design.widget.TabLayout;

import meh.watchdoge.ui.Home;
import meh.watchdoge.ui.Sniff;
import meh.watchdoge.ui.Track;
import meh.watchdoge.ui.Dig;
import meh.watchdoge.ui.Stare;

import org.jetbrains.anko.*;
import meh.watchdoge.ui.util.*;
import meh.watchdoge.ui.packet.Packet;

class Main(): AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState);
		startKovenant();

		setContentView(R.layout.main);

		find<Toolbar>(R.id.toolbar).tap {
			setSupportActionBar(it);
		}

		val pager = find<ViewPager>(R.id.viewpager);
		pager.setAdapter(Pager(getSupportFragmentManager()));

		val tabs = find<TabLayout>(R.id.tabs);
		tabs.setupWithViewPager(pager);
	}

	override fun onDestroy() {
		stopKovenant();

		super.onDestroy();
	}

	class Pager(manager: FragmentManager): FragmentPagerAdapter(manager) {
		override fun getCount(): Int {
			return 5;
		}

		override fun getItem(index: Int): Fragment {
			return when (index) {
				0 -> Home()
				1 -> Sniff.Control()
				2 -> Track()
				3 -> Dig()
				4 -> Stare()

				else -> throw IndexOutOfBoundsException("${index} out of range")
			};
		}

		override fun getPageTitle(index: Int): CharSequence {
			return when (index) {
				0 -> "wow"
				1 -> "sniff"
				2 -> "track"
				3 -> "dig"
				4 -> "stare"

				else -> throw IndexOutOfBoundsException("${index} out of range")
			};
		}
	}
}
