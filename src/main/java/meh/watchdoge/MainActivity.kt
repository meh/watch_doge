package meh.watchdoge;
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
	var _connection: ServiceConnection? = null;
	var _messenger:  Messenger?         = null;

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState);
		
		_connection = object: ServiceConnection {
			override fun onServiceDisconnected(name: ComponentName) {
				_messenger = null;
			}

			override fun onServiceConnected(name: ComponentName, service: IBinder) {
				_messenger = Messenger(service);
			}
		};

		bindService(intentFor<Backend>(), _connection, Context.BIND_AUTO_CREATE);
	}
	
	inner class Handler(): android.os.Handler() {
		override fun handleMessage(msg: Message) {
			super.handleMessage(msg);
		}
	}
}
