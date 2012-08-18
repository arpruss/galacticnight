package mobi.pruss.GalacticNight;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class PowerOnService extends Service {
	private ScreenOnOffReceiver receiver;
	private WindowManager wm;
	private ImageView image;
	private LayoutParams imageLayout;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		GalacticNight.log("PowerOnService:onCreate");
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		receiver = new ScreenOnOffReceiver();
		registerReceiver(receiver, filter);

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    	
        imageLayout = new WindowManager.LayoutParams(
        	WindowManager.LayoutParams.MATCH_PARENT,
        	WindowManager.LayoutParams.MATCH_PARENT,
        	WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
         	WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
         	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
         	PixelFormat.RGBA_8888);
        
        image = null;
		
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

	
	class ScreenOnOffReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
				handleScreenOn(context);
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
				handleScreenOff(context);
		}
		
		private void handleScreenOff(Context context) {
			String data = ScreenControlICS42xx.tuningControlRead();
			GalacticNight.log("Screen off");
			if (data != null && data.startsWith("/sdcard/mdnie/") &&
					PreferenceManager.getDefaultSharedPreferences(context)
					    .getBoolean(Options.PREF_SHOW_LOGO, true)) {		
		        
				GalacticNight.log("drawing image");
		        image = new ImageView(PowerOnService.this);
		        image.setClickable(false);
		        image.setFocusable(false);
		        image.setFocusableInTouchMode(false);
		        image.setLongClickable(false);
		        image.setScaleType(ScaleType.CENTER_INSIDE);
		        image.setBackgroundColor(Color.BLACK);
		        image.setImageResource(R.drawable.back_galaxy);
		        wm.addView(image, imageLayout);
			}
		}
		
		private void handleScreenOn(Context context) {
			String data = ScreenControlICS42xx.tuningControlRead();
			GalacticNight.log("Screen on");
			if (data != null) 
				GalacticNight.log("data:"+data);
			if (data != null && data.startsWith("/sdcard/mdnie/")) {
				GalacticNight.log("rewriting tuningControl");				
//				try {
//					Thread.sleep(6000,0);
//				} catch (Exception e) {
//				}
				ScreenControlICS42xx.tuningControlWrite(context, data.substring(14), false);
			}
			else {
				GalacticNight.log("no tuningControl");
				PowerOnService.this.stopSelf();
			}

			if (image != null) {
				wm.removeView(image);
				image = null;
			}
		}
	}
}
