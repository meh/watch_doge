package meh.watchdoge;

import android.util.Log;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import com.google.common.io.ByteStreams;

import java.io.FileNotFoundException;
import java.io.IOException;
import android.content.pm.PackageManager.NameNotFoundException;

public class Application extends android.app.Application {
	@Override
	public void onCreate() {
		super.onCreate();

		Context context = getApplicationContext();

		try {
			File installed = getFileStreamPath("backend");
			long updated   = context.getPackageManager()
				.getPackageInfo(context.getPackageName(), 0)
				.lastUpdateTime;

			if (!installed.exists() || installed.lastModified() < updated) {
				InputStream      input  = context.getResources().openRawResource(R.raw.backend);
				FileOutputStream output = context.openFileOutput("backend", Context.MODE_PRIVATE);

				ByteStreams.copy(input, output);

				input.close();
				output.close();

				getFileStreamPath("backend").setExecutable(true);
			}
		}
		catch (FileNotFoundException e) { /* the executable is still running */ }
		catch (NameNotFoundException e) { /* won't happen */ }
		catch (IOException e) {
			// TODO: report to user he dun goofed
			Log.e("A", "backend installation failed");
		}

		context.startService(new Intent(context, meh.watchdoge.backend.Service.class));
	}
}
