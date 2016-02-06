package meh.watchdoge;

import android.util.Log;
import org.jetbrains.anko.*;

import meh.watchdoge.backend.Backend;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.MaterialModule;
import com.joanzapata.iconify.fonts.MaterialCommunityModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import android.content.pm.PackageManager.NameNotFoundException;

public class Application(): android.app.Application() {
	override fun onCreate() {
		super.onCreate();

		Iconify.with(MaterialModule()).with(MaterialCommunityModule());

		try {
			val installed = getFileStreamPath("backend");
			val updated   = getPackageManager()
				.getPackageInfo(getPackageName(), 0)
				.lastUpdateTime;

			if (!installed.exists() || installed.lastModified() < updated) {
				getResources().openRawResource(R.raw.backend).use { input ->
					openFileOutput("backend", Context.MODE_PRIVATE).use { output ->
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
	}
}
