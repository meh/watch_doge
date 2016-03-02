package meh.watchdoge.backend;

import meh.watchdoge.R;
import meh.watchdoge.util.*;
import meh.watchdoge.backend.util.address;
import meh.watchdoge.Request;
import meh.watchdoge.response.Control;

import android.text.TextUtils;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import org.jetbrains.anko.*;

import android.app.Service;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;
import android.content.Intent;

import android.app.Notification;

import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessageTypeException;
import org.msgpack.core.MessageInsufficientBufferException;

import java.io.IOException;
import android.os.RemoteException;

public class Backend(): Service() {
	private          var _root:     Boolean = false;
	private lateinit var _process:  Process;
	private lateinit var _packer:   MessagePacker;
	private lateinit var _unpacker: MessageUnpacker;

	private lateinit var _messenger: Messenger;

	private var _request:  Int;
	private val _requests: HashMap<Int, Pair<Messenger, Request>>;

	private lateinit var _sniffer:  Module;
	private lateinit var _wireless: Module;
	private lateinit var _pinger:   Module;

	init {
		_request  = 0;
		_requests = HashMap();
	}

	fun unpacker(): MessageUnpacker {
		return _unpacker;
	}

	fun response(req: Request, status: Int, body: (Control.() -> Unit)? = null): Int {
		val id = synchronized(_requests) {
			_request += 1;
			_request
		};

		try {
			req.origin().response(req.id(id), status, body);
		}
		catch (e: RemoteException) { }

		return id;
	}

	fun forward(req: Request, body: (MessagePacker) -> Unit): Int {
		val id = synchronized(_requests) {
			_request += 1;
			_requests.put(_request, Pair(req.origin(), req.id(_request)));
			_request
		}

		synchronized(_packer) {
			_packer.packInt(id);
			_packer.packInt(req.family());
			_packer.packInt(req.command());

			body(_packer);

			_packer.flush();
		}

		return id;
	}

	override fun onCreate() {
		fun setting(name: String, body: (MessagePacker) -> Unit) {
			_packer.packString(name);
			body(_packer);
			_packer.flush();
		}

		_messenger = Messenger(Handler());

		try {
			_process  = Runtime.getRuntime().exec(arrayOf("su", "-c", getFileStreamPath("backend").getPath()));
			_packer   = MessagePack.newDefaultPacker(DataOutputStream(_process.getOutputStream()));
			_unpacker = MessagePack.newDefaultUnpacker(DataInputStream(_process.getInputStream()));
			_root     = _unpacker.unpackBoolean();

			setting("cache") {
				_packer.packString(getExternalCacheDir().getPath());
			}

			_packer.packNil();
		}
		catch (e: Exception) {
			_root = false;
		}

		startForeground(1, Notification.Builder(getApplicationContext())
			.setOngoing(true)
			.setContentTitle(getText(R.string.app_name))
			.setContentText(getString(if (_root) { R.string.yes_root } else { R.string.no_root }))
			.setSmallIcon(R.drawable.notification)
			.build());

		_sniffer  = Sniffer.Mod(this) as Module;
		_wireless = Wireless.Mod(this) as Module;
		_pinger   = Pinger.Mod(this) as Module;

		if (_root) {
			Looper().start();
		}
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		return START_STICKY;
  }

  override fun onBind(intent: Intent?): IBinder {
		return _messenger.getBinder();
  }

  override fun onDestroy() {
		_packer.packInt(0);
		_packer.packInt(Command.CONTROL);
		_packer.packInt(Command.Control.CLOSE);
		_packer.flush();

		try {
			_process.waitFor();
		} catch (e: InterruptedException) { }
  }

	inner class Handler(): android.os.Handler() {
		override fun handleMessage(msg: Message) {
			if (!msg.isRequest()) {
				super.handleMessage(msg);
				return;
			}

			val req = msg.intoRequest(0xBADB01);

			val handled = when (msg.family()) {
				Command.CONTROL -> {
					when (msg.command()) {
						Command.Control.ROOT -> {
							response(req, Command.SUCCESS) {
								arg = if (_root) { 1 } else { 0 };
							}

							true
						}

						else ->
							false
					}
				}

				Command.SNIFFER ->
					_sniffer.request(req)

				Command.WIRELESS ->
					_wireless.request(req)

				Command.PINGER ->
					_pinger.request(req)
				
				else ->
					false
			};

			if (!handled) {
				response(req, Command.UNKNOWN)
				super.handleMessage(msg);
			}
		}
	}

	inner class Looper(): Thread() {
		override fun run() {
			while (true) {
				val family = _unpacker.unpackInt();

				when (family) {
					Command.CONTROL -> {
						val id     = _unpacker.unpackInt();
						val status = _unpacker.unpackInt();

						val (messenger, request) = synchronized(_requests) {
							_requests.remove(id)!!
						}

						when (request.family()) {
							Command.SNIFFER ->
								_sniffer.response(messenger, request, status)

							Command.WIRELESS ->
								_wireless.response(messenger, request, status)

							Command.PINGER ->
								_pinger.response(messenger, request, status)

							else ->
								messenger.response(request, status)
						}
					}

					Command.SNIFFER ->
						_sniffer.receive()

					Command.WIRELESS ->
						_wireless.receive()

					Command.PINGER ->
						_pinger.receive()

					else ->
						throw IllegalArgumentException("unknown family: ${family}")
				}
			}
		}
	}
}
