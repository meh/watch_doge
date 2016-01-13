import android.os.IBinder;
import android.app.Service;
import android.content.Intent;

public class BackendService extends Service {
  @Override
  public void onCreate() {
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
		return null;
  }

  @Override
  public void onDestroy() {
  }
}
