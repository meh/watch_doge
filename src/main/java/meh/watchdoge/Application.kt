package meh.watchdoge;

import android.util.Log;
import org.jetbrains.anko.*;

import meh.watchdoge.backend.Backend;
import android.app.Application as Super;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import android.content.pm.PackageManager.NameNotFoundException;

public class Application(): Super() {
	override fun onCreate() {
		super.onCreate();

		var context = getApplicationContext();

		try {
			var installed = getFileStreamPath("backend");
			var updated   = context.getPackageManager()
				.getPackageInfo(context.getPackageName(), 0)
				.lastUpdateTime;

			if (!installed.exists() || installed.lastModified() < updated) {
				context.getResources().openRawResource(R.raw.backend).use { input ->
					context.openFileOutput("backend", Context.MODE_PRIVATE).use { output ->
						input.copyTo(output)
					}
				}

				getFileStreamPath("backend").setExecutable(true);
			}
		}
		catch (e: FileNotFoundException) { /* the executable is still running */ }
		catch (e: NameNotFoundException) { /* won't happen */ }
		catch (e: IOException) {
			// TODO: report to user he dun goofed
			Log.e("A", "backend installation failed");
		}

		startService(intentFor<Backend>());
	}
}
