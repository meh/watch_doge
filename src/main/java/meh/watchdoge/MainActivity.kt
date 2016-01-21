package meh.watchdoge;

import meh.watchdoge.util.*;
import meh.watchdoge.backend.Backend;

import org.jetbrains.anko.*;

import android.util.Log;

import android.app.Activity;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Messenger;
import android.os.Message;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;

public class MainActivity(): Activity() {
	lateinit var _receiver:   Messenger;
	lateinit var _connection: ServiceConnection;
	         var _sender:     Messenger? = null;

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState);
		_receiver = Messenger(Handler());

		bindService(intentFor<Backend>(), ServiceConnection(), Context.BIND_AUTO_CREATE);
	}
	
	inner class Handler(): android.os.Handler() {
		override fun handleMessage(msg: Message) {
			super.handleMessage(msg);
		}
	}

	inner class ServiceConnection(): android.content.ServiceConnection {
		override fun onServiceDisconnected(name: ComponentName) {
			_sender = null;
		}

		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			_connection = this;
			_sender     = Messenger(service);

			(_sender!! to _receiver).request { sniffer( ) { create() } }
			(_sender!! to _receiver).request { sniffer(1) { start() } }
			(_sender!! to _receiver).request { sniffer(1) { filter("icmp and ((icmp[icmptype] = icmp-echo) or (icmp[icmptype] = icmp-echoreply))") } }
		}
	}
}
