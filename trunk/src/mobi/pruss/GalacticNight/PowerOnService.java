package mobi.pruss.GalacticNight;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class PowerOnService extends Service {
	
	private ScreenOnReceiver receiver;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		GalacticNight.log("PowerOnService:onCreate");
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		receiver = new ScreenOnReceiver();
		registerReceiver(receiver, filter);

		Notification n = new Notification(
        		0,
        		"GalacticNight", 
        		System.currentTimeMillis());
        Intent i = new Intent(this, GalacticNight.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        n.setLatestEventInfo(this, "GalacticNight", "GalacticNight support service on",
        		PendingIntent.getActivity(this, 0, i, 0));

        startForeground(2, n);
	}
	
	@Override
	public void onStart(Intent intent, int flags) {
		GalacticNight.log("PowerOnService:onStart");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		GalacticNight.log("PowerOnService:onStartCommand");
		onStart(intent, flags);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		GalacticNight.log("destroyed PowerOnService");
		
		if (receiver != null)
			unregisterReceiver(receiver);
	}

	
	class ScreenOnReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String data = ScreenControlICS.tuningControlRead();
			GalacticNight.log("Screen on service, tunning="+data);
			if (data != null && data.startsWith("/sdcard/mdnie/")) {
				GalacticNight.log("rewriting tuningControl");				
				try {
					Thread.sleep(5000,0);
				} catch (Exception e) {
				}
				ScreenControlICS.tuningControlWrite(context, data.substring(14), false);
			}
			else {
				GalacticNight.log("no tuningControl");
				PowerOnService.this.stopSelf();
			}
		}
		
	}

}
