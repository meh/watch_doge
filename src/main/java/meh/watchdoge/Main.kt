package meh.watchdoge;

import meh.watchdoge.util.*;
import meh.watchdoge.backend.Connection;
import meh.watchdoge.Response;

import nl.komponents.kovenant.android.startKovenant;
import nl.komponents.kovenant.android.stopKovenant;

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
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.design.widget.TabLayout;
import android.view.Menu;
import android.view.MenuItem;

import meh.watchdoge.ui.Settings;
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

		setContentView(R.layout.app);
		setSupportActionBar(find<Toolbar>(R.id.toolbar));
		getSupportActionBar()!!.setHomeButtonEnabled(true);

		getSupportFragmentManager().beginTransaction()
			.replace(R.id.fragment_container, Layout())
			.commit();
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	override fun onDestroy() {
		stopKovenant();
		super.onDestroy();
	}

	override fun onBackPressed() {
		getSupportActionBar()!!.setDisplayHomeAsUpEnabled(false);

		super.onBackPressed();
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.getItemId()) {
			android.R.id.home -> {
				onBackPressed();
			}

			R.id.action_settings -> {
				getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true);

				getSupportFragmentManager().beginTransaction()
					.addToBackStack(null)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
					.replace(R.id.fragment_container, Settings())
					.commit();
			}

			else ->
				return super.onOptionsItemSelected(item)
		}

		return true;
	}

	class Layout(): Fragment() {
		override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
			return inflater.inflate(R.layout.main, container, false);
		}

		override fun onActivityCreated(bundle: Bundle?) {
			super.onActivityCreated(bundle);

			val pager = getView()!!.find<ViewPager>(R.id.viewpager);
			pager.setAdapter(Pager(getChildFragmentManager()));

			val tabs = getView()!!.find<TabLayout>(R.id.tabs);
			tabs.setupWithViewPager(pager);
		}

		class Pager(manager: FragmentManager): FragmentPagerAdapter(manager) {
			override fun getCount(): Int {
				return 5;
			}

			override fun getItem(index: Int): Fragment {
				return when (index) {
					0 -> Home()
					1 -> Sniff()
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
}
